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
import CategoryService from '../../services/category.service';
import ApisController from './apis.controller';
import TagService from '../../services/tag.service';
import GroupService from '../../services/group.service';
import * as _ from 'lodash';
import ApiService from '../../services/api.service';
import { StateProvider } from '../../../node_modules/@uirouter/angularjs';
import TenantService from '../../services/tenant.service';
import UserService from '../../services/user.service';
import PolicyService from '../../services/policy.service';
import EnvironmentService from '../../services/environment.service';
import { StateParams, StateService } from '@uirouter/core';

export default apisRouterConfig;

function apisRouterConfig($stateProvider: StateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis', {
      abstract: true,
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
        resolvedCategories: (CategoryService: CategoryService) => {
          return CategoryService.list().then((response) => {
            return response.data;
          });
        },
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then((response) => {
            return response.data;
          });
        },
        resolvedApiGroups: ($stateParams: StateParams, ApiService: ApiService, UserService: UserService) => {
          if (UserService.isUserHasPermissions(['api-member-r'])) {
            return ApiService.getGroupsWithMembers($stateParams.apiId).then((response) => {
              return response.data;
            });
          }
        },
        resolvedTags: (TagService: TagService) => {
          return TagService.list().then((response) => {
            return response.data;
          });
        },
        resolvedTenants: () => [],
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
        userTags: (UserService: UserService) => UserService.getCurrentUserTags().then((response) => response.data),
      },
    })
    .state('management.apis.new', {
      url: '/new',
      template: require('./creation/newApi.html'),
      controller: 'NewApiController',
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
    .state('management.apis.list', {
      url: '/?q',
      template: require('./apis.html'),
      controller: ApisController,
      controllerAs: '$ctrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService: ApiService) {
          if ($stateParams.q) {
            return ApiService.searchApis($stateParams.q, 1);
          }

          return ApiService.list(null, false, 1);
        },
      },
      data: {
        menu: {
          label: 'APIs',
          icon: 'dashboard',
          firstLevel: true,
          order: 10,
        },
        docs: {
          page: 'management-apis',
        },
        ncyBreadcrumb: {
          label: 'APIs',
        },
      },
      params: {
        q: {
          dynamic: true,
        },
      },
    });
}
