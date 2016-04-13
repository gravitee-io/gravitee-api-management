'use strict';

var BoundingBoxCtrl = function ($element, dragularService) {
  var boundingBox = $element[0];
  dragularService.cleanEnviroment();

  dragularService($element.children(), {
    boundingBox: boundingBox
  });
};

BoundingBoxCtrl.$inject = ['$element', 'dragularService'];

module.exports = BoundingBoxCtrl;
