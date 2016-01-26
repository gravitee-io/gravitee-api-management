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
  constructor (ApiService, NotificationService, $scope, $mdDialog, $mdEditDialog, $rootScope, resolvedApi) {
    'ngInject';
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$rootScope = $rootScope;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;
    this.initialApi = _.cloneDeep(resolvedApi.data);
    this.$scope.selected = [];

    this.initState();
  }

  initState() {
    this.$scope.apiEnabled = this.$scope.$parent.apiCtrl.api.state === 'started'? true : false;
  }

  changeLifecycle(id) {
    var started = this.$scope.$parent.apiCtrl.api.state === 'started';
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to ' + (started?'un':'') +'publish \'' + this.initialApi.name + '\' API ?',
      ok: 'OK',
      cancel: 'Cancel'
    });
    var that = this;
    this.$mdDialog
      .show(alert)
      .then(function () {
        if (started) {
          that.ApiService.stop(id).then(function () {
            that.$scope.$parent.apiCtrl.api.state = 'stopped';
            that.NotificationService.show('API ' + that.initialApi.name + ' has been stopped !');
          });
        } else {
          that.ApiService.start(id).then(function () {
            that.$scope.$parent.apiCtrl.api.state = 'started';
            that.NotificationService.show('API ' + that.initialApi.name + ' has been started !');
          });
        }
      })
      .catch(function () {
        that.initState();
      });
  }

  editEndpoint(event, endpoint) {
    event.stopPropagation(); // in case autoselect is enabled

    var editDialog = {
      modelValue: endpoint.target,
      placeholder: 'Target URL',
      save: function (input) {
        endpoint.target = input.$modelValue;
      },
      targetEvent: event,
      title: 'Set endpoint target URL',
      type: 'url',
      validators: {
        'ng-required': 'true'
      }
    };

    var promise = this.$mdEditDialog.large(editDialog);

    promise.then(function (ctrl) {
      var input = ctrl.getInput();

      input.$viewChangeListeners.push(function () {
        input.$setValidity('test', input.$modelValue !== 'test');
      });
    });
  }

  reset() {
    this.$scope.$parent.apiCtrl.api = _.cloneDeep(this.initialApi);
    this.$scope.formApi.$setPristine();
  }

  delete(id) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to delete \'' + this.$scope.$parent.apiCtrl.api.name + '\' API ?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        that.ApiService.delete(id).then(() => {
          that.NotificationService.show('API \'' + that.initialApi.name + '\' has been removed');
          that.$scope.$parent.apisCtrl.backToPreviousState();
        });
      });
  }

  update(api) {
    this.ApiService.update(api).then((updatedApi) => {
      this.$scope.$parent.apiCtrl.api = updatedApi.data;
      this.initState();
      this.$scope.formApi.$setPristine();
      this.$rootScope.$broadcast("apiChangeSuccess");
      this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
    });
  }
}

export default ApiAdminController;
