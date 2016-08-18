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
        model: '@model'
      },
      controller: DashboardModelController,
      controllerAs: 'dashboardModelCtrl'
    };

    return directive;
  }
}

class DashboardModelController {
  constructor(ApplicationService, ApiService, $scope) {
    'ngInject';
    this.ApplicationService = ApplicationService;
    this.ApiService = ApiService;
    this.$scope = $scope;
    this.$scope.entity = {};

    if (this.$scope.model === 'api') {
      this.getAPI(this.$scope.id, this.$scope.model);
    } else if (this.$scope.model === 'application') {
      this.getApplication(this.$scope.id, this.$scope.model);
    }
  }

  getApplication(id, model) {
    var _this = this;
    if (this.$scope.$parent.dashboardCtrl.cache[id + model]) {
      this.$scope.entity = this.$scope.$parent.dashboardCtrl.cache[id + model];
    } else {
      this.ApplicationService.get(id).then(response => {
        _this.$scope.entity = response.data;
        _this.$scope.$parent.dashboardCtrl.cache[id + model] = _this.$scope.entity;
      });
    }
  }

  getAPI(id, model) {
    var _this = this;
    if (this.$scope.$parent.dashboardCtrl.cache[id + model]) {
      this.$scope.entity = this.$scope.$parent.dashboardCtrl.cache[id + model];
    } else {
      this.ApiService.get(id).then(response => {
        _this.$scope.entity = response.data;
        _this.$scope.$parent.dashboardCtrl.cache[id + model] = _this.$scope.entity;
      });
    }
  }


}

export default DashboardModelDirective;
