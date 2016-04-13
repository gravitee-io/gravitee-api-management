'use strict';

var BoundingBoxLockXCtrl = function ($element, dragularService) {
  var boundingBox = $element.children().children()[0];
  dragularService.cleanEnviroment();
  dragularService(boundingBox, {
    boundingBox: boundingBox,
    lockX: true
  });
};

BoundingBoxLockXCtrl.$inject = ['$element', 'dragularService'];

module.exports = BoundingBoxLockXCtrl;
