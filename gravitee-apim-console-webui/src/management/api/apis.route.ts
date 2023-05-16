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
      template: require('./apiAdmin.html'),
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApiState: function ($stateParams, ApiService) {
          return ApiService.isAPISynchronized($stateParams.apiId);
        },
        resolvedApi: function (
          $stateParams: StateParams,
          ApiService: ApiService,
          $state: StateService,
          Constants: any,
          EnvironmentService: EnvironmentService,
        ) {
          return ApiService.get($stateParams.apiId).catch((err) => {
            if (err && err.interceptorFuture) {
              $state.go('management.apis.list', { environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id) });
            }
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
    .state('management.apis.create', {
      url: '/new/create/:definitionVersion',
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
