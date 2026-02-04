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

import { Component, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';
import { EnvLog, fakeEnvLogs } from './models/env-log.fixture';

import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'env-logs-v4',
  templateUrl: './env-logs-v4.component.html',
  styleUrls: ['./env-logs-v4.component.scss'],
  imports: [MatCardModule, GioBannerModule, EnvLogsTableComponent, RouterModule, MatIconModule, GioIconsModule],
})
export class EnvLogsV4Component {
  logs = signal<EnvLog[]>(fakeEnvLogs());

  pagination = signal({
    page: 1,
    perPage: 10,
    totalCount: 5,
  });

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update((p) => ({ ...p, page: event.index, perPage: event.size }));
  }
}
