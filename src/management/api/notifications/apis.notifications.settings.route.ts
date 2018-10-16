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
import _ = require('lodash');
import NotificationSettingsService from '../../../services/notificationSettings.service';
import {Scope} from '../../../entities/scope';
import AlertService from "../../../services/alert.service";
import {Alert} from "../../components/notifications/alert/alert";
import ApiService from "../../../services/api.service";

export default apisNotificationsRouterConfig;

function apisNotificationsRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.notifications', {
      url: '/notifications',
      component: 'notificationsComponent',
      data: {
        menu: {
          label: 'Notifications',
          icon: 'notifications',
        },
        perms: {
          only: ['api-notification-r', 'api-alert-r']
        }
      },
      resolve: {
        resolvedHookScope: () => Scope.API,
        resolvedHooks:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(Scope.API).then((response) =>
              response.data
            ),
        resolvedNotifiers:
          (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
            NotificationSettingsService.getNotifiers(Scope.API, $stateParams.apiId).then((response) =>
              response.data
            ),
        notificationSettings:
          (NotificationSettingsService: NotificationSettingsService, $stateParams) =>
            NotificationSettingsService.getNotificationSettings(Scope.API, $stateParams.apiId).then((response) =>
              response.data
            ),
        api: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId).then((response) => response.data);
        },
        alerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts($stateParams.apiId, 'api').then((response) => response.data),
        alertMetrics: function (AlertService: AlertService) {
          return AlertService.listMetrics().then((response) => response.data);
        },
        plans: function ($stateParams, ApiService: ApiService) {
          return ApiService.getPublishedApiPlans($stateParams.apiId).then((response) => response.data);
        }
      }
    })
    .state('management.apis.detail.notifications.notificationSetting', {
      url: '?notificationId',
      component: 'notificationSettingsComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-api-notifications'
        },
        perms: {
          only: ['api-notification-r']
        }
      }
    })
    .state('management.apis.detail.notifications.alert', {
      url: '?alertId',
      component: 'alertComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-alerts'
        },
        perms: {
          only: ['api-alert-r']
        }
      }
    });
}
