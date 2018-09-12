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
import {HookScope} from "../../entities/hookScope";
import NotificationSettingsService from "../../services/notificationSettings.service";

export default applicationsConfig;

function applicationsConfig($stateProvider) {
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
        devMode: true,
        docs: {
          page: 'management-applications'
        }
      },
      resolve: {
        applications: (ApplicationService: ApplicationService) => ApplicationService.list().then(response => response.data)
      }
    })
    .state('management.applications.new', {
      url: '/new',
      component: 'createApplication',
      data: {
        perms: {
          only: ['portal-application-c']
        },
        devMode: true,
        docs: {
          page: 'management-create-application'
        }
      },
      resolve: {
        applications: (ApplicationService: ApplicationService) => ApplicationService.list().then(response => response.data)
      }
    })
    .state('management.applications.application', {
      abstract: true,
      url: '/:applicationId',
      component: 'application',
      resolve: {
        application: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.get($stateParams.applicationId).then(response => response.data),
        resolvedApplicationPermissions: (ApplicationService, $stateParams) => ApplicationService.getPermissions($stateParams.applicationId),
        onEnter: function (UserService, resolvedApplicationPermissions) {
          if (!UserService.currentUser.userApplicationPermissions) {
            UserService.currentUser.userApplicationPermissions = [];
            _.forEach(_.keys(resolvedApplicationPermissions.data), function (permission) {
              _.forEach(resolvedApplicationPermissions.data[permission], function (right) {
                let permissionName = 'APPLICATION-' + permission + '-' + right;
                UserService.currentUser.userApplicationPermissions.push(_.toLower(permissionName));
              });
            });
            UserService.reloadPermissions();
          }
        }
      }
    })
    .state('management.applications.application.general', {
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
        },
        docs: {
          page: 'management-application'
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
    .state('management.applications.application.subscriptions', {
      abstract: true,
      url: '/subscriptions',
      template: '<div ui-view></div>'
    })
    .state('management.applications.application.subscriptions.list', {
      url: '',
      component: 'applicationSubscriptions',
      resolve: {
        subscriptions: ($stateParams, ApplicationService: ApplicationService) =>
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
        },
        docs: {
          page: 'management-application-subscriptions'
        }
      }
    })
    .state('management.applications.application.subscriptions.subscription', {
      url: '/:subscriptionId',
      component: 'applicationSubscription',
      resolve: {
        subscription: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getSubscription($stateParams.applicationId, $stateParams.subscriptionId).then(response => response.data)
      },
      data: {
        perms: {
          only: ['application-subscription-r']
        },
        docs: {
          page: 'management-application-subscriptions'
        }
      }
    })
    .state('management.applications.application.members', {
      url: '/members',
      component: 'applicationMembers',
      resolve: {
        members: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getMembers($stateParams.applicationId).then(response => response.data),
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
        },
        docs: {
          page: 'management-application-members'
        }
      }
    })
    .state('management.applications.application.analytics', {
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
        },
        docs: {
          page: 'management-application-analytics'
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
    .state('management.applications.application.logs', {
      url: '/logs?from&to&q',
      component: 'applicationLogs',
      data: {
        menu: {
          label: 'Logs',
          icon: 'receipt'
        },
        devMode: true,
        perms: {
          only: ['application-log-r']
        },
        docs: {
          page: 'management-application-logs'
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
    .state('management.applications.application.log', {
      url: '/logs/:logId?timestamp&from&to&q',
      component: 'applicationLog',
      resolve: {
        log: ($stateParams, ApplicationService: ApplicationService) =>
          ApplicationService.getLog($stateParams.applicationId, $stateParams.logId, $stateParams.timestamp).then(response => response.data)
      },
      data: {
        devMode: true,
        perms: {
          only: ['application-log-r']
        }
      }
    })
    .state('management.applications.application.notifications', {
      url: '/notifications',
      component: 'notificationSettingsComponent',
      data: {
        menu: {
          label: 'Notifications',
          icon: 'notifications',
        },
        docs: {
          page: 'management-application-notifications'
        },
        perms: {
          only: ['application-notification-r']
        }
      },
      resolve: {
        resolvedHookScope: () => HookScope.APPLICATION,
        resolvedHooks:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(HookScope.APPLICATION).then( (response) =>
              response.data
            ),
        resolvedNotifiers:
          (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
            NotificationSettingsService.getNotifiers(HookScope.APPLICATION, $stateParams.applicationId).then( (response) =>
              response.data
            ),
        resolvedNotificationSettings:
          (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
            NotificationSettingsService.getNotificationSettings(HookScope.APPLICATION, $stateParams.applicationId).then( (response) =>
              response.data
            )
      }
    });
}
