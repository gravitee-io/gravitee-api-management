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
