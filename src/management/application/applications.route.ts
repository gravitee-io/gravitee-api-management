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
import ApplicationService from '../../services/applications.service';
import GroupService from '../../services/group.service';
import * as _ from 'lodash';
import UserService from "../../services/user.service";

export default applicationsConfig;

function applicationsConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.applications', {
      abstract: true,
      url: '/applications'
    })
    .state('management.applications.list', {
      url: '/',
      component: 'applications',
      data: {
        menu: {
          label: 'Applications',
          icon: 'list',
          firstLevel: true,
          order: 20
        },
        devMode: true
      },
      resolve: {
        applications: (ApplicationService: ApplicationService) => ApplicationService.list().then(response => response.data)
      },
    })
    .state('management.applications.portal', {
      abstract: true,
      url: '/:applicationId',
      component: 'application',
      resolve: {
        application: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.get($stateParams.applicationId).then(response => response.data),
        onEnter: function (UserService, ApplicationService, $stateParams) {
          if (!UserService.currentUser.userApplicationPermissions) {
            UserService.currentUser.userApplicationPermissions = [];
            ApplicationService.getPermissions($stateParams.applicationId).then(permissions => {
              _.forEach(_.keys(permissions.data), function (permission) {
                _.forEach(permissions.data[permission], function (right) {
                  let permissionName = 'APPLICATION-' + permission + '-' + right;
                  UserService.currentUser.userApplicationPermissions.push(_.toLower(permissionName));
                });
              });
              UserService.reloadPermissions();
            });
          }
        }
      }
    })
    .state('management.applications.portal.general', {
      url: '/',
      component: 'applicationGeneral',
      data: {
        menu: {
          label: 'Global settings',
          icon: 'blur_on'
        },
        devMode: true,
        perms: {
          only: ['application-definition-r']
        }
      },
      resolve: {
        groups: (UserService: UserService, GroupService: GroupService) => {
          if (UserService.currentUser.isAdmin()) {
            return GroupService.list().then((groups) => {
              return groups.data;
            });
          } else {
            return [];
          }
        }
      }
    })
    .state('management.applications.portal.subscriptions', {
      url: '/subscriptions',
      component: 'applicationSubscriptions',
      resolve: {
        subscriptions: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.listSubscriptions($stateParams.applicationId).then(response => response.data)
      },
      data: {
        menu: {
          label: 'Subscriptions',
          icon: 'vpn_key'
        },
        devMode: true,
        perms: {
          only: ['application-subscription-r']
        }
      }
    })
    .state('management.applications.portal.members', {
      url: '/members',
      component: 'applicationMembers',
      resolve: {
        members: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.getMembers($stateParams['applicationId']).then(response => response.data),
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then(response => {
            return response.data;
          });
        },
      },
      data: {
        menu: {
          label: 'Members',
          icon: 'group'
        },
        devMode: true,
        perms: {
          only: ['application-member-r']
        }
      }
    })
    .state('management.applications.portal.analytics', {
      url: '/analytics?from&to&q',
      component: 'applicationAnalytics',
      data: {
        menu: {
          label: 'Analytics',
          icon: 'insert_chart'
        },
        devMode: true,
        perms: {
          only: ['application-analytics-r']
        }
      },
      params: {
        from: {
          type: "int",
          dynamic: true
        },
        to: {
          type: "int",
          dynamic: true
        },
        q: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.applications.portal.logs', {
      url: '/logs?from&to&q',
      component: 'applicationLogs',
      data: {
        menu: {
          label: 'Logs',
          icon: 'receipt'
        },
        perms: {
          only: ['application-log-r']
        }
      },
      params: {
        from: {
          type: 'int',
          dynamic: true
        },
        to: {
          type: 'int',
          dynamic: true
        },
        q: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.applications.portal.log', {
      url: '/logs/:logId',
      component: 'applicationLog',
      resolve: {
        log: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.getLog($stateParams['applicationId'], $stateParams['logId']).then(response => response.data)
      },
      data: {
        perms: {
          only: ['application-log-r']
        }
      }
    });
}
