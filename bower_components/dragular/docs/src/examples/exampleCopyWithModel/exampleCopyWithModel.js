'use strict';

var CopyModelCtrl = function ($scope, $element, dragularService) {
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
};

CopyModelCtrl.$inject = ['$scope', '$element', 'dragularService'];

module.exports = CopyModelCtrl;
