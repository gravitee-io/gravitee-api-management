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
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatIconModule } from '@angular/material/icon';
import {
  GioBannerModule,
  GioFormJsonSchemaModule,
  GioFormSlideToggleModule,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { RouterModule } from '@angular/router';

import { ApiEntrypointsV4GeneralComponent } from './api-entrypoints-v4-general.component';
import { ApiEntrypointsV4EditComponent } from './edit/api-entrypoints-v4-edit.component';
import { ApiEntrypointsV4AddDialogComponent } from './edit/api-entrypoints-v4-add-dialog.component';

import { GioFormListenersContextPathModule } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.module';
import { GioFormListenersVirtualHostModule } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioEntrypointsSelectionListModule } from '../component/gio-entrypoints-selection-list/gio-entrypoints-selection-list.module';
import { GioFormQosModule } from '../component/gio-form-qos/gio-form-qos.module';
import { GioLicenseBannerModule } from '../../../shared/components/gio-license-banner/gio-license-banner.module';
import { GioFormListenersTcpHostsModule } from '../component/gio-form-listeners/gio-form-listeners-tcp-hosts/gio-form-listeners-tcp-hosts.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    GioEntrypointsSelectionListModule,
    GioFormJsonSchemaModule,
    GioFormListenersContextPathModule,
    GioFormListenersVirtualHostModule,
    GioGoBackButtonModule,
    GioIconsModule,
    GioLoaderModule,

    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatLegacyDialogModule,
    MatIconModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    GioBannerModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    GioFormQosModule,
    GioFormSlideToggleModule,
    MatSlideToggleModule,
    GioLicenseBannerModule,
    GioFormListenersTcpHostsModule,
  ],
  declarations: [ApiEntrypointsV4GeneralComponent, ApiEntrypointsV4EditComponent, ApiEntrypointsV4AddDialogComponent],
})
export class ApiEntrypointsV4Module {}
