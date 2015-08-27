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
class ApiController {
  constructor (ApiService, $stateParams, PolicyService, $mdDialog) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;
    this.$mdDialog = $mdDialog;

    this.apis = [];
    if ($stateParams.apiName) {
      this.get($stateParams.apiName);
      this.listPolicies($stateParams.apiName);
    } else {
      this.list();
    }

    this.selectedPolicy = null;
  }

  get(apiName) {
    this.ApiService.get(apiName).then(response => {
      this.api = response.data;
      this.api.policies = [this.api.onRequestPolicies, this.api.onResponsePolicies];
    });
  }

  list() {
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  start(name) {
    this.ApiService.start(name).then(() => {
      this.list();
    });
  }

  stop(name) {
    this.ApiService.stop(name).then(() => {
      this.list();
    });
  }

  delete(name) {
    this.ApiService.delete(name).then(() => {
      this.list();
    });
  }

  listPolicies(apiName) {
    this.PolicyService.list(apiName).then(response => {
      // TODO filter request, response and request/response policies
      this.policies = {
        'OnRequest': response.data,
        'OnResponse': [],
        'OnRequest/OnResponse': []
      };
    });
  }

  showAddApiModal() {
    var that = this;
    this.$mdDialog.show({
      controller: DialogApiController,
      templateUrl: 'app/api/api.dialog.html',
      parent: angular.element(document.body)
    }).then(function (api) {
      if (api) {
        that.list();
      }
    });
  }
}

function DialogApiController($scope, $mdDialog, ApiService, TeamService) {
  'ngInject';

  TeamService.list().then(response => {
    $scope.teams = response.data;
  });

  $scope.hide = function () {
    $mdDialog.hide();
  };

  $scope.create = function (api) {
    ApiService.create(api, $scope.team).then(function () {
      $mdDialog.hide(api);
    }).catch(function (error) {
      console.log(error)
      $scope.error = error;
    });
  };
}

export default ApiController;
