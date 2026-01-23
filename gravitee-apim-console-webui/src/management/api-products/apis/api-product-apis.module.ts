/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { GioIconsModule, GioAvatarModule, GioConfirmDialogModule } from '@gravitee/ui-particles-angular';
import { RouterModule } from '@angular/router';

import { ApiProductApisComponent } from './api-product-apis.component';
import { ApiProductApisRoutingModule } from './api-product-apis-routing.module';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  declarations: [ApiProductApisComponent],
  exports: [ApiProductApisComponent],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatTooltipModule,
    MatDialogModule,
    GioIconsModule,
    GioAvatarModule,
    GioConfirmDialogModule,
    GioPermissionModule,
    GioTableWrapperModule,
    RouterModule,
    ApiProductApisRoutingModule,
  ],
})
export class ApiProductApisModule {}
