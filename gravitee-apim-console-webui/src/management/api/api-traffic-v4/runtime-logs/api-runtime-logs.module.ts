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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsListModule, ApiRuntimeLogsQuickFiltersModule } from './components';
import { QuickFiltersStoreService } from './services';

@NgModule({
  imports: [
    CommonModule,
    ApiRuntimeLogsListModule,
    ApiRuntimeLogsQuickFiltersModule,
    MatButtonModule,
    MatIconModule,
    GioLoaderModule,
    MatCardModule,
    RouterLink,
  ],
  declarations: [ApiRuntimeLogsComponent],
  exports: [ApiRuntimeLogsComponent],
  providers: [QuickFiltersStoreService],
})
export class ApiRuntimeLogsModule {}
