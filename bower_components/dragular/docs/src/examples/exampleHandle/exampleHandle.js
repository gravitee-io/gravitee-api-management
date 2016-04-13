'use strict';

var HandleCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
	dragularService($element.children(), {
    moves: function(el, container, handle) {
      return handle.className === 'handle';
    }
  });
};

HandleCtrl.$inject = ['$element', 'dragularService'];

module.exports = HandleCtrl;
