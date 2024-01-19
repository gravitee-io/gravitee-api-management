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
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { GioSaveBarModule, GioFormSlideToggleModule, GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { RouterModule } from '@angular/router';

import { ApiProxyGroupEndpointEditComponent } from './edit/api-proxy-group-endpoint-edit.component';
import { ApiProxyGroupEndpointEditGeneralComponent } from './edit/general/api-proxy-group-endpoint-edit-general.component';
import { ApiProxyGroupEndpointConfigurationComponent } from './edit/configuration/api-proxy-group-endpoint-configuration.component';

import { EndpointHttpConfigModule } from '../../components/endpoint-http-config/endpoint-http-config.module';
import { ApiHealthCheckFormModule } from '../../../component/health-check-form/api-health-check-form.module';
import { GioGoBackButtonModule } from '../../../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [
    ApiProxyGroupEndpointEditComponent,
    ApiProxyGroupEndpointEditGeneralComponent,
    ApiProxyGroupEndpointConfigurationComponent,
  ],
  exports: [ApiProxyGroupEndpointEditComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    ApiHealthCheckFormModule,
    EndpointHttpConfigModule,

    MatCardModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatSelectModule,
    MatTooltipModule,
    MatSlideToggleModule,

    GioGoBackButtonModule,
    GioFormFocusInvalidModule,
    GioSaveBarModule,
    GioFormSlideToggleModule,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ApiProxyGroupEndpointModule {}
