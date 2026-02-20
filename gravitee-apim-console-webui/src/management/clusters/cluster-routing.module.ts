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

import { ClusterNavigationComponent } from './cluster-navigation/cluster-navigation.component';
import { ClusterGeneralComponent } from './details/general/cluster-general.component';
import { ClusterConfigurationComponent } from './details/configuration/cluster-configuration.component';
import { ClusterListComponent } from './list/cluster-list.component';
import { ClusterUserPermissionsComponent } from './details/user-permissions/cluster-user-permissions.component';
import { ClusterExplorerPageComponent } from './details/explorer/cluster-explorer-page.component';
import { ClusterGuard } from './cluster.guard';

import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';

const clusterRoutes: Routes = [
  {
    path: '',
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
    path: ':clusterId',
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
        path: 'explorer',
        component: ClusterExplorerPageComponent,
        data: {
          permissions: {
            anyOf: ['cluster-definition-r'],
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
];

@NgModule({
  imports: [RouterModule.forChild(clusterRoutes), ClusterNavigationComponent],
  exports: [RouterModule],
})
export class ClusterRoutingModule {}
