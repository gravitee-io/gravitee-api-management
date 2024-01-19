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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { ApiRuntimeLogsListRowComponent } from './api-runtime-logs-list-row.component';

@NgModule({
  declarations: [ApiRuntimeLogsListRowComponent],
  exports: [ApiRuntimeLogsListRowComponent],
  imports: [RouterModule, CommonModule, MatButtonModule],
})
export class ApiRuntimeLogsListRowModule {}
