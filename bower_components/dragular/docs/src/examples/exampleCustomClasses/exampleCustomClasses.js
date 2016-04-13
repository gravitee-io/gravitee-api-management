'use strict';

var CustomClassesCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
  dragularService($element.children(), {
    classes: {
      mirror: 'custom-green-mirror'
    }
  });
};

CustomClassesCtrl.$inject = ['$element', 'dragularService'];

module.exports = CustomClassesCtrl;
