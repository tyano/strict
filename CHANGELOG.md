# Changelog #

## Version 2.0.0 ##

Date: 2023-03-21

First release of the `strict` library folked from `struct` library.

- Added a new validators 'nested' and `coll-of`.
- Introduced automatic conversion from plain map to 'nested' validator.
- validator can return a vector of [validation-result validation-context] typed as [boolean? map?].
- validator's :message now can be a fn that accepts validation-context, opts and args.

# History of the original struct library #

## Version 1.3.0 ##

Date: 2018-06-02

- Fix message formatting.


## Version 1.2.0 ##

Date: 2017-01-11

- Allow `number-str` and `integer-str` receive already coerced values.
- Minor code cleaning.
- Update dependencies.

## Version 1.1.0 ##

Date: 2017-08-16

- Add count validators.
- Update cuerdas to 2.0.3


## Version 1.0.0 ##

Date: 2016-06-24

- Add support for neested data structures.
- Add fast skip already validated and failed paths (performance improvement).
- BREAKING CHANGE: the errors are now simple strings. No additional list
  wrapping is done anymore. Because the design of the library is just fail
  fast and only one error is allowed.


## Version 0.1.0 ##

Date: 2016-04-19

Initial version.
