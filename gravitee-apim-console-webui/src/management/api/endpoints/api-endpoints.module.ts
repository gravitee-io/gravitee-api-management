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
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { ApiProxyEndpointListComponent } from './list/api-proxy-endpoint-list.component';
import { ApiProxyGroupsModule } from './groups/api-proxy-groups.module';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiProxyEndpointListComponent],
  exports: [ApiProxyEndpointListComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatAutocompleteModule,
    MatInputModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,

    GioIconsModule,
    GioPermissionModule,

    ApiProxyGroupsModule,
  ],
})
export class ApiEndpointsModule {}
