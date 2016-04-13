'use strict';

var RevertOnSpillCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
  dragularService($element.children(), {
    revertOnSpill: true
  });
};

RevertOnSpillCtrl.$inject = ['$element', 'dragularService'];

module.exports = RevertOnSpillCtrl;
