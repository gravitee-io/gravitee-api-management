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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ApplicationsModule } from './applications.module';
import { ApplicationNavigationComponent } from './application-navigation/application-navigation.component';
import { EnvApplicationListComponent } from './list/env-application-list.component';
import { HasApplicationPermissionGuard } from './has-application-permission.guard';
import { ApplicationMetadataComponent } from './details/metadata/application-metadata.component';
import { ApplicationSubscriptionsComponent } from './details/subscriptions/application-subscriptions.component';
import { ApplicationSubscriptionComponent } from './details/subscriptions/application-subscription.component';
import { ApplicationAnalyticsComponent } from './details/analytics/application-analytics.component';
import { ApplicationLogsComponent } from './details/logs/application-logs.component';
import { ApplicationLogComponent } from './details/logs/application-log.component';
import { ApplicationNotificationSettingsListComponent } from './details/notifications/notification-settings/notification-settings-list/application-notification-settings-list.component';
import { ApplicationNotificationSettingsDetailsComponent } from './details/notifications/notification-settings/notification-settings-details/application-notification-settings-details.component';
import { ApplicationCreationComponent } from './creation/steps/application-creation.component';
import { ApplicationSubscribeComponent } from './details/subscribe/application-subscribe.component';
import { ApplicationGeneralMembersComponent } from './details/user-group-access/members/application-general-members.component';
import { ApplicationGeneralGroupsComponent } from './details/user-group-access/groups/application-general-groups.component';
import { ApplicationGeneralTransferOwnershipComponent } from './details/user-group-access/transfer-ownership/application-general-transfer-ownership.component';
import { ApplicationGeneralNgComponent } from './details/general/general-ng/application-general-ng.component';

import { HasEnvironmentPermissionGuard } from '../has-environment-permission.guard';

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
    path: 'create',
    component: ApplicationCreationComponent,
    canActivate: [HasEnvironmentPermissionGuard],
    data: {
      perms: {
        only: ['environment-application-c'],
      },
      docs: {
        page: 'management-create-application',
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
        path: 'metadata',
        component: ApplicationMetadataComponent,
        data: {
          perms: {
            only: ['application-metadata-r'],
          },
          docs: {
            page: 'management-application-metadata',
          },
        },
      },
      {
        path: 'subscriptions',
        component: ApplicationSubscriptionsComponent,
        data: {
          perms: {
            only: ['application-subscription-r'],
          },
          docs: {
            page: 'management-application-subscriptions',
          },
        },
      },
      {
        path: 'subscriptions/subscribe',
        component: ApplicationSubscribeComponent,
        data: {
          perms: {
            only: ['application-subscription-r'],
          },
        },
      },
      {
        path: 'subscriptions/:subscriptionId',
        component: ApplicationSubscriptionComponent,
        data: {
          perms: {
            only: ['application-subscription-r'],
          },
          docs: {
            page: 'management-application-subscriptions',
          },
        },
      },
      {
        path: 'analytics',
        component: ApplicationAnalyticsComponent,
        data: {
          perms: {
            only: ['application-analytics-r'],
          },
          docs: {
            page: 'management-application-analytics',
          },
        },
      },
      {
        path: 'logs',
        component: ApplicationLogsComponent,
        data: {
          perms: {
            only: ['application-log-r'],
          },
          docs: {
            page: 'management-application-logs',
          },
        },
      },
      {
        path: 'logs/:logId',
        component: ApplicationLogComponent,
        data: {
          perms: {
            only: ['application-log-r'],
          },
        },
      },
      {
        path: 'notification-settings',
        component: ApplicationNotificationSettingsListComponent,
        data: {
          perms: {
            only: ['application-notification-r', 'application-alert-r'],
          },
          docs: {
            page: 'management-application-notifications',
          },
        },
      },
      {
        path: 'notification-settings/:notificationId',
        component: ApplicationNotificationSettingsDetailsComponent,
        data: {
          docs: {
            perms: {
              only: ['application-notification-r'],
            },
            page: 'management-application-notifications',
          },
        },
      },
      {
        path: 'members',
        component: ApplicationGeneralMembersComponent,
        data: {
          perms: {
            only: ['application-member-r'],
          },
          docs: {
            page: 'management-application-members',
          },
        },
      },
      {
        path: 'members-ng',
        component: ApplicationGeneralMembersComponent,
        data: {
          perms: {
            only: ['application-member-r'],
          },
          docs: {
            page: 'management-application-members',
          },
        },
      },
      {
        path: 'groups',
        component: ApplicationGeneralGroupsComponent,
        data: {
          perms: {
            only: ['application-member-r'],
          },
        },
      },
      {
        path: 'transfer-ownership',
        component: ApplicationGeneralTransferOwnershipComponent,
        data: {
          perms: {
            only: ['application-member-r'],
          },
        },
      },
      {
        path: 'general',
        component: ApplicationGeneralNgComponent,
        data: {
          perms: {
            only: ['application-definition-r'],
          },
          docs: {
            page: 'management-application',
          },
        },
      },
    ],
  },
];

@NgModule({
  imports: [ApplicationsModule, RouterModule.forChild(applicationRoutes)],
  exports: [RouterModule],
})
export class ApplicationsRouteModule {}
