angular.module('schemaForm')
  .config(['schemaFormProvider', 'schemaFormDecoratorsProvider', function(schemaFormProvider, schemaFormDecoratorsProvider) {
    // Add to the bootstrap directive
    schemaFormDecoratorsProvider.addMapping('bootstrapDecorator',
      'codemirror',
      'directives/decorators/bootstrap/codemirror/codemirror.html');
    schemaFormDecoratorsProvider.createDirective('codemirror',
      'directives/decorators/bootstrap/codemirror/codemirror.html');
  }])

  .directive('codemirrorButtons', function() {
    return {
      controller: ['$scope', function($scope) {
        $scope.getCodemirrorOptions = function() {
          var opts = angular.copy($scope.form.codemirrorOptions);
          opts.onLoad = function(cm) {
            $scope.cm = cm;
          };
          return opts;
        };
      }]
    };
  });
