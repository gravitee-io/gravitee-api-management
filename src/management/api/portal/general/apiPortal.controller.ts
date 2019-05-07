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
import SidenavService from '../../../../components/sidenav/sidenav.service';
import UserService from '../../../../services/user.service';
import {QualityMetrics} from "../../../../entities/qualityMetrics";
import ApiService from "../../../../services/api.service";

class ApiPortalController {
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
  private headers: string[];
  private qualityMetrics: QualityMetrics;
  private qualityMetricsDescription: Map<string, string>;
  private isQualityEnabled: boolean;

  constructor(
    private ApiService: ApiService,
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
    private resolvedTenants,
    private Constants
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

    this.$scope.loggingModes = [
      {
        name: 'None',
        value: 'NONE'
      }, {
        name: 'Client only',
        value: 'CLIENT'
      }, {
        name: 'Proxy only',
        value: 'PROXY'
      }, {
        name: 'Client and proxy',
        value: 'CLIENT_PROXY'
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

    this.isQualityEnabled = Constants.apiQualityMetrics && Constants.apiQualityMetrics.enabled;
    this.qualityMetricsDescription = new Map<string, string>();
    this.qualityMetricsDescription.set("api.quality.metrics.functional.documentation.weight", "A functional page must be published");
    this.qualityMetricsDescription.set("api.quality.metrics.technical.documentation.weight", "A swagger page must be published");
    this.qualityMetricsDescription.set("api.quality.metrics.healthcheck.weight", "An healthcheck must be configured");
    this.qualityMetricsDescription.set("api.quality.metrics.description.weight", "The API description must be filled");
    this.qualityMetricsDescription.set("api.quality.metrics.logo.weight", "Put your own logo");
    this.qualityMetricsDescription.set("api.quality.metrics.views.weight", "Link your API to views");
    this.qualityMetricsDescription.set("api.quality.metrics.labels.weight", "Add labels to your API");
  }

  $onInit() {
    this.computeQualityMetrics();
  }

  computeQualityMetrics() {
    //quality metrics
    if (this.isQualityEnabled) {
      this.ApiService.getQualityMetrics(this.api.id).then(response => {
        this.qualityMetrics = response.data;
      });
    }
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
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
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
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
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
    this.formApi.$setPristine();
    this.$rootScope.$broadcast('apiChangeSuccess', {api: _.cloneDeep(updatedApi)});
    this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
    this.SidenavService.setCurrentResource(this.api.name);
    this.initState();
    this.computeQualityMetrics();
  }

  update(api) {
    if (!this.failoverEnabled) {
      delete api.proxy.failover;
    }

    this.ApiService.update(api).then(updatedApi => {
      updatedApi.data.etag = updatedApi.headers('etag');
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

  showExportDialog(showExportVersion) {
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

  getQualityMetricCssClass() {
    return this.ApiService.getQualityMetricCssClass(this.qualityMetrics.score * 100);
  }

  changeLifecycle() {
    let started = this.api.state === 'started';
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: `Are you sure you want to ${started ? 'stop' : 'start'} the API?`,
        msg: '',
        confirmButton: (started ? 'stop' : 'start')
      }
    }).then((response: boolean) => {
      if (response) {
        if (started) {
          this.ApiService.stop(this.api).then((response) => {
            this.api.state = 'stopped';
            this.api.etag = response.headers('etag');
            this.$rootScope.$broadcast("apiChangeSuccess", {api: this.api});
            this.NotificationService.show(`API ${this.api.name} has been stopped with success`);
          });
        } else {
          this.ApiService.start(this.api).then((response) => {
            this.api.state = 'started';
            this.api.etag = response.headers('etag');
            this.NotificationService.show(`API ${this.api.name} has been started with success`);
            this.$rootScope.$broadcast("apiChangeSuccess", {api: this.api});
          });
        }
      }
    })
  }

  changeApiLifecycle() {
    let clonedApi = _.cloneDeep(this.api);
    let published = clonedApi.lifecycle_state === 'published';
    clonedApi.lifecycle_state = published ? 'unpublished' : 'published';
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: `Are you sure you want to ${published ? 'unpublish' : 'publish'} the API?`,
        msg: '',
        confirmButton: (published ? 'unpublish' : 'publish')
      }
    }).then((response: boolean) => {
      if (response) {
        this.api = clonedApi;
        if (published) {
          this.ApiService.update(clonedApi).then((response) => {
            this.api.lifecycle_state = 'unpublished';
            this.api.etag = response.headers('etag');
            this.$rootScope.$broadcast("apiChangeSuccess", {api: this.api});
            this.NotificationService.show(`API ${this.api.name} has been unpublished with success`);
          });
        } else {
          this.ApiService.update(clonedApi).then((response) => {
            this.api.lifecycle_state = 'published';
            this.api.etag = response.headers('etag');
            this.NotificationService.show(`API ${this.api.name} has been published with success`);
            this.$rootScope.$broadcast("apiChangeSuccess", {api: this.api});
          });
        }
      }
    })
  }

  canAskForReview(): boolean {
    return this.Constants.apiReview.enabled && (this.api.workflow_state === 'draft' || this.api.workflow_state === 'request_for_changes');
  }

  canChangeLifecycle(): boolean {
    return !this.Constants.apiReview.enabled || (this.Constants.apiReview.enabled && (!this.api.workflow_state || this.api.workflow_state === 'review_ok'));
  }

  canChangeApiLifecycle(): boolean {
    if (this.Constants.apiReview.enabled) {
      return !this.api.workflow_state || this.api.workflow_state === 'review_ok';
    } else {
      return this.api.lifecycle_state==='created' || this.api.lifecycle_state==='published' || this.api.lifecycle_state==='unpublished';
    }
  }

  canPublish(): boolean {
    return !this.api.lifecycle_state || this.api.lifecycle_state==='created' || this.api.lifecycle_state==='unpublished';
  }

  askForReview() {
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: `Are you sure you want to ask for a review of the API?`,
        msg: '',
        confirmButton: 'Ask for review'
      }
    }).then((response: boolean) => {
      if (response) {
        this.ApiService.askForReview(this.api).then((response) => {
          this.api.workflow_state = 'in_review';
          this.api.etag = response.headers('etag');
          this.$rootScope.$broadcast("apiChangeSuccess", {api: this.api});
          this.NotificationService.show(`Review has been asked for API ${this.api.name}`);
        });
      }
    })
  }
}

export default ApiPortalController;
