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
  constructor(ApiService, NotificationService, $scope, $mdDialog, $mdEditDialog, $rootScope, resolvedApi, base64, Upload) {
    'ngInject';
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$rootScope = $rootScope;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;
    this.initialApi = _.cloneDeep(resolvedApi.data);
    this.api = resolvedApi.data;
    this.$scope.selected = [];
    this.base64 = base64;
    this.Upload = Upload;

    this.$scope.lbs = [
      {
        name: 'Round-Robin',
        value: 'ROUND_ROBIN'
      }, {
        name: 'Random',
        value: 'RANDOM'
      }, {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN'
      }, {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM'
      }];

    this.initState();
  }

  initState() {
    this.$scope.apiEnabled = (this.$scope.$parent.apiCtrl.api.state === 'started');
    this.failoverEnabled = (this.api.proxy.failover !== undefined);
    if (!this.failoverEnabled) {
      // Set default values
      this.api.proxy.failover = {
        maxAttempts: 1,
        retryTimeout: 0,
        cases: ['TIMEOUT']
      }
    }
    var self = this;
    this.$scope.$on("apiChangeSucceed", function () {
      self.initialApi = _.cloneDeep(self.$scope.$parent.apiCtrl.api);
      self.api = self.$scope.$parent.apiCtrl.api;
    });
  }

  changeLifecycle(id) {
    var started = this.api.state === 'started';
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to ' + (started ? 'stop' : 'start') + ' the API?',
      ok: 'OK',
      cancel: 'Cancel'
    });
    var that = this;
    this.$mdDialog
      .show(alert)
      .then(function () {
        if (started) {
          that.ApiService.stop(id).then(function () {
            that.api.state = 'stopped';
            that.NotificationService.show('API ' + that.initialApi.name + ' has been stopped!');
          });
        } else {
          that.ApiService.start(id).then(function () {
            that.api.state = 'started';
            that.NotificationService.show('API ' + that.initialApi.name + ' has been started!');
          });
        }
      })
      .catch(function () {
        that.initState();
      });
  }

  editEndpoint(event, endpoint) {
    event.stopPropagation(); // in case autoselect is enabled
    var _that = this;

    var editDialog = {
      modelValue: endpoint.target,
      placeholder: 'Target URL',
      save: function (input) {
        endpoint.target = input.$modelValue;
        _that.$scope.formApi.$setDirty();
      },
      targetEvent: event,
      title: 'Endpoint target URL',
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

  editWeight(event, endpoint) {
    event.stopPropagation(); // in case autoselect is enabled
    var _that = this;

    var editDialog = {
      modelValue: endpoint.weight,
      placeholder: 'Weight',
      save: function (input) {
        endpoint.weight = input.$modelValue;
        _that.$scope.formApi.$setDirty();
      },
      targetEvent: event,
      title: 'Endpoint weight',
      type: 'number',
      validators: {
        'ng-required': 'true',
        'min': 1,
        'max': 99
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

  addEndpoint(event) {
    var _that = this;
    this.$mdDialog.show({
      clickOutsideToClose: true,
      controller: 'DialogEndpointController',
      controllerAs: 'ctrl',
      focusOnOpen: false,
      targetEvent: event,
      templateUrl: 'app/api/admin/general/add-endpoint-dialog.html',
    }).then(function (endpoint) {
      if (endpoint) {
        _that.api.proxy.endpoints.push(endpoint);
        _that.$scope.formApi.$setDirty();
      }
    });
  }

  removeEndpoints(event) {
    var _that = this;
    _(this.$scope.selected).forEach(function (endpoint) {
      _(_that.api.proxy.endpoints).forEach(function (endpoint2, index, object) {
        if (endpoint2 !== undefined && endpoint2.target === endpoint.target) {
          object.splice(index, 1);
        }
      });
    });

    this.$scope.formApi.$setDirty();
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);
    this.$scope.formApi.$setPristine();
  }

  delete(id) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to delete \'' + this.api.name + '\' API ?',
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

  onApiUpdate(updatedApi) {
    this.api = updatedApi;
    this.initState();
    this.$scope.formApi.$setPristine();
    this.$rootScope.$broadcast("apiChangeSuccess");
    this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
  }

  update(api) {
    if (!this.failoverEnabled) {
      delete api.proxy.failover;
    }

    this.ApiService.update(api).then(updatedApi => {
      this.onApiUpdate(updatedApi.data);
    });
  }

  showImportDialog() {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogApiDefinitionController',
      controllerAs: 'dialogApiDefinitionCtrl',
      templateUrl: 'app/api/admin/general/dialog/apiDefinition.dialog.html',
      apiId: this.$scope.$parent.apiCtrl.api.id
    }).then(function (response) {
      if (response) {
        that.onApiUpdate(response.data);
      }
    });
  }

  export(id) {
    var that = this;
    this.ApiService.export(id).then(function (response) {
      var link = document.createElement('a');
      document.body.appendChild(link);
      link.href = 'data:application/json;charset=utf-8;base64,' + that.base64.encode(JSON.stringify(response.data, null, 2));
      var contentDispositionHeader = response.headers('content-disposition') || response.headers('Content-Disposition');
      link.download = contentDispositionHeader.split('=')[1];      link.target = "_self";
      link.click();
      document.body.removeChild(link);
    });
  }
}

export default ApiAdminController;
