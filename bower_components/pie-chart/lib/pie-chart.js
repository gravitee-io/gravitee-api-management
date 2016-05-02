angular.module('n3-pie-chart', ['n3-pie-utils'])

.directive('pieChart', ['$utils', function($utils) {
  var link  = function($scope, element, attrs, ctrl) {
    var svg;
    var dim = $utils.getDefaultMargins();

    var updateDimensions = function(dimensions) {
      dimensions.width = element[0].parentElement.offsetWidth || 900;
      dimensions.height = element[0].parentElement.offsetHeight || 500;
    };

    var update = function(data, options) {
      $utils
        .updatePaths(svg, data, dim, options)
        .updateLegend(svg, data, dim, options)
      ;
    };

    var hard_update = function(data, options) {
      $utils.clean(element[0]);
      updateDimensions(dim);
      draw(data, options, dim);
    };

    var draw = function(data, options, dimensions) {
      if (!data || !options) {
        return;
      }

      options = $utils.sanitizeOptions(options);

      svg = $utils.bootstrap(element[0], dimensions);

      $utils
        .draw(svg)
        .updatePaths(svg, data, dimensions, options)
        .addLegend(svg)
        .updateLegend(svg, data, dimensions, options)
      ;
    };

    $scope.$watch('data', function(newValue, oldValue) {
      newValue = $utils.addDataForGauge(newValue, $scope.options);

      if (svg) {
        update(newValue, $scope.options);
      } else {
        hard_update(newValue, $scope.options);
      }
    }, true);
    $scope.$watch('options', function(newValue) {
      var data = $utils.addDataForGauge($scope.data, newValue);
      hard_update(data, newValue);
    }, true);
  };

  return {
    replace: true,
    restrict: 'E',
    scope: {data: '=', options: '='},
    template: '<div></div>',
    link: link
  };
}]);