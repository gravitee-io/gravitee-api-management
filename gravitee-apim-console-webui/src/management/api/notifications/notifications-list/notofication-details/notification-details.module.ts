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
import { GioFormFocusInvalidModule, GioFormHeadersModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { NotificationDetailsComponent } from './notification-details.component';

import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [NotificationDetailsComponent],
  exports: [NotificationDetailsComponent],
  imports: [
    CommonModule,
    GioPermissionModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioFormHeadersModule,
    GioFormFocusInvalidModule,
    GioGoBackButtonModule,
    ReactiveFormsModule,
    MatCheckboxModule,
    MatInputModule,
    MatCardModule,
    MatSnackBarModule,
  ],
})
export class NotificationDetailsModule {}
