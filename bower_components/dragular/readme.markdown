# Dragular

> Angular drag&drop based on [dragula](https://github.com/bevacqua/dragula)

Browser support includes every sane browser and **IE7+**. <sub>_(Granted you polyfill the functional `Array` methods in ES5)_</sub>

# [=== Demo ===][2]

# Inspiration

I am working on huge angular project and I am using several drag&drop libraries in it, one for UI, one for lists, etc.. I want to use one full-featured drag&drop library for whole project. As I could not find any suitable, I decided to create one. I have choosen great library [dragula](http://github.com/bevacqua/dragula) by [Nicolas Bevacqua](http://github.com/bevacqua) as my starting point, make it more angular and started to put features in it! If you wish light-weight angular version of dragula, there is [official angular version of dragula](http://github.com/bevacqua/angular-dragula).

<b>Actual version 3.2.0 is based on dragula 3.5.2 and tested with angular 1.4.3.</b>

# Differences of dragular (against dragula)

- replaced dragula crossvent with angulars event binding
- replaced dragula contra.emitter with $scope.$emit if scope provided in options (options.scope)
- provided as service or directive dragular where options can be passed via atribute dragular
- automatic direction if not provided in options, instead of default vertical
- accepting arraylike objects as containers array (jQuery, jQlite collections etc..)
- accepting custom classes via option.classes
- namespaced containers groups available via option.nameSpace (containers in same nameSpace cooperate)
- boundingBox (dragging element can me moved only in specific area)
- lockX/Y (dragging element can me moved only in specific direction)
- DOM can be synced with scope model
- support css selectors to define containers
- added syntax highlighter to example codes
- etc..

# Todo towards next versions

- improving docs

# Features

- provided as service and also as directive
- Easy to set up
- No bloated dependencies
- A shadow where the item would be dropped offers **visual feedback**
- Touch events!
- DOM can be synced with model
- area or axes of movement can be restricted

# Install

download dragular.js and dragular.css from dist folder

OR clone git

```
git clone http://github.com/luckylooke/dragular.git
```
OR use npm
```
[sudo] npm install dragular
```
OR use bower
```
bower install dragular
```
AND
include files into your project
```html
  <link href='styles/dragular.css' rel='stylesheet' type='text/css' />
  <script src='scripts/dragular.js'></script>
```
AND
put dragularModule into dependency array
```javascript
var app = angular.module('myApp', ['dragularModule', 'otherDependencies']);
```
DONE :)

# Usage

Dragular provides the easiest possible API to make drag and drop a breeze in your applications. You can use it as service or as directive. Both arguments are optional. But you need to tell dragular what element(s) to use as container(s)<b>(container is closest wrapping element of draggables and also it serves as droppable area)</b>. In service you can provide them in forst argument or in second via options.containers. <b>Options.containers property is higher priority!</b>

## As service `dragularService(containers?, options?)`

By default, `dragular` will allow the user to drag an element in any of the `containers` and drop it in any other container in the list. If the element is dropped anywhere that's not one of the `containers`, the event will be gracefully cancelled according to the `revertOnSpill` and `removeOnSpill` options.

Note that dragging is only triggered on left clicks, and only if no meta keys are pressed. Clicks on buttons and anchor tags are ignored, too.

The example below allows the user to drag elements from `left` into `right`, and from `right` into `left`.

```js
dragularService('#left, #right');
```

Containers supported types:

Type                    | Description
--------------------------|----------------------------------------------------------------------------
`element`    | single element of container
`string`    | css selector (document.querySelectorAll, beware browser support), one or multiple containers
`array`    | array of DOM elements
`array-like`    | object containing elements on numerical properties (jQuery wrapper, jQlite, etc..), must have length property

You can also provide an `options` object into service as second parameter.

```js
dragularService(containers, {
  moves: function (el, container, handle) {
    return true;         // elements are always draggable by default
  },
  accepts: function (el, target, source, sibling) { // applied with target container options
    return true;         // elements can be dropped in any of the `containers` by default
  },
  canBeAccepted: function (el, target, source, sibling) { // applied with source container options
    return true;         // elements can be dropped in any of the `containers` by default
  },
  direction: 'vertical', // Y axis is considered when determining where an element would be dropped
  copy: false,           // elements are moved by default, not copied
  revertOnSpill: false,  // spilling will put the element back where it was dragged from, if this is true
  removeOnSpill: false   // spilling will `.remove` the element, if this is true
});
```

## As directive

For now just as attribute (restrict: A)!
```html
<div dragular="dragularOptions"></div>
```

The dragularOptions can be any name of property where options object is located or angular expression resulting with options object.

```js
$scope.dragularOptions = {
  classes: {
    mirror: 'custom-green-mirror'
  },
  nameSpace: 'common',
  direction: 'horizontal'
};
```
OR providing options as JSON

```html
<div dragular='{"classes":{"mirror":"custom-green-mirror"},"nameSpace":"common"}'></div>
```

### dragular-model atribute

Model can be optionaly provided via `dragular-model` atribute, but only in case you are using dragular directive next to it. If presented it has higher priority than `options.containersModel` property and it **extends options provided in `dragula` attribute into new options object!**.

```html
<div dragular-model="items"></div>
```

## Options

The options are detailed below.

### `options.containersModel`

If you wish to have model synced with containers state, you need to provide it within this property. For single container you can provide an array with items in it. Items can by any type. For multiple containers you need to provide array of arrays (2D-array), where order of arrays representing containers (models) must be same as order of containers elements provided in `containers` parameter of service.

Please note that if you are using filters on your items you must provide filtered array no source one!
```html
  <input ng-model="query">
  <div id="container">
     <div ng-repeat="item in filteredItems = (sourceItems | orderBy:'order_prop' | filter:query | limitTo:4)">
       {{item}}
     </div>
  </div>
 ```
 ```js
dragularService('#container', {
  containersModel: filteredItems
});
 ```

### `options.moves`

You can define a `moves` method which will be invoked with `(el, container, handle)` whenever an element is clicked. If this method returns `false`, a drag event won't begin, and the event won't be prevented either. The `handle` element will be the original click target, which comes in handy to test if that element is an expected _"drag handle"_.

### `options.accepts`

You can set `accepts` to a method with the following signature: `(el, target, source, sibling)`. It'll be called to make sure that an element `el`, that came from container `source`, can be dropped on container `target` before a `sibling` element. The `sibling` can be `null`, which would mean that the element would be placed as the last element in the container. Note that if `options.copy` is set to `true`, `el` will be set to the copy, instead of the originally dragged element. Applied with options provided with initialisation of target container.

Also note that **the position where a drag starts is always going to be a valid place where to drop the element**, even if `accepts` returned `false` for all cases.

### `options.canBeAccepted`

Same as options.accepts but applied with options provided with initialisation of source container.

### `options.copy`

If `copy` is set to `true` _(or a method that returns `true`)_, items will be copied rather than moved. This implies the following differences:

Event     | Move                                     | Copy
----------|------------------------------------------|---------------------------------------------
`dragulardrag`    | Element will be concealed from `source`  | Nothing happens
`dragulardrop`    | Element will be moved into `target`      | Element will be cloned into `target`
`dragularremove`  | Element will be removed from DOM         | Nothing happens
`dragularcancel`  | Element will stay in `source`            | Nothing happens

If a method is passed, it'll be called whenever an element starts being dragged in order to decide whether it should follow `copy` behavior or not. Consider the following example.

```js
copy: function (el, source) {
  return el.className === 'you-may-copy-us';
}
```

#### `options.copySortSource`

If `copy` is set to `true` _(or a method that returns `true`)_ and `copySortSource` is `true` as well, users will be able to sort elements in `copy`-source containers.

```js
copy: true,
copySortSource: true
```

### `options.revertOnSpill`

By default, spilling an element outside of any containers will move the element back to the _drop position previewed by the feedback shadow_. Setting `revertOnSpill` to `true` will ensure elements dropped outside of any approved containers are moved back to the source element where the drag event began, rather than stay at the _drop position previewed by the feedback shadow_.

### `options.removeOnSpill`

By default, spilling an element outside of any containers will move the element back to the _drop position previewed by the feedback shadow_. Setting `removeOnSpill` to `true` will ensure elements dropped outside of any approved containers are removed from the DOM. Note that `remove` events won't fire if `copy` is set to `true`.

### `options.direction`

When an element is dropped onto a container, it'll be placed near the point where the mouse was released. If the `direction` is `'vertical'`, the Y axis will be considered. Otherwise, if the `direction` is `'horizontal'`, the X axis will be considered. Default is automatic, where simple logic determines direction by comparison of dimensions of parent and its first child.

### `options.scope`

Scope can be provided for emitting events, you can provide whichever scope you like.

## `Events`

If $scope is provided as options.scope the following events can be tracked using `$scope.on(type, listener)`:

Event Name | Listener Arguments      | Event Description
-----------|-------------------------|-------------------------------------------------------------------------------------
`dragulardrag`     | `el, container`         | `el` was lifted from `container`
`dragulardragend`  | `el`                    | Dragging event for `el` ended with either `cancel`, `remove`, or `drop`
`dragulardrop`     | `el, container, source` | `el` was dropped into `container`, and originally came from `source`
`dragularcancel`   | `el, container`         | `el` was being dragged but it got nowhere and went back into `container`, its last stable parent
`dragularremove`   | `el, container`         | `el` was being dragged but it got nowhere and it was removed from the DOM. Its last stable parent was `container`.
`dragularshadow`   | `el, container`         | `el`, _the visual aid shadow_, was moved into `container`. May trigger many times as the position of `el` changes, even within the same `container`
`dragularcloned`   | `clone, original`       | DOM element `original` was cloned as `clone`. Triggers for mirror images and when `copy: true`

## API

The `dragularService` method returns a tiny object with a concise API. We'll refer to the API returned by `dragularService` as `drake`.

### `drake.dragging`

This property will be `true` whenever an element is being dragged.

### `drake.start(item)`

Enter drag mode **without a shadow**. This method is most useful when providing complementary keyboard shortcuts to an existing drag and drop solution. Even though a shadow won't be created at first, the user will get one as soon as they click on `item` and start dragging it around. Note that if they click and drag something else, `.end` will be called before picking up the new item.

### `drake.end()`

Gracefully end the drag event as if using **the last position marked by the preview shadow** as the drop target. The proper `cancel` or `drop` event will be fired, depending on whether the item was dropped back where it was originally lifted from _(which is essentially a no-op that's treated as a `cancel` event)_.

### `drake.cancel(revert)`

If an element managed by `drake` is currently being dragged, this method will gracefully cancel the drag action. You can also pass in `revert` at the method invocation level, effectively producing the same result as if `revertOnSpill` was `true`.

Note that **a _"cancellation"_ will result in a `cancel` event** only in the following scenarios.

- `revertOnSpill` is `true`
- Drop target _(as previewed by the feedback shadow)_ is the source container **and** the item is dropped in the same position where it was originally dragged from

### `drake.remove()`

If an element managed by `drake` is currently being dragged, this method will gracefully remove it from the DOM.

### `drake.destroy()`

Removes all drag and drop events used by `dragularService` to manage drag and drop between the `containers`. If `.destroy` is called while an element is being dragged, the drag will be effectively cancelled.

# License

MIT

[![eyes.png][3]][4]

[1]: https://github.com/luckylooke/dragular/blob/master/resources/demo.png
[2]: http://luckylooke.github.io/dragular/
[3]: https://github.com/luckylooke/dragular/blob/master/resources/eyes.png
[4]: https://www.youtube.com/watch?v=EqQuihD0hoI
