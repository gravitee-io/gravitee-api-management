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
import { GioAvatarModule, GioConfirmDialogModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { UIRouterModule } from '@uirouter/angular';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';

import { ApiPortalGroupsComponent } from './groups/api-portal-groups.component';
import { ApiPortalMembersComponent } from './members/api-portal-members.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiPortalGroupsComponent, ApiPortalMembersComponent],
  exports: [ApiPortalGroupsComponent, ApiPortalMembersComponent],
  imports: [
    CommonModule,
    UIRouterModule,
    ReactiveFormsModule,

    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTableModule,
    MatDialogModule,

    GioAvatarModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioConfirmDialogModule,
  ],
})
export class ApiPortalUserGroupModule {}
