# ng-showdown

Angular integration for [Showdown](https://github.com/showdownjs/showdown)

## How to use it

1. Install it

  a. via bower:
  ```
  bower install --save ng-showdown
  ```
  b. via npm
  ```
  npm install --save ng-showdown
  ```
2. Include `'ng-showdown'` in your module dependencies
3. Use it
  ```
  <p markdown-to-html="vm.mymarkdown"></p>
  ```
  
## API

#### `$showdown`

`$showdown.makeHtml(markdown)` - Converts a markdown text into HTML

Input: *string* - markdown to be parsed

Output: *string* - html output from showdown

`$showdown.stripHtml` - Strips a text of it's HTML tags. See http://stackoverflow.com/questions/17289448/angularjs-to-output-plain-text-instead-of-html

Input: *string* - html to be stripped

Output: *string* - string without `<html>` tags

#### `markdownToHtml` directive

Example usage:
```
 <p markdown-to-html="vm.mymarkdown"></p>
```

Input: *string* - markdown to be parsed

Output: *string* - html output from showdown

#### `stripHtml` filter

Example usage:
```
 <p ng-bind="vm.someText | stripHtml"></p>
```

Input: *string* - Input to be stripped of html

Output: *string* - stripped html

## Configuration

You can configure the options and extensions passed to showdown by using the `$showdownProvider`. To see these options, visit the [Showdown page](https://github.com/showdownjs/showdown).

`$showdownProvider.setOption(key, value)` - sets the passed in option as a configuration option in showdown

`$showdownProvider.getOption(key)` - get the option as determined by the passed in key.

`$showdownProvider.loadExtension(extensionName)` - loads the extension into showdown as determined by the passed in extension name

