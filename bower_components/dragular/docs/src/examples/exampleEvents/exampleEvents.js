'use strict';

var EventsCtrl = function ($scope, $element, dragularService, $timeout) {
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
};

EventsCtrl.$inject = ['$scope', '$element', 'dragularService', '$timeout'];

module.exports = EventsCtrl;
