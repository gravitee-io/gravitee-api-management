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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatIconModule } from '@angular/material/icon';
import {
  GioBannerModule,
  GioFormFocusInvalidModule,
  GioFormJsonSchemaModule,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatStepperModule } from '@angular/material/stepper';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { RouterModule } from '@angular/router';

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';
import { ApiEndpointGroupGeneralComponent } from './general/api-endpoint-group-general.component';
import { ApiEndpointGroupConfigurationComponent } from './configuration/api-endpoint-group-configuration.component';
import { ApiEndpointGroupSelectionComponent } from './selection/api-endpoint-group-selection.component';
import { ApiEndpointGroupCreateComponent } from './create/api-endpoint-group-create.component';

import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioConnectorListModule } from '../../../../shared/components/gio-connector-list-option/gio-connector-list.module';

@NgModule({
  declarations: [
    ApiEndpointGroupComponent,
    ApiEndpointGroupGeneralComponent,
    ApiEndpointGroupConfigurationComponent,
    ApiEndpointGroupCreateComponent,
    ApiEndpointGroupSelectionComponent,
  ],
  exports: [ApiEndpointGroupComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatProgressBarModule,
    MatRadioModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatStepperModule,
    MatTabsModule,

    GioBannerModule,
    GioConnectorListModule,
    GioFormFocusInvalidModule,
    GioFormJsonSchemaModule,
    GioFormSlideToggleModule,
    GioGoBackButtonModule,
    GioPermissionModule,
    GioSaveBarModule,
  ],
})
export class ApiEndpointGroupModule {}
