'use strict';

var DirectiveCtrl = function ($scope) {
  $scope.dragularOptions = {
    classes: {
      mirror: 'custom-green-mirror'
    },
    nameSpace: 'same' // just connecting left and right container
  };
};

DirectiveCtrl.$inject = ['$scope'];

module.exports = DirectiveCtrl;
