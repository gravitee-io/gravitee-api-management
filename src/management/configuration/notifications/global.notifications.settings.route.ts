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
import NotificationSettingsService from '../../../services/notificationSettings.service';
import AlertService from '../../../services/alert.service';
import {Scope} from '../../../entities/scope';
import NotifierService from '../../../services/notifier.service';

export default applicationsNotificationsRouterConfig;

function applicationsNotificationsRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.settings.notifications', {
      url: '/notifications',
      component: 'notificationsComponent',
      data: {
        menu: {
          label: 'Notifications',
          icon: 'notifications',
        },
        perms: {
          only: ['environment-notification-r']
        }
      },
      resolve: {
        resolvedHookScope: () => Scope.PORTAL,
        resolvedHooks:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(Scope.PORTAL).then( (response) =>
              response.data
            ),
        resolvedNotifiers:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotifiers(Scope.PORTAL, null).then( (response) =>
              response.data
            ),
        notificationSettings:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotificationSettings(Scope.PORTAL, null).then( (response) =>
              response.data
            ),
      }
    })
    .state('management.settings.notifications.notification', {
      url: '/:notificationId',
      component: 'notificationSettingsComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-notifications'
        },
        perms: {
          only: ['environment-notification-r']
        }
      }
    })
    .state('management.settings.alerts', {
      url: '/alerts',
      component: 'alertsComponent',
      data: {
        menu: {
          label: 'Alerts',
          icon: 'alarm',
          parameter: 'alert.enabled'
        },
        perms: {
          only: ['environment-alert-r']
        }
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(undefined, 2).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus($stateParams.apiId, 2).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) =>
          NotifierService.list().then( (response) => response.data)
      }
    })
    .state('management.settings.alertnew', {
      url: '/alert/create',
      component: 'alertComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-alerts'
        },
        perms: {
          only: ['environment-alert-c']
        }
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(undefined, 2).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus($stateParams.apiId, 2).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) =>
          NotifierService.list().then( (response) => response.data)
      }
    })
    .state('management.settings.alert', {
      url: '/alert/:alertId',
      component: 'alertComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-alerts'
        },
        perms: {
          only: ['environment-alert-u']
        }
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(undefined, 2).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus($stateParams.apiId, 2).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) =>
          NotifierService.list().then( (response) => response.data)
      }
    });
}
