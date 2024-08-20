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

import { IntegrationsComponent } from './integrations.component';
import { CreateIntegrationComponent } from './create-integration/create-integration.component';
import { IntegrationsNavigationComponent } from './integrations-navigation/integrations-navigation.component';
import { IntegrationOverviewComponent } from './integration-overview/integration-overview.component';
import { IntegrationConfigurationComponent } from './integration-configuration/integration-configuration.component';
import { IntegrationAgentComponent } from './integration-agent/integration-agent.component';
import { DiscoveryPreviewComponent } from './discovery-preview/discovery-preview.component';
import { IntegrationGeneralConfigurationComponent } from './integration-configuration/general/integration-general-configuration.component';
import { IntegrationUserPermissionsComponent } from './integration-configuration/user-permissions/integration-user-permissions.component';

import { hasEnterpriseLicenseGuard } from '../../shared/components/gio-license/has-enterprise-license.guard';

const routes: Routes = [
  {
    path: '',
    component: IntegrationsComponent,
  },
  {
    path: 'new',
    component: CreateIntegrationComponent,
    canActivate: [hasEnterpriseLicenseGuard],
    data: {
      permissions: {
        anyOf: ['environment-integration-c'],
      },
    },
  },
  {
    path: ':integrationId',
    component: IntegrationsNavigationComponent,
    canActivate: [hasEnterpriseLicenseGuard],
    children: [
      {
        path: '',
        component: IntegrationOverviewComponent,
        data: {
          permissions: {
            anyOf: ['environment-integration-r'],
          },
        },
      },
      {
        path: 'discover',
        component: DiscoveryPreviewComponent,
        data: {
          permissions: {
            anyOf: ['environment-integration-r'],
          },
        },
      },
      {
        path: 'agent',
        component: IntegrationAgentComponent,
        data: {
          permissions: {
            anyOf: ['environment-integration-r'],
          },
        },
      },
      {
        path: 'configuration',
        component: IntegrationConfigurationComponent,
        data: {
          permissions: {
            anyOf: ['environment-integration-u', 'environment-integration-d'],
          },
        },
        children: [
          {
            path: '',
            component: IntegrationGeneralConfigurationComponent,
          },
          {
            path: 'members',
            component: IntegrationUserPermissionsComponent,
          },
          // Uncomment when component ready:
          // {
          //   path: 'discovery',
          //   component: ,
          // },
        ],
      },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class IntegrationsRoutingModule {}
