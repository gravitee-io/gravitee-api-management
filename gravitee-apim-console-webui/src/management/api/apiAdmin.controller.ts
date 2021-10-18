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
import SidenavService from '../../components/sidenav/sidenav.service';
import UserService from '../../services/user.service';
import NotificationService from '../../services/notification.service';
import ApiService from '../../services/api.service';
import { IScope } from 'angular';

import { StateService } from '@uirouter/core';
import QualityRuleService from '../../services/qualityRule.service';

class ApiAdminController {
  private api: any;
  private apiJustDeployed: boolean;
  private apiIsSynchronized: boolean;
  private menu: any;

  constructor(
    private resolvedApi: any,
    private $state: StateService,
    private $scope: IScope,
    private $rootScope: IScope,
    private $mdDialog: angular.material.IDialogService,
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private resolvedApiState: any,
    private SidenavService: SidenavService,
    private UserService: UserService,
    private Constants,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$state = $state;
    this.$mdDialog = $mdDialog;
    this.$rootScope = $rootScope;

    this.api = resolvedApi.data;
    this.api.etag = resolvedApi.headers('etag');

    SidenavService.setCurrentResource(this.api.name);

    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.UserService = UserService;
    this.apiJustDeployed = false;
    this.apiIsSynchronized = resolvedApiState.data.is_synchronized;
    this.menu = {};
    this.init();
  }

  init() {
    this.$scope.$on('apiPictureChangeSuccess', (event, args) => {
      this.api.picture = args.image;
      this.api.picture_url = args.imageUrl;
      this.update(this.api);
    });
    this.$scope.$on('apiBackgroundChangeSuccess', (event, args) => {
      this.api.background = args.image;
      this.api.background_url = args.imageUrl;
      this.update(this.api);
    });
    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
      this.checkAPISynchronization(this.api);
    });

    this.menu = {
      plans: {
        perm: this.UserService.isUserHasPermissions(['api-plan-r']),
        goTo: 'management.apis.detail.portal.plans.list',
      },
      subscriptions: {
        perm: this.UserService.isUserHasPermissions(['api-subscription-r']),
        goTo: 'management.apis.detail.portal.subscriptions.list',
      },
      documentation: {
        perm: this.UserService.isUserHasPermissions(['api-documentation-r']),
        goTo: 'management.apis.detail.portal.documentation',
      },
      metadata: {
        perm: this.UserService.isUserHasPermissions(['api-metadata-r']),
        goTo: 'management.apis.detail.portal.metadata',
      },
      members: {
        perm: this.UserService.isUserHasPermissions(['api-member-r']),
        goTo: 'management.apis.detail.portal.members',
      },
      groups: {
        perm: this.UserService.isUserHasPermissions(['api-member-r']),
        goTo: 'management.apis.detail.portal.groups',
      },
    };
  }

  checkAPISynchronization(api) {
    this.ApiService.isAPISynchronized(api.id).then((response) => {
      this.apiJustDeployed = false;
      this.apiIsSynchronized = !!response.data.is_synchronized;
      this.$rootScope.$broadcast('checkAPISynchronizationSucceed');
    });
  }

  showDeployAPIConfirm(ev, api) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../components/dialog/confirm.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to deploy your API?',
          confirmButton: 'OK',
        },
      })
      .then((response) => {
        if (response) {
          this.deploy(api);
        }
      });
  }

  showReviewConfirm(ev, api) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogReviewController',
        controllerAs: '$ctrl',
        template: require('./review/review.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          api: api,
        },
        resolve: {
          qualityRules: (QualityRuleService: QualityRuleService) => {
            'ngInject';
            return QualityRuleService.list().then((response) => response.data);
          },
          apiQualityRules: (QualityRuleService: QualityRuleService) => {
            'ngInject';
            return QualityRuleService.listByApi(api.id).then((response) => response.data);
          },
        },
      })
      .then((response) => {
        if (response) {
          if (response.accept) {
            this.ApiService.acceptReview(api, response.message).then((response) => {
              this.api.workflow_state = 'review_ok';
              this.api.etag = response.headers('etag');
              this.NotificationService.show(`Changes accepted for API ${this.api.name}`);
              this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            });
          } else {
            this.ApiService.rejectReview(api, response.message).then((response) => {
              this.api.workflow_state = 'request_for_changes';
              this.api.etag = response.headers('etag');
              this.NotificationService.show(`Changes rejected for API ${this.api.name}`);
              this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            });
          }
        }
      });
  }

  deploy(api) {
    this.ApiService.deploy(api.id).then((deployedApi) => {
      this.NotificationService.show('API deployed');
      this.api = deployedApi.data;
      this.api.etag = deployedApi.headers('etag');
      this.api.picture_url = api.picture_url;
      this.apiJustDeployed = true;
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    });
  }

  update(api) {
    this.ApiService.update(api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.NotificationService.show("API '" + this.api.name + "' saved");
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    });
  }

  canDeploy(): boolean {
    if (this.Constants.env.settings.apiReview.enabled) {
      return !this.api.workflow_state || this.api.workflow_state === 'review_ok';
    } else {
      return true;
    }
  }

  canReview(): boolean {
    return this.Constants.env.settings.apiReview.enabled && this.api.workflow_state === 'in_review';
  }

  isRequestForChanges(): boolean {
    return this.Constants.env.settings.apiReview.enabled && this.api.workflow_state === 'request_for_changes';
  }

  isInDraft(): boolean {
    return this.Constants.env.settings.apiReview.enabled && this.api.workflow_state === 'draft';
  }

  isReviewOK(): boolean {
    return this.Constants.env.settings.apiReview.enabled && this.api.workflow_state === 'review_ok';
  }

  isDeprecated(): boolean {
    return this.api.lifecycle_state === 'deprecated';
  }

  showRequestForChangesConfirm() {
    this.$mdDialog
      .show({
        controller: 'DialogRequestForChangesController',
        controllerAs: '$ctrl',
        template: require('./portal/general/dialog/requestForChanges.dialog.html'),
        clickOutsideToClose: true,
      })
      .then((response) => {
        if (response) {
          this.ApiService.rejectReview(this.api, response.message).then((response) => {
            this.api.workflow_state = 'request_for_changes';
            this.api.etag = response.headers('etag');
            this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            this.NotificationService.show(`Changes has been requested for API ${this.api.name}`);
          });
        }
      });
  }
}

export default ApiAdminController;
