'use strict';

var BasicCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
  dragularService('.containerVertical');
};

BasicCtrl.$inject = ['$element', 'dragularService'];

module.exports = BasicCtrl;
