'use strict';

var NestedNgRepeatCtrl = function ($timeout, $scope, $element, dragularService) {
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
};

NestedNgRepeatCtrl.$inject = ['$timeout', '$scope', '$element', 'dragularService'];

module.exports = NestedNgRepeatCtrl;
