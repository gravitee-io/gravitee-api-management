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
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { ApiProxyGroupEndpointEditComponent } from './edit/api-proxy-group-endpoint-edit.component';

import { GioFormFocusInvalidModule } from '../../../../../../shared/components/gio-form-focus-first-invalid/gio-form-focus-first-invalid.module';
import { GioGoBackButtonModule } from '../../../../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [ApiProxyGroupEndpointEditComponent],
  exports: [ApiProxyGroupEndpointEditComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatCheckboxModule,

    GioGoBackButtonModule,
    GioFormFocusInvalidModule,
    GioSaveBarModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ApiProxyGroupsEndpointModule {}
