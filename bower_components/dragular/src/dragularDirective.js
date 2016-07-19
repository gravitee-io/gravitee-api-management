'use strict';

/**
 * dragular Directive by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */

var dragular = function (dragularService) {
  return {
    restrict: 'A',
    link: function($scope, iElm, iAttrs) {

      var drake,
        options = $scope.$eval(iAttrs.dragular) || tryJson(iAttrs.dragular) || {};

      function tryJson(json) {
        try { // I dont like try catch solutions but I havent find sattisfying way of chcecking json validity.
          return JSON.parse(json);
        } catch (e) {
          return undefined;
        }
      }

      if(options && options.containersModel && typeof options.containersModel === 'string'){
        options.containersModel = $scope.$eval(options.containersModel);
      }

      if(options && options.dynamicModelAttribute){
        // watch for model changes
        $scope.$watch(function () {
          return $scope.$eval(iAttrs.dragularModel);
        }, function (newVal) {
          if(newVal){
            drake.containersModel = drake.sanitizeContainersModel($scope.$eval(newVal));
          }
        });
      }else if(iAttrs.dragularModel){
        // bind once and keep reference
        options.containersModel = $scope.$eval(iAttrs.dragularModel);
      }

      if(iAttrs.dragularNameSpace){
        options.nameSpace = iAttrs.dragularNameSpace.split(' ');
      }

      if(iAttrs.dragularOnInit){
        options.onInit = $scope.$eval(iAttrs.dragularOnInit);
      }

      drake = dragularService(iElm[0], options);
    }
  };
};

dragular.$inject = ['dragularService'];

module.exports = dragular;
