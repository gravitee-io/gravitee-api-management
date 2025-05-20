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

import { GvButtonCreateApplicationComponent } from '../../components/gv-button-create-application/gv-button-create-application.component';
import { GvHeaderItemComponent } from '../../components/gv-header-item/gv-header-item.component';
import { GvSelectDashboardComponent } from '../../components/gv-select-dashboard/gv-select-dashboard.component';
import { FeatureEnum } from '../../model/feature.enum';
import { enabledApplicationTypesResolver } from '../../resolvers/enabled-application-types.resolver';
import { ApplicationAnalyticsComponent } from '../application/application-analytics/application-analytics.component';
import { ApplicationCreationComponent } from '../application/application-creation/application-creation.component';
import { ApplicationGeneralComponent } from '../application/application-general/application-general.component';
import { ApplicationLogsComponent } from '../application/application-logs/application-logs.component';
import { ApplicationMembersComponent } from '../application/application-members/application-members.component';
import { ApplicationMetadataComponent } from '../application/application-metadata/application-metadata.component';
import { ApplicationNotificationsComponent } from '../application/application-notifications/application-notifications.component';
import { ApplicationSubscriptionsComponent } from '../application/application-subscriptions/application-subscriptions.component';
import { SubscriptionsComponent } from '../subscriptions/subscriptions.component';
import { ApplicationAlertsComponent } from '../application/application-alerts/application-alerts.component';
import { applicationResolver } from '../../resolvers/application.resolver';
import { featureGuard } from '../../services/feature-guard.service';
import { permissionGuard } from '../../services/permission-guard.service';
import { permissionsResolver } from '../../resolvers/permissions-resolver.service';
import { applicationTypeResolver } from '../../resolvers/application-type.resolver';
import { dashboardResolver } from '../../resolvers/dashboards.resolver';

import { ApplicationsComponent } from './applications.component';

const routes: Routes = [
  { path: '', redirectTo: 'mine', pathMatch: 'full' },
  {
    path: 'mine',
    component: ApplicationsComponent,
    data: {
      title: 'route.myApplications',
      icon: 'devices:server',
      animation: { type: 'slide', group: 'apps', index: 1 },
      menu: {
        slots: {
          right: GvButtonCreateApplicationComponent,
          expectedFeature: FeatureEnum.applicationCreation,
          expectedPermissions: ['APPLICATION-C'],
        },
      },
    },
  },
  {
    path: 'subscriptions',
    component: SubscriptionsComponent,
    data: {
      title: 'route.mySubscriptions',
      icon: 'finance:share',
      animation: { type: 'slide', group: 'apps', index: 2 },
      menu: {
        slots: {
          right: GvButtonCreateApplicationComponent,
          expectedFeature: FeatureEnum.applicationCreation,
          expectedPermissions: ['APPLICATION-C'],
        },
      },
    },
  },
  {
    path: 'creation',
    component: ApplicationCreationComponent,
    canActivate: [featureGuard, permissionGuard],
    data: {
      title: 'route.applicationCreation',
      expectedFeature: FeatureEnum.applicationCreation,
      animation: { type: 'fade' },
      expectedPermissions: ['APPLICATION-C'],
    },
    resolve: {
      enabledApplicationTypes: enabledApplicationTypesResolver,
    },
  },
  {
    path: ':applicationId',
    data: {
      menu: { slots: { top: GvHeaderItemComponent }, animation: { type: 'fade' } },
    },
    resolve: {
      application: applicationResolver,
      permissions: permissionsResolver,
    },
    children: [
      {
        path: '',
        component: ApplicationGeneralComponent,
        data: {
          icon: 'general:clipboard',
          title: 'route.catalogApi',
          animation: { type: 'slide', group: 'app', index: 1 },
          expectedPermissions: [],
        },
        resolve: {
          applicationType: applicationTypeResolver,
        },
      },
      {
        path: 'metadata',
        component: ApplicationMetadataComponent,
        data: {
          icon: 'home:book-open',
          title: 'route.metadata',
          animation: { type: 'slide', group: 'app', index: 2 },
          expectedPermissions: ['METADATA-R'],
        },
      },
      {
        path: 'subscriptions',
        component: ApplicationSubscriptionsComponent,
        data: {
          icon: 'home:key',
          title: 'route.subscriptions',
          animation: { type: 'slide', group: 'app', index: 3 },
          expectedPermissions: ['SUBSCRIPTION-R'],
        },
      },
      {
        path: 'members',
        component: ApplicationMembersComponent,
        data: {
          icon: 'communication:group',
          title: 'route.members',
          animation: { type: 'slide', group: 'app', index: 4 },
          expectedPermissions: ['MEMBER-R'],
        },
      },
      {
        path: 'analytics',
        component: ApplicationAnalyticsComponent,
        data: {
          icon: 'shopping:chart-line#1',
          menu: { slots: { right: GvSelectDashboardComponent } },
          title: 'route.analyticsApplication',
          animation: { type: 'slide', group: 'app', index: 5 },
          expectedPermissions: ['ANALYTICS-R'],
        },
        resolve: {
          dashboards: dashboardResolver,
        },
      },
      {
        path: 'logs',
        component: ApplicationLogsComponent,
        data: {
          icon: 'communication:clipboard-list',
          title: 'route.logsApplication',
          animation: { type: 'slide', group: 'app', index: 6 },
          expectedPermissions: ['LOG-R'],
        },
      },
      {
        path: 'notifications',
        component: ApplicationNotificationsComponent,
        data: {
          icon: 'general:notifications#2',
          title: 'route.notifications',
          animation: { type: 'slide', group: 'app', index: 7 },
          expectedPermissions: ['NOTIFICATION-R'],
        },
      },
      {
        path: 'alerts',
        component: ApplicationAlertsComponent,
        canActivate: [featureGuard],
        data: {
          icon: 'home:alarm-clock',
          title: 'route.alerts',
          expectedFeature: FeatureEnum.alert,
          animation: { type: 'slide', group: 'app', index: 8 },
          expectedPermissions: ['ALERT-R'],
        },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ApplicationsRoutingModule {}
