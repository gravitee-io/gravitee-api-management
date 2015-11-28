'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('BoundingBox', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    var boundingBox = $element[0];
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      boundingBox: boundingBox
    });
  }]);
