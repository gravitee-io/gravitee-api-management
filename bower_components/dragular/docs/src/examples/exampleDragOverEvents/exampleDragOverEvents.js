/* global angular */
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('DragOverEvents', ['$element', 'dragularService', function TodoCtrl($element, dragularService) {
    dragularService.cleanEnviroment();
    dragularService([$element.children()[0], $element.children()[2]], {
      nameSpace: 'apples'
    });
    dragularService([$element.children()[1], $element.children()[3]], {
      nameSpace: 'oranges'
    });

    // containers events handling
    function registerEvents(el) {
      el.on('dragularenter', function(e) {
        if (el[0] === e.target) { // filter bubbled
          el.addClass(dragularService.shared.extra ? 'gu-over-accept' : 'gu-over-decline');
        }
      });
      el.on('dragularleave dragularrelease', function(e) {
        if ((el[0] === e.target && // filter bubbled
          dragularService.shared.extra && // extra on dragleave contains element the drag is leaving to
          dragularService.shared.extra.parentElement !== e.target) || // is that element child of this container?
          e.type === 'dragularrelease') {
          el.removeClass('gu-over-accept');
          el.removeClass('gu-over-decline');
        }
      });
    }

    angular.forEach($element.children(), function forEachChild(el) {
      registerEvents(angular.element(el));
    });

    // notContainer events handling
    var notContainer = angular.element(document.getElementsByClassName('notContainer'));
    notContainer.on('dragularenter', function() {
      notContainer.addClass('gu-over');
    });
    notContainer.on('dragularleave dragularrelease', function() {
      notContainer.removeClass('gu-over');
    });
  }]);
