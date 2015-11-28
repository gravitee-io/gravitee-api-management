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
