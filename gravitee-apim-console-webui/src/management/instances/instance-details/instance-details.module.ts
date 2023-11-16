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

import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { makeChildProviders, Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';

import { InstanceDetailsComponent } from './instance-details.component';
import { InstanceDetailsHeaderModule } from './instance-details-header/instance-details-header.module';
import { InstanceDetailsEnvironmentComponent } from './instance-details-environment/instance-details-environment.component';
import { InstanceDetailsMonitoringComponent } from './instance-details-monitoring/instance-details-monitoring.component';
import { InstanceDetailsMonitoringModule } from './instance-details-monitoring/instance-details-monitoring.module';
import { InstanceDetailsEnvironmentModule } from './instance-details-environment/instance-details-environment.module';
import { InstanceListComponent } from '../list/instance-list.component';
import { MatTableModule } from '@angular/material/table';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@NgModule({
  declarations: [InstanceDetailsComponent, InstanceListComponent],
  exports: [InstanceDetailsComponent],
  imports: [
    CommonModule,
    UIRouterModule.forChild(),
    InstanceDetailsHeaderModule,
    InstanceDetailsMonitoringModule,
    InstanceDetailsEnvironmentModule,
    MatTableModule,
    GioTableWrapperModule,
    MatIconModule,
    MatTooltipModule,
  ],
})
export class InstanceDetailsModule {
  public static withRouting(config: { stateNamePrefix: string }): ModuleWithProviders<InstanceDetailsModule> {
    const states = [
      {
        // TMP State during migration of this component
        name: `${config.stateNamePrefix}.list`,
        url: '/list',
        component: InstanceListComponent,
        data: {
          useAngularMaterial: true,
        },
      },
      {
        name: `${config.stateNamePrefix}`,
        url: '/:instanceId',
        component: InstanceDetailsComponent,
        data: {
          useAngularMaterial: true,
        },
      },
      {
        name: `${config.stateNamePrefix}.environment`,
        url: '/environment',
        component: InstanceDetailsEnvironmentComponent,
        data: {
          docs: {
            page: 'management-gateway-environment',
          },
          useAngularMaterial: true,
        },
      },
      {
        name: `${config.stateNamePrefix}.monitoring`,
        url: '/monitoring',
        component: InstanceDetailsMonitoringComponent,
        data: {
          docs: {
            page: 'management-gateway-monitoring',
          },
          useAngularMaterial: true,
        },
      },
    ] as Ng2StateDeclaration[];

    return {
      ngModule: InstanceDetailsModule,
      providers: [...makeChildProviders({ states })],
    };
  }
}
