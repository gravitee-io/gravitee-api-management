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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ApplicationsModule } from './applications.module';
import { ApplicationNavigationComponent } from './application-navigation/application-navigation.component';
import { EnvApplicationListComponent } from './list/env-application-list.component';
import { HasApplicationPermissionGuard } from './has-application-permission.guard';
import { ApplicationGeneralComponent } from './details/general/application-general.component';
import { ApplicationMetadataComponent } from './details/metadata/application-metadata.component';
import { ApplicationSubscriptionsComponent } from './details/subscriptions/application-subscriptions.component';
import { ApplicationSubscriptionComponent } from './details/subscriptions/application-subscription.component';
import { ApplicationAnalyticsComponent } from './details/analytics/application-analytics.component';
import { ApplicationLogsComponent } from './details/logs/application-logs.component';
import { ApplicationLogComponent } from './details/logs/application-log.component';

import { ApplicationType } from '../../entities/application';
import ApplicationService from '../../services/application.service';
import ApplicationTypesService from '../../services/applicationTypes.service';
import EnvironmentService from '../../services/environment.service';
import GroupService from '../../services/group.service';
import { HasEnvironmentPermissionGuard } from '../has-environment-permission.guard';

export default applicationsConfig;

function applicationsConfig($stateProvider) {
  $stateProvider
    .state('management.applications.create', {
      url: '/create',
      component: 'createApplication',
      resolve: {
        enabledApplicationTypes: [
          'ApplicationTypesService',
          (ApplicationTypesService: ApplicationTypesService) =>
            ApplicationTypesService.getEnabledApplicationTypes().then((response) =>
              response.data.map((appType) => new ApplicationType(appType)),
            ),
        ],
        groups: ['GroupService', (GroupService: GroupService) => GroupService.list().then((response) => response.data)],
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
      template: require('./details/application.html'),
      controller: [
        'application',
        function (application) {
          this.application = application;
        },
      ],
      controllerAs: '$ctrl',
      resolve: {
        application: [
          '$stateParams',
          'ApplicationService',
          '$state',
          'EnvironmentService',
          'Constants',
          (
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
        ],
        applicationType: [
          '$stateParams',
          'ApplicationService',
          ($stateParams: StateParams, ApplicationService: ApplicationService) =>
            ApplicationService.getApplicationType($stateParams.applicationId)
              .then((response) => response.data)
              .catch((err) => {
                if (err && err.interceptorFuture) {
                  err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
                }
              }),
        ],
        resolvedApplicationPermissions: [
          '$stateParams',
          'ApplicationService',
          ($stateParams: StateParams, ApplicationService: ApplicationService) =>
            ApplicationService.getPermissions($stateParams.applicationId).catch((err) => {
              if (err && err.interceptorFuture) {
                err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
              }
            }),
        ],
        onEnter: [
          'UserService',
          'resolvedApplicationPermissions',
          function (UserService, resolvedApplicationPermissions) {
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
        ],
      },
    })
    .state('management.applications.application.subscriptions.subscribe', {
      url: '/subscribe',
      component: 'applicationSubscribe',
      resolve: {
        groups: ['GroupService', (GroupService: GroupService) => GroupService.list().then((response) => response.data)],
        subscriptions: [
          '$stateParams',
          'ApplicationService',
          ($stateParams, ApplicationService: ApplicationService) =>
            ApplicationService.listSubscriptions($stateParams.applicationId, '?expand=security').then((response) => response.data),
        ],
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
        members: [
          '$stateParams',
          'ApplicationService',
          ($stateParams, ApplicationService: ApplicationService) =>
            ApplicationService.getMembers($stateParams.applicationId).then((response) => response.data),
        ],
        resolvedGroups: [
          'GroupService',
          (GroupService: GroupService) => {
            return GroupService.list().then((response) => {
              return response.data;
            });
          },
        ],
      },
      data: {
        perms: {
          only: ['application-member-r'],
        },
        docs: {
          page: 'management-application-members',
        },
      },
    })
    .state('management.applications.application.membersng', {
      url: '/membersng',
      component: 'applicationGeneralMembersNg',
      data: {
        perms: {
          only: ['application-member-r'],
        },
        useAngularMaterial: true,
        docs: {
          page: 'management-application-members',
        },
      },
    })
    .state('management.applications.application.groupsng', {
      url: '/groupsng',
      component: 'applicationGeneralGroupsNg',
      data: {
        perms: {
          only: ['application-definition-r'],
        },
        useAngularMaterial: true,
        docs: {
          page: 'management-application-groups',
        },
      },
    })
    .state('management.applications.application.transferownershipng', {
      url: '/transferownershipng',
      component: 'applicationGeneralTransferOwnershipNg',
      data: {
        perms: {
          only: ['application-definition-r'],
        },
        useAngularMaterial: true,
        docs: {
          page: 'management-application-transferownership',
        },
      },
    })
    .state('management.applications.application.notification-settings', {
      url: '/notification-settings',
      component: 'applicationNotificationSettingsList',
      data: {
        perms: {
          only: ['application-notification-r', 'application-alert-r'],
        },
        useAngularMaterial: true,
        docs: {
          page: 'management-application-notifications',
        },
      },
    })
    .state('management.applications.application.notification-settings-details', {
      url: '/notification-settings/:notificationId',
      component: 'applicationNotificationSettingsDetails',
      data: {
        docs: {
          perms: {
            only: ['application-notification-r'],
          },
          page: 'management-application-notifications',
        },
        useAngularMaterial: true,
      },
    });
}

const applicationRoutes: Routes = [
  {
    path: '',
    component: EnvApplicationListComponent,
    canActivate: [HasEnvironmentPermissionGuard],
    canActivateChild: [HasEnvironmentPermissionGuard],
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-application-r'],
      },
      docs: {
        page: 'management-applications',
      },
    },
  },
  {
    path: ':applicationId',
    component: ApplicationNavigationComponent,
    canActivate: [HasApplicationPermissionGuard],
    canActivateChild: [HasApplicationPermissionGuard],
    children: [
      {
        path: '',
        redirectTo: 'general',
        pathMatch: 'full',
      },
      {
        path: 'general',
        component: ApplicationGeneralComponent,
      },
      {
        path: 'metadata',
        component: ApplicationMetadataComponent,
      },
      {
        path: 'subscriptions',
        component: ApplicationSubscriptionsComponent,
      },
      {
        path: 'subscriptions/:subscriptionId',
        component: ApplicationSubscriptionComponent,
      },
      {
        path: 'analytics',
        component: ApplicationAnalyticsComponent,
      },
      {
        path: 'logs',
        component: ApplicationLogsComponent,
      },
      {
        path: 'logs/:logId',
        component: ApplicationLogComponent,
      },
    ],
  },
];

@NgModule({
  imports: [ApplicationsModule, RouterModule.forChild(applicationRoutes)],
  exports: [RouterModule],
})
export class ApplicationsRouteModule {}
