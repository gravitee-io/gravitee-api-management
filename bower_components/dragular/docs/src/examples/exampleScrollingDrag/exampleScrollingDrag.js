/* global angular */
'use strict';

var examplesAppModule = require('../examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .controller('ScrollingDrag', ['$interval', '$element', 'dragularService', function TodoCtrl($interval, $element, dragularService) {


    var timer,
      leftScrollContainer = document.getElementById('leftScroll'),
      rightScrollContainer = document.getElementById('rightScroll'),
      leftTopBar = document.getElementById('leftTopBar'),
      leftBottomBar = document.getElementById('leftBottomBar'),
      rightTopBar = document.getElementById('rightTopBar'),
      rightBottomBar = document.getElementById('rightBottomBar');

    dragularService.cleanEnviroment();
    dragularService([leftScrollContainer, rightScrollContainer]);

    registerEvents(leftTopBar, leftScrollContainer, -5);
    registerEvents(leftBottomBar, leftScrollContainer, 5);
    registerEvents(rightTopBar, rightScrollContainer, -5);
    registerEvents(rightBottomBar, rightScrollContainer, 5);

    function registerEvents(bar, container, inc, speed) {
      if (!speed) {
        speed = 20;
      }
      angular.element(bar).on('dragularenter', function() {
        container.scrollTop += inc;
        timer = $interval(function moveScroll() {
          container.scrollTop += inc;
        }, speed);
      });
      angular.element(bar).on('dragularleave dragularrelease', function() {
        $interval.cancel(timer);
      });
    }
  }]);
