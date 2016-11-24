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
class DashboardModelDirective {
  constructor () {
    let directive = {
      restrict: 'E',
      templateUrl: 'app/platform/dashboard/dashboardModel.html',
      scope: {
        id: '@id',
        model: '@model',
        metadata: '@metadata'
      },
      controller: DashboardModelController,
      controllerAs: 'dashboardModelCtrl'
    };

    return directive;
  }
}

class DashboardModelController {
  constructor($scope) {
    'ngInject';
    this.$scope = $scope;
    if (this.$scope.metadata) {
      this.$scope.entity = JSON.parse(this.$scope.metadata);
      this.$scope.entity.id = this.$scope.id;
    }
  }
}

export default DashboardModelDirective;
