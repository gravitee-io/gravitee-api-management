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
} from '@gravitee/ui-particles-angular';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterModule } from '@angular/router';

import { ApplicationGeneralGroupsComponent } from './groups/application-general-groups.component';
import { ApplicationGeneralMembersComponent } from './members/application-general-members.component';
import { ApplicationGeneralTransferOwnershipComponent } from './transfer-ownership/application-general-transfer-ownership.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioUsersSelectorModule } from '../../../../shared/components/gio-users-selector/gio-users-selector.module';
import { GioFormUserAutocompleteModule } from '../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.module';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { ApiUserGroupModule } from '../../../api/user-group-access/api-user-group.module';

@NgModule({
  declarations: [ApplicationGeneralGroupsComponent, ApplicationGeneralMembersComponent, ApplicationGeneralTransferOwnershipComponent],
  exports: [ApplicationGeneralGroupsComponent, ApplicationGeneralMembersComponent, ApplicationGeneralTransferOwnershipComponent],
  imports: [
    CommonModule,
    RouterModule,
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
    ApiUserGroupModule,
    FormsModule,
  ],
})
export class ApplicationGeneralUserGroupModule {}
