'use strict';

var RemoveOnSpillCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
	dragularService($element.children(), {
    removeOnSpill: true
  });
};

RemoveOnSpillCtrl.$inject = ['$element', 'dragularService'];

module.exports = RemoveOnSpillCtrl;
