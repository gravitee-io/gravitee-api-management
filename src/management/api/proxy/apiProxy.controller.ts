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
import angular = require('angular');
import SidenavService from '../../../components/sidenav/sidenav.service';
import UserService from '../../../services/user.service';

class ApiProxyController {
  private initialApi: any;
  private initialDiscovery: any;

  private api: any;
  private groups: any;
  private views: any;
  private tags: any;
  private tenants: any;
  private failoverEnabled: boolean;
  private contextPathEditable: boolean;
  private formApi: any;
  private apiPublic: boolean;
  private headers: string[];
  private discovery: any;

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
    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.discovery = this.api.services && this.api.services['discovery'];
    this.discovery = this.discovery || {enabled: false, configuration: {}};
    this.initialDiscovery = _.cloneDeep(this.discovery);
    this.tenants = resolvedTenants.data;
    this.$scope.selected = [];

    this.$scope.searchHeaders = null;

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

    this.$scope.methods = ['GET','DELETE','PATCH','POST','PUT','TRACE','HEAD'];

    this.initState();

    this.views = resolvedViews;
    _.remove( this.views, (item) => {
      return item['id'] === 'all';
    });

    this.tags = resolvedTags;
    this.groups = resolvedGroups;

    this.headers = [
      'Accept','Accept-Charset','Accept-Encoding','Accept-Language','Accept-Ranges','Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers','Access-Control-Allow-Methods','Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers','Access-Control-Max-Age','Access-Control-Request-Headers',
      'Access-Control-Request-Method','Age','Allow','Authorization','Cache-Control','Connection','Content-Disposition',
      'Content-Encoding','Content-ID','Content-Language','Content-Length','Content-Location','Content-MD5','Content-Range',
      'Content-Type','Cookie','Date','ETag','Expires','Expect','Forwarded','From','Host','If-Match','If-Modified-Since',
      'If-None-Match','If-Unmodified-Since','Keep-Alive','Last-Modified','Location','Link','Max-Forwards','MIME-Version',
      'Origin','Pragma','Proxy-Authenticate','Proxy-Authorization','Proxy-Connection','Range','Referer','Retry-After',
      'Server','Set-Cookie','Set-Cookie2','TE','Trailer','Transfer-Encoding','Upgrade','User-Agent','Vary','Via',
      'Warning','WWW-Authenticate','X-Forwarded-For','X-Forwarded-Proto','X-Forwarded-Server','X-Forwarded-Host'
    ];

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
    });
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
    this.contextPathEditable =this.UserService.currentUser.id === this.api.owner.id;

    this.api.proxy.cors = this.api.proxy.cors || {allowOrigin: ['*'], allowHeaders: [], allowMethods: [], exposeHeaders: [], maxAge: -1, allowCredentials: false};
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
          _(_that.api.proxy.groups).forEach(function (group) {
            _(group.endpoints).forEach(function (endpoint2, index, object) {
              if (endpoint2 !== undefined && endpoint2.name === endpoint.name) {
                object.splice(index, 1);
              }
            });
          });
        });

        that.update(that.api);
      }
    });
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);
    this.discovery = _.cloneDeep(this.initialDiscovery);

    this.initState();

    if (this.formApi) {
      this.formApi.$setPristine();
      this.formApi.$setUntouched();
    }
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
    this.initialApi = _.cloneDeep(updatedApi);
    this.initState();
    this.formApi.$setPristine();
    this.$rootScope.$broadcast('apiChangeSuccess', {api: _.cloneDeep(updatedApi)});
    this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
    this.SidenavService.setCurrentResource(this.api.name);
  }

  update(api) {
    if (!this.failoverEnabled) {
      delete api.proxy.failover;
    }
    // Discovery is disabled, set dummy values
    if (this.discovery.enabled === false) {
      delete this.discovery.configuration;
    } else {
      // Set default provider
      this.discovery.provider = 'CONSUL';
    }
    this.api.services['discovery'] = this.discovery;

    this.ApiService.update(api).then(updatedApi => {
      updatedApi.data.etag = updatedApi.headers('etag');
      this.onApiUpdate(updatedApi.data);
    });
  }

  getTenants(tenants) {
    if (tenants !== undefined) {
      return _(tenants)
        .map((tenant) => _.find(this.tenants, {'id': tenant}))
        .map((tenant: any) => tenant.name)
        .join(', ');
    }

    return '';
  }

  getGroup(groupId) {
    return _.find(this.groups, { 'id': groupId });
  }

  /**
   * Search for HTTP Headers.
   */
  querySearchHeaders(query) {
    return query ? this.headers.filter(this.createFilterFor(query)) : [];
  }

  /**
   * Create filter function for a query string
   */
  createFilterFor(query) {
    let lowercaseQuery = angular.lowercase(query);

    return function filterFn(header) {
      return angular.lowercase(header).indexOf(lowercaseQuery) === 0;
    };
  }

  createGroup() {
    this.$state.go('management.apis.detail.proxy.group', {groupName: ''});
  }

  deleteGroup(groupname) {
    var _that = this;
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to delete group ' + groupname + '?',
        msg: '',
        confirmButton: 'Delete group'
      }
    }).then(function (response) {
      if (response) {
          _(_that.api.proxy.groups).forEach(function (group, index, object) {
            if (group.name !== undefined && group.name === groupname) {
              object.splice(index, 1);
              that.update(that.api);
            }
          });
      }
    });
  }
}

export default ApiProxyController;
