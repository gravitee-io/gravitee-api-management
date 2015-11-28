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
