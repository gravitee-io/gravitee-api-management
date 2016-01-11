

// vPaneHeader directive
angular.module('vAccordion.directives')
  .directive('vPaneHeader', vPaneHeaderDirective);


function vPaneHeaderDirective () {
  return {
    restrict: 'E',
    require: ['^vPane', '^vAccordion'],
    transclude: true,
    template: '<div ng-transclude></div>',
    scope: {},
    link: function (scope, iElement, iAttrs, ctrls) {
      iAttrs.$set('role', 'tab');

      var paneCtrl = ctrls[0],
          accordionCtrl = ctrls[1];

      var isInactive = angular.isDefined(iAttrs.inactive);

      function onClick () {
        if (isInactive) { return false; }
        scope.$apply(function () { paneCtrl.toggle(); });
      }

      function onKeyDown (event) {
        if (event.keyCode === 32  || event.keyCode === 13) {
          scope.$apply(function () { paneCtrl.toggle(); });
          event.preventDefault();
        } else if (event.keyCode === 39) {
          scope.$apply(function () { accordionCtrl.focusNext(); });
          event.preventDefault();
        } else if (event.keyCode === 37) {
          scope.$apply(function () { accordionCtrl.focusPrevious(); });
          event.preventDefault();
        }
      }

      iElement[0].onfocus = function () {
        paneCtrl.focusPane();
      };

      iElement[0].onblur = function () {
        paneCtrl.blurPane();
      };

      iElement.bind('click', onClick);
      iElement.bind('keydown', onKeyDown);

      scope.$on('$destroy', function () {
        iElement.unbind('click', onClick);
        iElement.unbind('keydown', onKeyDown);
        iElement[0].onfocus = null;
        iElement[0].onblur = null;
      });
    }
  };
}
