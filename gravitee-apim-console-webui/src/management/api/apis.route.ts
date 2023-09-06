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
import { StateParams, StateService } from '@uirouter/core';
import { StateProvider } from '@uirouter/angularjs';
import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import EnvironmentService from '../../services/environment.service';
import GroupService from '../../services/group.service';
import PolicyService from '../../services/policy.service';
import TagService from '../../services/tag.service';
import TenantService from '../../services/tenant.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';
import { isApiV4 } from '../../util';

export default apisRouterConfig;

/* @ngInject */
function apisRouterConfig($stateProvider: StateProvider) {
  $stateProvider
    .state('management.apis', {
      abstract: true,
      template: '<div flex layout="column" ui-view></div>',
      url: '/apis',
    })
    .state('management.apis.detail', {
      abstract: true,
      url: '/:apiId',
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApiState: function ($stateParams, ApiService) {
          return ApiService.isAPISynchronized($stateParams.apiId);
        },
        resolvedApiV1V2: function (
          $stateParams: StateParams,
          ApiService: ApiService,
          ngApiV2Service: ApiV2Service,
          $state: StateService,
          Constants: any,
          EnvironmentService: EnvironmentService,
          $timeout: angular.ITimeoutService,
        ) {
          return ngApiV2Service
            .get($stateParams.apiId)
            .toPromise()
            .then((api) => {
              if (isApiV4(api)) {
                // 📝 Best effort to redirect to the right new route state (no mapping for $stateParams as not necessary for current routes)
                // Used for the task page links
                const ngStateRedirectionMap = {
                  'management.apis.detail.portal.general': 'management.apis.ng.general',
                  'management.apis.detail.portal.subscription.edit': 'management.apis.ng.subscription.edit',
                };

                const ngRedirectToState = ngStateRedirectionMap[$state.transition.to().name];
                if (ngRedirectToState) {
                  $state.transition.abort();

                  $timeout(() => {
                    $state.go(ngRedirectToState, $stateParams, { location: 'replace' });
                  });
                  return;
                }
                // If thrown, Check origin state link and update it to new Angular one. Or add redirect mapping upper
                throw new Error(`Illegal state: V4 API should never trigger navigation to this route ${$state.current.name}.`);
              }
              return ApiService.get($stateParams.apiId).catch((err) => {
                if (err && err.interceptorFuture) {
                  $state.go('management.apis.list', {
                    environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id),
                  });
                }
              });
            });
        },
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then((response) => {
            return response.data;
          });
        },
        resolvedApiPermissions: (ApiService: ApiService, $stateParams: StateParams) =>
          ApiService.getPermissions($stateParams.apiId).catch((err) => {
            if (err && err.interceptorFuture) {
              err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
            }
          }),
        onEnter: function (UserService, resolvedApiPermissions) {
          UserService.currentUser.userApiPermissions = [];
          if (resolvedApiPermissions && resolvedApiPermissions.data) {
            _.forEach(_.keys(resolvedApiPermissions.data), (permission) => {
              _.forEach(resolvedApiPermissions.data[permission], (right) => {
                const permissionName = 'API-' + permission + '-' + right;
                UserService.currentUser.userApiPermissions.push(_.toLower(permissionName));
              });
            });
          }
          UserService.reloadPermissions();
        },
      },
    })
    .state('management.apis.new', {
      url: '/new',
      component: 'ngApiCreationGetStartedComponent',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create',
        },
      },
    })
    .state('management.apis.new-import', {
      url: '/new/import/:definitionVersion',
      template: require('./creation/newApiImport.html'),
      controller: 'NewApiImportController',
      controllerAs: '$ctrl',
      resolve: {
        policies: (PolicyService: PolicyService) => PolicyService.listSwaggerPolicies().then((response) => response.data),
      },
      data: {
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create',
        },
      },
    })
    .state('management.apis.create-v4-confirmation', {
      url: '/new/create/v4/confirmation/:apiId',
      component: 'ngApiCreationV4ConfirmationComponent',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
        },
      },
    })
    .state('management.apis.create-v4', {
      url: '/new/create/v4',
      component: 'ngApiCreationV4Component',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
        },
      },
    })
    .state('management.apis.create-v2', {
      url: '/new/create/v2',
      component: 'apiCreation',
      resolve: {
        groups: (GroupService: GroupService) => GroupService.list().then((response) => response.data),
        tenants: (TenantService: TenantService) => TenantService.list().then((response) => response.data),
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
      },
      data: {
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create-steps',
        },
      },
    })
    .state('management.apis.ng-list', {
      url: '/?q&page&size&order',
      component: 'ngApiList',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-apis',
        },
        ncyBreadcrumb: {
          label: 'APIs',
        },
      },
      params: {
        page: {
          value: '1',
          dynamic: true,
        },
        q: {
          dynamic: true,
        },
        size: {
          value: '10',
          dynamic: true,
        },
        order: {
          dynamic: true,
        },
      },
    });
}
