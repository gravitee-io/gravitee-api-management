(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
/* global angular */
'use strict';

/**
 * dragular Directive by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */
 var dragularModule = require('./dragularModule');

/**
* @ngInject
*/

dragularModule.directive('dragular', ['dragularService', function(dragularService) {
  return {
    restrict: 'A',
    link: function($scope, iElm, iAttrs) {

      var options = $scope.$eval(iAttrs.dragular) || tryJson(iAttrs.dragular) || {};

      function tryJson(json) {
        try { // I dont like try catch solutions but I havent find sattisfying way of chcecking json validity.
          return JSON.parse(json);
        } catch (e) {
          return undefined;
        }
      }

      if(iAttrs.dragularModel){
        options = angular.extend({containersModel: $scope.$eval(iAttrs.dragularModel)}, options);
      }else if(options && options.containersModel && typeof options.containersModel === 'string'){
        options.containersModel = $scope.$eval(options.containersModel);
      }

      dragularService(iElm[0], options);
    }
  };
}]);

},{"./dragularModule":2}],2:[function(require,module,exports){
/* global angular */
'use strict';



/**
 * Dragular 3.1.0 by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */
module.exports = angular.module('dragularModule', []);

({"dragularDirective":require("./dragularDirective.js"),"dragularService":require("./dragularService.js")});

},{"./dragularDirective.js":1,"./dragularService.js":3}],3:[function(require,module,exports){
/* global angular */
'use strict';

/**
 * dragular Service by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */

var dragularModule = require('./dragularModule');

/**
 * @ngInject
 */

dragularModule.factory('dragularService', ['$rootScope', function dragula($rootScope) {

  var shared = { // function returned as service
      classesCache: {}, // classes lookup cache
      containersCtx: {}, // containers model
      containers: {}, // containers managed by the drake
      mirror: null, // mirror image
      source: null, // source container
      item: null, // item being dragged
      copy: null, // isCopy flag
      sourceItem: null, // item originaly dragged if copy is enabled
      sourceModel: null, // source container model
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
      isContainerModel: null, // if o.isContainer is used, model can be provided as well, here it is kept
      dragOverEvents: {}, // drag over events fired on element behind cursor
      lastElementBehindCursor: null, // last element behind cursor
      grabbed: null // holds mousedown context until first mousemove
    },
    serviceFn = function(initialContainers, options) {

      if (arguments.length === 1 && !Array.isArray(initialContainers) && !angular.isElement(initialContainers) && !initialContainers[0] && typeof initialContainers !== 'string') {
        // then containers are not provided, only options
        options = initialContainers;
        initialContainers = [];
      } else if (typeof initialContainers === 'string') {
        initialContainers = document.querySelectorAll(initialContainers);
      }

      var body = document.body,
        documentElement = document.documentElement,
        defaultClasses = {
          mirror: 'gu-mirror',
          hide: 'gu-hide',
          unselectable: 'gu-unselectable',
          transit: 'gu-transit'
        },
        isContainer, // internal isContainer
        o = { // options
          dragOverEventNames: ['dragularenter', 'dragularleave', 'dragularrelease'],
          classes: defaultClasses,
          containers: false,
          moves: always,
          accepts: always,
          canBeAccepted: always,
          isContainer: never,
          copy: false,
          invalid: invalidTarget,
          revertOnSpill: false,
          removeOnSpill: false,
          lockX: false,
          lockY: false,
          boundingBox: false,
          containersModel: false,
          isContainerModel: emptyObj
        };

      if (!isElement(o.boundingBox)) {
        o.boundingBox = null;
      }

      if (options && options.classes) {
        angular.extend(defaultClasses, options.classes);
        angular.extend(options.classes, defaultClasses);
      }

      angular.extend(o, options);

      if (!o.mirrorContainer) {
        o.mirrorContainer = document.body;
      }

      // get initial containers from options or parameter or fall back to empty array (containers can be also added later)
      initialContainers = o.containers || initialContainers || [];
      initialContainers = makeArray(initialContainers);

      if (o.containersModel) {
        //                            is 2D array?
        o.containersModel = Array.isArray(o.containersModel[0]) ? o.containersModel : [o.containersModel];
      } else {
        o.containersModel = [];
      }

      // feed containers groups and optionaly shadow it by models
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
            m: o.containersModel[i] // can be undefined
          };
        }
      });

      //register events
      events();

      angular.forEach(o.dragOverEventNames, function prepareDragOverEvents(dragOverEvent) {
        if (document.createEvent) {
          shared.dragOverEvents[dragOverEvent] = document.createEvent('HTMLEvents');
          shared.dragOverEvents[dragOverEvent].initEvent(dragOverEvent, true, true);
        } else {
          shared.dragOverEvents[dragOverEvent] = document.createEventObject();
          shared.dragOverEvents[dragOverEvent].eventType = dragOverEvent;
        }
      });

      isContainer = function isContainer(el) {
        var i = o.nameSpace.length;
        while (i--) {
          if (shared.containers[o.nameSpace[i]].indexOf(el) !== -1) {
            return true;
          }
        }
        if (o.isContainer(el)) {
          shared.isContainerModel = o.isContainerModel(el);
          return true;
        } else {
          shared.isContainerModel = null;
        }
        return false;
      };

      var drake = {
        containers: shared.containers,
        containersCtx: shared.containersCtx,
        isContainer: isContainer,
        start: manualStart,
        end: end,
        cancel: cancel,
        remove: remove,
        destroy: destroy,
        dragging: false
      };

      return drake;

      // make array from array-like objects or from single element (based on bevacqua/atoa)
      function makeArray(all, startIndex) {
        if (Array.isArray(all)) {
          return all;
        }
        if (all.length) { // is array-like
          return Array.prototype.slice.call(all, startIndex);
        } else { // is one element
          return [all];
        }
      }

      // add or remove containers - deprecated
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

      function events(remove) {
        var op = remove ? 'off' : 'on';
        regEvent(documentElement, op, 'mouseup', release);
        // regEvent(documentElement, op, 'mousemove', startBecauseMouseMoved);

        initialContainers.forEach(function addMouseDown(container) {
          regEvent(container, 'on', 'mousedown', grab);
        });
      }

      function eventualMovements(remove) {
        var op = remove ? 'off' : 'on';
        regEvent(documentElement, op, 'mousemove', startBecauseMouseMoved);
      }

      function movements(remove) {
        var op = remove ? 'off' : 'on';
        regEvent(documentElement, op, 'selectstart', preventGrabbed); // IE8
        regEvent(documentElement, op, 'click', preventGrabbed);
        regEvent(documentElement, op, 'touchmove', preventGrabbed); // fixes touch devices scrolling while drag
      }

      function destroy() {
        events(true);
        removeContainers(initialContainers);
        release({});
      }

      function preventGrabbed(e) {
        if (shared.grabbed) {
          e.preventDefault();
        }
      }

      function grab(e) {
        e = e || window.event;
        shared.moveX = e.clientX;
        shared.moveY = e.clientY;

        // filter some odd situations
        if (whichMouseButton(e) !== 1 || e.metaKey || e.ctrlKey) {
          return; // we only care about honest-to-god left clicks and touch events
        }

        var context = canStart(e.target);
        if (!context || !context.item) {
          return;
        }

        shared.grabbed = context;
        eventualMovements();
        if (e.type === 'mousedown') {
          if (isInput(context.item)) { // see also: https://github.com/bevacqua/dragula/issues/208
            context.item.focus(); // fixes https://github.com/bevacqua/dragula/issues/176
          } else {
            e.preventDefault(); // fixes https://github.com/bevacqua/dragula/issues/155
          }
        }
      }

      function startBecauseMouseMoved(e) {
        if (!shared.grabbed || drake.dragging) {
          return;
        }
        if (whichMouseButton(e) === 0) {
          release({});
          return; // when text is selected on an input and then dragged, mouseup doesn't fire. this is our only hope
        }
        if (e.clientX === shared.moveX && e.clientY === shared.moveY) {
          return;
        }
        if (o.ignoreInputTextSelection) {
          var clientX = getCoord('clientX', e),
            clientY = getCoord('clientY', e),
            elementBehindCursor = document.elementFromPoint(clientX, clientY);
          if (isInput(elementBehindCursor)) {
            return;
          }
        }

        var grabbed = shared.grabbed; // call to end() unsets shared.grabbed
        eventualMovements(true); // remove mousemove listener
        movements();
        end();
        start(grabbed);

        // automaticly detect direction of elements if not set in options
        if (!o.direction) {
          var parent = shared.sourceItem.parentElement,
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

        addClass(shared.item, o.classes.transit);
        renderMirrorImage();
        // initial position
        shared.mirror.style.left = shared.clientX - shared.offsetX + 'px';
        shared.mirror.style.top = shared.clientY - shared.offsetY + 'px';

        drag(e);
      }


      function canStart(item) {
        if (drake.dragging && shared.mirror) {
          return; // already dragging
        }

        var handle = item;

        while (item.parentElement &&
          !isContainer(item.parentElement)) {
          // break loop if user tries to drag item which is considered invalid handle
          if (o.invalid(item, handle)) {
            return;
          }
          item = item.parentElement; // drag target should be immediate child of container
          if (!item) {
            return;
          }
        }

        var source = item.parentElement;
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

        if (isCopy(context.item, context.source)) {
          shared.item = context.item.cloneNode(true);
          shared.copy = true;
          if (o.scope) {
            o.scope.$emit('dragularcloned', shared.item, context.item);
          }
        } else {
          shared.copy = false;
        }

        // prepare models operations
        var containerIndex = initialContainers.indexOf(context.source);
        shared.sourceModel = o.containersModel[containerIndex];
        shared.initialIndex = domIndexOf(context.item, context.source);

        drake.dragging = true;
        if (o.scope) {
          o.scope.$emit('dragulardrag', shared.sourceItem, shared.source);
        }

        return true;
      }

      function invalidTarget() {
        return false;
      }

      function end() {
        if (!drake.dragging || !shared.item) {
          return;
        }
        drop(shared.item, shared.item.parentElement);
      }

      function ungrab() {
        shared.grabbed = false;
        eventualMovements(true);
        movements(true);
      }

      function release(e) {
        ungrab();
        if (!drake.dragging) {
          return;
        }
        e = e || window.event;

        shared.clientX = getCoord('clientX', e);
        shared.clientY = getCoord('clientY', e);

        var elementBehindCursor = getElementBehindPoint(shared.mirror, shared.clientX, shared.clientY),
          dropTarget = findDropTarget(elementBehindCursor, shared.clientX, shared.clientY);

        if (dropTarget && ((shared.copy && o.copySortSource) || (!shared.copy || dropTarget !== shared.source))) {
          // found valid target and (is not copy case or target is not initial container)
          drop(shared.item, dropTarget);
        } else if (o.removeOnSpill) {
          remove();
        } else {
          cancel();
        }

        // after release there is no container hovered
        shared.target = null;

        if (shared.lastElementBehindCursor) {
          fireEvent(shared.lastElementBehindCursor, shared.dragOverEvents['dragularrelease'], elementBehindCursor);
        }

        if (o.scope) {
          o.scope.$emit('dragularrelease', shared.item, shared.source);
        }
      }

      function drop(item, target) {
        if (shared.copy && o.copySortSource && target === shared.source) {
          item.parentElement.removeChild(shared.sourceItem);
        }

        function emitDropEvent() {
          if (o.scope) {
            if (isInitialPlacement(target)) {
              o.scope.$emit('dragularcancel', item, shared.source, shared.sourceModel, shared.initialIndex);
            } else {
              o.scope.$emit('dragulardrop', item, target, shared.source, shared.sourceModel, shared.initialIndex, shared.targetModel);
            }
          }
        }

        if (shared.sourceModel && !isInitialPlacement(target)) {
          var dropElm = item,
            dropIndex = domIndexOf(dropElm, target);
          $rootScope.$applyAsync(function applyDrop() {
            if (target === shared.source) {
              shared.sourceModel.splice(dropIndex, 0, shared.sourceModel.splice(shared.initialIndex, 1)[0]);
            } else {
              shared.dropElmModel = shared.copy ? angular.copy(shared.sourceModel[shared.initialIndex]) : shared.sourceModel[shared.initialIndex];

              if (!shared.isContainerModel) {
                shared.targetModel = shared.targetCtx.m;
              } else {
                shared.targetModel = shared.isContainerModel;
              }

              target.removeChild(dropElm); // element must be removed for ngRepeat to apply correctly

              if (!shared.copy) {
                shared.sourceModel.splice(shared.initialIndex, 1);
              }
              shared.targetModel.splice(dropIndex, 0, shared.dropElmModel);
            }

            if (item.parentElement) {
              item.parentElement.removeChild(item);
            }

            emitDropEvent();
            cleanup();
          });
        } else {
          emitDropEvent();
          cleanup();
        }
      }

      function remove() {
        if (!drake.dragging) {
          return;
        }
        var parent = shared.item.parentElement;

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
          o.scope.$emit(shared.copy ? 'dragularcancel' : 'dragularremove', shared.item, parent, shared.sourceModel, shared.initialIndex);
        }
        if (!shared.sourceModel) {
          cleanup();
        }
      }

      function cancel(revert) {
        if (!drake.dragging) {
          return;
        }
        var reverts = arguments.length > 0 ? revert : o.revertOnSpill,
          parent = shared.item.parentElement;

        var initial = isInitialPlacement(parent);
        if (!initial && !shared.copy && reverts) {
          shared.source.insertBefore(shared.item, shared.initialSibling);
        }
        if (shared.sourceModel && !shared.copy && !reverts) {
          drop(shared.item, parent);
        } else if (o.scope) {
          if (initial || reverts) {
            o.scope.$emit('dragularcancel', shared.item, shared.source);
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

        if (o.removeOnSpill === true) {
          spillOut();
        }

        if (o.scope) {
          if(shared.lastDropTarget){
           o.scope.$emit('dragularout', shared.item, shared.lastDropTarget, shared.source);
          }
          o.scope.$emit('dragulardragend', shared.item);
        }

        shared.source = shared.item = shared.sourceItem = shared.initialSibling = shared.currentSibling = shared.sourceModel = null;
        shared.initialIndex = shared.currentIndex = shared.lastDropTarget = shared.isContainerModel = shared.targetModel = null;
        shared.dropElmModel = shared.targetCtx = shared.copy = shared.moveX = shared.moveY = null;
      }

      // is item currently placed in original container and original position?
      function isInitialPlacement(target, s) {
        var sibling = s || (shared.mirror ? shared.currentSibling : nextEl(shared.item));
        return target === shared.source && sibling === shared.initialSibling;
      }

      // find valid drop container
      function findDropTarget(elementBehindCursor, clientX, clientY) {
        var target = elementBehindCursor;

        while (target && !accepted()) {
          target = target.parentElement;
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
                shared.targetCtx = shared.containersCtx['dragularCommon'][shared.containers['dragularCommon'].indexOf(target)];
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

      function drag(e) {
        if (!shared.mirror) {
          return;
        }
        e = e || window.event;

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
          fireEvent(elementBehindCursor, shared.dragOverEvents['dragularenter'], !!dropTarget);
          if (shared.lastElementBehindCursor) {
            fireEvent(shared.lastElementBehindCursor, shared.dragOverEvents['dragularleave'], elementBehindCursor);
          }
          shared.lastElementBehindCursor = elementBehindCursor;
        }

        if (changed) {
          out();
          shared.lastDropTarget = dropTarget;
          over();
        }

        // do not copy in same container
        if (dropTarget === shared.source && shared.copy && !o.copySortSource) {
          if (shared.item.parentElement) {
            shared.item.parentElement.removeChild(shared.item);
          }
          return;
        }

        var reference,
          immediate = getImmediateChild(dropTarget, elementBehindCursor);

        if (immediate !== null) {
          reference = getReference(dropTarget, immediate, shared.clientX, shared.clientY);
        } else if (o.revertOnSpill === true && !shared.copy) {
          // the case that mirror is not over valid target and reverting is on and copy is off
          reference = shared.initialSibling;
          dropTarget = shared.source;
        } else {
          // the case that mirror is not over valid target and removing is on or copy is on
          if (shared.copy && shared.item.parentElement !== null) {
            // remove item or copy of item
            shared.item.parentElement.removeChild(shared.item);
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
            o.scope.$emit('dragularshadow', shared.item, dropTarget);
          }
        }

        function moved(type) {
          if (o.scope) {
            o.scope.$emit('dragular' + type, shared.item, shared.lastDropTarget, shared.source);
          }
          if (o.removeOnSpill === true) {
            type === 'over' ? spillOver() : spillOut();
          }
        }

        function over() {
          if (changed) {
            moved('over');
          }
        }

        function out() {
          if (shared.lastDropTarget) {
            moved('out');
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
        rmClass(shared.mirror, o.classes.transit);
        addClass(shared.mirror, o.classes.mirror);
        o.mirrorContainer.appendChild(shared.mirror);
        regEvent(documentElement, 'on', 'mousemove', drag);
        addClass(body, o.classes.unselectable);
        regEvent(shared.mirror, 'on', 'wheel', scrollContainer);
        if (o.scope) {
          o.scope.$emit('dragularcloned', shared.mirror, shared.sourceItem);
        }
      }

      function removeMirrorImage() {
        if (shared.mirror) {
          rmClass(body, o.classes.unselectable);
          regEvent(documentElement, 'off', 'mousemove', drag);
          regEvent(shared.mirror, 'off', 'wheel', scrollContainer);
          shared.mirror.parentElement.removeChild(shared.mirror);
          shared.mirror = null;
        }
      }

      function getImmediateChild(dropTarget, target) {
        var immediate = target;
        while (immediate !== dropTarget && immediate.parentElement !== dropTarget) {
          immediate = immediate.parentElement;
        }
        if (immediate === documentElement) {
          return null;
        }
        return immediate;
      }

      function getReference(dropTarget, target, x, y) {
        var horizontal = o.direction === 'horizontal',
          reference = target !== dropTarget ? inside() : outside();
        return reference;

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

      function isCopy(item, container) {
        return typeof o.copy === 'boolean' ? o.copy : o.copy(item, container);
      }

      function getScroll(scrollProp, offsetProp) {
        if (typeof window[offsetProp] !== 'undefined') {
          return window[offsetProp];
        }
        if (documentElement.clientHeight) {
          return documentElement[scrollProp];
        }
        return body[scrollProp];
      }

      function getOffset(el) {
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

      function getElementBehindPoint(point, x, y) {
        var p = point || {},
          state = p.className,
          el;
        p.className += ' ' + o.classes.hide;
        el = document.elementFromPoint(x, y);
        p.className = state;
        return el;
      }
    };

  // clean common/shared objects
  serviceFn.cleanEnviroment = function cleanEnviroment() {
    shared.classesCache = {};
    shared.containersCtx = {};
    shared.containers = {};
    shared.mirror = undefined;
  };

  serviceFn.shared = shared;

  return serviceFn;

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
      $el = angular.element(el);

    if (touch[type]) {
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

  function getRectWidth(rect) {
    return rect.width || (rect.right - rect.left);
  }

  function getRectHeight(rect) {
    return rect.height || (rect.bottom - rect.top);
  }

  function emptyObj() {
    return {};
  }

  function nextEl(el) {
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
  function isElement(o) {
    return (
      typeof HTMLElement === 'object' ? o instanceof HTMLElement : //DOM2
      o && typeof o === 'object' && o !== null && o.nodeType === 1 && typeof o.nodeName === 'string'
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

  function getCoord(coord, e) {
    var host = getEventHost(e);
    var missMap = {
      pageX: 'clientX', // IE8
      pageY: 'clientY' // IE8
    };
    if (coord in missMap && !(coord in host) && missMap[coord] in host) {
      coord = missMap[coord];
    }

    // Adding support for touch events, as they are not functional in the original
    if (host.type.indexOf('touch') < 0) {
      return host[coord];
    } else {
      if (host.type.indexOf('end') > -1) {
        // Nothing should happen for touchend
        return;
      } else {
        // No clientX or clientY in a touch event
        return host.originalEvent.touches[0][coord.replace('client', 'page')];
      }
    }
  }

  function whichMouseButton (e) {
    if (e.buttons !== undefined) { return e.buttons; }
    if (e.which !== undefined) { return e.which; }
    var button = e.button;
    if (button !== undefined) { // see https://github.com/jquery/jquery/blob/99e8ff1baa7ae341e94bb89c3e84570c7c3ad9ea/src/event.js#L573-L575
      return button & 1 ? 1 : button & 2 ? 3 : (button & 4 ? 2 : 0);
    }
  }

  function isInput (el) {
    return el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT';
  }

  function domIndexOf(child, parent) {
    return Array.prototype.indexOf.call(angular.element(parent).children(), child);
  }

  function fireEvent(target, e, extra) {
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

}]);

},{"./dragularModule":2}]},{},[2]);
