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
import { GioBannerModule, GioClipboardModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { CdkAccordionModule } from '@angular/cdk/accordion';
import { MatIconModule } from '@angular/material/icon';

import { IntegrationAgentComponent } from './integration-agent.component';

import { IntegrationStatusComponent } from '../components/integration-status/integration-status.component';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [IntegrationAgentComponent],
  exports: [IntegrationAgentComponent],
  imports: [
    CommonModule,

    MatCardModule,
    MatButtonModule,
    MatIconModule,
    CdkAccordionModule,

    GioLoaderModule,
    GioClipboardModule,
    GioBannerModule,
    GioIconsModule,
    GioPermissionModule,

    IntegrationStatusComponent,
  ],
})
export class IntegrationAgentModule {}
