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
import { KAFKA_EXPLORER_BASE_URL } from '@gravitee/gravitee-kafka-explorer';

import { inject, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ClusterNavigationComponent } from './cluster-navigation/cluster-navigation.component';
import { ClusterGeneralComponent } from './general/cluster-general.component';
import { ClusterUserPermissionsComponent } from './user-permissions/cluster-user-permissions.component';
import { ClusterConfigurationComponent } from './kafka-connections/details/configuration/cluster-configuration.component';
import { ClusterListComponent } from './kafka-connections/list/cluster-list.component';
import { KafkaClusterListComponent } from './kafka-clusters/list/kafka-cluster-list.component';
import { KafkaClusterConfigurationComponent } from './kafka-clusters/details/configuration/kafka-cluster-configuration.component';
import { ClusterGuard } from './cluster.guard';

import { Constants } from '../../entities/Constants';
import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';
import { HasLicenseGuard } from '../../shared/components/gio-license/has-license.guard';
import { ApimFeature } from '../../shared/components/gio-license/gio-license-data';

const clusterRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'kafka-connections',
  },
  {
    path: 'kafka-connections',
    component: ClusterListComponent,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    canDeactivate: [ClusterGuard.clearPermissions],
    data: {
      useAngularMaterial: true,
      permissions: {
        anyOf: ['environment-cluster-r'],
      },
    },
  },
  {
    path: 'kafka-connections/:clusterId',
    component: ClusterNavigationComponent,
    canActivate: [ClusterGuard.loadPermissions],
    canActivateChild: [PermissionGuard.checkRouteDataPermissions, HasLicenseGuard],
    canDeactivate: [ClusterGuard.clearPermissions],
    children: [
      {
        path: '',
        redirectTo: 'general',
        pathMatch: 'full',
      },
      {
        path: 'general',
        component: ClusterGeneralComponent,
        data: {
          permissions: {
            anyOf: ['cluster-definition-r'],
          },
        },
      },
      {
        path: 'explorer',
        loadChildren: () => import('@gravitee/gravitee-kafka-explorer').then(m => m.KAFKA_EXPLORER_ROUTES),
        providers: [{ provide: KAFKA_EXPLORER_BASE_URL, useFactory: () => inject(Constants).env.v2BaseURL }],
        data: {
          permissions: {
            anyOf: ['cluster-definition-r'],
          },
          requireLicense: {
            license: { feature: ApimFeature.APIM_NATIVE_KAFKA_EXPLORER },
            redirect: '/',
          },
        },
      },
      {
        path: 'configuration',
        component: ClusterConfigurationComponent,
        data: {
          permissions: {
            anyOf: ['cluster-definition-r'],
          },
        },
      },
      {
        path: 'user-permissions',
        component: ClusterUserPermissionsComponent,
        data: {
          permissions: {
            anyOf: ['cluster-member-r'],
          },
        },
      },
    ],
  },
  {
    path: 'kafka-clusters',
    component: KafkaClusterListComponent,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      useAngularMaterial: true,
      permissions: {
        anyOf: ['environment-cluster-r'],
      },
    },
  },
  {
    path: 'kafka-clusters/:clusterId',
    component: ClusterNavigationComponent,
    canActivate: [ClusterGuard.loadPermissions],
    canActivateChild: [PermissionGuard.checkRouteDataPermissions],
    canDeactivate: [ClusterGuard.clearPermissions],
    children: [
      {
        path: '',
        redirectTo: 'general',
        pathMatch: 'full',
      },
      {
        path: 'general',
        component: ClusterGeneralComponent,
        data: {
          permissions: {
            anyOf: ['cluster-definition-r'],
          },
        },
      },
      {
        path: 'configuration',
        component: KafkaClusterConfigurationComponent,
        data: {
          permissions: {
            anyOf: ['cluster-configuration-r'],
          },
        },
      },
      {
        path: 'user-permissions',
        component: ClusterUserPermissionsComponent,
        data: {
          permissions: {
            anyOf: ['cluster-member-r'],
          },
        },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(clusterRoutes), ClusterNavigationComponent],
  exports: [RouterModule],
})
export class ClusterRoutingModule {}
