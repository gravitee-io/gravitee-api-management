(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else {
		var a = factory();
		for(var i in a) (typeof exports === 'object' ? exports : root)[i] = a[i];
	}
})(this, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	/* global angular */
	'use strict';
	var dragularDirective = __webpack_require__(1);
	var dragularService = __webpack_require__(2);

	/**
	 * Dragular 4.2.4 by Luckylooke https://github.com/luckylooke/dragular
	 * Angular version of dragula https://github.com/bevacqua/dragula
	 */
	module.exports = 'dragularModule';

	angular
	  .module('dragularModule', [])
	  .factory('dragularService', dragularService)
	  .directive('dragular', dragularDirective);


/***/ },
/* 1 */
/***/ function(module, exports) {

	'use strict';

	/**
	 * dragular Directive by Luckylooke https://github.com/luckylooke/dragular
	 * Angular version of dragula https://github.com/bevacqua/dragula
	 */

	var dragular = function (dragularService) {
	  return {
	    restrict: 'A',
	    link: function($scope, iElm, iAttrs) {

	      var drake,
	        options = $scope.$eval(iAttrs.dragular) || tryJson(iAttrs.dragular) || {};

	      function tryJson(json) {
	        try { // I dont like try catch solutions but I havent find sattisfying way of chcecking json validity.
	          return JSON.parse(json);
	        } catch (e) {
	          return undefined;
	        }
	      }

	      if(options && options.containersModel && typeof options.containersModel === 'string'){
	        options.containersModel = $scope.$eval(options.containersModel);
	      }

	      if(options && options.dynamicModelAttribute){
	        // watch for model changes
	        $scope.$watch(function () {
	          return $scope.$eval(iAttrs.dragularModel);
	        }, function (newVal) {
	          if(newVal){
	            drake.containersModel = drake.sanitizeContainersModel($scope.$eval(newVal));
	          }
	        });
	      }else if(iAttrs.dragularModel){
	        // bind once and keep reference
	        options.containersModel = $scope.$eval(iAttrs.dragularModel);
	      }

	      if(iAttrs.dragularNameSpace){
	        options.nameSpace = iAttrs.dragularNameSpace.split(' ');
	      }

	      if(iAttrs.dragularOnInit){
	        options.onInit = $scope.$eval(iAttrs.dragularOnInit);
	      }

	      drake = dragularService(iElm[0], options);
	    }
	  };
	};

	dragular.$inject = ['dragularService'];

	module.exports = dragular;


/***/ },
/* 2 */
/***/ function(module, exports) {

	/* global angular */
	'use strict';

	/**
	 * dragular Service by Luckylooke https://github.com/luckylooke/dragular
	 * Angular version of dragula https://github.com/bevacqua/dragula
	 */

	var shared = { // sahred object between all service instances
	  classesCache: {}, // classes lookup cache
	  containersCtx: {}, // containers model
	  containers: {}, // containers
	  mirror: null, // mirror image
	  source: null, // source container
	  item: null, // item being dragged
	  copy: null, // copy flag
	  sourceItem: null, // item originaly dragged if copy is enabled
	  sourceModel: null, // source container model
	  sourceFilteredModel: null, // source container filtered model if relevant
	  target: null, // droppable container under drag item
	  targetCtx: null, // target container context
	  targetModel: null, // target container model
	  lastDropTarget: null, // last container item was over
	  offsetX: null, // reference x
	  offsetY: null, // reference y
	  moveX: null, // reference move x
	  moveY: null, // reference move y
	  offsetXr: null, // reference x right for boundingBox feature
	  offsetYb: null, // reference y bottom for boundingBox feature
	  clientX: null, // cache client x, init at grab, update at drag
	  clientY: null, // cache client y, init at grab, update at drag
	  mirrorWidth: null, // mirror width for boundingBox feature
	  mirrorHeight: null, // mirror height for boundingBox feature
	  initialSibling: null, // reference sibling when grabbed
	  currentSibling: null, // reference sibling now
	  initialIndex: null, // reference model index when grabbed
	  currentIndex: null, // reference model index now
	  tempModel: null, // if o.isContainer is used, model can be provided as well, it is temporary saved here during drags
	  dragOverEvents: {}, // drag over events fired on element behind cursor
	  lastElementBehindCursor: null, // last element behind cursor
	  grabbed: null // holds mousedown context until first mousemove
	};

	var dragularService = function ($rootScope, $compile) {
	  // abbreviations
	  var doc = document,
	      docElm = doc.documentElement;

	  // clean common/shared objects
	  service.cleanEnviroment = function cleanEnviroment() {
	    shared.classesCache = {};
	    shared.containersCtx = {};
	    shared.containers = {};
	    shared.mirror = undefined;
	  };

	  service.shared = shared;

	  return service;

	  // service definition
	  function service(arg0, arg1) {
	    var initialContainers = arg0 || [],
	      options = arg1 || {},
	      o, // shorthand for options
	      g = getBool, // shorthand for getBool
	      // defaults
	      defaultClasses = {
	        mirror: 'gu-mirror',
	        hide: 'gu-hide',
	        unselectable: 'gu-unselectable',
	        transit: 'gu-transit'
	      },
	      defaultEventNames = {
	        // drag-over DOM events
	        dragularenter: 'dragularenter',
	        dragularleave: 'dragularleave',
	        dragularrelease: 'dragularrelease',
	        // $scope events
	        dragularcloned: 'dragularcloned',
	        dragulardrag: 'dragulardrag',
	        dragularcancel: 'dragularcancel',
	        dragulardrop: 'dragulardrop',
	        dragularremove: 'dragularremove',
	        dragulardragend: 'dragulardragend',
	        dragularshadow: 'dragularshadow',
	        dragularover: 'dragularover',
	        dragularout: 'dragularout'
	      },
	      defaultOptions = { // options with defaults
	        copyOptions: false, // copy options object when provided
	        classes: defaultClasses, // classes used by dragular
	        eventNames: defaultEventNames, // event names used by dragular
	        containers: false, // initial containers provided via options object (are provided via parameter by default)
	        containersModel: false, // if provided, model will be synced with DOM
	        containersFilteredModel: false, // if provided, dragular will handle filtered model cases
	        isContainer: never, // potential target can be forced to be container by custom logic
	        isContainerModel: getEmptyObject, // if isContainer function is provided, you can provide also respective model
	        moves: always, // can drag start?
	        accepts: always, // can target accept dragged item? (target context used)
	        canBeAccepted: always, // can be dragged item accepted by target? (source context used)
	        copy: false, // dragged item will be copy of source? flag or function
	        copySortSource: false, // enable sorting in source when copying item
	        dontCopyModel: false, // dont make copy of model when coping item (#61)
	        invalid: never, // target (in)validity function
	        revertOnSpill: false, // item returns to original place
	        removeOnSpill: false, // item will be removed if not placed into valid target
	        lockX: false, // lock movement into x-axis
	        lockY: false, // lock movement into y-axis
	        boundingBox: false, // lock movement inside this element boundaries
	        mirrorContainer: doc.body, // element for appending mirror
	        ignoreInputTextSelection: true, // text selection in inputs wont be considered as drag
	        compileItemOnDrop: false,
	        onInit: false // function callback called after dragular initialisation and providing drake as first argument
	      },
	      drake = {
	        containers: shared.containers,
	        containersCtx: shared.containersCtx,
	        sanitizeContainersModel: sanitizeContainersModel,
	        isContainer: isContainer,
	        start: manualStart,
	        end: end,
	        cancel: cancel,
	        remove: remove,
	        destroy: destroy,
	        dragging: false
	      };

	    processServiceArguments(); // both arguments (containers and options) are optional, this function handle this
	    extendOptions();
	    processOptionsObject();
	    registerEvents();
	    
	    if(o.onInit){
	       o.onInit(drake); 
	    }

	    return drake;

	    // Function definitions: ==============================================================================================================
	    // Initial functions: -----------------------------------------------------------------------------------------------------------------

	    function sanitizeContainersModel(containersModel) {
	      if (typeof(containersModel) === 'function') {
	        return containersModel;
	      }
	      if (Array.isArray(containersModel)) {
	        //                  |-------- is 2D array? -----------|
	        return Array.isArray(containersModel[0]) ? containersModel : [containersModel];
	      } else {
	        return [];
	      }
	    }

	    function processServiceArguments(){
	      if (arguments.length === 1 && // if there is only one argument we need to distinguish if it is options object or container(s) reference
	          !Array.isArray(arg0) && // array of containers elements
	          !angular.isElement(arg0) && // one container element
	          !arg0[0] && // array-like object with containers elements
	          typeof arg0 !== 'string') { // selector
	        // then arg0 is options object
	        options = arg0 || {};
	        initialContainers = []; // containers are not provided on init
	      } else if (typeof arg0 === 'string') {
	        initialContainers = document.querySelectorAll(arg0);
	      }
	      o = options.copyOptions ? angular.copy(options) : options;
	    }

	    function extendOptions(){
	      var tmp = angular.extend({}, defaultOptions, o); // tmp for keeping defaults untouched
	      angular.extend(o, tmp); // merge defaults back into options
	      if(o.classes){
	        tmp = angular.extend({}, defaultClasses, o.classes);
	        angular.extend(o.classes, tmp);
	      }
	      if(o.eventNames){
	        tmp = angular.extend({}, defaultEventNames, o.eventNames);
	        angular.extend(o.eventNames, tmp);
	      }
	    }

	    function processOptionsObject(){
	      // bounding box must be pure DOM element, not jQuery wrapper or something else..
	      if (!isElement(o.boundingBox)) {
	        o.boundingBox = false;
	      }

	      // initial containers provided via options are higher priority then by parameter
	      if(o.containers){
	        initialContainers = o.containers;
	      }
	      // sanitize initialContainers
	      initialContainers = makeArray(initialContainers);

	      // sanitize o.containersModel
	      o.containersModel = sanitizeContainersModel(o.containersModel);

	      // sanitize o.containersFilteredModel
	      if (Array.isArray(o.containersFilteredModel)) {
	        //                  |-------- is 2D array? -----------|
	        o.containersFilteredModel = Array.isArray(o.containersFilteredModel[0]) ? o.containersFilteredModel : [o.containersFilteredModel];
	      } else {
	        o.containersFilteredModel = [];
	      }

	      // feed containers groups and optionaly do same for models
	      if (!o.nameSpace) {
	        o.nameSpace = ['dragularCommon'];
	      }
	      if (!Array.isArray(o.nameSpace)) {
	        o.nameSpace = [o.nameSpace];
	      }
	      o.nameSpace.forEach(function eachNameSpace(nameSpace) {
	        if (!shared.containers[nameSpace]) {
	          shared.containers[nameSpace] = [];
	          shared.containersCtx[nameSpace] = [];
	        }
	        var len = initialContainers.length,
	          shLen = shared.containers[nameSpace].length;
	        for (var i = 0; i < len; i++) {
	          shared.containers[nameSpace][i + shLen] = initialContainers[i];
	          shared.containersCtx[nameSpace][i + shLen] = {
	            o: o,
	            m: getContainersModel()[i], // can be undefined
	            fm: o.containersFilteredModel[i] // can be undefined
	          };
	        }
	      });
	    }

	    function registerEvents(remove) {
	      var op = remove ? 'off' : 'on';
	      regEvent(docElm, op, 'mouseup', release);

	      initialContainers.forEach(function addMouseDown(container) {
	        regEvent(container, 'on', 'mousedown', grab);
	      });

	      if(!remove){ // create dragular DOM events
	        angular.forEach(['dragularenter', 'dragularleave', 'dragularrelease'], function prepareDragOverEvents(name) {
	          var eventName = o.eventNames[name];
	          if(!shared.dragOverEvents[eventName]){
	            if (doc.createEvent) {
	              shared.dragOverEvents[eventName] = doc.createEvent('HTMLEvents');
	              shared.dragOverEvents[eventName].initEvent(eventName, true, true);
	            } else {
	              shared.dragOverEvents[eventName] = doc.createEventObject();
	              shared.dragOverEvents[eventName].eventType = eventName;
	            }
	          }
	        });
	      }
	    }

	    // Event handlers functions (end of initial functions): -----------------------------------------------------------------------------------------------------------------

	    function grab(e) {
	      // filter some odd situations
	      if (whichMouseButton(e) !== 1 || e.metaKey || e.ctrlKey) {
	        return; // we only care about honest-to-god left clicks and touch events
	      }

	      // set itial values
	      shared.moveX = e.clientX;
	      shared.moveY = e.clientY;

	      var context = canStart(e.target);
	      if (!context || !context.item) {
	        return;
	      }

	      shared.grabbed = context;
	      eventualMovements();
	      if (e.type === 'mousedown') {
	        if (isInput(e.target)) { // see also: https://github.com/bevacqua/dragula/issues/208
	          e.target.focus(); // fixes https://github.com/bevacqua/dragula/issues/176
	          // changed from context.item to e.target fixing https://github.com/luckylooke/dragular/issues/87#issuecomment-256865796
	        } else {
	          e.preventDefault(); // fixes https://github.com/bevacqua/dragula/issues/155
	        }
	      }
	    }

	    function release(e) {
	      ungrab();
	      if (!drake.dragging) {
	        return;
	      }
	      if (e.originalEvent){
	          e = e.originalEvent; // jQuery enviroment
	      }

	      shared.clientX = getCoord('clientX', e);
	      shared.clientY = getCoord('clientY', e);

	      var elementBehindCursor = getElementBehindPoint(shared.mirror, shared.clientX, shared.clientY),
	        dropTarget = findDropTarget(elementBehindCursor, shared.clientX, shared.clientY);

	      if (dropTarget && ((shared.copy && g(o.copySortSource)) || (!shared.copy || dropTarget !== shared.source))) {
	        // found valid target and (is not copy case or target is not initial container)
	        drop(shared.item, dropTarget);
	      } else if (g(o.removeOnSpill)) {
	        remove();
	      } else {
	        cancel();
	      }

	      // after release there is no container hovered
	      shared.target = null;

	      if (shared.lastElementBehindCursor) {
	        fireEvent(shared.lastElementBehindCursor, shared.dragOverEvents.dragularrelease, elementBehindCursor);
	      }

	      if (o.scope) {
	        o.scope.$emit(o.eventNames.dragularrelease, shared.item, shared.source, e);
	      }
	    }

	    // Main logic functions (end of event handler functions): -----------------------------------------------------------------------------------------------------------------

	    function isContainer(el) {
	      if(!el){
	        return false;
	      }
	      var i = o.nameSpace.length;
	      while (i--) {
	        if (shared.containers[o.nameSpace[i]].indexOf(el) !== -1) {
	          return true;
	        }
	      }
	      if (o.isContainer(el)) {
	        shared.tempModel = o.isContainerModel(el);
	        return true;
	      } else {
	        shared.tempModel = null;
	      }
	      return false;
	    }

	    function getContainersModel() {
	      return (typeof(o.containersModel) === 'function') ? sanitizeContainersModel(o.containersModel(drake, shared)) : o.containersModel;
	    }

	    function removeContainers(all) {
	      $rootScope.$applyAsync(function applyDestroyed() {
	        var changes = Array.isArray(all) ? all : makeArray(all);
	        changes.forEach(function forEachContainer(container) {
	          angular.forEach(o.nameSpace, function forEachNs(nameSpace) {
	            var index;
	            index = shared.containers[nameSpace].indexOf(container);
	            shared.containers[nameSpace].splice(index, 1);
	            shared.containersCtx[nameSpace].splice(index, 1);
	          });
	        });
	      });
	    }

	    function eventualMovements(remove) {
	      var op = remove ? 'off' : 'on';
	      regEvent(docElm, op, 'mousemove', startBecauseMouseMoved);
	    }

	    function movements(remove) {
	      var op = remove ? 'off' : 'on';
	      regEvent(docElm, op, 'selectstart', preventGrabbed); // IE8
	      regEvent(docElm, op, 'click', preventGrabbed);
	      regEvent(docElm, op, 'touchmove', preventGrabbed); // fixes touch devices scrolling while drag
	    }

	    function destroy() {
	      registerEvents(true);
	      removeContainers(initialContainers);
	      release({});
	    }

	    function startBecauseMouseMoved(e) {
	      if (!shared.grabbed || drake.dragging) {
	        return;
	      }
	      if (e.originalEvent){
	          e = e.originalEvent; // jQuery enviroment
	      }
	      if (whichMouseButton(e) === 0) {
	        release({});
	        return; // when text is selected on an input and then dragged, mouseup doesn't fire. this is our only hope
	      }
	      // truthy check fixes dragula-#239, equality fixes dragula-#207
	      if (e.clientX && e.clientX === shared.moveX && e.clientY && e.clientY === shared.moveY) {
	        return;
	      }
	      if (g(o.ignoreInputTextSelection)) {
	        var clientX = getCoord('clientX', e),
	          clientY = getCoord('clientY', e),
	          elementBehindCursor = doc.elementFromPoint(clientX, clientY);
	        if (isInput(elementBehindCursor)) {
	          return;
	        }
	      }

	      var grabbed = shared.grabbed; // calling end() unsets shared.grabbed
	      eventualMovements('remove'); // remove mousemove listener
	      movements();
	      end();
	      start(grabbed);

	      // automaticly detect direction of elements if not set in options
	      if (!o.direction && getParent(shared.sourceItem)) {
	        var parent = shared.sourceItem.parentNode,
	          parentHeight = parent.offsetHeight,
	          parentWidth = parent.offsetWidth,
	          childHeight = shared.sourceItem.clientHeight,
	          childWidth = shared.sourceItem.clientWidth;
	        o.direction = parentHeight / childHeight < parentWidth / childWidth ? 'horizontal' : 'vertical';
	      }

	      // get initial coordinates, used to render shared.mirror for first time
	      var offset = getOffset(shared.sourceItem);
	      shared.offsetX = getCoord('pageX', e) - offset.left;
	      shared.offsetY = getCoord('pageY', e) - offset.top;
	      shared.clientX = getCoord('clientX', e);
	      shared.clientY = getCoord('clientY', e);

	      // limiting area of shared.mirror movement, get initial coordinates
	      if (o.boundingBox) {
	        shared.offsetXr = getCoord('pageX', e) - offset.right;
	        shared.offsetYb = getCoord('pageY', e) - offset.bottom;
	      }

	      e.preventDefault();

	      renderMirrorImage();
	      addClass(shared.item, o.classes.transit);
	      // initial position
	      shared.mirror.style.left = shared.clientX - shared.offsetX + 'px';
	      shared.mirror.style.top = shared.clientY - shared.offsetY + 'px';

	      drag(e);
	    }

	    function canStart(item) {
	      if (drake.dragging && shared.mirror) {
	        console.log('usecase?');
	        return; // already dragging
	      }

	      var handle = item;
	      while (getParent(item) && !isContainer(getParent(item))) {
	        // break loop if user tries to drag item which is considered invalid handle
	        if (o.invalid(item, handle)) {
	          return;
	        }
	        item = getParent(item); // drag target should be immediate child of container
	        if (!item) {
	          return;
	        }
	      }

	      var source = getParent(item);
	      if (!source ||
	        o.invalid(item, handle) ||
	        !o.moves(item, source, handle, nextEl(item))) {
	        return;
	      }

	      return {
	        item: item,
	        source: source
	      };
	    }

	    function manualStart(item) {
	      var context = canStart(item);
	      if (context) {
	        start(context);
	      }
	    }

	    function start(context) {
	      shared.sourceItem = shared.item = context.item;
	      shared.source = context.source;
	      shared.initialSibling = shared.currentSibling = nextEl(context.item);

	      if (g(o.copy, [context.item, context.source])) {
	        shared.item = context.item.cloneNode(true);
	        shared.copy = true;
	        if (o.scope) {
	          o.scope.$emit(o.eventNames.dragularcloned, shared.item, context.item);
	        }
	      } else {
	        shared.copy = false;
	      }

	      // prepare models operations
	      var containerIndex = initialContainers.indexOf(context.source);
	      shared.sourceModel = getContainersModel()[containerIndex];

	      shared.sourceFilteredModel = o.containersFilteredModel[containerIndex];
	      shared.initialIndex = domIndexOf(context.item, context.source);

	      drake.dragging = true;
	      if (o.scope) {
	        o.scope.$emit(o.eventNames.dragulardrag, shared.sourceItem, shared.source);
	      }

	      return true;
	    }

	    function end() {
	      if (!drake.dragging || !shared.item) {
	        return;
	      }
	      drop(shared.item, getParent(shared.item));
	    }

	    function ungrab() {
	      shared.grabbed = false;
	      eventualMovements('remove');
	      movements('remove');
	    }

	    function drop(item, target) {
	      if(!item){ // https://github.com/luckylooke/dragular/issues/102
	        cleanup();
	        return;
	      }
	      var sourceItem = shared.sourceItem,
	          currentSibling = shared.currentSibling,
	          dropIndex = domIndexOf(item, target);
	        
	      if (shared.copy && g(o.copySortSource) && target === shared.source && getParent(item)) {
	        item.parentNode.removeChild(shared.sourceItem);
	      }

	      if (shared.sourceModel && !isInitialPlacement(target)) {
	        if(shared.targetCtx.fm){ // target has filtered model
	          // convert index from index-in-filteredModel to index-in-model
	          dropIndex = shared.targetCtx.m.indexOf(shared.targetCtx.fm[dropIndex]);
	        }
	        if(shared.sourceFilteredModel){ // target has filtered model
	          // convert index from index-in-filteredModel to index-in-model
	          shared.initialIndex = shared.sourceModel.indexOf(shared.sourceFilteredModel[shared.initialIndex]);
	        }
	        $rootScope.$applyAsync(function applyDrop() {
	          if(!shared.sourceModel){
	              return;
	          }
	          if (target === shared.source) {
	            shared.sourceModel.splice(dropIndex, 0, shared.sourceModel.splice(shared.initialIndex, 1)[0]);
	          } else {
	            shared.dropElmModel = shared.copy && !o.dontCopyModel ? angular.copy(shared.sourceModel[shared.initialIndex]) : shared.sourceModel[shared.initialIndex];

	            if (!shared.tempModel) {
	              shared.targetModel = shared.targetCtx.m;
	            } else {
	              shared.targetModel = shared.tempModel;
	            }
	            
	            target.removeChild(item); // element must be removed for ngRepeat to apply correctly

	            if (!shared.copy) {
	              shared.sourceModel.splice(shared.initialIndex, 1);
	            }
	            shared.targetModel.splice(dropIndex, 0, shared.dropElmModel);
	          }

	          if (getParent(item)) {
	            item.parentNode.removeChild(item);
	          }

	          afterDrop();
	        });
	      } else {
	        afterDrop();
	      }

	      function afterDrop() {
	        if(o.compileItemOnDrop){
	            var scope = angular.element(target).scope();
	            scope.$applyAsync(function(){
	                var content = $compile(shared.copy ? sourceItem.cloneNode(true) : sourceItem)(scope);                   
	                if(item.parentNode === target){
	                    target.removeChild(item);
	                }
	                target.insertBefore(content[0], currentSibling);
	            });
	        }
	        
	        if (o.scope) {
	          if (isInitialPlacement(target)) {
	            o.scope.$emit(o.eventNames.dragularcancel, item, shared.source, shared.sourceModel, shared.initialIndex);
	          } else {
	            o.scope.$emit(o.eventNames.dragulardrop, item, target, shared.source, shared.sourceModel, shared.initialIndex, shared.targetModel, dropIndex);
	          }
	        }
	        
	        cleanup();
	      }
	    }

	    function remove() {
	      if (!drake.dragging) {
	        return;
	      }
	      var parent = getParent(shared.item);

	      if (parent) {
	        parent.removeChild(shared.item);
	      }

	      if (shared.sourceModel) {
	        $rootScope.$applyAsync(function removeModel() {
	          shared.sourceModel.splice(shared.initialIndex, 1);
	          cleanup();
	        });
	      }

	      if (o.scope) {
	        o.scope.$emit(shared.copy ? o.eventNames.dragularcancel : o.eventNames.dragularremove, shared.item, parent, shared.sourceModel, shared.initialIndex);
	      }
	      if (!shared.sourceModel) {
	        cleanup();
	      }
	    }

	    function cancel(revert) {
	      if (!drake.dragging) {
	        return;
	      }
	      var reverts = arguments.length > 0 ? revert : g(o.revertOnSpill),
	        parent = getParent(shared.item);

	      var initial = isInitialPlacement(parent);
	      if (!initial && !shared.copy && reverts) {
	        shared.source.insertBefore(shared.item, shared.initialSibling);
	      }
	      if (shared.sourceModel && !shared.copy && !reverts) {
	        drop(shared.item, parent);
	      } else if (o.scope) {
	        if (initial || reverts) {
	          o.scope.$emit(o.eventNames.dragularcancel, shared.item, shared.source, shared.sourceModel, shared.initialIndex);
	        }
	      }

	      if (!shared.sourceModel || shared.copy || reverts || initial) {
	        cleanup();
	      }
	    }

	    function cleanup() {
	      ungrab();
	      removeMirrorImage();

	      if (shared.item) {
	        rmClass(shared.item, o.classes.transit);
	      }

	      drake.dragging = false;

	      if (g(o.removeOnSpill) === true) {
	        spillOut();
	      }

	      if (o.scope) {
	        if(shared.lastDropTarget){
	         o.scope.$emit(o.eventNames.dragularout, shared.item, shared.lastDropTarget, shared.source);
	        }
	        o.scope.$emit(o.eventNames.dragulardragend, shared.item);
	      }

	      shared.source = shared.item = shared.sourceItem = shared.initialSibling = shared.currentSibling = shared.sourceModel = null;
	      shared.initialIndex = shared.currentIndex = shared.lastDropTarget = shared.tempModel = shared.targetModel = null;
	      shared.dropElmModel = shared.targetCtx = shared.copy = shared.moveX = shared.moveY = null;
	    }

	    // is item currently placed in original container and original position?
	    function isInitialPlacement(target, s) { // watch performance - running each move several times!
	      var sibling = s || (shared.mirror ? shared.currentSibling : nextEl(shared.item));
	      return target === shared.source && sibling === shared.initialSibling;
	    }

	    // find valid drop container
	    function findDropTarget(elementBehindCursor, clientX, clientY) {  // watch performance - running each move!
	      var target = elementBehindCursor;

	      while (target && !accepted()) {
	        target = getParent(target);
	      }
	      return target;

	      function accepted() {
	        var accepts = false;

	        if (isContainer(target)) { // is droppable?

	          var immediate = getImmediateChild(target, elementBehindCursor),
	            reference = getReference(target, immediate, clientX, clientY),
	            initial = isInitialPlacement(target, reference),
	            i = o.nameSpace.length;

	          while (i--) {
	            if (shared.containers[o.nameSpace[i]].indexOf(target) !== -1) {
	              shared.targetCtx = shared.containersCtx[o.nameSpace[i]][shared.containers[o.nameSpace[i]].indexOf(target)];
	              break;
	            }
	            if (!shared.targetCtx) {
	              shared.targetCtx = shared.containersCtx.dragularCommon[shared.containers.dragularCommon.indexOf(target)];
	            }
	          }

	          accepts = initial ||
	            (shared.targetCtx.o.accepts(shared.item, target, shared.source, reference, shared.sourceModel, shared.initialIndex) &&
	              o.canBeAccepted(shared.item, target, shared.source, reference, shared.sourceModel, shared.initialIndex));

	          if (shared.target !== target) { // used for scroll issue
	            shared.target = target;
	          }
	        }
	        return accepts;
	      }
	    }

	    function drag(e) { // watch performance - running each move!
	      if (!shared.mirror) {
	        return;
	      }
	      if (e.originalEvent){
	          e = e.originalEvent; // jQuery enviroment
	      }

	      // update coordinates
	      shared.clientX = getCoord('clientX', e);
	      shared.clientY = getCoord('clientY', e);

	      // count mirror coordiates
	      var x = shared.clientX - shared.offsetX,
	        y = shared.clientY - shared.offsetY,
	        pageX,
	        pageY,
	        offsetBox;

	      // fill extra properties if boundingBox is used
	      if (o.boundingBox) {
	        pageX = getCoord('pageX', e);
	        pageY = getCoord('pageY', e);
	        offsetBox = getOffset(o.boundingBox);
	      }

	      if (!o.lockY) {
	        if (!o.boundingBox || (pageX > offsetBox.left + shared.offsetX && pageX < offsetBox.right + shared.offsetXr)) {
	          shared.mirror.style.left = x + 'px';
	        } else if (o.boundingBox) { // check again in case user scrolled the view
	          if (pageX < offsetBox.left + shared.offsetX) {
	            shared.mirror.style.left = shared.clientX - (pageX - offsetBox.left) + 'px';
	          } else {
	            shared.mirror.style.left = shared.clientX - shared.mirrorWidth - (pageX - offsetBox.right) + 'px';
	          }
	        }
	      }
	      if (!o.lockX) {
	        if (!o.boundingBox || (pageY > offsetBox.top + shared.offsetY && pageY < offsetBox.bottom + shared.offsetYb)) {
	          shared.mirror.style.top = y + 'px';
	        } else if (o.boundingBox) { // check again in case user scrolled the view
	          if (pageY < offsetBox.top + shared.offsetY) {
	            shared.mirror.style.top = shared.clientY - (pageY - offsetBox.top) + 'px';
	          } else {
	            shared.mirror.style.top = shared.clientY - shared.mirrorHeight - (pageY - offsetBox.bottom) + 'px';
	          }
	        }
	      }

	      var elementBehindCursor = getElementBehindPoint(shared.mirror, shared.clientX, shared.clientY),
	        dropTarget = findDropTarget(elementBehindCursor, shared.clientX, shared.clientY),
	        changed = dropTarget !== shared.lastDropTarget;

	      if (elementBehindCursor !== shared.lastElementBehindCursor) {
	        fireEvent(elementBehindCursor, shared.dragOverEvents.dragularenter, !!dropTarget);
	        if (shared.lastElementBehindCursor) {
	          fireEvent(shared.lastElementBehindCursor, shared.dragOverEvents.dragularleave, elementBehindCursor);
	        }
	        shared.lastElementBehindCursor = elementBehindCursor;
	      }

	      if (changed) {
	        if (shared.lastDropTarget) {
	          moved('out');
	        }
	        shared.lastDropTarget = dropTarget;
	        moved('over');
	      }

	      // do not copy in same container
	      if (dropTarget === shared.source && shared.copy && !g(o.copySortSource)) {
	        if (getParent(shared.item)) {
	          shared.item.parentNode.removeChild(shared.item);
	        }
	        return;
	      }

	      var reference,
	        immediate = getImmediateChild(dropTarget, elementBehindCursor);

	      if (immediate !== null) {
	        reference = getReference(dropTarget, immediate, shared.clientX, shared.clientY);
	      } else if (g(o.revertOnSpill) === true && !shared.copy) {
	        // the case that mirror is not over valid target and reverting is on and copy is off
	        reference = shared.initialSibling;
	        dropTarget = shared.source;
	      } else {
	        // the case that mirror is not over valid target and removing is on or copy is on
	        if (shared.copy && getParent(shared.item)) {
	          // remove item or copy of item
	          shared.item.parentNode.removeChild(shared.item);
	        }
	        return;
	      }

	      if (reference === null ||
	        reference !== shared.item &&
	        reference !== nextEl(shared.item) &&
	        reference !== shared.currentSibling) {
	        // moving item/copy to new container from previous one
	        shared.currentSibling = reference;

	        dropTarget.insertBefore(shared.item, reference); // if reference is null item is inserted at the end

	        if (o.scope) {
	          o.scope.$emit(o.eventNames.dragularshadow, shared.item, dropTarget, e);
	        }
	      }

	      function moved(type) {
	        if (o.scope) {
	          o.scope.$emit(o.eventNames['dragular' + type], shared.item, shared.lastDropTarget, shared.source, e);
	        }
	        if (g(o.removeOnSpill) === true) {
	          type === 'over' ? spillOver() : spillOut();
	        }
	      }
	    }

	    function spillOver() {
	      rmClass(shared.item, o.classes.hide);
	    }

	    function spillOut() {
	      if (drake.dragging) {
	        addClass(shared.item, o.classes.hide);
	      }
	    }

	    function scrollContainer(e) {
	      if (shared.target) {
	        if (e.originalEvent){
	            e = e.originalEvent; // jQuery enviroment
	        }
	        var before = shared.target.scrollTop;
	        shared.target.scrollTop += e.deltaY;
	        // block scroll of the document when container can be scrolled
	        if (before !== shared.target.scrollTop) {
	          e.stopPropagation();
	          e.preventDefault();
	        }
	      }
	    }

	    function renderMirrorImage() {
	      if (shared.mirror) {
	        return;
	      }
	      var rect = shared.sourceItem.getBoundingClientRect();
	      shared.mirror = shared.sourceItem.cloneNode(true);
	      shared.mirrorWidth = rect.width;
	      shared.mirrorHeight = rect.height;
	      shared.mirror.style.width = getRectWidth(rect) + 'px';
	      shared.mirror.style.height = getRectHeight(rect) + 'px';
	      addClass(shared.mirror, o.classes.mirror);
	      o.mirrorContainer.appendChild(shared.mirror);
	      regEvent(docElm, 'on', 'mousemove', drag);
	      addClass(doc.body, o.classes.unselectable);
	      regEvent(shared.mirror, 'on', 'wheel', scrollContainer);
	      if (o.scope) {
	        o.scope.$emit(o.eventNames.dragularcloned, shared.mirror, shared.sourceItem);
	      }
	    }

	    function removeMirrorImage() {
	      if (shared.mirror) {
	        rmClass(doc.body, o.classes.unselectable);
	        regEvent(docElm, 'off', 'mousemove', drag);
	        regEvent(shared.mirror, 'off', 'wheel', scrollContainer);
	        if(getParent(shared.mirror)){
	          shared.mirror.parentNode.removeChild(shared.mirror);
	        }
	        shared.mirror = null;
	      }
	    }

	    function getImmediateChild(dropTarget, target) { // watch performance - running each move several times!
	      var immediate = target;
	      while (immediate !== dropTarget && getParent(immediate) !== dropTarget) {
	        immediate = getParent(immediate);
	      }
	      if (immediate === docElm) {
	        return null;
	      }
	      return immediate;
	    }

	    function getReference(dropTarget, target, x, y) { // watch performance - running each move several times!
	      var horizontal = o.direction === 'horizontal';
	      return target !== dropTarget ? inside() : outside();

	      function outside() { // slower, but able to figure out any position
	        var len = dropTarget.children.length,
	          i, el, rect;
	        for (i = 0; i < len; i++) {
	          el = dropTarget.children[i];
	          rect = el.getBoundingClientRect();
	          if (horizontal && rect.left > x) {
	            return el;
	          }
	          if (!horizontal && rect.top > y) {
	            return el;
	          }
	        }
	        return null;
	      }

	      function inside() { // faster, but only available if dropped inside a child element
	        var rect = target.getBoundingClientRect();
	        if (horizontal) {
	          return resolve(x > rect.left + getRectWidth(rect) / 2);
	        }
	        return resolve(y > rect.top + getRectHeight(rect) / 2);
	      }

	      function resolve(after) {
	        return after ? nextEl(target) : target;
	      }
	    }

	    function getElementBehindPoint(point, x, y) { // watch performance - running each move!
	      var p = point || {},
	        state = p.className,
	        el;
	      p.className += ' ' + o.classes.hide;
	      el = doc.elementFromPoint(x, y);
	      p.className = state;
	      return el;
	    }
	  } // end of service

	  /****************************************************************************************************************************/
	  /****************************************************************************************************************************/
	  /****************************************************************************************************************************/

	  // HELPERS FUNCTIONS:

	  function regEvent(el, op, type, fn) {
	    var touch = {
	        mouseup: 'touchend',
	        mousedown: 'touchstart',
	        mousemove: 'touchmove'
	      },
	      pointers = {
	        mouseup: 'pointerup',
	        mousedown: 'pointerdown',
	        mousemove: 'pointermove'
	      },
	      microsoft = {
	        mouseup: 'MSPointerUp',
	        mousedown: 'MSPointerDown',
	        mousemove: 'MSPointerMove'
	      },
	      $el = angular.element(el);

	    if (typeof navigator !== 'undefined' && navigator.pointerEnabled && pointers[type]) {
	      $el[op](pointers[type], fn);
	    } else if (typeof navigator !== 'undefined' && navigator.msPointerEnabled && microsoft[type]) {
	      $el[op](microsoft[type], fn);
	    } else if (touch[type]) {
	      $el[op](touch[type], fn);
	    }
	    $el[op](type, fn);
	  }

	  function never() {
	    return false;
	  }

	  function always() {
	    return true;
	  }

	  // make array from array-like objects or from single element (based on bevacqua/atoa)
	  function makeArray(all, startIndex) {
	    if (Array.isArray(all)) {
	      return all;
	    }
	    if (all.length) { // is array-like
	      return Array.prototype.slice.call(all, startIndex); // convert to vanilla js array
	    } else { // is one element
	      return [all];
	    }
	  }

	  function whichMouseButton (e) {
	    if (e.touches) { return e.touches.length; }
	    if (e.originalEvent && e.originalEvent.touches) { return e.originalEvent.touches.length; }
	    if (e.which !== void 0 && e.which !== 0) { return e.which; } // github.com/bevacqua/dragula/issues/261
	    if (e.buttons !== undefined) { return e.buttons; }
	    var button = e.button;
	    if (button !== undefined) { // see github.com/jquery/jquery/blob/99e8ff1baa7ae341e94bb89c3e84570c7c3ad9ea/src/event.js#L573-L575
	      return button & 1 ? 1 : button & 2 ? 3 : (button & 4 ? 2 : 0);
	    }
	  }

	  function preventGrabbed(e) {
	    if (e.originalEvent){
	        e = e.originalEvent; // jQuery enviroment
	    }
	    if (shared.grabbed) {
	      e.preventDefault();
	    }
	  }

	  function getScroll(scrollProp, offsetProp) {
	    if (typeof window[offsetProp] !== 'undefined') {
	      return window[offsetProp];
	    }
	    if (docElm.clientHeight) {
	      return docElm[scrollProp];
	    }
	    return doc.body[scrollProp];
	  }

	  function getOffset(el) { // watch performance - running each move!
	    var rect = el.getBoundingClientRect(),
	      scrollTop = getScroll('scrollTop', 'pageYOffset'),
	      scrollLeft = getScroll('scrollLeft', 'pageXOffset');
	    return {
	      left: rect.left + scrollLeft,
	      right: rect.right + scrollLeft,
	      top: rect.top + scrollTop,
	      bottom: rect.bottom + scrollTop
	    };
	  }

	  function getRectWidth(rect) {
	    return rect.width || (rect.right - rect.left);
	  }

	  function getRectHeight(rect) {
	    return rect.height || (rect.bottom - rect.top);
	  }

	  function getEmptyObject() {
	    return {};
	  }

	  function nextEl(el) {
	    if(!el){ // https://github.com/luckylooke/dragular/issues/102
	      return;
	    }
	    return el.nextElementSibling || manually();

	    function manually() {
	      var sibling = el;
	      do {
	        sibling = sibling.nextSibling;
	      } while (sibling && sibling.nodeType !== 1);
	      return sibling;
	    }
	  }

	  //Cannot use angular.isElement because we need to check plain dom element, no jQlite wrapped
	  function isElement(obj) {
	    return (
	      typeof HTMLElement === 'object' ? obj instanceof HTMLElement : //DOM2
	      obj && typeof obj === 'object' && obj !== null && obj.nodeType === 1 && typeof obj.nodeName === 'string'
	    );
	  }

	  function lookupClass(className) {
	    var cached = shared.classesCache[className];
	    if (cached) {
	      cached.lastIndex = 0;
	    } else {
	      shared.classesCache[className] = cached = new RegExp('(?:^|\\s)' + className + '(?:\\s|$)', 'g');
	    }
	    return cached;
	  }

	  function addClass(el, className) {
	    var current = el.className;
	    if (!current.length) {
	      el.className = className;
	    } else if (!lookupClass(className).test(current)) {
	      el.className += ' ' + className;
	    }
	  }

	  function rmClass(el, className) {
	    el.className = el.className.replace(lookupClass(className), ' ').trim();
	  }

	  function getEventHost(e) {
	    // on touchend event, we have to use `e.changedTouches`
	    // see http://stackoverflow.com/questions/7192563/touchend-event-properties
	    // see https://github.com/bevacqua/dragula/issues/34
	    if (e.targetTouches && e.targetTouches.length) {
	      return e.targetTouches[0];
	    }
	    if (e.changedTouches && e.changedTouches.length) {
	      return e.changedTouches[0];
	    }
	    return e;
	  }

	  function getCoord(coord, e) { // watch performance - running each move several times!
	    var host = getEventHost(e);
	    var missMap = {
	      pageX: 'clientX', // IE8
	      pageY: 'clientY' // IE8
	    };
	    if (coord in missMap && !(coord in host) && missMap[coord] in host) {
	      coord = missMap[coord];
	    }

	    // Adding support for touch events, as they are not functional in the original
	    if (!host.type || host.type.indexOf('touch') < 0) {
	      return host[coord];
	    } else {
	      if (host.type.indexOf('end') === -1) {
	        // No clientX or clientY in a touch event
	        return host.originalEvent.touches[0][coord.replace('client', 'page')];
	      }
	      // Nothing should happen for touchend
	      return false;
	    }
	  }

	  function getParent (el) { // watch performance - running each move!
	    return el.parentNode === document ? null : el.parentNode;
	  }

	  function isInput (el) {
	    return el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || isEditable(el);
	  }

	  function isEditable (el) {
	    if (!el) { return false; } // no parents were editable
	    if (el.contentEditable === 'false') { return false; } // stop the lookup
	    if (el.contentEditable === 'true') { return true; } // found a contentEditable element in the chain
	    return isEditable(getParent(el)); // contentEditable is set to 'inherit'
	  }

	  function domIndexOf(child, parent) {
	    return Array.prototype.indexOf.call(angular.element(parent).children(), child);
	  }

	  function fireEvent(target, e, extra) { // watch performance - running each move!
	    if (!target) {
	      return;
	    }
	    shared.extra = extra;
	    if (target.dispatchEvent) {
	      target.dispatchEvent(e);
	    } else {
	      target.fireEvent('on' + e.eventType, e);
	    }
	  }

	  function getBool(prop, args, context){
	    if(angular.isFunction(prop)){
	      return !!prop.apply(context || this, args || shared);
	    }else{
	      return !!prop;
	    }
	  }

	};

	dragularService.$inject = ['$rootScope', '$compile'];

	module.exports = dragularService;


/***/ }
/******/ ])
});
;