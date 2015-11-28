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
