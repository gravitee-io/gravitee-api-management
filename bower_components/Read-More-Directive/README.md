Read-More-Directive
===================

An AngularJs directive that creates a read-more ui component that allows to partially display the content of model and on click expands or collapses it


### Getting started:
Simple add the readmore.js file to your project, 
```html
<script src="path/to/file/readmore.js"></script>
```

add the module to your app,
```html
angular.module("myApp", ["readMore"]);
```

add the directive on the tag you want to convert as a read-more field
```html
<p read-more ng-model="content" words="true" length="50"></p>
```
or
```html
<li read-more ng-model="content" words="false" length="50"></li>
```

### Description of attributes
| Attribute        | Description           | Required | Example  |
| :------------- |:-------------| :-----:| :-----|
| ngModel | An angular model containing the text you would like to display | Yes | $scope.content |
| length | The desired length of the text you would like to display when it is collapsed | Yes | 50 |
| words | Whether you would like to display n words or characters | No - defaults to true | "true" |

### What it does
 * This directive truncates the given text according to the specified length (n)
 * If words = true then the directive will display n number of words
 * If words = false then the directive will display n number of characters
 * If words flag is omitted then default behaviour is that words == true
 
