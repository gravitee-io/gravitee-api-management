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
class ApiAdminController {
  constructor (resolvedApi, $state, $scope, $rootScope, $mdDialog, ApiService, NotificationService, resolvedApiState) {
    'ngInject';
    this.$scope = $scope;
    this.$state = $state;
    this.$mdDialog = $mdDialog;
    this.$rootScope = $rootScope;
    this.api = resolvedApi.data;

    $rootScope.currentResource = this.api.name;

    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.apiJustDeployed = false;
    this.apiIsSynchronized = resolvedApiState.data.is_synchronized;
    this.init();
  }

  init() {
    var self = this;
    this.$scope.$on("apiChangeSuccess", function() {
      self.checkAPISynchronization(self.api);
      self.$rootScope.$broadcast("apiChangeSucceed");
    });
  }

  checkAPISynchronization(api) {
    this.ApiService.isAPISynchronized(api.id).then(response => {
      this.apiJustDeployed = false;
      if (response.data.is_synchronized) {
        this.apiIsSynchronized = true;
      } else {
        this.apiIsSynchronized = false;
      }
      this.$rootScope.$broadcast("checkAPISynchronizationSucceed");
    });
  }

  showDeployAPIConfirm(ev, api) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to deploy your API?')
      .ariaLabel('deploy-api')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var self = this;
    this.$mdDialog.show(confirm).then(function() {
      self.deploy(api);
    }, function() {
      self.$mdDialog.cancel();
    });
  }

  deploy(api) {
    this.ApiService.deploy(api.id).then((deployedApi) => {
      this.NotificationService.show("API deployed");
      this.api = deployedApi.data;
      this.apiJustDeployed = true;
      this.$rootScope.$broadcast("apiChangeSuccess");
    });
  }

  isOwner() {
    return this.api.permission && (this.api.permission === 'owner' || this.api.permission === 'primary_owner');
  }
}

export default ApiAdminController;
