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
import { Scope } from '../../../../entities/scope';
import AlertService from '../../../../services/alert.service';
import NotificationSettingsService from '../../../../services/notificationSettings.service';
import { Scope as AlertScope } from '../../../../entities/alert';

export default applicationsNotificationsRouterConfig;

/* @ngInject */
function applicationsNotificationsRouterConfig($stateProvider) {
  $stateProvider
    .state('management.applications.application.notifications', {
      url: '/notifications',
      component: 'notificationsComponent',
      data: {
        perms: {
          only: ['application-notification-r', 'application-alert-r'],
        },
      },
      resolve: {
        resolvedHookScope: () => Scope.APPLICATION,
        resolvedHooks: (NotificationSettingsService: NotificationSettingsService) =>
          NotificationSettingsService.getHooks(Scope.APPLICATION).then((response) => response.data),
        resolvedNotifiers: (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
          NotificationSettingsService.getNotifiers(Scope.APPLICATION, $stateParams.applicationId).then((response) => response.data),
        notificationSettings: (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
          NotificationSettingsService.getNotificationSettings(Scope.APPLICATION, $stateParams.applicationId).then(
            (response) => response.data,
          ),
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(AlertScope.APPLICATION, true, $stateParams.applicationId).then((response) => response.data),
      },
    })
    .state('management.applications.application.notifications.notification', {
      url: '/:notificationId',
      component: 'notificationSettingsComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-application-notifications',
        },
        perms: {
          only: ['application-notification-r'],
        },
      },
    });
}
