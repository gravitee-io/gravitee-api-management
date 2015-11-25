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
  constructor (ApiService, resolvedApi, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.apis = [];


    if ($state.current.name.includes('general')) {
      $scope.selectedTab = 0;
    } else if ($state.current.name.includes('policies')) {
      $scope.selectedTab = 1;
    } else if ($state.current.name.includes('documentation')) {
      $scope.selectedTab = 2;
    } else if ($state.current.name.includes('analytics')) {
      $scope.selectedTab = 3;
    } else if ($state.current.name.endsWith('members')) {
      $scope.selectedTab = 4;
    }

    var that = this;
    $scope.$on('$stateChangeSuccess', function (ev, to, toParams, from) {
      if (from.name.startsWith('apis.list.')) {
        that.previousState = from.name;
      }
    });

    this.initState();
  }

  initState() {
    this.$scope.apiEnabled = this.api.state === 'started'? true : false;
  }

  changeLifecycle(id) {
    var started = this.api.state === 'started';
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to ' + (started?'un':'') +'publish the api ' + id + '?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        if (started) {
          that.ApiService.stop(id);
        } else {
          that.ApiService.start(id);
        }
      })
      .catch(function () {
        that.initState();
      });
  }

  delete(id) {
    this.ApiService.delete(id).then(() => {
      this.backToPreviousState();
    });
  }

  update(api) {
    this.ApiService.update(api).then(() => {
    });
  }

  backToPreviousState() {
    if (!this.previousState) {
      this.previousState = 'apis.list.thumb';
    }
    this.$state.go(this.previousState);
  }
}

export default ApiAdminController;
