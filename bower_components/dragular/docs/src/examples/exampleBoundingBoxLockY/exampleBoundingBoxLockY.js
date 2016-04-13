'use strict';

var BoundingBoxLockYCtrl = function ($element, dragularService) {
  var boundingBox = $element.children().children()[0];
  dragularService.cleanEnviroment();
  dragularService(boundingBox, {
    boundingBox: boundingBox,
    lockY: true
  });
};

BoundingBoxLockYCtrl.$inject = ['$element', 'dragularService'];

module.exports = BoundingBoxLockYCtrl;
