
# AngularJS multi-level accordion

  - Allows for a nested structure
  - Works with (or without) `ng-repeat`
  - Allows multiple sections to be open at once


## Demo

  - [GitHub](http://lukaszwatroba.github.io/v-accordion)


## Usage

  - If you use [bower](http://bower.io/) or [npm](https://www.npmjs.com/), just `bower/npm install v-accordion`. If not, download files [from the github repo](./dist).

  - Include `angular.js`, `angular-animate.js`, `v-accordion.js`, and `v-accordion.css`:
  ```html
  <link href="v-accordion.css" rel="stylesheet" />

  <script src="angular.js"></script>
  <script src="angular-animate.js"></script>

  <script src="v-accordion.js"></script>
  ```

  - Add `vAccordion` as a dependency to your application module:
  ```js
  angular.module('myApp', ['vAccordion']);
  ```

  - Put the following markup in your template:
  ```html
  <!-- add `multiple` attribute to allow multiple sections to open at once -->
  <v-accordion class="vAccordion--default" multiple>

    <!-- add expanded attribute to open the section -->
    <v-pane expanded>
      <v-pane-header>
        Pane header #1
      </v-pane-header>

      <v-pane-content>
        Pane content #1
      </v-pane-content>
    </v-pane>

    <v-pane disabled>
      <v-pane-header>
        Pane header #2
      </v-pane-header>

      <v-pane-content>
        Pane content #2
      </v-pane-content>
    </v-pane>

  </v-accordion>
  ```

  - You can also use `v-accordion` with `ng-repeat`:
  ```html
  <v-accordion class="vAccordion--default">

    <v-pane ng-repeat="pane in panes" expanded="$first">
      <v-pane-header>
        {{ ::pane.header }}
      </v-pane-header>

      <v-pane-content>
        {{ ::pane.content }}

        <!-- accordions can be nested :) -->
        <v-accordion ng-if="pane.subpanes">
          <v-pane ng-repeat="subpane in pane.subpanes">
            <v-pane-header>
              {{ ::subpane.header }}
            </v-pane-header>
            <v-pane-content>
              {{ ::subpane.content }}
            </v-pane-content>
          </v-pane>
        </v-accordion>
      </v-pane-content>
    </v-pane>

  </v-accordion>
  ```


## API

#### Control

Add `control` attribute and use following methods to control vAccordion from it's parent scope:

  - `toggle(indexOrId)`
  - `expand(indexOrId)`
  - `collapse(indexOrId)`
  - `expandAll()`
  - `collapseAll()`
  - `hasExpandedPane()`

```html
<v-accordion id="my-accordion" multiple control="accordion">

  <v-pane id="{{ pane.id }}" ng-repeat="pane in panes">
    <v-pane-header>
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>

<button ng-click="accordion.toggle(0)">Toggle first pane</button>
<button ng-click="accordion.expandAll()">Expand all</button>
<button ng-click="accordion.collapseAll()">Collapse all</button>
```

```js
$scope.$on('my-accordion:onReady', function () {
  var firstPane = $scope.panes[0];
  $scope.accordion.toggle(firstPane.id);
});
```

#### Transcluded scope

`$accordion` and `$pane` scope properties allows you to control the directive from it's transcluded scope.

##### $accordion

  - `toggle(indexOrId)`
  - `expand(indexOrId)`
  - `collapse(indexOrId)`
  - `expandAll()`
  - `collapseAll()`
  - `hasExpandedPane()`

##### $pane

  - `toggle()`
  - `expand()`
  - `collapse()`
  - `isExpanded()`

```html
<v-accordion multiple>

  <v-pane ng-repeat="pane in panes">
    <v-pane-header inactive>
      {{ ::pane.header }}
      <button ng-click="$pane.toggle()">Toggle me</button>
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

  <button ng-click="$accordion.expandAll()">Expand all</button>

</v-accordion>
```


#### Events
  - `vAccordion:onReady` or `yourAccordionId:onReady`
  - `vAccordion:onExpand` or `yourAccordionId:onExpand`
  - `vAccordion:onExpandAnimationEnd` or `yourAccordionId:onExpandAnimationEnd`
  - `vAccordion:onCollapse` or `yourAccordionId:onCollapse`
  - `vAccordion:onCollapseAnimationEnd` or `yourAccordionId:onCollapseAnimationEnd`


## Callbacks

Use these callbacks to get expanded/collapsed pane index and id:

```html
<v-accordion onexpand="expandCallback(index, id)" oncollapse="collapseCallback(index, id)">

  <v-pane id="{{ ::pane.id }}" ng-repeat="pane in panes">
    <v-pane-header>
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>
```


```js
$scope.expandCallback = function (index, id) {
  console.log('expanded pane:', index, id);
};

$scope.collapseCallback = function (index) {
  console.log('collapsed pane:', index, id));
};
```

## Accessibility
vAccordion manages keyboard focus and adds some common aria-* attributes. BUT you should additionally place the `aria-controls` and `aria-labelledby` as follows:

```html
<v-accordion>

  <v-pane ng-repeat="pane in panes">
    <v-pane-header id="{{ ::pane.id }}-header" aria-controls="{{ ::pane.id }}-content">
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content id="{{ ::pane.id }}-content" aria-labelledby="{{ ::pane.id }}-header">
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>
```

```html
<v-accordion multiple control="myAccordion" onexpand="expandCb(index, id)">
  <v-pane id="myPane1">
    <v-pane-header>
      First pane header
    </v-pane-header>

    <v-pane-content>
      First pane content
    </v-pane-content>
  </v-pane>

  <v-pane id="myPane2">
    <v-pane-header>
      Second pane header
    </v-pane-header>

    <v-pane-content>
      Second pane content

      <v-accordion multiple control="mySubAccordion">
        <v-pane id="mySubPane1">
          <v-pane-header inactive>
            First pane header
            <button ng-click="$pane.toggle()">Toggle me</button>
          </v-pane-header>

          <v-pane-content>
            First pane content
          </v-pane-content>
        </v-pane>
      </v-accordion>
    </v-pane-content>
  </v-pane>
</v-accordion>
```

```js
myApp.controller('myAccordionController', function ($scope) {

  $scope.expandCb = function (index, id) {
    if (id === 'pane1') {
      $scope.mySubAccordion.collapse('mySubPane1');
    }
  };

});
```
