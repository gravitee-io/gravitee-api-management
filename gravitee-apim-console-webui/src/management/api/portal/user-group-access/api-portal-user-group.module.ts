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
import { GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiPortalGroupsComponent } from './groups/api-portal-groups.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiPortalGroupsComponent],
  exports: [ApiPortalGroupsComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatSelectModule,
    MatCardModule,
    MatSnackBarModule,

    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
  ],
})
export class ApiPortalUserGroupModule {}
