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
import { GioSaveBarModule, GioFormSlideToggleModule, GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterModule } from '@angular/router';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';

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
    MatLegacySnackBarModule,
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
