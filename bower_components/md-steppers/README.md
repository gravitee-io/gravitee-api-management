# md-steppers
angular directive for material stepper
https://www.google.com/design/spec/components/steppers.html#steppers-types-of-steppers

  - basic directive based on material steppers

demo https://ipiz.herokuapp.com/demo/index.html
fiddle https://jsfiddle.net/ipiz/vcdbuyru/2/

### usage
####  using bower
```shell
bower install md-steppers --save
```
#### or using npm
```shell
npm install md-steppers --save
```
```javascript
//module
var app = angular.module('app', ['ngMaterial', 'md-steppers']);
```
```html
<!--markup-->
<md-steppers>
    <md-step label="Step 1" md-complete="step1.completed"  ng-disabled="step1.disabled">
        <md-content></md-content>
    </md-step>
    <md-step label="Step 2" md-complete="step2.completed"  ng-disabled="step2.disabled">
        <md-content></md-content>
    </md-step>
    <md-step label="Finish" md-complete="step3.completed"  ng-disabled="step3.disabled">
        <md-content></md-content>
    </md-step>
</md-steppers>
```

```
clone repository and run gulp for demo http://localhost.com:3333/demo/index.html
```

![md-steppers Screenshot](https://raw.githubusercontent.com/ipiz/md-steppers/master/md-steppers.png "md-steppers Screenshot")

### Todos

 - Unit Tests
 - Refactor
 - Code Cleanup
 - $mdSteppers service

License
----

MIT


**Version 0.1.0**
