/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
angular.module("schemaForm").run(["$templateCache", function($templateCache) {$templateCache.put("directives/decorators/bootstrap/codemirror/codemirror.html","<div class=\"form-group\" ng-class=\"{\'has-error\': hasError()}\" ng-init=\"codemirrorOptions = {foo: \'bar\'}\">\n  <label class=\"control-label\" ng-show=\"showTitle()\">{{form.title}}</label>\n  <div ng-if=\"form.codemirrorButtons\" class=\"cm-buttons\">\n    <span class=\"btn-group\" ng-repeat=\"buttonGroup in form.codemirrorButtons\">\n      <button ng-repeat=\"button in buttonGroup\" ng-click=\"evalInScope(button.onClick)\" type=\"button\" class=\"btn btn-sm btn-default\" id=\"btnBold\" title=\"{{ button.title }}\">\n        <span ng-if=\"button.icon\" class=\"glyphicon\" ng-class=\"\'glyphicon-\' + button.icon\"></span>\n        <span ng-if=\"button.label\" ng-bind-html=\"button.label\"></span>\n      </button>\n    </span>\n  </div>\n  <div codemirror-buttons ui-codemirror ui-codemirror-opts=\"getCodemirrorOptions()\" ng-style=\"form.style\" ng-model=\"$$value$$\" schema-validate=\"form\"></div>\n  <span class=\"help-block\">{{ (hasError() && errorMessage(schemaError())) || form.description}}</span>\n</div>\n");}]);
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
