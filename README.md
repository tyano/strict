# struct #

A structural validation library for Clojure(Script).


[![Clojars Project](https://clojars.org/org.clojars.t_yano/struct/latest-version.svg)](https://clojars.org/org.clojars.t_yano/struct)

This is originally created by funcool and personally maintained for adding some small new functionalities.

Original Documentation: http://funcool.github.io/struct/latest/


# 'nested' validator

You can nest another validator by using [st/nested another-validator].

```
(def user-validator {:name  [[st/nested {:first st/string :last st/string}]]
                     :age st/integer
                     :division [[st/nested {:department [[st/nested {:name st/string
                                                                     :dev st/boolean-str}]]}]]})

;; validate nested maps

(st/validate {:name {:first "First" :last "Name"}
              :age 12
              :division {:department {:name "product" :dev "true"}}} user-validator)

;; => [nil {:name {:first "First", :last "Name"}, :age 12, :division {:department {:name "product", :dev true}}}]


;; validate nested map with some errors

(st/validate {:name {:first "First" :last "Name"}
              :age 12
              :division {:department {:name 111 :dev "true"}}} user-validator)

;; => [{:division {:department {:name "must be a string"}}}
       {:name {:first "First", :last "Name"}, :age 12, :division {:department {:dev true}}}]
```

# validator's :validate function can return a vector of [result context]

From 1.5.0, validator can return a vector instead of a boolean result.
the vector must contain [result context]. result must be a boolean and context must be a map.
the context will be passed to :message fn.


# validator's :message can be a function

From 1.5.0, validator's :message can be a function returning an any object instead of a simple string.
The message function must have 3 arities of [context opts args].
context is a validation-context which the validator returns or nil.
opts is a option-map which passed to (st/validate) fn.
args is a vector of validator arguments.

'nested' validator uses this functionality.
'nested' validator's :message fn return an error map (not a string) which is passed as a part of
validation-context. the error map is used as a validation error message of a nested validator.