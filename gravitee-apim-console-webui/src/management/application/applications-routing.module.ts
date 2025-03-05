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

import { ApplicationNavigationComponent } from './application-navigation/application-navigation.component';
import { EnvApplicationListComponent } from './list/env-application-list.component';
import { ApplicationGuard } from './application.guard';
import { ApplicationMetadataComponent } from './details/metadata/application-metadata.component';
import { ApplicationAnalyticsComponent } from './details/analytics/application-analytics.component';
import { ApplicationLogsComponent } from './details/logs/application-logs.component';
import { ApplicationLogComponent } from './details/logs/application-log.component';
import { ApplicationGeneralMembersComponent } from './details/user-group-access/members/application-general-members.component';
import { ApplicationGeneralGroupsComponent } from './details/user-group-access/groups/application-general-groups.component';
import { ApplicationGeneralTransferOwnershipComponent } from './details/user-group-access/transfer-ownership/application-general-transfer-ownership.component';
import { ApplicationGeneralComponent } from './details/general/application-general.component';
import { ApplicationNotificationComponent } from './details/notification/application-notification.component';
import { ApplicationSubscriptionListComponent } from './details/subscriptions/list/application-subscription-list.component';
import { ApplicationCreationComponent } from './creation/application-creation.component';
import { ApplicationSubscriptionComponent } from './details/subscriptions/subscription/application-subscription.component';
import { ApplicationSharedApiKeysComponent } from './details/subscriptions/shared-api-keys/application-shared-api-keys.component';

import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';

const applicationRoutes: Routes = [
  {
    path: '',
    component: EnvApplicationListComponent,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      useAngularMaterial: true,
      permissions: {
        anyOf: ['environment-application-r'],
      },
      docs: {
        page: 'management-applications',
      },
    },
  },
  {
    path: 'create',
    component: ApplicationCreationComponent,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      permissions: {
        anyOf: ['environment-application-c'],
      },
      docs: {
        page: 'management-create-application',
      },
    },
  },
  {
    path: ':applicationId',
    component: ApplicationNavigationComponent,
    canActivate: [ApplicationGuard.loadPermissions],
    canActivateChild: [PermissionGuard.checkRouteDataPermissions],
    canDeactivate: [ApplicationGuard.clearPermissions],
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
          permissions: {
            anyOf: ['application-metadata-r'],
          },
          docs: {
            page: 'management-application-metadata',
          },
        },
      },
      {
        path: 'subscriptions',
        component: ApplicationSubscriptionListComponent,
        data: {
          permissions: {
            anyOf: ['application-subscription-r'],
          },
        },
      },
      {
        path: 'shared-api-keys',
        component: ApplicationSharedApiKeysComponent,
        data: {
          permissions: {
            anyOf: ['application-subscription-r'],
          },
        },
      },
      {
        path: 'subscriptions/:subscriptionId',
        component: ApplicationSubscriptionComponent,
        data: {
          permissions: {
            anyOf: ['application-subscription-r'],
          },
        },
      },
      {
        path: 'analytics',
        component: ApplicationAnalyticsComponent,
        data: {
          permissions: {
            anyOf: ['application-analytics-r'],
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
          permissions: {
            anyOf: ['application-log-r'],
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
          permissions: {
            anyOf: ['application-log-r'],
          },
        },
      },
      {
        path: 'notifications',
        component: ApplicationNotificationComponent,
        data: {
          permissions: {
            anyOf: ['application-notification-r', 'application-alert-r'],
          },
          docs: {
            page: 'management-application-notifications',
          },
        },
      },
      {
        path: 'members',
        component: ApplicationGeneralMembersComponent,
        data: {
          permissions: {
            anyOf: ['application-member-r'],
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
          permissions: {
            anyOf: ['application-member-r'],
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
          permissions: {
            anyOf: ['application-member-r'],
          },
        },
      },
      {
        path: 'transfer-ownership',
        component: ApplicationGeneralTransferOwnershipComponent,
        data: {
          permissions: {
            anyOf: ['application-member-r'],
          },
        },
      },
      {
        path: 'general',
        component: ApplicationGeneralComponent,
        data: {
          permissions: {
            anyOf: ['application-definition-r'],
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
  imports: [RouterModule.forChild(applicationRoutes)],
  exports: [RouterModule],
})
export class ApplicationsRoutingModule {}
