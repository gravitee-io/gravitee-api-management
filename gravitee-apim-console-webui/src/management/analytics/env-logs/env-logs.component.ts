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

import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';
import { delay, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsFilterBarComponent } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';
import { fakeEnvLogs } from './models/env-log.fixture';

import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';

@Component({
  selector: 'env-logs',
  templateUrl: './env-logs.component.html',
  styleUrl: './env-logs.component.scss',
  imports: [EnvLogsTableComponent, EnvLogsFilterBarComponent, MatCardModule, GioBannerModule],
  standalone: true,
})
export class EnvLogsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  logs = signal<EnvLog[]>([]);
  isLoading = signal(true);
  pagination = signal<Pagination>({ page: 1, perPage: 10, totalCount: 0 });

  ngOnInit() {
    this.refresh();
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update((prev) => ({ ...prev, page: event.index, perPage: event.size }));
    this.refresh();
  }

  refresh() {
    this.isLoading.set(true);
    // TODO: Replace with real API call — send activeFilters() as query params
    of(fakeEnvLogs())
      .pipe(
        delay(500),
        tap((logs) => {
          this.logs.set(logs);
          this.pagination.update((prev) => ({ ...prev, totalCount: logs.length }));
          this.isLoading.set(false);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
