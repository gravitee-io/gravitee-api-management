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
import {
  GioAvatarModule,
  GioBannerModule,
  GioFormFocusInvalidModule,
  GioFormSlideToggleModule,
  GioFormTagsInputModule,
  GioIconsModule,
  GioLicenseModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { RouterModule } from '@angular/router';
import { MatRadioModule } from '@angular/material/radio';

import { ClientRegistrationProvidersComponent } from './client-registration-providers.component';
import { ClientRegistrationProviderComponent } from './client-registration-provider/client-registration-provider.component';

import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,

    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    MatOptionModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,

    GioIconsModule,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioAvatarModule,
    GioGoBackButtonModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    GioFormTagsInputModule,
    GioPermissionModule,
    GioLicenseModule,
  ],
  declarations: [ClientRegistrationProvidersComponent, ClientRegistrationProviderComponent],
  exports: [ClientRegistrationProvidersComponent, ClientRegistrationProviderComponent],
})
export class ClientRegistrationProvidersModule {}
