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
import _ = require('lodash');
import SidenavService from '../../../components/sidenav/sidenav.service';
import UserService from "../../../services/user.service";

class ApiAdminController {
  private initialApi: any;
  private api: any;
  private groups: any;
  private views: any;
  private tags: any;
  private tenants: any;
  private failoverEnabled: boolean;
  private contextPathEditable: boolean;
  private formApi: any;
  private apiPublic: boolean;

  constructor(
    private ApiService,
    private NotificationService,
    private UserService: UserService,
    private $scope,
    private $mdDialog,
    private $mdEditDialog,
    private $rootScope,
    private $state,
    private GroupService,
    private SidenavService: SidenavService,
    private resolvedViews,
    private resolvedGroups,
    private resolvedTags,
    private resolvedTenants
  ) {
    'ngInject';

    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.UserService = UserService;
    this.GroupService = GroupService;
    this.$scope = $scope;
    this.$rootScope = $rootScope;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;
    this.initialApi = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.api = this.$scope.$parent.apiCtrl.api;
    this.tenants = resolvedTenants.data;
    this.$scope.selected = [];

    this.api.labels = this.api.labels || [];

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

    this.views = resolvedViews;
    this.tags = resolvedTags;
    this.groups = resolvedGroups;
  }

  toggleVisibility() {
    if (this.api.visibility === 'public') {
      this.api.visibility = 'private';
    } else {
      this.api.visibility = 'public';
    }
    this.formApi.$setDirty();
  }

  initState() {
    this.$scope.apiEnabled = (this.$scope.$parent.apiCtrl.api.state === 'started');
    this.$scope.apiPublic = (this.$scope.$parent.apiCtrl.api.visibility === 'public');

    // Failover
    this.failoverEnabled = (this.api.proxy.failover !== undefined);

    // Context-path editable
    this.contextPathEditable =this.UserService.currentUser.username === this.api.owner.username;
  }

  editWeight(event, endpoint) {
    event.stopPropagation(); // in case autoselect is enabled
    var _that = this;

    var editDialog = {
      modelValue: endpoint.weight,
      placeholder: 'Weight',
      save: function (input) {
        endpoint.weight = input.$modelValue;
        _that.formApi.$setDirty();
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

  removeEndpoints() {
    var _that = this;
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to delete endpoint(s) ?',
        msg: '',
        confirmButton: 'Delete'
      }
    }).then(function (response) {
      if (response) {
        _(_that.$scope.selected).forEach(function (endpoint) {
          _(_that.api.proxy.endpoints).forEach(function (endpoint2, index, object) {
            if (endpoint2 !== undefined && endpoint2.name === endpoint.name) {
              object.splice(index, 1);
            }
          });
        });

        that.update(that.api);
      }
    });
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);
    this.formApi.$setPristine();
    this.formApi.$setUntouched();
  }

  delete(id) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to delete \'' + this.api.name + '\' API ?',
        msg: '',
        confirmButton: 'Delete'
      }
    }).then(function (response) {
      if (response) {
        that.ApiService.delete(id).then(() => {
          that.NotificationService.show('API \'' + that.initialApi.name + '\' has been removed');
          that.$state.go('management.apis.list', {}, {reload: true});
        });
      }
    });
  }

  onApiUpdate(updatedApi) {
    this.api = updatedApi;
    this.initState();
    this.formApi.$setPristine();
    this.$rootScope.$broadcast('apiChangeSuccess', {api: this.api});
    this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
    this.SidenavService.setCurrentResource(this.api.name);
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
      controller: 'DialogApiImportController',
      controllerAs: 'dialogApiImportCtrl',
      template: require('./dialog/apiImport.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        apiId: this.$scope.$parent.apiCtrl.api.id
      }
    }).then(function (response) {
      if (response) {
        that.onApiUpdate(response.data);
      }
    });
  }

  showExportDialog() {
    this.$mdDialog.show({
      controller: 'DialogApiExportController',
      controllerAs: 'dialogApiExportCtrl',
      template: require('./dialog/apiExport.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        apiId: this.$scope.$parent.apiCtrl.api.id
      }
    });
  }

  getTenant(tenantId) {
    return _.find(this.tenants, { 'id': tenantId });
  }

  getGroup(groupId) {
    return _.find(this.groups, { 'id': groupId });
  }
}

export default ApiAdminController;
