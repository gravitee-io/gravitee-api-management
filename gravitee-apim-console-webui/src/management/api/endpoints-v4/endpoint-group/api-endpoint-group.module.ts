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
  GioBannerModule,
  GioFormFocusInvalidModule,
  GioFormJsonSchemaModule,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';
import { ApiEndpointGroupGeneralComponent } from './general/api-endpoint-group-general.component';
import { ApiEndpointGroupConfigurationComponent } from './configuration/api-endpoint-group-configuration.component';
import { ApiEndpointGroupSelectionComponent } from './selection/api-endpoint-group-selection.component';
import { ApiEndpointGroupCreateComponent } from './create/api-endpoint-group-create.component';

import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioSelectionListModule } from '../../../../shared/components/gio-selection-list-option/gio-selection-list.module';
import { ApiHealthCheckV4FormModule } from '../../component/health-check-v4-form/api-health-check-v4-form.module';
import { GioLicenseBannerModule } from '../../../../shared/components/gio-license-banner/gio-license-banner.module';

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

    ApiHealthCheckV4FormModule,
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
    GioSelectionListModule,
    GioFormFocusInvalidModule,
    GioFormJsonSchemaModule,
    GioFormSlideToggleModule,
    GioGoBackButtonModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioLicenseBannerModule,
  ],
})
export class ApiEndpointGroupModule {}
