'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('RemoveOnSpill', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      removeOnSpill: true
    });
  }]);
