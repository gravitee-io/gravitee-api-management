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

/**
 * This is the directive to custom ui-components integration with ng-model angular directive
 *
 * @example
 * <gv-switch gv-model ng-model="$ctrl.enabled"></gv-switch>
 */
class GvModelDirective {
  constructor() {
    'ngInject';

    return {
      restrict: 'A',
      require: '?ngModel',
      link: function(scope, element, attrs, ngModel) {
        if (!ngModel) {
          return;
        }

        ngModel.$render = () => {
          element[0].value = ngModel.$viewValue;
        };

        element.on(`input`, (e) => {
          ngModel.$setViewValue(e.target.value);
        });

      }
    };

  }
}

export default GvModelDirective;
