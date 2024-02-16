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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  GioFormFocusInvalidModule,
  GioFormHeadersModule,
  GioFormSlideToggleModule,
  GioIconsModule,
  GioLoaderModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterModule } from '@angular/router';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';

import { NotificationSettingsAddDialogModule } from './notifications-settings-add-dialog/notification-settings-add-dialog.module';
import { NotificationSettingsListComponent } from './notification-settings-list.component';
import { NotificationSettingsDetailsComponent } from './notification-settings-details/notification-settings-details.component';

import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  declarations: [NotificationSettingsListComponent, NotificationSettingsDetailsComponent],
  exports: [NotificationSettingsListComponent, NotificationSettingsDetailsComponent],
  imports: [
    CommonModule,
    RouterModule,

    GioIconsModule,
    GioTableWrapperModule,
    GioFormFocusInvalidModule,
    GioFormHeadersModule,
    GioGoBackButtonModule,
    GioLoaderModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioFormSlideToggleModule,

    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatLegacySnackBarModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,

    NotificationSettingsAddDialogModule,
    ReactiveFormsModule,
  ],
})
export class NotificationSettingsListModule {}
