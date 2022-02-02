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
import * as _ from 'lodash';
import * as angular from 'angular';
import '@gravitee/ui-components/wc/gv-icon';

import { PromoteApiDialogController } from './dialog/promote-api/promoteApiDialog.controller';

import SidenavService from '../../../../components/sidenav/sidenav.service';
import { QualityMetrics } from '../../../../entities/qualityMetrics';
import { ApiService } from '../../../../services/api.service';
import PolicyService from '../../../../services/policy.service';
import UserService from '../../../../services/user.service';
import InstallationService from '../../../../services/installation.service';
import { Constants } from '../../../../entities/Constants';

class ApiPortalController {
  private initialApi: any;
  private api: any;
  private groups: any;
  private attachableGroups: any;
  private attachedGroups: any;
  private categories: any;
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
  private apiLabelsDictionary = [];

  constructor(
    private ApiService: ApiService,
    private NotificationService,
    private UserService: UserService,
    private PolicyService: PolicyService,
    private $scope,
    private $mdDialog: angular.material.IDialogService,
    private $mdEditDialog,
    private $rootScope,
    private $state,
    private GroupService,
    private SidenavService: SidenavService,
    private resolvedCategories,
    private resolvedGroups,
    private resolvedTags,
    private resolvedTenants,
    private Constants: Constants,
    private qualityRules,
    private InstallationService: InstallationService,
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
        value: 'ROUND_ROBIN',
      },
      {
        name: 'Random',
        value: 'RANDOM',
      },
      {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN',
      },
      {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM',
      },
    ];

    this.$scope.loggingModes = [
      {
        name: 'None',
        value: 'NONE',
      },
      {
        name: 'Client only',
        value: 'CLIENT',
      },
      {
        name: 'Proxy only',
        value: 'PROXY',
      },
      {
        name: 'Client and proxy',
        value: 'CLIENT_PROXY',
      },
    ];

    this.$scope.methods = ['GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'TRACE', 'HEAD'];

    this.initState();

    this.categories = resolvedCategories;

    this.tags = resolvedTags;
    this.groups = resolvedGroups;
    this.attachableGroups = resolvedGroups.filter((group) => group.apiPrimaryOwner == null);
    this.attachedGroups = this.api.groups?.map((groupId) => this.getGroup(groupId)).filter((group) => group.apiPrimaryOwner == null);

    this.headers = [
      'Accept',
      'Accept-Charset',
      'Accept-Encoding',
      'Accept-Language',
      'Accept-Ranges',
      'Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers',
      'Access-Control-Allow-Methods',
      'Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers',
      'Access-Control-Max-Age',
      'Access-Control-Request-Headers',
      'Access-Control-Request-Method',
      'Age',
      'Allow',
      'Authorization',
      'Cache-Control',
      'Connection',
      'Content-Disposition',
      'Content-Encoding',
      'Content-ID',
      'Content-Language',
      'Content-Length',
      'Content-Location',
      'Content-MD5',
      'Content-Range',
      'Content-Type',
      'Cookie',
      'Date',
      'ETag',
      'Expires',
      'Expect',
      'Forwarded',
      'From',
      'Host',
      'If-Match',
      'If-Modified-Since',
      'If-None-Match',
      'If-Unmodified-Since',
      'Keep-Alive',
      'Last-Modified',
      'Location',
      'Link',
      'Max-Forwards',
      'MIME-Version',
      'Origin',
      'Pragma',
      'Proxy-Authenticate',
      'Proxy-Authorization',
      'Proxy-Connection',
      'Range',
      'Referer',
      'Retry-After',
      'Server',
      'Set-Cookie',
      'Set-Cookie2',
      'TE',
      'Trailer',
      'Transfer-Encoding',
      'Upgrade',
      'User-Agent',
      'Vary',
      'Via',
      'Warning',
      'WWW-Authenticate',
      'X-Forwarded-For',
      'X-Forwarded-Proto',
      'X-Forwarded-Server',
      'X-Forwarded-Host',
    ];

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
      this.computeQualityMetrics();
    });

    this.isQualityEnabled = Constants.env.settings.apiQualityMetrics && Constants.env.settings.apiQualityMetrics.enabled;
    this.apiLabelsDictionary = Constants.env.settings.api.labelsDictionary;
    this.qualityMetricsDescription = new Map<string, string>();
    this.qualityMetricsDescription.set('api.quality.metrics.functional.documentation.weight', 'A functional page must be published');
    this.qualityMetricsDescription.set('api.quality.metrics.technical.documentation.weight', 'A swagger page must be published');
    this.qualityMetricsDescription.set('api.quality.metrics.healthcheck.weight', 'An healthcheck must be configured');
    this.qualityMetricsDescription.set('api.quality.metrics.description.weight', 'The API description must be filled');
    this.qualityMetricsDescription.set('api.quality.metrics.logo.weight', 'Put your own logo');
    this.qualityMetricsDescription.set('api.quality.metrics.categories.weight', 'Link your API to categories');
    this.qualityMetricsDescription.set('api.quality.metrics.labels.weight', 'Add labels to your API');
    _.forEach(this.qualityRules, (qualityRule) => {
      this.qualityMetricsDescription.set(qualityRule.id, qualityRule.description);
    });
  }

  $onInit() {
    this.computeQualityMetrics();
    this.$scope.$on('apiPictureChangeSuccess', (event, args) => {
      this.api.picture = args.image;
    });
  }

  computeQualityMetrics() {
    // quality metrics
    if (this.isQualityEnabled) {
      this.ApiService.getQualityMetrics(this.api.id).then((response) => {
        this.qualityMetrics = response.data;
      });
    }
  }

  toggleVisibility() {
    if (this.api.visibility === 'PUBLIC') {
      this.api.visibility = 'PRIVATE';
    } else {
      this.api.visibility = 'PUBLIC';
    }
    this.formApi.$setDirty();
  }

  initState() {
    this.$scope.apiEnabled = this.$scope.$parent.apiCtrl.api.state === 'STARTED';
    this.$scope.apiPublic = this.$scope.$parent.apiCtrl.api.visibility === 'PUBLIC';

    // Failover
    this.failoverEnabled = this.api.proxy.failover !== undefined;

    // Context-path editable
    this.contextPathEditable = this.UserService.currentUser.id === this.api.owner.id;

    this.api.proxy.cors = this.api.proxy.cors || {
      allowOrigin: ['*'],
      allowHeaders: [],
      allowMethods: [],
      exposeHeaders: [],
      maxAge: -1,
      allowCredentials: false,
    };
  }

  editWeight(event, endpoint) {
    event.stopPropagation(); // in case autoselect is enabled
    const editDialog = {
      modelValue: endpoint.weight,
      placeholder: 'Weight',
      save: (input) => {
        endpoint.weight = input.$modelValue;
        this.formApi.$setDirty();
      },
      targetEvent: event,
      title: 'Endpoint weight',
      type: 'number',
      validators: {
        'ng-required': 'true',
        min: 1,
        max: 99,
      },
    };

    const promise = this.$mdEditDialog.large(editDialog);
    promise.then((ctrl) => {
      const input = ctrl.getInput();

      input.$viewChangeListeners.push(() => {
        input.$setValidity('test', input.$modelValue !== 'test');
      });
    });
  }

  removeEndpoints() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete endpoint(s)?',
          msg: '',
          confirmButton: 'Delete',
        },
      })
      .then((response) => {
        if (response) {
          _(this.$scope.selected).forEach((endpoint) => {
            _(this.api.proxy.endpoints).forEach((endpoint2, index, object) => {
              if (endpoint2 !== undefined && endpoint2.name === endpoint.name) {
                (object as any[]).splice(index, 1);
              }
            });
          });

          this.update(this.api);
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
    this.$mdDialog
      .show({
        controller: 'DialogConfirmAndValidateController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmAndValidate.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: "Are you sure you want to delete '" + this.api.name + "'?",
          warning: 'This operation is irreversible.',
          msg: 'The API must be stopped and without any active plans and subscriptions.',
          validationMessage: 'Please, type in the name of the api <code>' + this.api.name + '</code> to confirm.',
          validationValue: this.api.name,
          confirmButton: 'Yes, delete this API',
        },
      })
      .then((response) => {
        if (response) {
          this.ApiService.delete(id).then(() => {
            this.NotificationService.show('API ' + this.initialApi.name + ' has been removed');
            this.$state.go('management.apis.list', {}, { reload: true });
          });
        }
      });
  }

  onApiUpdate(updatedApi) {
    this.api = updatedApi;
    this.initialApi = _.cloneDeep(updatedApi);
    this.formApi.$setPristine();
    this.$rootScope.$broadcast('apiChangeSuccess', { api: _.cloneDeep(updatedApi) });
    this.NotificationService.show("API '" + this.initialApi.name + "' saved");
    this.SidenavService.setCurrentResource(this.api.name);
    this.initState();
    this.computeQualityMetrics();
  }

  update(api) {
    if (!this.failoverEnabled) {
      delete api.proxy.failover;
    }

    this.ApiService.update(api).then((updatedApi) => {
      updatedApi.data.etag = updatedApi.headers('etag');
      this.onApiUpdate(updatedApi.data);
    });
  }

  showImportDialog() {
    this.PolicyService.listSwaggerPolicies().then((policies) => {
      this.$mdDialog
        .show({
          controller: 'DialogApiImportController',
          controllerAs: 'dialogApiImportCtrl',
          template: require('./dialog/apiImport.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            apiId: this.$scope.$parent.apiCtrl.api.id,
            policies: policies.data,
            definitionVersion: this.$scope.$parent.apiCtrl.api.gravitee,
          },
        })
        .then((response) => {
          if (response) {
            this.onApiUpdate(response.data);
          }
        });
    });
  }

  showExportDialog() {
    this.$mdDialog
      .show({
        controller: 'DialogApiExportController',
        controllerAs: 'dialogApiExportCtrl',
        template: require('./dialog/apiExport.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          apiId: this.$scope.$parent.apiCtrl.api.id,
        },
      })
      .then((response) => {
        if (response) {
          this.onApiUpdate(response.data);
        }
      });
  }

  getTenants(tenants) {
    if (tenants !== undefined) {
      return _(tenants)
        .map((tenant) => _.find(this.tenants, { id: tenant }))
        .map((tenant: any) => tenant.name)
        .join(', ');
    }

    return '';
  }

  getGroup(groupId) {
    return _.find(this.groups, { id: groupId });
  }

  /*
   * Search for Labels
   */
  querySearchLabels(query) {
    return query ? this.apiLabelsDictionary.filter(this.createFilterFor(query)) : [];
  }

  /**
   * Create filter function for a query string
   */
  createFilterFor(query) {
    const lowercaseQuery = query.toLowerCase();

    return function filterFn(item) {
      return item.toLowerCase().indexOf(lowercaseQuery) !== -1;
    };
  }

  getQualityMetricCssClass() {
    return this.ApiService.getQualityMetricCssClass(this.qualityMetrics.score * 100);
  }

  changeLifecycle() {
    const started = this.api.state === 'STARTED';
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: `Are you sure you want to ${started ? 'stop' : 'start'} the API?`,
          msg: '',
          confirmButton: started ? 'stop' : 'start',
        },
      })
      .then((response: boolean) => {
        if (response) {
          if (started) {
            this.ApiService.stop(this.api).then((response) => {
              this.api.state = 'STOPPED';
              this.api.etag = response.headers('etag');
              this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
              this.NotificationService.show(`API ${this.api.name} has been stopped with success`);
            });
          } else {
            this.ApiService.start(this.api).then((response) => {
              this.api.state = 'STARTED';
              this.api.etag = response.headers('etag');
              this.NotificationService.show(`API ${this.api.name} has been started with success`);
              this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            });
          }
        }
      });
  }

  changeApiLifecycle(lifecycleState: string) {
    const clonedApi = _.cloneDeep(this.api);
    clonedApi.lifecycle_state = lifecycleState;
    let actionLabel = lifecycleState.slice(0, -1).toLowerCase();
    actionLabel = actionLabel.replace('publishe', 'publish');
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: `Are you sure you want to ${actionLabel} the API?`,
          msg: '',
          confirmButton: _.capitalize(actionLabel),
        },
      })
      .then((response: boolean) => {
        if (response) {
          this.api = clonedApi;
          this.ApiService.update(clonedApi).then((response) => {
            this.api = response.data;
            this.api.etag = response.headers('etag');
            this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            this.NotificationService.show(`API ${this.api.name} has been ${lifecycleState} with success`);
          });
        }
      });
  }

  canAskForReview(): boolean {
    return (
      this.Constants.env.settings.apiReview.enabled &&
      (this.api.workflow_state === 'DRAFT' || this.api.workflow_state === 'REQUEST_FOR_CHANGES' || !this.api.workflow_state)
    );
  }

  canChangeLifecycle(): boolean {
    return (
      !this.Constants.env.settings.apiReview.enabled ||
      (this.Constants.env.settings.apiReview.enabled && (!this.api.workflow_state || this.api.workflow_state === 'REVIEW_OK'))
    );
  }

  canChangeApiLifecycle(): boolean {
    if (this.Constants.env.settings.apiReview.enabled) {
      return !this.api.workflow_state || this.api.workflow_state === 'REVIEW_OK';
    } else {
      return (
        this.api.lifecycle_state === 'CREATED' || this.api.lifecycle_state === 'PUBLISHED' || this.api.lifecycle_state === 'UNPUBLISHED'
      );
    }
  }

  canPublish(): boolean {
    return !this.api.lifecycle_state || this.api.lifecycle_state === 'CREATED' || this.api.lifecycle_state === 'UNPUBLISHED';
  }

  isDeprecated(): boolean {
    return this.api.lifecycle_state === 'DEPRECATED';
  }

  askForReview() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to ask for a review of the API?',
          msg: '',
          confirmButton: 'Ask for review',
        },
      })
      .then((response: boolean) => {
        if (response) {
          this.ApiService.askForReview(this.api).then((response) => {
            this.api.workflow_state = 'IN_REVIEW';
            this.api.etag = response.headers('etag');
            this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            this.NotificationService.show(`Review has been asked for API ${this.api.name}`);
          });
        }
      });
  }

  showDuplicateDialog() {
    this.$mdDialog
      .show({
        controller: 'DialogApiDuplicateController',
        controllerAs: '$ctrl',
        template: require('./dialog/apiDuplicate.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          api: this.$scope.$parent.apiCtrl.api,
        },
      })
      .then((api) => {
        if (api) {
          this.$state.go('management.apis.detail.portal.general', { apiId: api.id });
        }
      });
  }

  showPromoteDialog(): void {
    this.$mdDialog.show({
      controller: PromoteApiDialogController,
      controllerAs: '$ctrl',
      template: require('./dialog/promote-api/promoteApi.dialog.html'),
      clickOutsideToClose: true,
      locals: { api: this.api },
    });
  }

  canPromote(): boolean {
    return this.canChangeApiLifecycle() && !this.isDeprecated();
  }
}

export default ApiPortalController;
