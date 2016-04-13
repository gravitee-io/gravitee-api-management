'use strict';

var CopyCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
  dragularService($element.children(), {
    copy: true
  });
};

CopyCtrl.$inject = ['$element', 'dragularService'];

module.exports = CopyCtrl;
