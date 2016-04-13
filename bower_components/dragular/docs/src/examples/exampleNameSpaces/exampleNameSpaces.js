'use strict';

var NameSpacesCtrl = function ($element, dragularService) {
  dragularService.cleanEnviroment();
  dragularService([$element.children()[0], $element.children()[2]], {
    nameSpace: 'apples'
  });
  dragularService($element.children()[1], {
    nameSpace: 'oranges'
  });
  dragularService($element.children()[3], { // mixed
    nameSpace: ['oranges', 'apples']
  });
};

NameSpacesCtrl.$inject = ['$element', 'dragularService'];

module.exports = NameSpacesCtrl;
