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
import {
  GioAvatarModule,
  GioBannerModule,
  GioConfirmDialogModule,
  GioIconsModule,
  GioSaveBarModule,
  GioFormSlideToggleModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { ApiGeneralGroupsComponent } from './groups/api-general-groups.component';
import { ApiGeneralMembersComponent } from './members/api-general-members.component';
import { ApiGeneralTransferOwnershipComponent } from './transfer-ownership/api-general-transfer-ownership.component';
import { ApiGeneralGroupMembersComponent } from './members/api-general-group-members/api-general-group-members.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioUsersSelectorModule } from '../../../shared/components/gio-users-selector/gio-users-selector.module';
import { GioFormUserAutocompleteModule } from '../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.module';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  declarations: [
    ApiGeneralGroupsComponent,
    ApiGeneralMembersComponent,
    ApiGeneralGroupMembersComponent,
    ApiGeneralTransferOwnershipComponent,
  ],
  exports: [ApiGeneralGroupsComponent, ApiGeneralMembersComponent, ApiGeneralTransferOwnershipComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatSelectModule,
    MatSnackBarModule,

    MatTableModule,
    MatDialogModule,
    MatSlideToggleModule,
    MatRadioModule,
    MatButtonToggleModule,

    GioAvatarModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioConfirmDialogModule,
    GioFormUserAutocompleteModule,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioUsersSelectorModule,
    GioTableWrapperModule,
    GioLoaderModule,
  ],
})
export class ApiUserGroupModule {}
