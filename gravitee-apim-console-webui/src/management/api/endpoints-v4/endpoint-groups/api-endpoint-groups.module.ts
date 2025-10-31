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
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { ApiEndpointGroupsComponent } from './api-endpoint-groups.component';
import { ApiEndpointGroupsLlmComponent } from './llm/api-endpoint-groups-llm.component';
import { ApiEndpointGroupsStandardComponent } from './standard/api-endpoint-groups-standard.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioLicenseBannerModule } from '../../../../shared/components/gio-license-banner/gio-license-banner.module';

@NgModule({
  declarations: [ApiEndpointGroupsComponent, ApiEndpointGroupsLlmComponent, ApiEndpointGroupsStandardComponent],
  exports: [ApiEndpointGroupsComponent],
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatTableModule,
    MatTooltipModule,
    MatIconModule,
    MatSnackBarModule,
    GioIconsModule,

    GioPermissionModule,
    GioBannerModule,
    GioLicenseBannerModule,
  ],
})
export class ApiEndpointGroupsModule {}
