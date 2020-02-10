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
import ViewService from '../../services/view.service';
import ApisController from './apis.controller';
import TagService from '../../services/tag.service';
import GroupService from '../../services/group.service';
import * as _ from 'lodash';
import ApiService from '../../services/api.service';
import {StateProvider} from '../../../node_modules/@uirouter/angularjs';
import TenantService from '../../services/tenant.service';
import UserService from '../../services/user.service';

export default apisRouterConfig;

function apisRouterConfig($stateProvider: StateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis', {
      abstract: true,
      url: '/apis'
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
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        },
        resolvedViews: (ViewService: ViewService) => {
          return ViewService.list(true).then(response => {
            return response.data;
          });
        },
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then(response => {
            return response.data;
          });
        },
        resolvedTags: (TagService: TagService) => {
          return TagService.list().then(response => {
            return response.data;
          });
        },
        resolvedTenants: () => [],
        resolvedApiPermissions: (ApiService, $stateParams) => ApiService.getPermissions($stateParams.apiId),
        onEnter: function (UserService, resolvedApiPermissions) {
          UserService.currentUser.userApiPermissions = [];
          _.forEach(_.keys(resolvedApiPermissions.data), function (permission) {
            _.forEach(resolvedApiPermissions.data[permission], function (right) {
              let permissionName = 'API-' + permission + '-' + right;
              UserService.currentUser.userApiPermissions.push(_.toLower(permissionName));
            });
          });
          UserService.reloadPermissions();
        },
        userTags: (UserService: UserService) => UserService.getCurrentUserTags().then(response => response.data)
      }
    })
    .state('management.apis.new', {
      url: '/new',
      template: require('./creation/newApi.html'),
      data: {
        perms: {
          only: ['environment-api-c']
        },
        docs: {
          page: 'management-apis-create'
        }
      }
    })
    .state('management.apis.create', {
      url: '/new/create',
      component: 'apiCreation',
      resolve: {
        tenants: (TenantService: TenantService) => TenantService.list().then(response => response.data),
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        perms: {
          only: ['environment-api-c']
        },
        docs: {
          page: 'management-apis-create-steps'
        }
      }
    })
    .state('management.apis.list', {
      url: '/?q',
      template: require('./apis.html'),
      controller: ApisController,
      controllerAs: '$ctrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService: ApiService) {
          if ($stateParams.q) {
            return ApiService.searchApis($stateParams.q);
          }

          return ApiService.list();
        }
      },
      data: {
        menu: {
          label: 'APIs',
          icon: 'dashboard',
          firstLevel: true,
          order: 10
        },
        docs: {
          page: 'management-apis'
        },
        devMode: true,
        ncyBreadcrumb: {
          label: 'APIs'
        }
      },
      params: {
        q: {
          dynamic: true
        }
      }
    });
}
