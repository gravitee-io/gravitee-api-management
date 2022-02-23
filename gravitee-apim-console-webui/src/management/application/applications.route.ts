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
import * as _ from 'lodash';

import { ApplicationType } from '../../entities/application';
import { ApiService } from '../../services/api.service';
import ApplicationService from '../../services/application.service';
import ApplicationTypesService from '../../services/applicationTypes.service';
import EnvironmentService from '../../services/environment.service';
import GroupService from '../../services/group.service';
import MetadataService from '../../services/metadata.service';
import UserService from '../../services/user.service';

export default applicationsConfig;

function applicationsConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.applications', {
      abstract: true,
      url: '/applications',
    })
    .state('management.applications.list', {
      url: '/',
      component: 'applications',
      data: {
        menu: {
          label: 'Applications',
          icon: 'list',
          firstLevel: true,
          order: 20,
        },
        perms: {
          only: ['environment-application-r'],
        },
        docs: {
          page: 'management-applications',
        },
      },
      resolve: {
        applications: (ApplicationService: ApplicationService) => ApplicationService.list().then((response) => response.data),
      },
    })
    .state('management.applications.create', {
      url: '/create',
      component: 'createApplication',
      resolve: {
        enabledApplicationTypes: (ApplicationTypesService: ApplicationTypesService) =>
          ApplicationTypesService.getEnabledApplicationTypes().then((response) =>
            response.data.map((appType) => new ApplicationType(appType)),
          ),
        groups: (GroupService: GroupService) => GroupService.list().then((response) => response.data),
      },
      data: {
        perms: {
          only: ['environment-application-c'],
        },
        docs: {
          page: 'management-create-application',
        },
      },
    })
    .state('management.applications.application', {
      abstract: true,
      url: '/:applicationId',
      component: 'application',
      resolve: {
        application: (
          $stateParams: StateParams,
          ApplicationService: ApplicationService,
          $state: StateService,
          EnvironmentService: EnvironmentService,
          Constants: any,
        ) =>
          ApplicationService.get($stateParams.applicationId)
            .then((response) => response.data)
            .catch((err) => {
              if (err && err.interceptorFuture) {
                $state.go('management.applications.list', {
                  environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id),
                });
              }
            }),
        applicationType: ($stateParams: StateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getApplicationType($stateParams.applicationId)
            .then((response) => response.data)
            .catch((err) => {
              if (err && err.interceptorFuture) {
                err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
              }
            }),
        resolvedApplicationPermissions: ($stateParams: StateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getPermissions($stateParams.applicationId).catch((err) => {
            if (err && err.interceptorFuture) {
              err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
            }
          }),
        onEnter: function (UserService, resolvedApplicationPermissions) {
          UserService.currentUser.userApplicationPermissions = [];
          if (resolvedApplicationPermissions && resolvedApplicationPermissions.data) {
            _.forEach(_.keys(resolvedApplicationPermissions.data), (permission) => {
              _.forEach(resolvedApplicationPermissions.data[permission], (right) => {
                const permissionName = 'APPLICATION-' + permission + '-' + right;
                UserService.currentUser.userApplicationPermissions.push(_.toLower(permissionName));
              });
            });
          }
          UserService.reloadPermissions();
        },
      },
    })
    .state('management.applications.application.general', {
      url: '/',
      component: 'applicationGeneral',
      data: {
        menu: {
          label: 'Global settings',
          icon: 'blur_on',
        },
        perms: {
          only: ['application-definition-r'],
        },
        docs: {
          page: 'management-application',
        },
      },
      resolve: {
        groups: (UserService: UserService, GroupService: GroupService) => {
          return GroupService.list().then((groups) => {
            return _.filter(groups.data, 'manageable');
          });
        },
      },
    })
    .state('management.applications.application.metadata', {
      url: '/metadata',
      component: 'metadata',
      resolve: {
        metadataFormats: (MetadataService: MetadataService) => MetadataService.listFormats(),
        metadata: function ($stateParams, ApplicationService) {
          return ApplicationService.listMetadata($stateParams.applicationId).then((response) => {
            return response.data;
          });
        },
      },
      data: {
        menu: {
          label: 'Metadata',
          icon: 'collections_bookmark',
        },
        perms: {
          only: ['application-metadata-r'],
        },
        docs: {
          page: 'management-application-metadata',
        },
      },
    })
    .state('management.applications.application.subscriptions', {
      abstract: true,
      url: '/subscriptions',
      template: '<div ui-view></div>',
    })
    .state('management.applications.application.subscriptions.list', {
      url: '?page&size&:shared_page&:shared_size&:api&:status&:api_key',
      component: 'applicationSubscriptions',
      resolve: {
        subscribers: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getSubscribedAPI($stateParams.applicationId).then((response) => response.data),
      },
      data: {
        menu: {
          label: 'Subscriptions',
          icon: 'vpn_key',
        },
        perms: {
          only: ['application-subscription-r'],
        },
        docs: {
          page: 'management-application-subscriptions',
        },
      },
      params: {
        status: {
          type: 'string',
          dynamic: true,
        },
        api: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        shared_page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        shared_size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        api_key: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.applications.application.subscriptions.subscription', {
      url: '/:subscriptionId?page&size&:shared_page&:shared_size&:api&:status&:api_key',
      component: 'applicationSubscription',
      resolve: {
        subscription: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getSubscription($stateParams.applicationId, $stateParams.subscriptionId).then((response) => response.data),
      },
      data: {
        perms: {
          only: ['application-subscription-r'],
        },
        docs: {
          page: 'management-application-subscriptions',
        },
      },
      params: {
        status: {
          type: 'string',
          dynamic: true,
        },
        api: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        shared_page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        shared_size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        api_key: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.applications.application.subscriptions.subscribe', {
      url: '/subscribe',
      component: 'applicationSubscribe',
      resolve: {
        apis: (ApiService: ApiService) => ApiService.list(null, true).then((response) => response.data),
        groups: (GroupService: GroupService) => GroupService.list().then((response) => response.data),
        subscriptions: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.listSubscriptions($stateParams.applicationId, '?expand=security').then((response) => response.data),
      },
      data: {
        perms: {
          only: ['application-subscription-r'],
        },
      },
    })
    .state('management.applications.application.members', {
      url: '/members',
      component: 'applicationMembers',
      resolve: {
        members: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getMembers($stateParams.applicationId).then((response) => response.data),
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then((response) => {
            return response.data;
          });
        },
      },
      data: {
        menu: {
          label: 'Members',
          icon: 'group',
        },
        perms: {
          only: ['application-member-r'],
        },
        docs: {
          page: 'management-application-members',
        },
      },
    })
    .state('management.applications.application.analytics', {
      url: '/analytics?from&to&q&dashboard',
      component: 'applicationAnalytics',
      resolve: {},
      data: {
        menu: {
          label: 'Analytics',
          icon: 'insert_chart',
        },
        perms: {
          only: ['application-analytics-r'],
        },
        docs: {
          page: 'management-application-analytics',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        dashboard: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.applications.application.logs', {
      url: '/logs?from&to&q&page&size',
      component: 'applicationLogs',
      data: {
        menu: {
          label: 'Logs',
          icon: 'receipt',
        },
        perms: {
          only: ['application-log-r'],
        },
        docs: {
          page: 'management-application-logs',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
      },
      resolve: {
        apis: ($stateParams: StateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getSubscribedAPI($stateParams.applicationId),
      },
    })
    .state('management.applications.application.log', {
      url: '/logs/:logId?timestamp&from&to&q&page&size',
      component: 'applicationLog',
      resolve: {
        log: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getLog($stateParams.applicationId, $stateParams.logId, $stateParams.timestamp).then(
            (response) => response.data,
          ),
      },
      data: {
        perms: {
          only: ['application-log-r'],
        },
      },
    });
}
