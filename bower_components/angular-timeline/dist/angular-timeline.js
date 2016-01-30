'use strict';
angular.module('angular-timeline', []);// Source: src/timeline-badge-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline.directive:timeline-badge
 * @restrict AE
 *
 * @description
 * Shown in the centre pane (or left on narrow devices) to indicate the activity.
 */
angular.module('angular-timeline').directive('timelineBadge', function() {
  return {
    require: '^timelineEvent',
    restrict: 'AE',
    transclude: true,
    template: '<div ng-transclude class="timeline-badge"></div>'
  };
});

// Source: src/timeline-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline
 * @restrict AE
 *
 * @description
 * Primary container for displaying a vertical set of timeline events.
 */
angular.module('angular-timeline').directive('timeline', function() {
  return {
    restrict: 'AE',
    transclude: true,
    template: '<ul class="timeline" ng-transclude></ul>',
    controller: function() {}
  };
});

// Source: src/timeline-event-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline.directive:timeline
 * @restrict AE
 *
 * @description
 * Represents an event occuring at a point in time, displayed on the left or the right
 * of the timeline line.
 *
 * You typically embed a `timeline-badge` and `timeline-panel` element within a `timeline-event`.
 *
 * @param {string=} side  Define the side of the element (i.e. side="left", side="right", or use an {{ expression }}).
 */

angular.module('angular-timeline').directive('timelineEvent', function() {
  return {
    require: '^timeline',
    restrict: 'AE',
    transclude: true,
    template: '<li ng-class-odd="oddClass" ng-class-even="evenClass" ng-transclude></li>',
    link: function(scope, element, attrs, controller) {

      var checkClass = function(side, leftSide) {

        var leftClass = '';
        var rightClass = 'timeline-inverted';

        if (side === 'left' || (!side && leftSide === true)) {
          return leftClass;
        }
        else if ((side === 'alternate' || !side) && leftSide === false) {
          return rightClass;
        }
        else if (side === 'right') {
          return rightClass;
        }
        else {
          return leftClass;
        }
      };

      var updateRowClasses = function(value) {
        scope.oddClass = checkClass(value, true);
        scope.evenClass = checkClass(value, false);
      };

      attrs.$observe('side', function(newValue) {
        updateRowClasses(newValue);
      });

      updateRowClasses(attrs.side);
    }
  };
});

// Source: src/timeline-footer-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline.directive:timeline-footer
 * @restrict AE
 *
 * @description
 * Optional element to add a footer section to the `timeline-panel` for links or other actions.
 */
angular.module('angular-timeline').directive('timelineFooter', function() {
  return {
    require: '^timelinePanel',
    restrict: 'AE',
    transclude: true,
    template: '<div class="timeline-footer" ng-transclude></div>'
  };
});

// Source: src/timeline-heading-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline.directive:timeline-heading
 * @restrict AE
 *
 * @description
 * Optional element to show the heading for a `timeline-panel`.
 */
angular.module('angular-timeline').directive('timelineHeading', function() {
  return {
    require: '^timelinePanel',
    restrict: 'AE',
    transclude: true,
    template: '<div class="timeline-heading" ng-transclude></div>'
  };
});

// Source: src/timeline-panel-directive.js
/**
 * @ngdoc directive
 * @name angular-timeline.directive:timeline-panel
 * @restrict AE
 *
 * @description
 * An panel inside the `timeline-event` which shows detailed information about the event.
 */
angular.module('angular-timeline').directive('timelinePanel', function() {
  return {
    require: '^timeline',
    restrict: 'AE',
    transclude: true,
    template: '<div class="timeline-panel" ng-transclude></div>'
  };
});
