(ns strict.core
  (:refer-clojure :exclude [keyword uuid vector boolean long map set])
  (:require [cuerdas.core :as str]
            [clojure.core :as core]))

;; --- Impl details

(def ^:private map' #?(:cljs cljs.core/map
                       :clj clojure.core/map))

(defn- apply-validation
  [step data value]
  (if-let [validate (:validate step nil)]
    (let [args (:args step [])]
      (if (:state step)
        (apply validate data value args)
        (apply validate value args)))
    true))

(defn- dissoc-in
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn format-message
  [msg opts args]
  (let [tr (:translate opts identity)]
    (apply str/format (tr msg) args)))

(defn- prepare-message
  [opts step context]
  (cond
    (::nomsg opts)
    ::nomsg

    (fn? (:message step))
    (let [msg-fn (:message step)]
      (msg-fn context opts (vec (:args step))))

    :else
    (let [msg (:message step "errors.invalid")]
      (format-message msg opts (vec (:args step))))))

(def ^:const ^:private opts-params
  #{:coerce :message :optional})

(def ^:private notopts?
  (complement opts-params))

(declare nested validator?)

(defn- simple-map?
  [m]
  (and (map? m) (not (validator? m))))

(defn- convert-to-nested-validator
  [validator]
  (if (simple-map? validator)
    [nested validator]
    validator))

(defn- build-step
  [key item]
  (letfn [(coerce-key [key] (if (vector? key) key [key]))
          (check-validator [validator] (when-not (validator? validator)
                                         (throw (ex-info (str "Invalid validator. validators must be a Validator record. validator: " (pr-str validator)) {:validator validator}))))]
    (let [item (convert-to-nested-validator item)]
      (if (vector? item)
        (let [validator (first item)
              result (split-with notopts? (rest item))
              args (first result)
              opts (apply hash-map (second result))]
          (check-validator validator)
          (merge (assoc validator :args args :path (coerce-key key))
                 (select-keys opts [:coerce :message :optional])))

        (do
          (check-validator item)
          (assoc item :args [] :path (coerce-key key)))))))

(defn- normalize-step-map-entry
  [acc key value]
  (if (vector? value)
    (reduce #(conj! %1 (build-step key %2)) acc value)
    (conj! acc (build-step key value))))

(defn- normalize-step-entry
  [acc [key & values]]
  (reduce #(conj! %1 (build-step key %2)) acc values))

(defn- build-steps
  [schema]
  (cond
    (vector? schema)
    (persistent!
     (reduce normalize-step-entry (transient []) schema))

    (map? schema)
    (persistent!
     (reduce-kv normalize-step-map-entry (transient []) schema))

    :else
    (throw (ex-info "Invalid schema." {}))))

(defn- strip-values
  [data steps]
  (reduce (fn [acc path]
            (let [value (get-in data path ::notexists)]
              (if (not= value ::notexists)
                (assoc-in acc path value)
                acc)))
          {}
          (into #{} (map' :path steps))))

(defn- validate-internal
  [data steps opts]
  (loop [skip #{}
         errors nil
         data data
         steps steps]
    (if-let [step (first steps)]
      (let [path (:path step)
            value (get-in data path)]
        (cond
          (contains? skip path)
          (recur skip errors data (rest steps))

          (and (nil? value) (:optional step))
          (recur skip errors data (rest steps))

          :else
          (let [validation-result (apply-validation step data value)
                [valid? {context-value :value :as context}] (if (vector? validation-result)
                                                              validation-result
                                                              [validation-result {}])
                value (if valid? (or context-value value) context-value)]

            (if valid?
              (let [value ((:coerce step identity) value)]
                (recur skip errors (assoc-in data path value) (rest steps)))

              (let [message (prepare-message opts step context)]
                (recur (conj skip path)
                       (assoc-in errors path message)
                       (if (some? value)
                         (assoc-in data path value)
                         (dissoc-in data path))
                       (rest steps)))))))
      [errors data])))

;; --- Public Api

(defn validate
  "Validate data with specified schema.

  This function by default strips all data that are not defined in
  schema, but this behavior can be changed by passing `{:strip false}`
  as third argument."
  ([data schema]
   (validate data schema nil))
  ([data schema {:keys [strip]
                 :or {strip false}
                 :as opts}]
   (let [steps  (build-steps schema)
         data   (if strip (strip-values data steps) data)]
     (validate-internal data steps opts))))

(defn validate-single
  "A helper that used just for validate one value."
  ([data schema] (validate-single data schema nil))
  ([data schema opts]
   (let [data {:field data}
         steps (build-steps {:field schema})]
     (mapv :field (validate-internal data steps opts)))))

(defn validate!
  "Analogous function to the `validate` that instead of return
  the errors, just raise a ex-info exception with errors in case
  them are or just return the validated data.

  This function accepts the same parameters as `validate` with
  an additional `:message` that serves for customize the exception
  message."
  ([data schema]
   (validate! data schema nil))
  ([data schema {:keys [message] :or {message "Schema validation error"} :as opts}]
   (let [[errors data] (validate data schema opts)]
     (if (seq errors)
       (throw (ex-info message errors))
       data))))

(defn valid?
  "Return true if the data matches the schema, otherwise
  return false."
  [data schema]
  (nil? (first (validate data schema {::nomsg true}))))

(defn valid-single?
  "Analogous function to `valid?` that just validates single value."
  [data schema]
  (nil? (first (validate-single data schema {::nomsg true}))))

;; --- Validators
(defrecord Validator [name message optional validate])

(defn validator [spec-map] (map->Validator spec-map))

(defn validator? [obj] (instance? Validator obj))

(def keyword
  (validator {:name "keyword"
              :message "must be a keyword"
              :optional true
              :validate keyword?
              :coerce identity}))

(def uuid
  (validator {:name "uuid"
              :message "must be an uuid"
              :optional true
              :validate #?(:clj #(instance? java.util.UUID %)
                           :cljs #(instance? cljs.core.UUID %))}))

(def ^:const ^:private +uuid-re+
  #"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

(def uuid-str
  (validator {:name "uuid-str"
              :message "must be an uuid"
              :optional true
              :validate #(and (string? %)
                              (re-seq +uuid-re+ %))
              :coerce #?(:clj #(java.util.UUID/fromString %)
                         :cljs #(uuid %))}))

(def email
  (let [rx #"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"]
    (validator {:name "email"
                :message "must be a valid email"
                :optional true
                :validate #(and (string? %)
                                (re-seq rx %))})))

(def required
  (validator {:name "required"
              :message "this field is mandatory"
              :optional false
              :validate #(if (string? %)
                           (not (empty? %))
                           (not (nil? %)))}))

(def number
  (validator {:name "number"
              :message "must be a number"
              :optional true
              :validate number?}))

(def number-str
  (validator {:name "number-str"
              :message "must be a number"
              :optional true
              :validate #(or (number? %) (and (string? %) (str/numeric? %)))
              :coerce #(if (number? %) % (str/parse-number %))}))

(def integer
  (validator {:name "integer"
              :message "must be a integer"
              :optional true
              :validate #?(:cljs #(js/Number.isInteger %)
                           :clj #(integer? %))}))

(def integer-str
  (validator {:name "integer-str"
              :message "must be a long"
              :optional true
              :validate #(or (number? %) (and (string? %) (str/numeric? %)))
              :coerce #(if (number? %) (int %) (str/parse-int %))}))

(def boolean
  (validator {:name "boolean"
              :message "must be a boolean"
              :optional true
              :validate #(or (= false %) (= true %))}))

(def boolean-str
  (validator {:name "boolean-str"
              :message "must be a boolean"
              :optional true
              :validate #(and (string? %)
                              (re-seq #"^(?:t|true|false|f|0|1)$" %))
              :coerce #(contains? #{"t" "true" "1"} %)}))

(def string
  (validator {:name "string"
              :message "must be a string"
              :optional true
              :validate string?}))

(def string-like
  (validator {:name "string-like"
              :message "must be a string"
              :optional true
              :coerce str}))

(def in-range
  (validator {:name "in-range"
              :message "not in range %s and %s"
              :optional true
              :validate #(and (number? %1)
                              (number? %2)
                              (number? %3)
                              (<= %2 %1 %3))}))

(def positive
  (validator {:name "positive"
              :message "must be positive"
              :optional true
              :validate pos?}))

(def negative
  (validator {:name "negative"
              :message "must be negative"
              :optional true
              :validate neg?}))

(def map
  (validator {:name "map"
              :message "must be a map"
              :optional true
              :validate map?}))

(def set
  (validator {:name "set"
              :message "must be a set"
              :optional true
              :validate set?}))

(def coll
  (validator {:name "coll"
              :message "must be a collection"
              :optional true
              :validate coll?}))

(def vector
  (validator {:name "vector"
              :message "must be a vector instance"
              :optional true
              :validate vector?}))

(def every
  (validator {:name "every"
              :message "must match the predicate"
              :optional true
              :validate #(every? %2 %1)}))

(def member
  (validator {:name "member"
              :message "not in coll"
              :optional true
              :validate #(some #{%1} %2)}))

(def function
  (validator {:name "function"
              :message "must be a function"
              :optional true
              :validate ifn?}))

(def identical-to
  (validator {:name "identical-to"
              :message "does not match"
              :optional true
              :state true
              :validate (fn [state v ref]
                          (let [prev (get state ref)]
                            (= prev v)))}))

(def min-count
  (letfn [(validate [v minimum]
            {:pre [(number? minimum)]}
            (>= (count v) minimum))]
    (validator {:name "min-count"
                :message "less than the minimum %s"
                :optional true
                :validate validate})))

(def max-count
  (letfn [(validate [v maximum]
            {:pre [(number? maximum)]}
            (<= (count v) maximum))]
    (validator {:name "max-count"
                :message "longer than the maximum %s"
                :optional true
                :validate validate})))

(def nested
  (validator {:name "nested"
              :message (fn [{:keys [error]} opts args] error)
              :optional true
              :validate (fn [v spec & opts]
                          (if-not (map? v)
                            [false {:error "must be a map"}]
                            (let [[errors success] (validate v spec opts)
                                  context {:error (not-empty errors)
                                           :value (not-empty success)}]
                              (if (seq errors)
                                [false context]
                                [true context]))))}))

(def coll-of
  (letfn [(data-or-nil [coll] (when-not (empty? (sequence (filter some?) coll)) coll))]
    (validator {:name "coll-of"
                :message (fn [{:keys [error]} opts args]
                           error)
                :optional true
                :validate (fn [v validators & opts]
                            (let [option-map (if (seq opts)
                                               (->> (partition 2 opts)
                                                    (mapv vec)
                                                    (into {}))
                                               {})]
                              (cond
                                (not (sequential? v))
                                [false {:error "must be a list"}]

                                (and (not (seq v))
                                     (not (:allow-empty option-map true)))
                                [false {:error "must not be empty"}]

                                :else
                                (let [results (core/map #(validate {:value %} {:value validators} opts) v)
                                      errors  (sequence (comp (core/map first) (core/map :value)) results)
                                      values  (sequence (comp (core/map second) (core/map :value)) results)
                                      context {:error (data-or-nil errors) :value (data-or-nil values)}]
                                  (if (data-or-nil errors)
                                    [false context]
                                    [true context])))))})))