# n3-charts.pie-chart [![Build Status](https://drone.io/github.com/n3-charts/pie-chart/status.png)](https://drone.io/github.com/n3-charts/pie-chart/latest)

Yummy pies and donuts for [AngularJS](http://angularjs.org/) applications. Built on top of the wonderful [D3.js](http://d3js.org/) library.

Here is a [demo page](http://n3-charts.github.io/pie-chart/).

### How to install
 + Copy `pie-chart.min.js` wherever you want
 + Reference it in your index.html file
 + Reference the module in your app file :
     angular.module('myApp', [
      'n3-pie-chart'
    ])

### How to use
A pie chart is called using this syntax :

```html
<pie-chart data="data" options="options"></pie-chart>
```

The pie chart directives needs two attributes : `data` and `options`. If one is missing, nothing happens.

#### Data
Your data be an array. Depending whether you wan a pie/dout or a gauge, the array can contain at least two rows, or only one.

##### Standard
```js
$scope.data = [
  {label: "one", value: 12.2, color: "red"}, 
  {label: "two", value: 45, color: "#00ff00"},
  {label: "three", value: 10, color: "rgb(0, 0, 255)"}
];
```

##### Gauge
```js
$scope.gauge_data = [
  {label: "CPU", value: 75, suffix: "%", color: "steelblue"}
];
```

#### Options
Options must be an object. Depending whether you want a pie/donut or a gauge, additional settings can be required.

##### Standard

```js
$scope.options = {thickness: 10};
```

+ `thickness` : optional (default is 10). Defines the chart's thickness and has an impact on the legend display (thicker chart means less space for legend).

##### Gauge

```js
$scope.gauge_options = {thickness: 5, mode: "gauge", total: 100};
```

+ `thickness` : optional (default is 10). Defines the chart's thickness and has an impact on the legend display (thicker chart means less space for legend).
+ `mode` : optional (default is ""). Can be "gauge" or anything else (but anything else will default to standard pie/donut mode). "gauge" only works when there is exactly one data row in the chart's `data` attribute.
+ `total` : optional (default is 100). Ignored if `mode` is not "gauge". Make the chart able to display the data relatively to a maximum (for instance, if you want to display a percentage, the maximum should be 100).


### Building
Fetch the repo :
```sh
$ git clone https://github.com/angular-d3/pie-chart.git
```

Install stuff :
```sh
$ npm install
```

Install moar stuff :
```sh
$ bower install
```

Install teh uterz stuff :
```sh
$ grunt
```

Hack.

### Testing
AngularJS is designed to be testable, and so is this project.
It has a good coverage rate (between 85% and 95%), and we want to keep it this way.

  [1]: https://github.com/mbostock/d3/wiki/SVG-Shapes#wiki-line_interpolate
