'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BoundingBoxLockX', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    var boundingBox = $element.children().children()[0];
    dragularService.cleanEnviroment();
	dragularService(boundingBox, {
      boundingBox: boundingBox,
      lockX: true
    });
  }]);
