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
class AnalyticsAPIModelDirective {
  constructor () {
    let directive = {
      restrict: 'E',
      templateUrl: 'app/application/details/analytics/analyticsAPIModel.html',
      scope: {
        id: '@id'
      },
      controller: AnalyticsAPIModelController,
      controllerAs: 'analyticsAPIModelCtrl'
    };

    return directive;
  }
}

class AnalyticsAPIModelController {
  constructor($scope) {
    'ngInject';
    this.$scope = $scope;
    this.$scope.entity = {};
    this.getAPI(this.$scope.id);
  }

  getAPI(id) {
    if (this.$scope.$parent.analyticsCtrl.cache[id]) {
      this.$scope.entity = this.$scope.$parent.analyticsCtrl.cache[id];
    }
  }


}

export default AnalyticsAPIModelDirective;
