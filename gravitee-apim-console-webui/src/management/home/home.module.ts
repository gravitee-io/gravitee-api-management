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
import { GioAvatarModule } from '@gravitee/ui-particles-angular';
import { Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';
import { MatCardModule } from '@angular/material/card';

import { HomeApiStatusComponent } from './home-api-status/home-api-status.component';
import { HomeLayoutComponent } from './home-layout/home-layout.component';
import { HomeOverviewComponent } from './home-overview/home-overview.component';

import { GioCircularPercentageModule } from '../../shared/components/gio-circular-percentage/gio-circular-percentage.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

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
    name: 'home.apiStatus',
    url: '/api-status?{q:string}{page:int}{size:int}{order:string}',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: HomeApiStatusComponent,
  },
];

@NgModule({
  imports: [
    CommonModule,
    UIRouterModule.forChild({ states }),

    MatTabsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,

    GioAvatarModule,
    GioTableWrapperModule,
    GioCircularPercentageModule,
  ],
  declarations: [HomeLayoutComponent, HomeOverviewComponent, HomeApiStatusComponent],
})
export class HomeModule {}
