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
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioAvatarModule, GioBannerModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';

import { HomeApiHealthCheckComponent } from './home-api-health-check/home-api-health-check.component';
import { HomeLayoutComponent } from './home-layout/home-layout.component';
import { HomeOverviewComponent } from './home-overview/home-overview.component';
import { HealthAvailabilityTimeFrameModule } from './home-api-health-check/health-availability-time-frame/health-availability-time-frame.module';
import { GioQuickTimeRangeModule } from './widgets/gio-quick-time-range/gio-quick-time-range.module';
import { GioRequestStatsModule } from './widgets/gio-request-stats/gio-request-stats.module';
import { GioTopApisTableModule } from './widgets/gio-top-apis-table/gio-top-apis-table.module';
import { GioApiResponseStatusModule } from './widgets/gio-api-response-status/gio-api-response-status.module';

import { GioCircularPercentageModule } from '../../shared/components/gio-circular-percentage/gio-circular-percentage.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { TasksComponent } from '../tasks/tasks.component';

export const states: Ng2StateDeclaration[] = [
  {
    parent: 'management',
    name: 'home',
    url: '/home',
    redirectTo: 'home.overview',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: HomeLayoutComponent,
  },
  {
    name: 'home.overview',
    url: '/overview',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: HomeOverviewComponent,
  },
  {
    name: 'home.apiHealthCheck',
    url: '/api-health-check?{q:string}{page:int}{size:int}{order:string}',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    params: {
      q: {
        dynamic: true,
      },
      page: {
        dynamic: true,
      },
      size: {
        dynamic: true,
      },
      order: {
        dynamic: true,
      },
    },
    component: HomeApiHealthCheckComponent,
  },
  {
    name: 'home.tasks',
    url: '/tasks',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: TasksComponent,
  },
];

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UIRouterModule.forChild({ states }),

    MatTabsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,

    GioAvatarModule,
    GioTableWrapperModule,
    GioCircularPercentageModule,
    GioBannerModule,
    GioIconsModule,
    GioLoaderModule,
    GioRequestStatsModule,
    HealthAvailabilityTimeFrameModule,
    GioQuickTimeRangeModule,
    GioTopApisTableModule,
    GioApiResponseStatusModule,
  ],
  declarations: [HomeLayoutComponent, HomeOverviewComponent, HomeApiHealthCheckComponent],
})
export class HomeModule {}
