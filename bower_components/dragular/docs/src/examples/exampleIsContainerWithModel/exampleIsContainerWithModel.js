'use strict';

var IsContainerModelCtrl = function ($scope, $element, dragularService) {
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
};

IsContainerModelCtrl.$inject = ['$scope', '$element', 'dragularService'];

module.exports = IsContainerModelCtrl;
