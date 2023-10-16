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
import { Scope } from '../../../entities/scope';
import AlertService from '../../../services/alert.service';
import NotificationSettingsService from '../../../services/notificationSettings.service';
import NotifierService from '../../../services/notifier.service';
import { Scope as AlertScope } from '../../../entities/alert';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

export default applicationsNotificationsRouterConfig;

function applicationsNotificationsRouterConfig($stateProvider) {
  $stateProvider
    .state('management.settings.notifications', {
      url: '/notifications',
      component: 'notificationsComponentAjs',
      data: {
        perms: {
          only: ['environment-notification-r'],
          unauthorizedFallbackTo: 'management.home',
        },
      },
      resolve: {
        resolvedHookScope: () => Scope.PORTAL,
        resolvedHooks: [
          'NotificationSettingsService',
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(Scope.PORTAL).then((response) => response.data),
        ],
        resolvedNotifiers: [
          'NotificationSettingsService',
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotifiers(Scope.PORTAL, null).then((response) => response.data),
        ],
        notificationSettings: [
          'NotificationSettingsService',
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotificationSettings(Scope.PORTAL, null).then((response) => response.data),
        ],
      },
    })
    .state('management.settings.notifications.notification', {
      url: '/:notificationId',
      component: 'notificationsComponentAjs',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-notifications',
        },
        perms: {
          only: ['environment-notification-r'],
        },
      },
    })
    .state('management.alerts', {
      url: '/alerts',
      template: require('../../../components/alerts/alertTabs/alert-tabs.html'),
      controller: 'AlertTabsController',
      controllerAs: '$ctrl',
    })
    .state('management.alerts.activity', {
      url: '/activity',
      template: require('../../../components/alerts/activity/alerts-activity.html'),
      controller: 'AlertsActivityController',
      controllerAs: '$ctrl',
      resolve: {
        configuredAlerts: [
          'AlertService',
          (AlertService: AlertService) => AlertService.listAlerts(AlertScope.ENVIRONMENT, false).then((response) => response.data),
        ],
        alertingStatus: [
          'AlertService',
          (AlertService: AlertService) => AlertService.getStatus(AlertScope.ENVIRONMENT).then((response) => response.data),
        ],
      },
      data: {
        docs: {
          page: 'management-dashboard-alerts',
        },
      },
    })
    .state('management.alerts.list', {
      url: '/list',
      component: 'alertsComponentAjs',
      data: {
        requireLicense: {
          license: { feature: ApimFeature.ALERT_ENGINE },
          redirect: 'management',
        },
        perms: {
          only: ['environment-alert-r'],
        },
        docs: {
          page: 'management-alerts',
        },
      },
      resolve: {
        alerts: [
          'AlertService',
          (AlertService: AlertService) => AlertService.listAlerts(AlertScope.ENVIRONMENT).then((response) => response.data),
        ],
        status: [
          'AlertService',
          '$stateParams',
          (AlertService: AlertService, $stateParams) =>
            AlertService.getStatus(AlertScope.ENVIRONMENT, $stateParams.apiId).then((response) => response.data),
        ],
        notifiers: ['NotifierService', (NotifierService: NotifierService) => NotifierService.list().then((response) => response.data)],
        mode: () => 'detail',
      },
    })
    .state('management.alertnew', {
      url: '/alerts/create',
      component: 'alertComponentAjs',
      data: {
        menu: null,
        docs: {
          page: 'management-alerts',
        },
        perms: {
          only: ['environment-alert-c'],
        },
      },
      resolve: {
        alerts: [
          'AlertService',
          (AlertService: AlertService) => AlertService.listAlerts(AlertScope.ENVIRONMENT).then((response) => response.data),
        ],
        status: [
          'AlertService',
          '$stateParams',
          (AlertService: AlertService, $stateParams) =>
            AlertService.getStatus(AlertScope.ENVIRONMENT, $stateParams.apiId).then((response) => response.data),
        ],
        notifiers: ['NotifierService', (NotifierService: NotifierService) => NotifierService.list().then((response) => response.data)],
        mode: () => 'create',
      },
    })
    .state('management.editalert', {
      url: '/alerts/:alertId?:tab',
      component: 'alertComponentAjs',
      data: {
        menu: null,
        docs: {
          page: 'management-alerts',
        },
        perms: {
          only: ['environment-alert-r'],
        },
      },
      resolve: {
        alerts: [
          'AlertService',
          (AlertService: AlertService) => AlertService.listAlerts(AlertScope.ENVIRONMENT).then((response) => response.data),
        ],
        status: [
          'AlertService',
          '$stateParams',
          (AlertService: AlertService, $stateParams) =>
            AlertService.getStatus(AlertScope.ENVIRONMENT, $stateParams.apiId).then((response) => response.data),
        ],
        notifiers: ['NotifierService',(NotifierService: NotifierService) => NotifierService.list().then((response) => response.data)],
      },
    });
}
applicationsNotificationsRouterConfig.$inject = ['$stateProvider'];
