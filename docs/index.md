# Strict - validation library for Clojure(Script)

A structural validation library for Clojure and ClojureScript.

**STRICT** is a fork of [funcool/struct](https://github.com/funcool/struct).
Some new features are added and have some incompatibilities with original *struct* library.

## Introduction

A structural validation library for Clojure and ClojureScript.

Highlights:

* **No macros**: validators are defined using plain data.
* **Dependent**: validators: the ability to access to already validated data.
* **Coercion**: the ability to coerce incoming values to other types.
* **No exceptions**: no exceptions used in the validation process.

Based on similar ideas of [bouncer](https://github.com/leonardoborges/bouncer).


## Install

Just include that in your dependency vector on **project.clj**:

```
org.clojars.t_yano/strict 2.0.0
```

## User Guide

### Quick Start

Let’s require the main **strict** namespace:

```clojure
(require '[strict.core :as st])
```

Define a small schema for the example purpose:

```clojure
(def +scheme+
  {:name [st/required st/string]
   :year [st/required st/number]})
```

You can observe that it consists in a simple map when you declare keys and corresponding validators for that key. A vector as value allows us to put more than one validator for the same key. If you have only one validator for the key, you can omit the vector and put it as single value.

The same schema can be defined using vectors, if the order of validation matters:

```clojure
(def +scheme+
  [[:name st/required st/string]
   [:year st/required st/number]])
```

By default, all validators are optional so if the value is missing, no error will reported. If you want make the value mandatory, you should use a specific required validator.

And finally, start validating your data:

```clojure
(-> {:name "Blood of Elves" :year 1994}
    (st/validate +scheme+))
;; => [nil {:name "Blood of Elves" :year 1994}]

(-> {:name "Blood of Elves" :year "1994"}
    (st/validate +scheme+))
;; => [{:year "must be a number"} {:name "Blood of Elves", :year "1994"}]

(-> {:year "1994"}
    (st/validate +scheme+))
;; => [{:name "this field is mandatory", :year "must be a number"} {}]
```
If only want to know if some data is valid or not, you can use the valid? predicate for that purpose:

```clojure
(st/valid? {:year "1994"} +scheme+)
;; => false
```

The additional entries in the map are not stripped by default, but this behavior can be changed passing an additional flag as the third argument:

```clojure
(-> {:name "Blood of Elves" :year 1994 :foo "bar"}
    (st/validate +scheme+))
;; => [nil {:name "Blood of Elves" :year 1994 :foo "bar"}]

(-> {:name "Blood of Elves" :year 1994 :foo "bar"}
    (st/validate +scheme+ {:strip true}))
;; => [nil {:name "Blood of Elves" :year 1994}]
```

With similar syntax you can validate neested data structures, specifying in the key part the proper path to the neested data structure:

```clojure
(def +scheme+
  {[:a :b] st/integer
   [:c :d] st/string})

(-> {:a {:b "foo"} {:c {:d "bar"}}}
    (st/validate +scheme+))
;; => [{:a {:b "must be a number"}} {:c {:d "bar"}}]
```

### Parametrized validators

In addition to simple validators, one may use additional contraints (e.g. `in-range`). This is how they can be passed to the validator:

```clojure
(def schema {:num [[st/in-range 10 20]]})

(st/validate {:num 21} schema)
;; => [{:num "not in range"} {}]

(st/validate {:num 19} schema)
;; => [nil {:num 19}]
```

Note the double vector; the outer denotes a list of validatiors and the inner denotes a validator with patameters.

### Custom messages

The builtin validators comes with default messages in human readable format, but sometimes you may want to change them (e.g. for i18n purposes). This is how you can do it:

```clojure
(def schema
  {:num [[st/in-range 10 20 :message "errors.not-in-range"]]})

(st/validate {:num 21} schema)
;; => [{:num "errors.not-in-range"} {}]
```

A message can contains format wildcards `%s`, these wildcards will be replaced by `args` of validator, e.g.:

```clojure
(def schema
  {:age [[st/in-range 18 26 :message "The age must be between %s and %s"]]})

(st/validate {:age 30} schema)
;; => [{:age "The age must be between 18 and 26"} {}]
```

### Data coercions

In addition to simple validations, this library includes the ability to coerce values, and a collection of validators that matches over strings. Let’s see some code:

Example attaching custom coercions

```clojure
(def schema
  {:year [[st/integer :coerce str]]})

(st/validate {:year 1994} schema))
;; => [nil {:year "1994"}]
```

Looking at the data returned from the validation process, one can see that the value is properly coerced with the specified coercion function.

This library comes with a collection of validators that already have attached coercion functions. These serve to validate parameters that arrive as strings but need to be converted to the appropriate type:

```clojure
(def schema {:year [st/required st/integer-str]
             :id [st/required st/uuid-str]})

(st/validate {:year "1994"
              :id "543e7472-6624-4cb5-b65e-f3c341843d0f"}
             schema)
;; => [nil {:year 1994, :id #uuid "543e7472-6624-4cb5-b65e-f3c341843d0f"}]
```

To facilitate this operation, the validate! function receives the data and schema, then returns the resulting data. If data not matches the schema an exception will be raised using ex-info clojure facility:

```clojure
(st/validate! {:year "1994" :id "543e7472-6624-4cb5-b65e-f3c341843d0f"} schema)
;; => {:year 1994, :id #uuid "543e7472-6624-4cb5-b65e-f3c341843d0f"}
```

### Builtin Validators

This is the table with available builtin validators:

<div style="text-align: center; margin-top: 20px;">
Table 1. Builtin Validators
</div>

|Identifier|Coercion|Description|
|----------|:------:|-----------|
|`strict.core/keyword`|no|Validator for clojure’s keyword|
|`strict.core/uuid`|no|Validator for UUID’s|
|`strict.core/uuid-str`|yes|Validator for uuid strings with coercion to UUID|
|`strict.core/email`|no|Validator for email string.|
|`strict.core/required`|no|Marks field as required.|
|`strict.core/number`|no|Validator for Number.|
|`strict.core/number-str`|yes|Validator for number string.|
|`strict.core/integer`|no|Validator for integer.|
|`strict.core/integer-str`|yes|Validator for integer string.|
|`strict.core/boolean`|no|Validator for boolean.|
|`strict.core/boolean-str`|yes|Validator for boolean string.|
|`strict.core/string`|no|Validator for string.|
|`strict.core/string-str`|yes|Validator for string like.|
|`strict.core/in-range`|no|Validator for a number range.|
|`strict.core/member`|no|Validator for check if a value is member of coll.|
|`strict.core/positive`|no|Validator for positive number.|
|`strict.core/negative`|no|Validator for negative number.|
|`strict.core/function`|no|Validator for IFn interface.|
|`strict.core/vector`|no|Validator for clojure vector.|
|`strict.core/map`|no|Validator for clojure map.|
|`strict.core/set`|no|Validator for clojure set.|
|`strict.core/coll`|no|Validator for clojure coll.|
|`strict.core/every`|no|Validator to check if pred match for every item in coll.|
|`strict.core/identical-to`|no|Validator to check that value is identical to other field.|
|`strict.core/min-count`|no|Validator to check that value is has at least a minimum number of characters.|
|`strict.core/max-count`|no|Validator to check that value is not larger than a maximum number of characters.|

Additional notes:

* number-str coerces to java.lang.Double or float (cljs)
* boolean-str coerces to true ("t", "true", "1") or false ("f", "false", "0").
* string-str coerces anything to string using str function.

### Define your own validator

As mentioned previously, the validators in *strict* library are defined using plain hash-maps. For example, this is how the builtin integer validator is defined:

```clojure
(def integer
  {:message "must be a integer"
   :optional true
   :validate integer?}))
```

If the validator needs access to previously validated data, the :state key should be present with the value true. Let see the identical-to validator as example:

```clojure
(def identical-to
  {:message "does not match"
   :optional true
   :state true
   :validate (fn [state v ref]
               (let [prev (get state ref)]
                 (= prev v)))})
```

Validators that access the state receive an additional argument with the state for validator function.

## Developers Guide

### Contributing

Just open an issue or pull request in Github.

### Get the Code

*Strict* is open source and can be found on [github](https://github.com/tyano/strict).

You can clone the public repository with this command:

```
git clone https://github.com/tyano/strict
```

### Run tests

To run the tests execute the following:

For the JVM platform:

```sh
lein test
```

And for JS platform:

```sh
./scripts/build
node out/tests.js
```

You will need to have nodejs installed on your system.

### License

*strict* is under public domain:

This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
