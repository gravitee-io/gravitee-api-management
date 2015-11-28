(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('Basic', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService('.containerVertical');
  }]);
},{"../examplesApp":25}],2:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BasicModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items1 = [{
      content: 'Move me, but you can only drop me in one of these containers.'
    }, {
      content: 'If you try to drop me somewhere other than these containers, I\'ll just come back.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.items2 = [{
      content: 'Item 5'
    }, {
      content: 'Item 6'
    }, {
      content: 'Item 7'
    }, {
      content: 'Item 8'
    }];
    var containers = $element.children().eq(0).children();
    dragularService.cleanEnviroment();
    dragularService([containers[0],containers[1]],{
      containersModel: [$scope.items1, $scope.items2]
    });
  }]);

},{"../examplesApp":25}],3:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BoundingBox', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    var boundingBox = $element[0];
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      boundingBox: boundingBox
    });
  }]);

},{"../examplesApp":25}],4:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BoundingBoxLockX', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    var boundingBox = $element.children().children()[0];
    dragularService.cleanEnviroment();
	dragularService(boundingBox, {
      boundingBox: boundingBox,
      lockX: true
    });
  }]);

},{"../examplesApp":25}],5:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BoundingBoxLockY', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    var boundingBox = $element.children().children()[0];
    dragularService.cleanEnviroment();
	dragularService(boundingBox, {
      boundingBox: boundingBox,
      lockY: true
    });
  }]);
},{"../examplesApp":25}],6:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('Copy', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      copy: true
    });
  }]);

},{"../examplesApp":25}],7:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('CopyModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items1 = [{
      content: 'Move me, and make copy on drop.'
    }, {
      content: 'If you try to drop me somewhere other than these containers, I\'ll just come back.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.items2 = [{
      content: 'Item 5'
    }, {
      content: 'Item 6'
    }, {
      content: 'Item 7'
    }, {
      content: 'Item 8'
    }];
    var containers = $element.children().eq(0).children();
    dragularService.cleanEnviroment();
    dragularService([containers[0],containers[1]],{
      containersModel: [$scope.items1, $scope.items2],
      copy: true
    });
  }]);

},{"../examplesApp":25}],8:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('CustomClasses', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      classes: {
        mirror: 'custom-green-mirror'
      }
    });
  }]);

},{"../examplesApp":25}],9:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('DifferentOptionsModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items1 = [{
      content: 'Move me, but you can only drop me in one of these containers.'
    }, {
      content: 'If you try to drop me somewhere other than these containers, I\'ll just come back.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.items2 = [{
      content: 'Item 5'
    }, {
      content: 'Item 6'
    }, {
      content: 'Item 7'
    }, {
      content: 'Item 8'
    }];

    var containerLeft = document.querySelector('#containerLeft'),
      containerRight = document.querySelector('#containerRight');

    function accepts(el, target, source) {
      // left->right || in same container
      if (source === containerLeft || source === target) {
        return true;
      }
    }

    dragularService.cleanEnviroment();
    dragularService([containerLeft], {
      containersModel: [$scope.items1],
      copy: true,
      copySortSource: true,
      //move only from left to right
      accepts: accepts
    });

    dragularService([containerRight], {
      containersModel: [$scope.items2],
      removeOnSpill: true,
      //move only from left to right
      accepts: accepts
    });

  }]);

},{"../examplesApp":25}],10:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('Directive', ['$scope', function DirectiveCtrl($scope) {
    $scope.dragularOptions = {
      classes: {
        mirror: 'custom-green-mirror'
      },
      nameSpace: 'same' // just connecting left and right container
    };
  }]);
},{"../examplesApp":25}],11:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('DirectiveModel', ['$scope', function TodoCtrl($scope) {
    $scope.items1 = [{
      content: 'Move me, and make copy on drop.'
    }, {
      content: 'If you try to drop me somewhere other than these containers, I\'ll just come back.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.items2 = [{
      content: 'Item 5'
    }, {
      content: 'Item 6'
    }, {
      content: 'Item 7'
    }, {
      content: 'Item 8'
    }];
    $scope.dragularOptions = {
      containersModel: $scope.items1,
      classes: {
        mirror: 'custom-green-mirror'
      },
      nameSpace: 'common' // just connecting left and right container
    };
  }]);

},{"../examplesApp":25}],12:[function(require,module,exports){
/* global angular */
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('DragOverEvents', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
    dragularService([$element.children()[0], $element.children()[2]], {
      nameSpace: 'apples'
    });
    dragularService([$element.children()[1], $element.children()[3]], {
      nameSpace: 'oranges'
    });

    // containers events handling
    function registerEvents(el) {
      el.on('dragularenter', function(e) {
        if (el[0] === e.target) { // filter bubbled
          el.addClass(dragularService.shared.extra ? 'gu-over-accept' : 'gu-over-decline');
        }
      });
      el.on('dragularleave dragularrelease', function(e) {
        if ((el[0] === e.target && // filter bubbled
          dragularService.shared.extra && // extra on dragleave contains element the drag is leaving to
          dragularService.shared.extra.parentElement !== e.target) || // is that element child of this container?
          e.type === 'dragularrelease') {
          el.removeClass('gu-over-accept');
          el.removeClass('gu-over-decline');
        }
      });
    }

    angular.forEach($element.children(), function forEachChild(el) {
      registerEvents(angular.element(el));
    });

    // notContainer events handling
    var notContainer = angular.element(document.getElementsByClassName('notContainer'));
    notContainer.on('dragularenter', function() {
      notContainer.addClass('gu-over');
    });
    notContainer.on('dragularleave dragularrelease', function() {
      notContainer.removeClass('gu-over');
    });
  }]);

},{"../examplesApp":25}],13:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('Events', ['$scope', '$element', 'dragularService', '$timeout', function TodoCtrl($scope, $element, dragularService, $timeout) {
    dragularService.cleanEnviroment();
    dragularService($element.children(), {
      scope: $scope
    });
    $scope.$on('dragulardrag', function(e, el) {
      e.stopPropagation();
      el.className = el.className.replace(' ex-moved', '');
    });
    $scope.$on('dragulardrop', function(e, el) {
      e.stopPropagation();
      $timeout(function() {
        el.className += ' ex-moved';
      }, 0);
    });

    $scope.$on('dragularcloned', myFn('cloned'));
    $scope.$on('dragulardrag', myFn('drag'));
    $scope.$on('dragularcancel', myFn('cancel'));
    $scope.$on('dragulardrop', myFn('drop'));
    $scope.$on('dragularremove', myFn('remove'));
    $scope.$on('dragulardragend', myFn('dragend'));
    $scope.$on('dragularshadow', myFn('shadow'));

    function myFn(eventName) {
      return function() {
        console.log(eventName, arguments);
      };
    }

  }]);

},{"../examplesApp":25}],14:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('Handle', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      moves: function(el, container, handle) {
        return handle.className === 'handle';
      }
    });
  }]);

},{"../examplesApp":25}],15:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('IsContainerModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items1 = [{
      content: 'Move me, but you can only drop me in one of these containers.'
    }, {
      content: 'If you try to drop me somewhere other than these containers, I\'ll just come back.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.cartModel = [];

    var containerLeft = document.querySelector('#containerLeft');

    dragularService.cleanEnviroment();
    dragularService([containerLeft], {
      containersModel: [$scope.items1],
      copy: true,
      isContainer: function isContainer (el) {
        return el.id === 'cart';
      },
      isContainerModel: function getModel (){
        return $scope.cartModel;
      }
    });

    $scope.removeItem = function removeItem() {
      var index = $scope.cartModel.indexOf(this.item);
      $scope.cartModel.splice(index, 1);
    };

  }]);

},{"../examplesApp":25}],16:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('NameSpaces', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
    dragularService([$element.children()[0], $element.children()[2]], {
      nameSpace: 'apples'
    });
    dragularService($element.children()[1], {
      nameSpace: 'oranges'
    });
    dragularService($element.children()[3], { // mixed
      nameSpace: ['oranges', 'apples']
    });
  }]);

},{"../examplesApp":25}],17:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('NestedNgRepeat', ['$timeout', '$scope', '$element', 'dragularService', function NestedNgRepeatCtrl($timeout, $scope, $element, dragularService) {
    $timeout(function() { // timeount due to ngRepeat to be ready
      dragularService.cleanEnviroment();
      dragularService($element, {
        nameSpace: 'rows',
        moves: function rowOnly (el, container, handle) {
          return handle.classList.contains('row-handle');
        }
      });

      dragularService($element.children(), {
        nameSpace: 'cells',
        moves: function excludeHandle (el, container, handle) {
          return !handle.classList.contains('row-handle');
        }
      });
    }, 0);
    $scope.items = [{
      items: [{
        content: 'Item a1'
      }, {
        content: 'Item a2'
      }, {
        content: 'Item a3'
      }, {
        content: 'Item a4'
      }]
    }, {
      items: [{
        content: 'Item b1'
      }, {
        content: 'Item b2'
      }, {
        content: 'Item b3'
      }, {
        content: 'Item b4'
      }]
    }, {
      items: [{
        content: 'Item c1'
      }, {
        content: 'Item c2'
      }, {
        content: 'Item c3'
      }, {
        content: 'Item c4'
      }]
    }];
  }]);

},{"../examplesApp":25}],18:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('NestedNgRepeatWithModel', ['$timeout', '$scope', '$element', 'dragularService', function NestedNgRepeatWithModelCtrl($timeout, $scope, $element, dragularService) {
    $timeout(function() { // timeount due to nested ngRepeat to be ready
      var container = $element.children().eq(0).children(),
        parentContainers = container.children(),
        nestedContainers = [];

      dragularService.cleanEnviroment();
      dragularService(container, {
        moves: function(el, container, handle) {
          return handle.classList.contains('row-handle');
        },
        containersModel: $scope.items,
        nameSpace: 'rows'
      });

      // collect nested contianers
      for (var i = 0; i < parentContainers.length; i++) {
        nestedContainers.push(parentContainers.eq(i).children()[1]);
      }
    
      dragularService(nestedContainers, {
        moves: function(el, container, handle) {
          return !handle.classList.contains('row-handle');
        },
        containersModel: (function getNestedContainersModel(){
          var parent = $scope.items,
            containersModel = [];
          for (var i = 0; i < parent.length; i++) {
            containersModel.push(parent[i].items);
          }
          return containersModel;
        })(),
        nameSpace: 'cells'
      });
    }, 0);
    $scope.items = [{
      items: [{
        content: 'Item a1'
      }, {
        content: 'Item a2'
      }, {
        content: 'Item a3'
      }, {
        content: 'Item a4'
      }]
    }, {
      items: [{
        content: 'Item b1'
      }, {
        content: 'Item b2'
      }, {
        content: 'Item b3'
      }, {
        content: 'Item b4'
      }]
    }, {
      items: [{
        content: 'Item c1'
      }, {
        content: 'Item c2'
      }, {
        content: 'Item c3'
      }, {
        content: 'Item c4'
      }]
    }];
  }]);

},{"../examplesApp":25}],19:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('NgRepeat', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    dragularService.cleanEnviroment();
    dragularService($element.children());
    $scope.items = [{
      content: 'Try to add or remove some elements (click on +- buttons), you will see that it is not problem for dragular.'
    }, {
      content: 'Item 2'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.addItem = function addItem() {
      var index = $scope.items.indexOf(this.item) + 1;
      $scope.items.splice(index, 0, {
        content: this.item.content + '-copy'
      });
    };
    $scope.removeItem = function removeItem() {
      var index = $scope.items.indexOf(this.item);
      $scope.items.splice(index, 1);
    };
  }]);

},{"../examplesApp":25}],20:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('NgRepeatWithModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items = [{
      content: 'Try to add or remove some elements (click on +- buttons), you will see that it is not problem for dragular.'
    }, {
      content: 'Item 2'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    dragularService.cleanEnviroment();
    dragularService($element.children().eq(0).children(), {containersModel: $scope.items});
    $scope.addItem = function addItem() {
      var index = $scope.items.indexOf(this.item) + 1;
      $scope.items.splice(index, 0, {
        content: this.item.content + '-copy'
      });
    };
    $scope.removeItem = function removeItem() {
      var index = $scope.items.indexOf(this.item);
      $scope.items.splice(index, 1);
    };
  }]);

},{"../examplesApp":25}],21:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('RemoveOnSpill', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      removeOnSpill: true
    });
  }]);

},{"../examplesApp":25}],22:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('RemoveOnSpillWithModel', ['$scope', '$element', 'dragularService', function TodoCtrl($scope, $element, dragularService) {
    $scope.items1 = [{
      content: 'Move me, but you can only drop me in containers.'
    }, {
      content: 'If you try to drop me somewhere other than containers, I\'ll die a fiery death.'
    }, {
      content: 'Item 3'
    }, {
      content: 'Item 4'
    }];
    $scope.items2 = [{
      content: 'You can drop me in the left container.'
    }, {
      content: 'Item 6'
    }, {
      content: 'Item 7'
    }, {
      content: 'Item 8'
    }];
    var containers = $element.children().eq(0).children();
    dragularService.cleanEnviroment();
    dragularService([containers[0], containers[1]], {
      containersModel: [$scope.items1, $scope.items2],
      removeOnSpill: true
    });
  }]);

},{"../examplesApp":25}],23:[function(require,module,exports){
'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('RevertOnSpill', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      revertOnSpill: true
    });
  }]);

},{"../examplesApp":25}],24:[function(require,module,exports){
/* global angular */
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('ScrollingDrag', ['$interval', '$element', 'dragularService', function TodoCtrl($interval, $element, dragularService) {


    var timer,
      leftScrollContainer = document.getElementById('leftScroll'),
      rightScrollContainer = document.getElementById('rightScroll'),
      leftTopBar = document.getElementById('leftTopBar'),
      leftBottomBar = document.getElementById('leftBottomBar'),
      rightTopBar = document.getElementById('rightTopBar'),
      rightBottomBar = document.getElementById('rightBottomBar');

    dragularService.cleanEnviroment();
    dragularService([leftScrollContainer, rightScrollContainer]);

    registerEvents(leftTopBar, leftScrollContainer, -5);
    registerEvents(leftBottomBar, leftScrollContainer, 5);
    registerEvents(rightTopBar, rightScrollContainer, -5);
    registerEvents(rightBottomBar, rightScrollContainer, 5);

    function registerEvents(bar, container, inc, speed) {
      if (!speed) {
        speed = 20;
      }
      angular.element(bar).on('dragularenter', function() {
        container.scrollTop += inc;
        timer = $interval(function moveScroll() {
          container.scrollTop += inc;
        }, speed);
      });
      angular.element(bar).on('dragularleave dragularrelease', function() {
        $interval.cancel(timer);
      });
    }
  }]);

},{"../examplesApp":25}],25:[function(require,module,exports){
/* global angular, hljs */
'use strict';

// var angular = require('angular');


require('../../../src/dragularModule');
require('./templates');

/**
 *  Module Example App
 *
 *  DEMO app for dragular https://github.com/luckylooke/dragular
 */

module.exports = angular.module('examplesApp', ['dragularModule', 'templates', 'ui.router']).controller('ExAppCtrl', ['$scope', function($scope) {
    $scope.examplesList = [{
        template: 'docsInstall/docsInstall.html',
        link: 'docsInstall',
        title: 'Installation'
    },{
        template: 'exampleBasic/exampleBasic.html',
        link: 'exampleBasic',
        title: 'Basic'
    },{
        template: 'exampleBasicWithModel/exampleBasicWithModel.html',
        link: 'exampleBasicWithModel',
        title: 'Basic - with model'
    },{
        template: 'exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel.html',
        link: 'exampleDifferentOptionsWithModel',
        title: 'Different options - with model'
    }, {
        template: 'exampleDirective/exampleDirective.html',
        link: 'exampleDirective',
        title: 'Directive'
    }, {
        template: 'exampleDirectiveWithModel/exampleDirectiveWithModel.html',
        link: 'exampleDirectiveWithModel',
        title: 'Directive - with model'
    }, {
        template: 'exampleEvents/exampleEvents.html',
        link: 'exampleEvents',
        title: 'Events'
    }, {
        template: 'exampleRemoveOnSpill/exampleRemoveOnSpill.html',
        link: 'exampleRemoveOnSpill',
        title: 'Remove on spill'
    }, {
        template: 'exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel.html',
        link: 'exampleRemoveOnSpillWithModel',
        title: 'Remove on spill - with model'
    }, {
        template: 'exampleRevertOnSpill/exampleRevertOnSpill.html',
        link: 'exampleRevertOnSpill',
        title: 'Revert on spill'
    }, {
        template: 'exampleCopy/exampleCopy.html',
        link: 'exampleCopy',
        title: 'Copy'
    }, {
        template: 'exampleCopyWithModel/exampleCopyWithModel.html',
        link: 'exampleCopyWithModel',
        title: 'Copy - with model'
    }, {
        template: 'exampleHandle/exampleHandle.html',
        link: 'exampleHandle',
        title: 'Handle'
    }, {
        template: 'exampleIsContainerWithModel/exampleIsContainerWithModel.html',
        link: 'exampleIsContainerWithModel',
        title: 'isContainer - with model'
    }, {
        template: 'exampleCustomClasses/exampleCustomClasses.html',
        link: 'exampleCustomClasses',
        title: 'Custom classes'
    }, {
        template: 'exampleNameSpaces/exampleNameSpaces.html',
        link: 'exampleNameSpaces',
        title: 'NameSpaces'
    }, {
        template: 'exampleDragOverEvents/exampleDragOverEvents.html',
        link: 'exampleDragOverEvents',
        title: 'Drag-over events'
    }, {
        template: 'exampleBoundingBox/exampleBoundingBox.html',
        link: 'exampleBoundingBox',
        title: 'BoundingBox'
    }, {
        template: 'exampleBoundingBoxLockX/exampleBoundingBoxLockX.html',
        link: 'exampleBoundingBoxLockX',
        title: 'BoundingBox + LockX'
    }, {
        template: 'exampleBoundingBoxLockY/exampleBoundingBoxLockY.html',
        link: 'exampleBoundingBoxLockY',
        title: 'BoundingBox + LockY'
    }, {
        template: 'exampleNgRepeat/exampleNgRepeat.html',
        link: 'exampleNgRepeat',
        title: 'ngRepeat'
    }, {
        template: 'exampleNgRepeatWithModel/exampleNgRepeatWithModel.html',
        link: 'exampleNgRepeatWithModel',
        title: 'ngRepeat - with model'
    }, {
        template: 'exampleNestedNgRepeat/exampleNestedNgRepeat.html',
        link: 'exampleNestedNgRepeat',
        title: 'Nested ngRepead'
    }, {
        template: 'exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel.html',
        link: 'exampleNestedNgRepeatWithModel',
        title: 'Nested ngRepead - with model'
    }, {
        template: 'exampleScrollingDrag/exampleScrollingDrag.html',
        link: 'exampleScrollingDrag',
        title: 'Scrolling drag'
    }];

    $scope.highlightCode = function () {
        if(document.getElementsByTagName('code').length){
            var codeBlocks = document.getElementsByTagName('code');
            for (var i = codeBlocks.length - 1; i >= 0; i--) {
                hljs.highlightBlock(codeBlocks[i]);
            }
        }
    };

    var rowOffcanvas;
    $scope.toggleSidebar = function toggleSidebar () {
        if(!rowOffcanvas){
            rowOffcanvas = angular.element(document.getElementById('rowOffcanvas'));
        }
        rowOffcanvas.toggleClass('active');
    };

}]);

({"exampleBasic":({"exampleBasic":require("./exampleBasic/exampleBasic.js")}),"exampleBasicWithModel":({"exampleBasicWithModel":require("./exampleBasicWithModel/exampleBasicWithModel.js")}),"exampleBoundingBox":({"exampleBoundingBox":require("./exampleBoundingBox/exampleBoundingBox.js")}),"exampleBoundingBoxLockX":({"exampleBoundingBoxLockX":require("./exampleBoundingBoxLockX/exampleBoundingBoxLockX.js")}),"exampleBoundingBoxLockY":({"exampleBoundingBoxLockY":require("./exampleBoundingBoxLockY/exampleBoundingBoxLockY.js")}),"exampleCopy":({"exampleCopy":require("./exampleCopy/exampleCopy.js")}),"exampleCopyWithModel":({"exampleCopyWithModel":require("./exampleCopyWithModel/exampleCopyWithModel.js")}),"exampleCustomClasses":({"exampleCustomClasses":require("./exampleCustomClasses/exampleCustomClasses.js")}),"exampleDifferentOptionsWithModel":({"exampleDifferentOptionsWithModel":require("./exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel.js")}),"exampleDirective":({"exampleDirective":require("./exampleDirective/exampleDirective.js")}),"exampleDirectiveWithModel":({"exampleDirectiveWithModel":require("./exampleDirectiveWithModel/exampleDirectiveWithModel.js")}),"exampleDragOverEvents":({"exampleDragOverEvents":require("./exampleDragOverEvents/exampleDragOverEvents.js")}),"exampleEvents":({"exampleEvents":require("./exampleEvents/exampleEvents.js")}),"exampleHandle":({"exampleHandle":require("./exampleHandle/exampleHandle.js")}),"exampleIsContainerWithModel":({"exampleIsContainerWithModel":require("./exampleIsContainerWithModel/exampleIsContainerWithModel.js")}),"exampleNameSpaces":({"exampleNameSpaces":require("./exampleNameSpaces/exampleNameSpaces.js")}),"exampleNestedNgRepeat":({"exampleNestedNgRepeat":require("./exampleNestedNgRepeat/exampleNestedNgRepeat.js")}),"exampleNestedNgRepeatWithModel":({"exampleNestedNgRepeatWithModel":require("./exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel.js")}),"exampleNgRepeat":({"exampleNgRepeat":require("./exampleNgRepeat/exampleNgRepeat.js")}),"exampleNgRepeatWithModel":({"exampleNgRepeatWithModel":require("./exampleNgRepeatWithModel/exampleNgRepeatWithModel.js")}),"exampleRemoveOnSpill":({"exampleRemoveOnSpill":require("./exampleRemoveOnSpill/exampleRemoveOnSpill.js")}),"exampleRemoveOnSpillWithModel":({"exampleRemoveOnSpillWithModel":require("./exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel.js")}),"exampleRevertOnSpill":({"exampleRevertOnSpill":require("./exampleRevertOnSpill/exampleRevertOnSpill.js")}),"exampleScrollingDrag":({"exampleScrollingDrag":require("./exampleScrollingDrag/exampleScrollingDrag.js")}),"examplesRouter":require("./examplesRouter.js"),"templates":require("./templates.js")});

},{"../../../src/dragularModule":29,"./exampleBasic/exampleBasic.js":1,"./exampleBasicWithModel/exampleBasicWithModel.js":2,"./exampleBoundingBox/exampleBoundingBox.js":3,"./exampleBoundingBoxLockX/exampleBoundingBoxLockX.js":4,"./exampleBoundingBoxLockY/exampleBoundingBoxLockY.js":5,"./exampleCopy/exampleCopy.js":6,"./exampleCopyWithModel/exampleCopyWithModel.js":7,"./exampleCustomClasses/exampleCustomClasses.js":8,"./exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel.js":9,"./exampleDirective/exampleDirective.js":10,"./exampleDirectiveWithModel/exampleDirectiveWithModel.js":11,"./exampleDragOverEvents/exampleDragOverEvents.js":12,"./exampleEvents/exampleEvents.js":13,"./exampleHandle/exampleHandle.js":14,"./exampleIsContainerWithModel/exampleIsContainerWithModel.js":15,"./exampleNameSpaces/exampleNameSpaces.js":16,"./exampleNestedNgRepeat/exampleNestedNgRepeat.js":17,"./exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel.js":18,"./exampleNgRepeat/exampleNgRepeat.js":19,"./exampleNgRepeatWithModel/exampleNgRepeatWithModel.js":20,"./exampleRemoveOnSpill/exampleRemoveOnSpill.js":21,"./exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel.js":22,"./exampleRevertOnSpill/exampleRevertOnSpill.js":23,"./exampleScrollingDrag/exampleScrollingDrag.js":24,"./examplesRouter.js":26,"./templates":27,"./templates.js":27}],26:[function(require,module,exports){
'use strict';

var examplesAppModule = require('./examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .config(["$stateProvider", "$urlRouterProvider", function($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise('/home');

    var timer,
      ctrl = function routerCtrl($state, $stateParams, $timeout) {
        // go to install notes by default
        if (!$stateParams.link) {
          timer = $timeout(function timer() {
            $state.go('docs.detail', {
              link: 'docsInstall'
            });
          },0);
        }else{
          $timeout.cancel(timer);
        }
      };
      ctrl.$inject = ["$state", "$stateParams", "$timeout"];

    $stateProvider
      .state('home', {
        url: '/home',
        templateUrl: 'partials/partial-home.html'
      })
      .state('docs', {
        url: '/docs',
        templateUrl: 'partials/partial-docs.html',
        controller: ctrl
      })
      .state('docs.detail', {
        url: '/:link',
        templateUrl: function($stateParams) {
          return $stateParams.link + '/' + $stateParams.link + '.html';
        },
        controller: ctrl
      })
      .state('contribute', {
        url: '/contribute',
        templateUrl: 'partials/partial-contribute.html'
      });
  }]);

},{"./examplesApp":25}],27:[function(require,module,exports){
'use strict'; module.exports = angular.module("templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("docsInstall/docsInstall.html","<h2>Install</h2>\n<p>download dragular.js and dragular.css from dist folder</p>\n<p>OR clone git</p>\n<pre><code>\ngit clone http://github.com/luckylooke/dragular.git\n</code></pre>\n<p>OR use npm</p>\n<pre><code>\n[sudo] npm install dragular\n</code></pre>\n<p>OR use bower</p>\n<pre><code>\nbower install dragular\n</code></pre>\n<p>AND include files into your project</p>\n<pre><code>\n&lt;link href=\'styles/dragular.css\' rel=\'stylesheet\' type=\'text/css\' /&gt;\n&lt;script src=\'scripts/dragular.js\'&gt;&lt;/script&gt;\n</code></pre>\n<p>AND put dragularModule into dependency array</p>\n<pre><code>\nvar app = angular.module(\'myApp\', [\'dragularModule\', \'otherDependencies\']);\n</code></pre>\n<p>DONE :)</p>\n");
$templateCache.put("exampleBasic/exampleBasic.html","<div class=\'parent\'>\n  <h2>Basic</h2>\n  <label for=\'hy\'>Move stuff between these two containers. Note how the stuff gets inserted near the mouse pointer? Great stuff.</label>\n  <div class=\'wrapper\' ng-controller=\"Basic\">\n    <div class=\'containerVertical\'>\n      <div>Move me, but you can only drop me in one of these containers.</div>\n      <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n      <div>Item 3.</div>\n      <div>Item 6.</div>\n    </div>\n    <div class=\'containerVertical\'>\n      <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n      <div ng-click=\"clicked = !clicked\" ng-class=\"clicked && \'clickedClass\'\">Try to click me, dragular distinguish drag from click</div>\n      <div>Item 5.</div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  controller(\'Basic\', [\'$element\', \'dragularService\', function TodoCtrl($element, dragularService) {\n    dragularService(\'.containerVertical\');\n  }])\n        </code>\n        <code>\n// CSS\n.clickedClass {\n  background-color: orange !important;\n}\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n  &lt;div class=\'wrapper\' ng-controller=&quot;Basic&quot;&gt;\n    &lt;div class=\'containerVertical\'&gt;\n        &lt;div&gt;Move me, but you can only drop me in one of these containers.&lt;/div&gt;\n        &lt;div&gt;If you try to drop me somewhere other than these containers, I\'ll just come back.&lt;/div&gt;\n        &lt;div&gt;Item 3.&lt;/div&gt;\n        &lt;div&gt;Item 6.&lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=\'containerVertical\'&gt;\n        &lt;div&gt;You can drop me in the left container, otherwise I\'ll stay here.&lt;/div&gt;\n        &lt;div ng-click=\"clicked = !clicked\" ng-class=\"clicked && \'clickedClass\'\"&gt;Try to click me, dragular distinguish drag from click&lt;/div&gt;\n        &lt;div&gt;Item 5.&lt;/div&gt;\n    &lt;/div&gt;\n&lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleBasicWithModel/exampleBasicWithModel.html","<div class=\'parent\'>\n  <h2>Basic - with model</h2>\n  <label for=\'hy\'>Move stuff between these two containers. Note how the stuff gets inserted near the mouse pointer? Great stuff.</label>\n  <div class=\'wrapper\' ng-controller=\"BasicModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items2\">{{item.content}}</div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <pre>Items1:\n          <br/>{{items1 | json}}</pre>\n      </div>\n      <div class=\'containerVertical\'>\n        <pre>Items2:\n          <br/>{{items2 | json}}</pre>\n      </div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  controller(\'BasicModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items1 = [{\n      content: \'Move me, but you can only drop me in one of these containers.\'\n    }, {\n      content: \'If you try to drop me somewhere other than these containers, I\\\'ll just come back.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.items2 = [{\n      content: \'Item 5\'\n    }, {\n      content: \'Item 6\'\n    }, {\n      content: \'Item 7\'\n    }, {\n      content: \'Item 8\'\n    }];\n    var containers = $element.children().children();\n    dragularService([containers[0],containers[1]],{\n      containersModel: [$scope.items1, $scope.items2]\n    });\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;Basic&quot;&gt;\n    &lt;div class=\'tableRow\'&gt;\n        &lt;div class=\'containerVertical\'&gt;\n            &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n        &lt;/div&gt;\n        &lt;div class=\'containerVertical\'&gt;\n            &lt;div ng-repeat=&quot;item in items2&quot;&gt;{{item.content}}&lt;/div&gt;\n        &lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;tableRow&quot;&gt;\n        &lt;div class=&quot;container&quot;&gt;\n            &lt;div&gt;Items1:\n                &lt;br/&gt;{{items1 | json}}&lt;/div&gt;\n        &lt;/div&gt;\n        &lt;div class=&quot;container&quot;&gt;\n            &lt;div&gt;Items2:\n                &lt;br/&gt;{{items2 | json}}&lt;/div&gt;\n        &lt;/div&gt;\n    &lt;/div&gt;\n&lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleBoundingBox/exampleBoundingBox.html","        <div class=\'parent\'>\n            <h2>BoundingBox</h2>\n            <label for=\'hy\'>You can provide element in options.boundingBox to limit crossing element borders.</label>\n            <div class=\'wrapper\' ng-controller=\"BoundingBox\">\n                <div class=\'containerVertical\'>\n                    <div>This items cannot cross its example element, just try it your selves.</div>\n                    <div>Item 2.</div>\n                    <div>Item 3.</div>\n                    <div>Item 6.</div>\n                </div>\n                <div class=\'containerVertical\'>\n                    <div>This items cannot cross its example element, just try it your selves.</div>\n                    <div>Item 4.</div>\n                    <div>Item 5.</div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  dragularService([$element.children(), {\n    boundingBox: $element\n  });\n        </code>\n      </pre>\n        </div>");
$templateCache.put("exampleBoundingBoxLockX/exampleBoundingBoxLockX.html","<div class=\'parent\'>\n            <h2>BoundingBox and lockX</h2>\n            <label for=\'hy\'>Movement can be locked to X or Y axis and also you can provide element in options.boundingBox to limit crossing element borders.</label>\n            <div class=\'wrapper\' ng-controller=\"BoundingBoxLockX\">\n                <div class=\'containerHorizontal\'>\n                    <div class=\'boundingBox\'>\n                        <div class=\"width25\">Items are locked in X axis movement and cannot cross its closest parent div.boundingBox, just try it your selves.</div>\n                        <div class=\"width25\">item 2</div>\n                        <div class=\"width25\">item 3</div>\n                        <div class=\"width25\">item 4</div>\n                    </div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  dragularService([$element.children()[0].children(), {\n    boundingBox: $element.children()[0],\n    lockX: true\n  });\n        </code>\n      </pre>\n        </div>");
$templateCache.put("exampleBoundingBoxLockY/exampleBoundingBoxLockY.html","        <div class=\'parent\'>\n            <h2>BoundingBox and LockY</h2>\n            <label for=\'hy\'>Movement can be locked to X or Y axis and also you can provide element in options.boundingBox to limit crossing element borders.</label>\n            <div class=\'wrapper\' ng-controller=\"BoundingBoxLockY\">\n                <div class=\'containerVertical\'>\n                    <div class=\'boundingBox\'>\n                        <div>Items are locked in Y axis movement and cannot cross its closest parent div.boundingBox, just try it your selves.</div>\n                        <div>item 2</div>\n                        <div>item 3</div>\n                        <div>item 4</div>\n                        <div>item 5</div>\n                        <div>item 6</div>\n                    </div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  dragularService([$element.children()[0].children(), {\n    boundingBox: $element.children()[0],\n    lockY: true\n  });\n        </code>\n      </pre>\n        </div>");
$templateCache.put("exampleCopy/exampleCopy.html","<div class=\'parent\'>\n  <h2>Copy</h2>\n  <label for=\'hy\'>Copying stuff is great too.</label>\n  <div class=\'wrapper\' ng-controller=\"Copy\" ng-hide=\"globals.showModelExamples\">\n    <div id=\'left2\' class=\'containerVertical\'>\n      <div>Move me, and make copy on drop.</div>\n      <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n    </div>\n    <div id=\'right2\' class=\'containerVertical\'>\n      <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  controller(\'Copy\', [\'$element\', \'dragularService\', function TodoCtrl($element, dragularService) {\n    dragularService($element.children(), {\n      copy: true\n    });\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;Copy&quot; ng-hide=&quot;globals.showModelExamples&quot;&gt;\n    &lt;div id=\'left2\' class=\'containerVertical\'&gt;\n      &lt;div&gt;Move me, and make copy on drop.&lt;/div&gt;\n      &lt;div&gt;If you try to drop me somewhere other than these containers, I\'ll just come back.&lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div id=\'right2\' class=\'containerVertical\'&gt;\n      &lt;div&gt;You can drop me in the left container, otherwise I\'ll stay here.&lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleCopyWithModel/exampleCopyWithModel.html","<div class=\'parent\'>\n  <h2>Copy - with model</h2>\n  <label for=\'hy\'>Copying stuff is great too.</label>\n  <div class=\'wrapper\' ng-controller=\"CopyModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items2\">{{item.content}}</div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <div>Items1:\n          <br/>{{items1 | json}}</div>\n      </div>\n      <div class=\'containerVertical\'>\n        <div>Items2:\n          <br/>{{items2 | json}}</div>\n      </div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  controller(\'CopyModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items1 = [{\n      content: \'Move me, and make copy on drop.\'\n    }, {\n      content: \'If you try to drop me somewhere other than these containers, I\\\'ll just come back.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.items2 = [{\n      content: \'Item 5\'\n    }, {\n      content: \'Item 6\'\n    }, {\n      content: \'Item 7\'\n    }, {\n      content: \'Item 8\'\n    }];\n    var containers = $element.children().children();\n    dragularService([containers[0],containers[1]],{\n      containersModel: [$scope.items1, $scope.items2],\n      copy: true\n    });\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;CopyModel&quot; ng-show=&quot;globals.showModelExamples&quot;&gt;\n    &lt;div class=\'tableRow\'&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items2&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;tableRow&quot;&gt;\n      &lt;div class=&quot;container&quot;&gt;\n        &lt;div&gt;Items1:\n          &lt;br/&gt;{{items1 | json}}&lt;/div&gt;\n      &lt;/div&gt;\n      &lt;div class=&quot;container&quot;&gt;\n        &lt;div&gt;Items2:\n          &lt;br/&gt;{{items2 | json}}&lt;/div&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleCustomClasses/exampleCustomClasses.html","<div class=\'parent\'>\n    <h2>Custom classes</h2>\n    <label for=\'hy\'>You can overwrite buildin classes by providing classes names in object via options.classes.</label>\n    <div class=\'wrapper\' ng-controller=\"CustomClasses\">\n        <div id=\'left4\' class=\'containerVertical\'>\n            <div>Move me, but you can only drop me in one of these containers.</div>\n            <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n            <div>Item 3.</div>\n            <div>Item 6.</div>\n        </div>\n        <div id=\'right3\' class=\'containerVertical\'>\n            <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n            <div>Item 4.</div>\n            <div>Item 5.</div>\n        </div>\n    </div>\n    <pre>\n        <code>\n  dragularService([document.getElementById(left), document.getElementById(right)], { classes: {\n    mirror: \'custom-green-mirror\'\n  } });\n\n  // Default classes are:\n  option.classes = {\n    mirror: \'gu-mirror\',\n    hide: \'gu-hide\',\n    unselectable: \'gu-unselectable\',\n    transit: \'gu-transit\',\n    overActive: \'gu-over-active\',\n    overAccepts: \'gu-over-accept\',\n    overDeclines: \'gu-over-decline\'\n  };\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel.html","<div class=\'parent\'>\n  <h2>Different options - with model</h2>\n  <label for=\'hy\'>You may need to setup different options (rules) for each container. For such setup you need to initialize each container separately. <b>Do not initialize same container twice!</b> In example bellow you can copy from left to right, but not otherwise. And from right container items can be removed on spill.</label>\n  <div class=\'wrapper\' ng-controller=\"DifferentOptionsModel\">\n    <div class=\'tableRow\'>\n      <div id=\"containerLeft\" class=\'containerVertical\'>\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div id=\"containerRight\" class=\'containerVertical\'>\n        <div ng-repeat=\"item in items2\">{{item.content}}</div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <pre>Items1:\n          <br/>{{items1 | json}}</pre>\n      </div>\n      <div class=\'containerVertical\'>\n        <pre>Items2:\n          <br/>{{items2 | json}}</pre>\n      </div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  .controller(\'DifferentOptionsModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items1 = [{\n      content: \'Move me, but you can only drop me in one of these containers.\'\n    }, {\n      content: \'If you try to drop me somewhere other than these containers, I\\\'ll just come back.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.items2 = [{\n      content: \'Item 5\'\n    }, {\n      content: \'Item 6\'\n    }, {\n      content: \'Item 7\'\n    }, {\n      content: \'Item 8\'\n    }];\n\n    var containerLeft = document.querySelector(\'#containerLeft\'),\n      containerRight = document.querySelector(\'#containerRight\');\n\n    function accepts(el, target, source) {\n      if (source === containerLeft || source === target) {\n        return true;\n      }\n    }\n\n    dragularService([containerLeft], {\n      containersModel: [$scope.items1],\n      copy: true,\n      //move only from left to right  \n      accepts: accepts\n    });\n\n    dragularService([containerRight], {\n      containersModel: [$scope.items2],\n      removeOnSpill: true,\n      //move only from left to right  \n      accepts: accepts\n    });\n\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;DifferentOptionsModel&quot;&gt;\n    &lt;div class=\'tableRow\'&gt;\n      &lt;div id=&quot;containerLeft&quot; class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n      &lt;div id=&quot;containerRight&quot; class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items2&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;tableRow&quot;&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;pre&gt;Items1:\n          &lt;br/&gt;{{items1 | json}}&lt;/pre&gt;\n      &lt;/div&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;pre&gt;Items2:\n          &lt;br/&gt;{{items2 | json}}&lt;/pre&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleDirective/exampleDirective.html","<div class=\'parent\'>\n  <h2>Directive</h2>\n  <label for=\'hy\'>Same as custom classes example, but showing use of directive.</label>\n  <div class=\'wrapper\' ng-controller=\"Directive\">\n    <div class=\'containerVertical\' dragular=\"dragularOptions\">\n      <div>Move me, but you can only drop me in one of these containers.</div>\n      <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n      <div>Item 3.</div>\n      <div>Item 6.</div>\n    </div>\n    <div class=\'containerVertical\' dragular=\'{\"classes\":{\"mirror\":\"custom-green-mirror\"},\"nameSpace\":\"same\"}\'>\n      <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n      <div>Item 4.</div>\n      <div>Item 5.</div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  controller(\'Directive\', [\'$scope\', \'dragularService\', function TodoCtrl($scope) {\n    $scope.dragularOptions = {\n      classes: {\n        mirror: \'custom-green-mirror\'\n      },\n      nameSpace: \'common\' // just connecting left and right container\n    };\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;Directive&quot;&gt;\n    &lt;div class=\'containerVertical\' dragular=&quot;dragularOptions&quot;&gt;\n      &lt;div&gt;Move me, but you can only drop me in one of these containers.&lt;/div&gt;\n      &lt;div&gt;If you try to drop me somewhere other than these containers, I\'ll just come back.&lt;/div&gt;\n      &lt;div&gt;Item 3.&lt;/div&gt;\n      &lt;div&gt;Item 6.&lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=\'containerVertical\' dragular=\'{&quot;classes&quot;:{&quot;mirror&quot;:&quot;custom-green-mirror&quot;},&quot;nameSpace&quot;:&quot;same&quot;}\'&gt;\n      &lt;div&gt;You can drop me in the left container, otherwise I\'ll stay here.&lt;/div&gt;\n      &lt;div&gt;Item 4.&lt;/div&gt;\n      &lt;div&gt;Item 5.&lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleDirectiveWithModel/exampleDirectiveWithModel.html","<div class=\'parent\'>\n  <h2>Directive - with model</h2>\n  <label for=\'hy\'>Same as custom classes example, but showing use of directive.</label>\n  <div class=\'wrapper\' ng-controller=\"DirectiveModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\' dragular=\"dragularOptions\">\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div class=\'containerVertical\' dragular=\'{\"containersModel\":\"items2\",\"classes\":{\"mirror\":\"custom-green-mirror\"},\"nameSpace\":\"common\"}\'>\n        <div ng-repeat=\"item in items2\">{{item.content}}</div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <div>Items1:\n          <br/>{{items1 | json}}</div>\n      </div>\n      <div class=\'containerVertical\'>\n        <div>Items2:\n          <br/>{{items2 | json}}</div>\n      </div>\n    </div>\n  </div>\n  <pre>\n       \n        <code>\n// JS\n  controller(\'DirectiveModel\', [\'$scope\', function TodoCtrl($scope) {\n    $scope.items1 = [{\n      content: \'Move me, and make copy on drop.\'\n    }, {\n      content: \'If you try to drop me somewhere other than these containers, I\\\'ll just come back.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.items2 = [{\n      content: \'Item 5\'\n    }, {\n      content: \'Item 6\'\n    }, {\n      content: \'Item 7\'\n    }, {\n      content: \'Item 8\'\n    }];\n    $scope.dragularOptions = {\n      containersModel: $scope.items1,\n      classes: {\n        mirror: \'custom-green-mirror\'\n      },\n      nameSpace: \'common\' // just connecting left and right container\n    };\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n &lt;div class=\'wrapper\' ng-controller=&quot;DirectiveModel&quot;&gt;\n  &lt;div class=\'containerVertical\' dragular=&quot;dragularOptions&quot;&gt;\n    &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n  &lt;/div&gt;\n  &lt;div class=\'containerVertical\' dragular=\'{&quot;containersModel&quot;:&quot;items2&quot;,&quot;classes&quot;:{&quot;mirror&quot;:&quot;custom-green-mirror&quot;},&quot;nameSpace&quot;:&quot;common&quot;}\'&gt;\n    &lt;div ng-repeat=&quot;item in items2&quot;&gt;{{item.content}}&lt;/div&gt;\n  &lt;/div&gt;\n&lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleDragOverEvents/exampleDragOverEvents.html","<div class=\'parent\'>\n  <h2>Drag-over events</h2>\n  <p>You can interact with dragging element by litening to dragOver events. Usually you want to containers show wheather they accepts element or not, but you can use it anywhere. DragOver events are: dragenter, dragleave and dragrelease. On dragOver events dragularService reveals several useful properties.</p>\n\n<div class=\"table-responsive\">\n  <table class=\"table table-hover\">\n    <tr>\n      <td>dragularService.shared.item</td>\n      <td>item beeing dragged (it is copy of item if copy is enabled via options)</td>\n    </tr>\n    <tr>\n      <td>dragularService.shared.source</td>\n      <td>source container item is dragged from</td>\n    </tr>\n    <tr>\n      <td>dragularService.shared.sourceModel</td>\n      <td>source container model representation if model was porvided</td>\n    </tr>\n    <tr>\n      <td>dragularService.shared.initialIndex</td>\n      <td>original index of item, can be used to get item model from sourceModel</td>\n    </tr>\n    <tr>\n      <td>dragularService.shared.extra</td>\n      <td>contains accepting information (boolean) on dragenter, element drag is leaving to on dragleave and element behind the cursor on dragrelease</td>\n    </tr>\n  </table>\n</div>\n\n  <p> Try to drag over the not-container too.</p>\n  <div class=\'wrapper\' ng-controller=\"DragOverEvents\">\n    <div class=\'containerVertical width25\'>\n      <div>apples and oranges cannot be mixed</div>\n      <div>apple 2</div>\n      <div>apple 3</div>\n      <div>apple 4</div>\n    </div>\n    <div class=\'containerVertical width25\'>\n      <div>orange 1</div>\n      <div>orange 2</div>\n      <div>orange 3</div>\n      <div>orange 4</div>\n    </div>\n    <div class=\'containerVertical width25\'>\n      <div>apple 5</div>\n      <div>apple 6</div>\n      <div>apple 7</div>\n      <div>apple 8</div>\n    </div>\n    <div class=\'containerVertical width25\'>\n      <div>orange 5</div>\n      <div>orange 6</div>\n      <div>orange 7</div>\n      <div>orange 8</div>\n    </div>\n  </div>\n  <div class=\"notContainer gu-over-active\"> Test active class on NOT container.</div>\n\n  <pre>\n    <code>\n&lt;!-- HTML --&gt;\n  &lt;div class=\'wrapper\' ng-controller=&quot;DragOverEvents&quot;&gt;\n    &lt;div class=\'container width25\'&gt;\n      &lt;div&gt;apples and oranges cannot be mixed&lt;/div&gt;\n      &lt;div&gt;apple 2&lt;/div&gt;\n      ...\n    &lt;/div&gt;\n    &lt;div class=\'container width25\'&gt;\n      &lt;div&gt;orange 1&lt;/div&gt;\n      &lt;div&gt;orange 2&lt;/div&gt;\n      ...\n    &lt;/div&gt;\n    &lt;div class=\'container width25\'&gt;\n      &lt;div&gt;apple 5&lt;/div&gt;\n      &lt;div&gt;apple 6&lt;/div&gt;\n      ...\n    &lt;/div&gt;\n    &lt;div class=\'container width25\'&gt;\n      &lt;div&gt;orange 5&lt;/div&gt;\n      &lt;div&gt;orange 6&lt;/div&gt;\n      ...\n    &lt;/div&gt;\n  &lt;/div&gt;\n  &lt;div class=&quot;notContainer&quot;&gt; Test active class on NOT container.&lt;/div&gt;\n    </code>\n  </pre>\n\n  <pre>\n    <code>\n  // CSS\n  \n.notContainer.gu-over {\n  background-color: yellow;\n}\n\n.containerVertical.gu-over-accept {\n  background-color: green;\n}\n\n.containerVertical.gu-over-decline {\n  background-color: red;\n}\n    </code>\n  </pre>\n\n  <pre>\n    <code>\n  // JS\n  controller(\'DragOverEvents\', [\'$element\', \'dragularService\', function TodoCtrl($element, dragularService) {\n    dragularService.cleanEnviroment();\n    dragularService([$element.children()[0], $element.children()[2]], {\n      nameSpace: \'apples\'\n    });\n    dragularService([$element.children()[1], $element.children()[3]], {\n      nameSpace: \'oranges\'\n    });\n\n    // containers events handling\n    function registerEvents(el) {\n      el.on(\'dragularenter\', function(e) {\n        if (el[0] === e.target) { // filter bubbled\n          el.addClass(dragularService.shared.extra ? \'gu-over-accept\' : \'gu-over-decline\');\n        }\n      });\n      el.on(\'dragularleave dragularrelease\', function(e) {\n        if ((el[0] === e.target && // filter bubbled\n          dragularService.shared.extra && // extra on dragleave contains element the drag is leaving to\n          dragularService.shared.extra.parentElement !== e.target) // is that element child of this container?\n          || e.type === \'dragularrelease\') {\n          el.removeClass(\'gu-over-accept\');\n          el.removeClass(\'gu-over-decline\');\n        }\n      });\n    }\n\n    angular.forEach($element.children(), function forEachChild(el) {\n      registerEvents(angular.element(el));\n    });\n\n    // notContainer events handling\n    var notContainer = angular.element(document.getElementsByClassName(\'notContainer\'));\n    notContainer.on(\'dragularenter\', function() {\n      notContainer.addClass(\'gu-over\');\n    });\n    notContainer.on(\'dragularleave dragularrelease\', function() {\n      notContainer.removeClass(\'gu-over\');\n    });\n  }])\n    </code>\n  </pre>\n</div>\n");
$templateCache.put("exampleEvents/exampleEvents.html","<div class=\'parent\'>\n    <h2>Events</h2>\n    <label for=\'hy\'>Add some fantastic events!</label>\n    <div class=\'wrapper\' ng-controller=\"Events\">\n        <div id=\'left3\' class=\'containerVertical\'>\n            <div>Move me, but you can only drop me in one of these containers.</div>\n            <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n            <div>Item 3.</div>\n            <div>Item 6.</div>\n        </div>\n        <div id=\'right3\' class=\'containerVertical\'>\n            <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n            <div>Item 4.</div>\n            <div>Item 5.</div>\n        </div>\n    </div>\n    <pre>\n        <code>\n.controller(\'Events\', [\'$scope\', \'$element\', \'dragularService\', \'$timeout\', function TodoCtrl($scope, $element, dragularService, $timeout) {\n    dragularService($element.children(), {\n      scope: $scope\n    });\n    $scope.$on(\'dragulardrag\', function(e, el) {\n      e.stopPropagation();\n      el.className = el.className.replace(\' ex-moved\', \'\');\n    });\n    $scope.$on(\'dragulardrop\', function(e, el) {\n      e.stopPropagation();\n      $timeout(function() {\n        el.className += \' ex-moved\';\n      }, 0);\n    });\n\n    $scope.$on(\'dragularcloned\', myFn(\'cloned\'));\n    $scope.$on(\'dragulardrag\', myFn(\'drag\'));\n    $scope.$on(\'dragularcancel\', myFn(\'cancel\'));\n    $scope.$on(\'dragulardrop\', myFn(\'drop\'));\n    $scope.$on(\'dragularremove\', myFn(\'remove\'));\n    $scope.$on(\'dragulardragend\', myFn(\'dragend\'));\n    $scope.$on(\'dragularshadow\', myFn(\'shadow\'));\n\n    function myFn(eventName) {\n      return function() {\n        console.log(eventName, arguments);\n      };\n    }\n\n  }]);\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleHandle/exampleHandle.html","<div class=\'parent\'>\n    <h2>Handle</h2>\n    <label for=\'hy\'>Drag handles float your cruise?</label>\n    <div class=\'wrapper\' ng-controller=\"Handle\">\n        <div id=\'left5\' class=\'containerVertical\'>\n            <div><span class=\'handle\'>+</span>Move me, but you can use the plus sign to drag me around.</div>\n        </div>\n        <div id=\'right5\' class=\'containerVertical\'>\n        </div>\n    </div>\n    <pre>\n        <code>\n  dragularService([document.getElementById(left), document.getElementById(right)], {\n    moves: function (el, container, handle) {\n      return handle.className === \'handle\';\n    }\n  });\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleIsContainerWithModel/exampleIsContainerWithModel.html","<div class=\'parent\'>\n  <h2>isContainer - with model</h2>\n  <label for=\'hy\'>Possible use case of <b>options.isContainer</b> combined with model. When you provide function <b>isContainer</b> and in case it returns true and you are providing models (<b>options.containersModel</b>), dragular check for model calling <b>options.isContainerModel(el)</b> function. If not provided, model is discarted.</label>\n  <div class=\'wrapper\' ng-controller=\"IsContainerModel\">\n    <div class=\'tableRow\'>\n      <div id=\"containerLeft\" class=\'containerVertical\'>\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div id=\"cart\" class=\'containerVertical\'>\n        <div class=\'cursorDefault\' ng-repeat=\"item in cartModel\">{{item.content}} \n        <button class=\'cursorDefault\' ng-click=\"removeItem()\">x</button></div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <pre>Items1:\n          <br/>{{items1 | json}}</pre>\n      </div>\n      <div class=\'containerVertical\'>\n        <pre>Cart:\n          <br/>{{cartModel | json}}</pre>\n      </div>\n    </div>\n  </div>\n  <pre>\n        <code>\n// JS\n  .controller(\'IsContainerModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items1 = [{\n      content: \'Move me, but you can only drop me in one of these containers.\'\n    }, {\n      content: \'If you try to drop me somewhere other than these containers, I\\\'ll just come back.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.cartModel = [];\n\n    var containerLeft = document.querySelector(\'#containerLeft\');\n\n    dragularService.cleanEnviroment();\n    dragularService([containerLeft], {\n      containersModel: [$scope.items1],\n      copy: true,\n      isContainer: function isContainer (el) {\n        return el.id === \'cart\';\n      },\n      isContainerModel: function getModel (){\n        return $scope.cartModel;\n      }\n    });\n\n    $scope.removeItem = function removeItem() {\n      var index = $scope.cartModel.indexOf(this.item);\n      $scope.cartModel.splice(index, 1);\n    };\n\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n &lt;div class=\'wrapper\' ng-controller=&quot;IsContainerModel&quot;&gt;\n    &lt;div class=\'tableRow\'&gt;\n      &lt;div id=&quot;containerLeft&quot; class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n      &lt;div id=&quot;cart&quot; class=\'containerVertical\'&gt;\n        &lt;div class=\'cursorDefault\' ng-repeat=&quot;item in cartModel&quot;&gt;{{item.content}} \n        &lt;button class=\'cursorDefault\' ng-click=&quot;removeItem()&quot;&gt;x&lt;/button&gt;&lt;/div&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;tableRow&quot;&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;pre&gt;Items1:\n          &lt;br/&gt;{{items1 | json}}&lt;/pre&gt;\n      &lt;/div&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;pre&gt;Cart:\n          &lt;br/&gt;{{cartModel | json}}&lt;/pre&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleNameSpaces/exampleNameSpaces.html","        <div class=\'parent\'>\n            <h2>NameSpaces</h2>\n            <label for=\'hy\'>Try to mix apples and oranges. You cannot place apples into oranges, but notice that you can place it into mixed. Mixed can be placed into apples and oranges. Notice that mixed becomes orange or apple if placed into their container. <b>So remember if you are using namespacing, then item is namespaced according to actual container it is placed in. If you need to item preserve thier state you need to use classes in combination with options.accepts and optionally options.isContainer.</b> It depends on setup. (See <a href=\"https://github.com/luckylooke/dragular/issues/9\">issue #9</a>.)</label>\n            <div class=\'wrapper\' ng-controller=\"NameSpaces\">\n                <div class=\'containerVertical width25\'>\n                    <div>try to mix oranges and apples</div>\n                    <div>apple 2</div>\n                    <div>apple 3</div>\n                    <div>apple 4</div>\n                </div>\n                <div class=\'containerVertical width25\'>\n                    <div>orange 1</div>\n                    <div>orange 2</div>\n                    <div>orange 3</div>\n                    <div>orange 4</div>\n                </div>\n                <div class=\'containerVertical width25\'>\n                    <div>apple 5</div>\n                    <div>apple 6</div>\n                    <div>apple 7</div>\n                    <div>apple 8</div>\n                </div>\n                <div class=\'containerVertical width25\'>\n                    <div>mixed 1</div>\n                    <div>mixed 2</div>\n                    <div>mixed 3</div>\n                    <div>mixed 4</div>\n                </div>\n            </div>\n            <pre>\n      <code>\ndragularService([$element.children()[0], $element.children()[2]], {\n  nameSpace: \'apples\'\n});\ndragularService($element.children()[1], {\n  nameSpace: \'oranges\'\n});\ndragularService($element.children()[3], { // mixed\n  nameSpace: [\'oranges\', \'apples\']\n});\n      </code>\n    </pre>\n        </div>");
$templateCache.put("exampleNestedNgRepeat/exampleNestedNgRepeat.html","<div class=\'parent\'>\n    <h2>Nested ngRepeat</h2>\n    <label for=\'hy\'> You can move whole rows by grabing row title, all items it selves. Try it out!\n        <hr/>\n        <b>Old IE doesnt support Flexible Box Layout</b> so it can look weird. But in modern browsers it is awsome! Dragular will be working well also in old IE but you have to use different css for layout.\n        <hr/>\n    </label>\n    <div ng-controller=\"NestedNgRepeat\">\n        <div ng-repeat=\"item in items\" class=\'exampleRow\'>\n            <div class=\"row-handle\">Row {{$index}}</div>\n            <div ng-repeat=\"item in item.items\" class=\"exampleCell\">{{item.content}}</div>\n        </div>\n    </div>\n    <pre>\n        <code>\n  // HTML\n\n  &lt;div ng-controller=&quot;Example15&quot;&gt;\n    &lt;div ng-repeat=&quot;item in items&quot; class=\'exampleRow\'&gt;\n      &lt;div class=&quot;row-handle&quot;&gt;Row {{$index}}&lt;/div&gt;\n      &lt;div ng-repeat=&quot;item in item.items&quot; class=&quot;exampleCell&quot;&gt;{{item.content}}&lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n    <pre>\n        <code>\n  // CSS\n\n  .exampleRow {\n    display: flex;\n    flex-direction: row;\n  }\n\n  .exampleCell {\n    flex-grow: 1;\n  }\n\n  .exampleRow,\n  .exampleCell {\n    margin: 10px;\n    padding: 10px;\n    background-color: rgba(0, 0, 0, 0.2);\n    cursor: move;\n    cursor: grab;\n    cursor: -moz-grab;\n    cursor: -webkit-grab;\n  }\n        </code>\n      </pre>\n    <pre>\n        <code>\n  // JS\n\n  .controller(\'NestedNgRepeat\', [\'$timeout\', \'$scope\', \'$element\', \'dragularService\', function NestedNgRepeatCtrl($timeout, $scope, $element, dragularService) {\n    $timeout(function() { // timeount due to ngRepeat to be ready\n      dragularService($element, {\n        nameSpace: \'rows\',\n        moves: function rowOnly (el, container, handle) {\n          return handle.classList.contains(\'row-handle\');\n        }\n      });\n      dragularService($element.children(), {\n        nameSpace: \'cells\',\n        moves: function excludeHandle (el, container, handle) {\n          return !handle.classList.contains(\'row-handle\');\n        }\n      });\n    }, 0);\n    $scope.items = [{\n      items: [{\n        content: \'Item a1\'\n      }, {\n        content: \'Item a2\'\n      }, {\n        content: \'Item a3\'\n      }, {\n        content: \'Item a4\'\n      }]\n    }, {\n      items: [{\n        content: \'Item b1\'\n      }, {\n        content: \'Item b2\'\n      }, {\n        content: \'Item b3\'\n      }, {\n        content: \'Item b4\'\n      }]\n    }, {\n      items: [{\n        content: \'Item c1\'\n      }, {\n        content: \'Item c2\'\n      }, {\n        content: \'Item c3\'\n      }, {\n        content: \'Item c4\'\n      }]\n    }];\n  }])\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel.html","<div class=\'parent\'>\n  <h2>Nested ngRepeat - with model</h2>\n  <label for=\'hy\'> You can move whole rows by grabing row title, all items it selves. Try it out!\n    <hr/>\n    <b>Old IE doesnt support Flexible Box Layout</b> so it can look weird. But in modern browsers it is awsome! Dragular will be working well also in old IE but you have to use different css for layout.\n    <hr/>\n  </label>\n  <div ng-controller=\"NestedNgRepeatWithModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items\" class=\'exampleRow\'>\n          <div class=\"row-handle\">Row {{::$index}}</div>\n          <div class=\"exampleRow exampleCell containerNested\">\n            <div ng-repeat=\"item in item.items\" class=\"exampleCell\">{{item.content}}</div>\n          </div>\n        </div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <pre>\n            <div>Items:\n              <br/>{{items | json}}</div>\n        </pre>\n      </div>\n    </div>\n  </div>\n  <pre>\n    <code>\n&lt;!-- HTML --&gt;\n&lt;div ng-controller=&quot;NestedNgRepeatWithModel&quot;&gt;\n  &lt;div class=\'containerVertical\'&gt;\n    &lt;div ng-repeat=&quot;item in items&quot; class=\'exampleRow\'&gt;\n      &lt;div class=&quot;row-handle&quot;&gt;Row {{::$index}}&lt;/div&gt;\n      &lt;div class=&quot;exampleRow exampleCell containerNested&quot;&gt;\n        &lt;div ng-repeat=&quot;item in item.items&quot; class=&quot;exampleCell&quot;&gt;{{item.content}}&lt;/div&gt;\n      &lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n&lt;/div&gt;\n    </code>\n  </pre>\n  <pre>\n    <code>\n  // CSS\n\n  .exampleRow {\n    display: flex;\n    flex-direction: row;\n  }\n\n  .exampleCell {\n    flex-grow: 1;\n  }\n\n  .exampleRow,\n  .exampleCell {\n    margin: 10px;\n    padding: 10px;\n    background-color: rgba(0, 0, 0, 0.2);\n    cursor: move;\n    cursor: grab;\n    cursor: -moz-grab;\n    cursor: -webkit-grab;\n  }\n    </code>\n  </pre>\n  <pre>\n    <code>\n  // JS\n.controller(\'NestedNgRepeatWithModel\', [\'$timeout\', \'$scope\', \'$element\', \'dragularService\', function NestedNgRepeatWithModelCtrl($timeout, $scope, $element, dragularService) {\n    $timeout(function() { // timeount due to nested ngRepeat to be ready\n      var container = $element.children().eq(0).children(),\n        parentContainers = container.children(),\n        nestedContainers = [];\n\n      dragularService(container, {\n        moves: function(el, container, handle) {\n          return handle.classList.contains(\'row-handle\');\n        },\n        containersModel: $scope.items,\n        nameSpace: \'rows\'\n      });\n\n      // collect nested contianers\n      for (var i = 0; i < parentContainers.length; i++) {\n        nestedContainers.push(parentContainers.eq(i).children()[1]);\n      }\n\n      dragularService(nestedContainers, {\n        moves: function(el, container, handle) {\n          return !handle.classList.contains(\'row-handle\');\n        },\n        containersModel: (function getNestedContainersModel(){\n          var parent = $scope.items,\n            containersModel = [];\n          for (var i = 0; i < parent.length; i++) {\n            containersModel.push(parent[i].items);\n          }\n          return containersModel;\n        })(),\n        nameSpace: \'cells\'\n      });\n    }, 0);\n    $scope.items = [{\n      items: [{\n        content: \'Item a1\'\n      }, {\n        content: \'Item a2\'\n      }, {\n        content: \'Item a3\'\n      }, {\n        content: \'Item a4\'\n      }]\n    }, {\n      items: [{\n        content: \'Item b1\'\n      }, {\n        content: \'Item b2\'\n      }, {\n        content: \'Item b3\'\n      }, {\n        content: \'Item b4\'\n      }]\n    }, {\n      items: [{\n        content: \'Item c1\'\n      }, {\n        content: \'Item c2\'\n      }, {\n        content: \'Item c3\'\n      }, {\n        content: \'Item c4\'\n      }]\n    }];\n  }])\n    </code>\n  </pre>\n</div>\n");
$templateCache.put("exampleNgRepeat/exampleNgRepeat.html","        <div class=\'parent\'>\n            <h2>ngRepeat</h2>\n            <label for=\'hy\'>Example of using ng-repeat with dragular and adding/removing items dynamicaly.</label>\n            <div class=\'wrapper\' ng-controller=\"NgRepeat\">\n                <div class=\'containerVertical\'>\n                    <div ng-repeat=\"item in items\">\n                        {{item.content}}\n                    </div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  // HTML:\n  &lt;div class=\'containerVertical\'&gt;\n    &lt;div ng-repeat=&quot;item in items&quot;&gt;\n      {{item.content}}\n    &lt;/div&gt;\n  &lt;/div&gt;\n\n  // JS:\n  dragularService($element.children());\n  $scope.items = [{\n    content: \'Try to add or remove some elements (click on +- buttons), you will see that it is not problem for dragular.\'\n  },{\n    content: \'Item 2\'\n  },{\n    content: \'Item 3\'\n  },{\n    content: \'Item 4\'\n  }];\n        </code>\n      </pre>\n        </div>\n");
$templateCache.put("exampleNgRepeatWithModel/exampleNgRepeatWithModel.html","<div class=\'parent\'>\n  <h2>ngRepeat - with model</h2>\n  <label for=\'hy\'>Example of using ng-repeat with dragular and adding/removing items dynamicaly.</label>\n  <div class=\'wrapper\' ng-controller=\"NgRepeatWithModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items\">\n          {{item.content}}\n          <button class=\'cursorDefault\' ng-click=\"addItem()\">+</button>\n          <button class=\'cursorDefault\' ng-click=\"removeItem()\">x</button>\n        </div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <div>Items:\n          <br/>{{items | json}}</div>\n      </div>\n    </div>\n  </div>\n  <pre>\n    <code>\n  // HTML:\n   &lt;div class=\'wrapper\' ng-controller=&quot;NgRepeatWithModel&quot;&gt;\n      &lt;div class=\'containerVertical\'&gt;\n        &lt;div ng-repeat=&quot;item in items&quot;&gt;\n          {{item.content}}\n          &lt;button class=\'cursorDefault\' ng-click=&quot;addItem()&quot;&gt;+&lt;/button&gt;\n          &lt;button class=\'cursorDefault\' ng-click=&quot;removeItem()&quot;&gt;x&lt;/button&gt;\n        &lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n    </code>\n  </pre>\n  <pre>\n    <code>\n  // JS:\n  controller(\'NgRepeatWithModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items = [{\n      content: \'Try to add or remove some elements (click on +- buttons), you will see that it is not problem for dragular.\'\n    }, {\n      content: \'Item 2\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    dragularService($element.children().eq(0).children(), {containersModel: $scope.items});\n    $scope.addItem = function addItem() {\n      var index = $scope.items.indexOf(this.item) + 1;\n      $scope.items.splice(index, 0, {\n        content: this.item.content + \'-copy\'\n      });\n    };\n    $scope.removeItem = function removeItem() {\n      var index = $scope.items.indexOf(this.item);\n      $scope.items.splice(index, 1);\n    };\n  }])\n    </code>\n  </pre>\n</div>\n");
$templateCache.put("exampleRemoveOnSpill/exampleRemoveOnSpill.html","        <div class=\'parent\'>\n            <h2>Remove on spill</h2>\n            <label for=\'hy\'>Need to be able to quickly delete stuff when it spills out of the chosen containers?</label>\n            <div class=\'wrapper\' ng-controller=\"RemoveOnSpill\">\n                <div id=\'left\' class=\'containerVertical\'>\n                    <div>Move me, but you can only drop me in containers.</div>\n                    <div>If you try to drop me somewhere other than containers, I\'ll die a fiery death.</div>\n                    <div>Item 3.</div>\n                    <div>Item 6.</div>\n                    <div>Item 4.</div>\n                    <div>Item 5.</div>\n                </div>\n                <div id=\'right\' class=\'containerVertical\'>\n                    <div>You can drop me in the left container.</div>\n                    <div>Item 4.</div>\n                    <div>Item 5.</div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  dragularService([document.getElementById(single)], { removeOnSpill: true });\n        </code>\n      </pre>\n        </div>");
$templateCache.put("exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel.html","<div class=\'parent\'>\n  <h2>Remove on spill - with model</h2>\n  <label for=\'hy\'>Need to be able to quickly delete stuff when it spills out of the chosen containers?</label>\n  <div class=\'wrapper\' ng-controller=\"RemoveOnSpillWithModel\">\n    <div class=\'tableRow\'>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items1\">{{item.content}}</div>\n      </div>\n      <div class=\'containerVertical\'>\n        <div ng-repeat=\"item in items2\">{{item.content}}</div>\n      </div>\n    </div>\n    <div class=\"tableRow\">\n      <div class=\'containerVertical\'>\n        <pre>Items1:\n          <br/>{{items1 | json}}</pre>\n      </div>\n      <div class=\'containerVertical\'>\n        <pre>Items2:\n          <br/>{{items2 | json}}</pre>\n      </div>\n    </div>\n  </div>\n   <pre>\n        <code>\n// JS\n  .controller(\'RemoveOnSpillWithModel\', [\'$scope\', \'$element\', \'dragularService\', function TodoCtrl($scope, $element, dragularService) {\n    $scope.items1 = [{\n      content: \'Move me, but you can only drop me in containers.\'\n    }, {\n      content: \'If you try to drop me somewhere other than containers, I\\\'ll die a fiery death.\'\n    }, {\n      content: \'Item 3\'\n    }, {\n      content: \'Item 4\'\n    }];\n    $scope.items2 = [{\n      content: \'You can drop me in the left container.\'\n    }, {\n      content: \'Item 6\'\n    }, {\n      content: \'Item 7\'\n    }, {\n      content: \'Item 8\'\n    }];\n    var containers = $element.children().eq(0).children();\n    dragularService.cleanEnviroment();\n    dragularService([containers[0],containers[1]],{\n      containersModel: [$scope.items1, $scope.items2],\n      removeOnSpill: true\n    });\n  }])\n        </code>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div class=\'wrapper\' ng-controller=&quot;Basic&quot;&gt;\n    &lt;div class=\'tableRow\'&gt;\n        &lt;div class=\'containerVertical\'&gt;\n            &lt;div ng-repeat=&quot;item in items1&quot;&gt;{{item.content}}&lt;/div&gt;\n        &lt;/div&gt;\n        &lt;div class=\'containerVertical\'&gt;\n            &lt;div ng-repeat=&quot;item in items2&quot;&gt;{{item.content}}&lt;/div&gt;\n        &lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;tableRow&quot;&gt;\n        &lt;div class=&quot;container&quot;&gt;\n            &lt;div&gt;Items1:\n                &lt;br/&gt;{{items1 | json}}&lt;/div&gt;\n        &lt;/div&gt;\n        &lt;div class=&quot;container&quot;&gt;\n            &lt;div&gt;Items2:\n                &lt;br/&gt;{{items2 | json}}&lt;/div&gt;\n        &lt;/div&gt;\n    &lt;/div&gt;\n&lt;/div&gt;\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("exampleRevertOnSpill/exampleRevertOnSpill.html","        <div class=\'parent\'>\n            <h2>Revert on spill</h2>\n            <label for=\'hy\'>By default, dropping an element outside of any known containers will keep the element in the last place it hovered over. You can make elements go back home if they\'re dropped outside of known containers, too.</label>\n            <div class=\'wrapper\' ng-controller=\"RevertOnSpill\">\n                <div id=\'left4\' class=\'containerVertical\'>\n                    <div>Move me, but you can only drop me in one of these containers.</div>\n                    <div>If you try to drop me somewhere other than these containers, I\'ll just come back.</div>\n                    <div>Item 3.</div>\n                    <div>Item 6.</div>\n                </div>\n                <div id=\'right4\' class=\'containerVertical\'>\n                    <div>You can drop me in the left container, otherwise I\'ll stay here.</div>\n                    <div>Item 4.</div>\n                    <div>Item 5.</div>\n                </div>\n            </div>\n            <pre>\n        <code>\n  dragularService([document.getElementById(left), document.getElementById(right)], { revertOnSpill: true });\n        </code>\n      </pre>\n        </div>");
$templateCache.put("exampleScrollingDrag/exampleScrollingDrag.html","<div class=\'parent\'>\n  <h2>Scrolling drag</h2>\n  <label for=\'hy\'> Containers can be scrolled by hovering up/down bar near containers. Bars on right side are transparent and moved a bit over containers. It can be scrolled also by mouse wheel.\n  </label>\n  <div ng-controller=\"ScrollingDrag\">\n    <div class=\"containerVertical scrollingDrag\">\n      <div class=\"scrollBar\" id=\"leftTopBar\">up</div>\n      <div id=\"leftScroll\" class=\"scrollingDragInner\">\n        <div>Item 1.</div>\n        <div>Item 2.</div>\n        <div>Item 3.</div>\n        <div>Item 4.</div>\n        <div>Item 5.</div>\n        <div>Item 6.</div>\n        <div>Item 7.</div>\n        <div>Item 9.</div>\n        <div>Item 10.</div>\n        <div>Item 11.</div>\n        <div>Item 12.</div>\n        <div>Item 13.</div>\n      </div>\n      <div class=\"scrollBar\" id=\"leftBottomBar\">down</div>\n    </div>\n    <div class=\"containerVertical scrollingDrag\">\n      <div class=\"scrollBar\" id=\"rightTopBar\"></div>\n      <div id=\"rightScroll\" class=\"scrollingDragInner\">\n        <div>Item 1.</div>\n        <div>Item 2.</div>\n        <div>Item 3.</div>\n        <div>Item 4.</div>\n        <div>Item 5.</div>\n        <div>Item 6.</div>\n        <div>Item 7.</div>\n        <div>Item 9.</div>\n        <div>Item 10.</div>\n        <div>Item 11.</div>\n        <div>Item 12.</div>\n        <div>Item 13.</div>\n      </div>\n      <div class=\"scrollBar\" id=\"rightBottomBar\"></div>\n    </div>\n  </div>\n   <pre>\n        <code>\n// JS\ncontroller(\'ScrollingDrag\', [\'$interval\', \'$element\', \'dragularService\', function TodoCtrl($interval, $element, dragularService) {\n\n\n    var timer,\n      leftScrollContainer = document.getElementById(\'leftScroll\'),\n      rightScrollContainer = document.getElementById(\'rightScroll\'),\n      leftTopBar = document.getElementById(\'leftTopBar\'),\n      leftBottomBar = document.getElementById(\'leftBottomBar\'),\n      rightTopBar = document.getElementById(\'rightTopBar\'),\n      rightBottomBar = document.getElementById(\'rightBottomBar\');\n\n    dragularService.cleanEnviroment();\n    dragularService([leftScrollContainer, rightScrollContainer]);\n\n    registerEvents(leftTopBar, leftScrollContainer, -5);\n    registerEvents(leftBottomBar, leftScrollContainer, 5);\n    registerEvents(rightTopBar, rightScrollContainer, -5);\n    registerEvents(rightBottomBar, rightScrollContainer, 5);\n\n    function registerEvents(bar, container, inc, speed) {\n      if (!speed) {\n        speed = 20;\n      }\n      angular.element(bar).on(\'dragularenter\', function() {\n        container.scrollTop += inc;\n        timer = $interval(function moveScroll() {\n          container.scrollTop += inc;\n        }, speed);\n      });\n      angular.element(bar).on(\'dragularleave dragularrelease\', function() {\n        $interval.cancel(timer);\n      });\n    }\n  }])\n        </code>\n      </pre>\n      <pre>\n        <code>\n&lt;!-- HTML --&gt;\n&lt;div ng-controller=&quot;ScrollingDrag&quot;&gt;\n    &lt;div class=&quot;containerVertical scrollingDrag&quot;&gt;\n      &lt;div class=&quot;scrollBar&quot; id=&quot;leftTopBar&quot;&gt;up&lt;/div&gt;\n      &lt;div id=&quot;leftScroll&quot; class=&quot;scrollingDragInner&quot;&gt;\n        &lt;div&gt;Item 1&lt;/div&gt;\n        &lt;div&gt;Item 2&lt;/div&gt;\n            ...\n      &lt;/div&gt;\n      &lt;div class=&quot;scrollBar&quot; id=&quot;leftBottomBar&quot;&gt;down&lt;/div&gt;\n    &lt;/div&gt;\n    &lt;div class=&quot;containerVertical scrollingDrag&quot;&gt;\n      &lt;div class=&quot;scrollBar&quot; id=&quot;rightTopBar&quot;&gt;up&lt;/div&gt;\n      &lt;div id=&quot;rightScroll&quot; class=&quot;scrollingDragInner&quot;&gt;\n        &lt;div&gt;Item 1&lt;/div&gt;\n        &lt;div&gt;Item 2&lt;/div&gt;\n            ...\n      &lt;/div&gt;\n      &lt;div class=&quot;scrollBar&quot; id=&quot;rightBottomBar&quot;&gt;down&lt;/div&gt;\n    &lt;/div&gt;\n  &lt;/div&gt;\n        </code>\n      </pre>\n       <pre>\n        <code>\n// CSS\n.scrollingDrag {\n  width: 45%;\n  display: inline-block;\n}\n\n.scrollingDragInner {\n  max-height: 200px;\n  overflow-y: auto;\n}\n\n#rightTopBar,\n#rightBottomBar {\n  background: transparent;\n  position: relative;\n  height: 20px;\n}\n\n#rightTopBar {\n  top: 10px;\n}\n\n#rightBottomBar {\n  bottom: 10px;\n}\n\ndiv.scrollBar {\n  background: yellow;\n  text-align: center;\n  padding: 1px;\n}\n\n        </code>\n      </pre>\n</div>\n");
$templateCache.put("partials/partial-contribute.html","<div class=\'container\'>\n  <div class=\"row\">\n    <div class=\"col-lg-12\">\n      <ng-include src=\"\'partials/autogenerated/contribute.html\'\"> </ng-include>\n    </div>\n  </div>\n</div>\n");
$templateCache.put("partials/partial-docs.html","<div class=\'container\'>\n  <div id=\"rowOffcanvas\" class=\"row row-offcanvas row-offcanvas-left\">\n    <div class=\"col-xs-6 col-sm-3 sidebar-offcanvas\" id=\"sidebar\">\n      <div class=\"list-group\">\n        <a ng-repeat=\"example in examplesList\" ui-sref=\"docs.detail({link:example.link})\" ui-sref-active=\"active\" class=\"list-group-item\">{{example.title}}</a>\n      </div>\n    </div>\n    <!--/.sidebar-offcanvas-->\n    <div class=\"col-xs-12 col-sm-9\">\n      <p class=\"pull-left visible-xs\">\n        <button type=\"button\" ng-click=\"toggleSidebar()\" class=\"btn btn-primary btn-xs\">Toggle nav</button>\n      </p>\n      <!-- docs.detail -->\n      <div ui-view onload=\"highlightCode()\"></div>\n    </div>\n    <!--/.col-xs-12.col-sm-9-->\n  </div>\n  <!--/row-->\n</div>\n");
$templateCache.put("partials/partial-home.html","<div class=\'container\'>\n  <div class=\"row\">\n    <!--/.sidebar-offcanvas-->\n    <div class=\"col-lg-12\">\n      <div class=\"jumbotron\">\n        <h1>DRAGULAR</h1>\n        <p>Angular drag&drop based on <a href=\"http://github.com/bevacqua/dragula\">dragula</a>.</p>\n        <p><a class=\"btn btn-primary btn-lg\" ui-sref=\"docs\" role=\"button\">Live examples in docs</a></p>\n      </div>\n      <div class=\"row\">\n        <div class=\"col-lg-12\">\n          <p>Browser support includes every sane browser and **IE7+**. <sub>_(Granted you polyfill the functional `Array` methods in ES5)_</sub></p>\n          <h2>Inspiration</h2>\n          <p>I am working on huge angular project and I am using several drag&drop libraries in it, one for UI, one for lists, etc.. I want to use one full-featured drag&drop library for whole project. As I could not find any suitable, I decided to create one. I have choosen great library <a href=\"http://github.com/bevacqua/dragula\">dragula</a> by <a href=\"https://github.com/bevacqua\">Nicolas Bevacqua</a> as my starting point, make it more angular and started to put features in it! If you wish light-weight angular version of dragula, there is <a href=\"http://github.com/bevacqua/angular-dragula\">official angular version of dragula</a>.</p>\n          <p><b>Actual version 3.1.0 is based on dragula 3.3.0 and tested with angular 1.4.3.</b></p>\n          <h2>Differences of dragular (against dragula)</h2>\n          <ul>\n            <li>replaced dragula crossvent with angulars event binding</li>\n            <li>replaced dragula contra.emitter with $scope.$emit if scope provided in options (options.scope)</li>\n            <li>provided as service or directive dragular where options can be passed via atribute dragular</li>\n            <li>automatic direction if not provided in options, instead of default vertical</li>\n            <li>accepting arraylike objects as containers array (jQuery, jQlite collections etc..)</li>\n            <li>accepting custom classes via option.classes</li>\n            <li>namespaced containers groups available via option.nameSpace (containers in same nameSpace cooperate)</li>\n            <li>boundingBox (dragging element can me moved only in specific area)</li>\n            <li>lockX/Y (dragging element can me moved only in specific direction)</li>\n            <li>DOM can be synced with scope model</li>\n            <li>support css selectors to define containers</li>\n            <li>added syntax highlighter to example codes</li>\n            <li>etc..</li>\n          </ul>\n          <h2>Todo</h2>\n          <ul>\n            <li>improve docs</li>\n          </ul>\n          <h2>Features</h2>\n          <ul>\n            <li>provided as service and also as directive</li>\n            <li>Super easy to set up</li>\n            <li>No bloated dependencies</li>\n            <li><strong>Figures out sort order</strong> on its own</li>\n            <li>A shadow where the item would be dropped offers <strong>visual feedback</strong></li>\n            <li>Touch events!</li>\n          </ul>\n          <h2>For installation, usage and examples go to <a ui-sref=\"docs\">docs</a></h2>\n        </div>\n      </div>\n      <!--/row-->\n    </div>\n    <!--/.col-xs-12.col-sm-9-->\n  </div>\n  <!--/row-->\n</div>\n");
$templateCache.put("partials/autogenerated/contribute.html","<h1 id=\"how-to-contribute\">How to contribute</h1>\n<p>It&#39;s important to us that you feel you can contribute towards the evolution of Dragular. This can take many forms: from helping to fix bugs or improve the docs, to adding in new features to the source. This guide should help you in making that process as smooth as possible.</p>\n<p>Before contributing, please read the <a href=\"https://github.com/luckylooke/dragular/blob/master/CODE_OF_CONDUCT.md\">code of conduct</a>.</p>\n<h2 id=\"reporting-issues\">Reporting issues</h2>\n<p><a href=\"https://github.com/luckylooke/dragular/issues\">GitHub Issues</a> is the place to report bugs you may have found in either the core library or any of the examples that are part of the repository. When submitting a bug please do the following:</p>\n<p><strong>1. Search for existing issues.</strong> Your bug may have already been fixed or addressed in a development branch version of Dragular, so be sure to search the issues first before putting in a duplicate issue.</p>\n<p><strong>2. Not sure if it&#39;s a bug?.</strong> Then please ask via issues and tag it [question].</p>\n<p><strong>3. Create an isolated and reproducible test case.</strong> If you are reporting a bug, make sure you also have a minimal, runnable, code example that reproduces the problem you have.</p>\n<p><strong>4. Include a live example.</strong> After narrowing your code down to only the problem areas, make use of <a href=\"http://jsfiddle.net\">jsFiddle</a>, <a href=\"http://jsbin.com/\">jsBin</a>, or a link to your live site so that we can view a live example of the problem.</p>\n<p><strong>5. Share as much information as possible.</strong> Include browser version affected, your OS, version of the library, steps to reproduce, etc. &quot;X isn&#39;t working!!!1!&quot; will probably just be closed.</p>\n<h2 id=\"dev-vs-master\">Dev vs. Master</h2>\n<p>The dev branch of Dragular is our &#39;current working&#39; version. It is always ahead of the master branch in terms of features and fixes. However it&#39;s also bleeding-edge and experimental and we cannot and do not guarantee it will compile or work for you. Very often we have to break things for a few days while we rebuild and patch. So by all means please export the dev branch and contribute towards it, indeed that is where all Pull Requests should be sent, but do so understanding the API may change beneath you.</p>\n<h2 id=\"making-changes\">Making Changes</h2>\n<p>To take advantage of our npm build script and jshint config it will be easiest for you if you have node.js installed locally.</p>\n<p>You can download node.js from <a href=\"http://nodejs.org\">nodejs.org</a>.</p>\n<p>After that you can clone the repository and run <code>npm i</code> inside the cloned folder. This will install dependencies necessary for building the project. For development workflow automation dragular uses <code>gulp &gt;= 3.9.0</code>. Before starting development, make sure that <code>gulp</code> is installed on your machine globally: <code>npm i -g gulp</code>.</p>\n<h3 id=\"developing\">Developing</h3>\n<p>There are several gulp tasks that are used for generating different builds:</p>\n<ul>\n<li><code>gulp dev</code> - Serves files with BrowserSync server, watches &amp; automatically refreshes connected browsers on changes, generates non-minified but concatenated styles &amp; scripts from the dragular source.</li>\n<li><code>gulp dev:docs</code> - Does exactly the same as <code>gulp dev</code>, except it works with the documentation source.</li>\n<li><code>gulp build</code> - Concatenates and minifies dragular source files.</li>\n<li><code>gulp build:docs</code> - Concatenates and minifies documentation source files.</li>\n</ul>\n<h3 id=\"linting\">Linting</h3>\n<ul>\n<li><code>gulp lint</code> &amp; <code>gulp lint:docs</code> - Lint JavaScript files.</li>\n</ul>\n<h3 id=\"making-a-pull-request\">Making a pull request</h3>\n<p>Once that is ready, make your changes and submit a Pull Request:</p>\n<ul>\n<li><p><strong>Send Pull Requests to the <code>dev</code> branch.</strong> All Pull Requests must be sent to the <code>dev</code> branch, <code>master</code> is the latest release and PRs to that branch will be closed.</p>\n</li>\n<li><p><strong>Ensure changes are jshint validated.</strong> Our JSHint configuration file is provided in the repository and you should check against it before submitting.</p>\n</li>\n<li><p><strong>Only commit relevant changes.</strong> Don&#39;t include changes that are not directly relevant to the fix you are making. The more focused a PR is, the faster it will get attention and be merged. Extra files changing only whitespace or trash files will likely get your PR closed.</p>\n</li>\n</ul>\n<p>Dependencies for building from source and running tests:</p>\n<h2 id=\"coding-style-preferences-are-not-contributions\">Coding style preferences are not contributions</h2>\n<p>If your PR is doing little more than changing the Dragular source code into a format / coding style that you prefer then we will automatically close it. All PRs must adhere to the coding style already set-out across the lines of code in Dragular. Your personal preferences for how things should &quot;look&quot; or be structured do not apply here, sorry. PRs should fix bugs, fix documentation or add features. No changes for the sake of change.</p>\n<h2 id=\"i-don-t-really-like-git-node-js-but-i-can-fix-this-bug\">I don&#39;t really like git / node.js, but I can fix this bug</h2>\n<p>That is fine too. While Pull Requests are the best thing in the world for us, they are not the only way to help. You&#39;re welcome to post fixes to our forum or even just email them to us. All we ask is that you still adhere to the guidelines presented here re: JSHint, etc.</p>\n");}]);
},{}],28:[function(require,module,exports){
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

},{"./dragularModule":29}],29:[function(require,module,exports){
/* global angular */
'use strict';



/**
 * Dragular 3.1.0 by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */
module.exports = angular.module('dragularModule', []);

({"dragularDirective":require("./dragularDirective.js"),"dragularService":require("./dragularService.js")});

},{"./dragularDirective.js":28,"./dragularService.js":30}],30:[function(require,module,exports){
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

},{"./dragularModule":29}]},{},[25]);
