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
import { GioLicenseExpirationNotificationModule, GioLicenseModule, GioMenuModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { RouterModule } from '@angular/router';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';

import { GioSideNavComponent } from './gio-side-nav.component';

import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  imports: [
    CommonModule,
    GioMenuModule,
    MatSelectModule,
    GioSubmenuModule,
    GioLicenseModule,
    RouterModule,
    GioLicenseExpirationNotificationModule,
    GioPermissionModule,
    MatButtonModule,
  ],
  declarations: [GioSideNavComponent],
  exports: [GioSideNavComponent],
})
export class GioSideNavModule {}
