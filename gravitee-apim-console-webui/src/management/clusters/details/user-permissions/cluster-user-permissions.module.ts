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
import { ClusterUserPermissionsComponent } from './cluster-user-permissions.component';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialogModule } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  GioAvatarModule,
  GioIconsModule,
  GioSaveBarModule,
  GioConfirmDialogModule,
  GioBannerModule,
  GioFormSlideToggleModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { GioPermissionModule } from 'src/shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from 'src/shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioFormUserAutocompleteModule } from 'src/shared/components/gio-user-autocomplete/gio-form-user-autocomplete.module';
import { GioUsersSelectorModule } from 'src/shared/components/gio-users-selector/gio-users-selector.module';

@NgModule({
  declarations: [ClusterUserPermissionsComponent],
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
export class ClusterUserPermissionsModule {}
