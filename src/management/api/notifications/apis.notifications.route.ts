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
import {HookScope} from '../../../entities/hookScope';

export default apisNotificationsRouterConfig;

function apisNotificationsRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.notifications', {
      url: '/notifications?:notificationId',
      component: 'notificationSettingsComponent',
      data: {
        menu: {
          label: 'Notifications',
          icon: 'notifications',
        },
        docs: {
          page: 'management-api-notifications'
        },
        perms: {
          only: ['api-notification-r']
        }
      },
      resolve: {
        resolvedHookScope: () => HookScope.API,
        resolvedHooks:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(HookScope.API).then((response) =>
              response.data
            ),
        resolvedNotifiers:
          (NotificationSettingsService: NotificationSettingsService, $stateParams: ng.ui.IStateParamsService) =>
            NotificationSettingsService.getNotifiers(HookScope.API, $stateParams.apiId).then((response) =>
              response.data
            ),
        resolvedNotificationSettings:
          (NotificationSettingsService: NotificationSettingsService, $stateParams: ng.ui.IStateParamsService) =>
            NotificationSettingsService.getNotificationSettings(HookScope.API, $stateParams.apiId).then((response) =>
              response.data
            ),
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId).then((response) => response.data);
        }
      }
    });
}
