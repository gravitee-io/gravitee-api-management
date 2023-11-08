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
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { UIRouterModule } from '@uirouter/angular';

import { ApiRuntimeLogsMessageItemModule } from './components';
import { ApiRuntimeLogsMessagesComponent } from './api-runtime-logs-messages.component';

import { ApiRuntimeLogsDetailsEmptyStateModule, ApiRuntimeLogsConnectionLogDetailsModule } from '../components';

@NgModule({
  declarations: [ApiRuntimeLogsMessagesComponent],
  exports: [],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    UIRouterModule,
    MatTabsModule,
    ApiRuntimeLogsMessageItemModule,
    ApiRuntimeLogsDetailsEmptyStateModule,
    ApiRuntimeLogsConnectionLogDetailsModule,
  ],
})
export class ApiRuntimeLogsMessagesModule {}
