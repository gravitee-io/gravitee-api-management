'use strict';

var examplesAppModule = require('../examplesApp');

/**
* @ngInject
*/

examplesAppModule
  .controller('Handle', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
	dragularService($element.children(), {
      moves: function(el, container, handle) {
        return handle.className === 'handle';
      }
    });
  }]);
