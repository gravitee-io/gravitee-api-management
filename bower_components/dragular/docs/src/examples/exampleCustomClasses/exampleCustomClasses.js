'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('CustomClasses', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      classes: {
        mirror: 'custom-green-mirror'
      }
    });
  }]);
