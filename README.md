# struct #

A structural validation library for Clojure(Script).


[![Clojars Project](https://clojars.org/org.clojars.t_yano/struct/latest-version.svg)](https://clojars.org/org.clojars.t_yano/struct)

This is originally created by funcool and personally maintained for adding some small new functionalities.

Original Documentation: http://funcool.github.io/struct/latest/

# Newly added functionalities

## 'nested' validator

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


## validator's :validate function can return a vector of [result context]

From 1.5.0, validator can return a vector instead of a boolean result.
the vector must contain [result context]. result must be a boolean and context must be a map.
the context will be passed to :message fn.


## validator's :message can be a function

From 1.5.0, validator's :message can be a function returning an any object instead of a simple string.
The message function must have 3 arities of [context opts args].
context is a validation-context which the validator returns or nil.
opts is a option-map which passed to (st/validate) fn.
args is a vector of validator arguments.

'nested' validator uses this functionality.
'nested' validator's :message fn return an error map (not a string) which is passed as a part of
validation-context. the error map is used as a validation error message of a nested validator.


# License

struct is under public domain:

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