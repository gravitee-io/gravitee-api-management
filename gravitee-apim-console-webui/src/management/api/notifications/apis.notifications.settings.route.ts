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

import { Scope } from '../../../entities/scope';
import AlertService from '../../../services/alert.service';
import { ApiService } from '../../../services/api.service';
import EnvironmentService from '../../../services/environment.service';
import NotificationSettingsService from '../../../services/notificationSettings.service';
import NotifierService from '../../../services/notifier.service';
import { Scope as AlertScope } from '../../../entities/alert';

export default apisNotificationsRouterConfig;

/* @ngInject */
function apisNotificationsRouterConfig($stateProvider) {
  $stateProvider
    .state('management.apis.detail.notifications', {
      url: '/notifications',
      component: 'notificationsComponentAjs',
      data: {
        perms: {
          only: ['api-notification-r'],
        },
      },
      resolve: {
        resolvedHookScope: () => Scope.API,
        resolvedHooks: (NotificationSettingsService: NotificationSettingsService) =>
          NotificationSettingsService.getHooks(Scope.API).then((response) => response.data),
        resolvedNotifiers: (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
          NotificationSettingsService.getNotifiers(Scope.API, $stateParams.apiId).then((response) => response.data),
        notificationSettings: (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
          NotificationSettingsService.getNotificationSettings(Scope.API, $stateParams.apiId).then((response) => response.data),
        api: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId).then((response) => response.data);
        },
        plans: function ($stateParams, ApiService: ApiService) {
          return ApiService.getPublishedApiPlans($stateParams.apiId).then((response) => response.data);
        },
      },
    })
    .state('management.apis.detail.notifications.notification', {
      url: '/:notificationId',
      component: 'notificationsComponentAjs',
      data: {
        docs: {
          page: 'management-api-notifications',
        },
        perms: {
          only: ['api-notification-r'],
        },
      },
    })
    .state('management.apis.detail.alerts', {
      abstract: true,
      url: '/alerts',
    })
    .state('management.apis.detail.alerts.list', {
      url: '/',
      component: 'alertsComponentAjs',
      data: {
        perms: {
          only: ['api-alert-r'],
        },
        docs: {
          page: 'management-alerts',
        },
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus(AlertScope.API, $stateParams.apiId).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) => NotifierService.list().then((response) => response.data),
      },
    })
    .state('management.apis.detail.alerts.alertnew', {
      url: '/create',
      component: 'alertComponentAjs',
      data: {
        docs: {
          page: 'management-alerts',
        },
        perms: {
          only: ['api-alert-c'],
        },
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus(AlertScope.API, $stateParams.apiId).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) => NotifierService.list().then((response) => response.data),
        mode: () => 'create',
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
      },
    })
    .state('management.apis.detail.alerts.editalert', {
      url: '/:alertId?:tab',
      component: 'alertComponentAjs',
      data: {
        docs: {
          page: 'management-alerts',
        },
        perms: {
          only: ['api-alert-r'],
        },
      },
      resolve: {
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data),
        status: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus(AlertScope.API, $stateParams.apiId).then((response) => response.data),
        notifiers: (NotifierService: NotifierService) => NotifierService.list().then((response) => response.data),
        mode: () => 'detail',
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
      },
    });
}
