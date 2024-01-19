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
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';

import { ApiRuntimeLogsConnectionLogDetailsComponent } from './api-runtime-logs-connection-log-details.component';
import { BodyAccordionModule } from './components/response-body-accordion';

@NgModule({
  declarations: [ApiRuntimeLogsConnectionLogDetailsComponent],
  exports: [ApiRuntimeLogsConnectionLogDetailsComponent],
  imports: [CommonModule, MatExpansionModule, MatIconModule, GioIconsModule, MatTabsModule, BodyAccordionModule],
})
export class ApiRuntimeLogsConnectionLogDetailsModule {}
