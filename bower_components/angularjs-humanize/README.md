# angularjs-humanize

angularjs-humanize is a thin wrapper around the
[humanize](https://github.com/taijinlee/humanize) library, providing a
variety of Angular filters which apply transformations to make data more
human-readable.

## Installation

Install the library via 

    bower install -S angularjs-humanize

Add it to your `index.html`:

    <script src="bower_components/humanize/humanize.js"></script>
    <script src="bower_components/angularjs-humanize/src/angular-humanize.js"></script>

Add it as a dependency to your angularjs project:

    var app = angular.module("myApp", ['angular-humanize']);



## Supported Filters

angularjs-humanize currently supports the following filters, mapping
to the humanize functions of the same name:

+ _humanizeFilesize_, which transforms a number or number-like string into
  a human-readable filesize such as "225.35 KB"
+ _humanizeOrdinal_, which transforms an integer into an ordinal such as
  "32nd" or "101st"
+ _humanizeNaturalDay_, which transforms an integer representing an epoch
  into a date representation or one of "yesterday", "today", or
  "tomorrow"
+ _humanizeRelativeTime_, which transforms an integer representing an epoch
  into a string such as "about an hour ago" or "in a month"

## License
Copyright © 2014 Say Media Ltd. All Rights Reserved.  See the LICENSE
file for distribution terms.

