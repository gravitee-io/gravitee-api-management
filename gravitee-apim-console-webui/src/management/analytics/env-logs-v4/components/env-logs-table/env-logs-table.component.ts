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

import { Component, computed, input, output } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';

import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { Pagination } from '../../../../../entities/management-api-v2';
import {
  GioTableWrapperFilters,
  GioTableWrapperPagination,
} from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { EnvLog } from '../../models/env-log.fixture';

export interface EnvLogsTableColumnDef {
  id: string;
  label: string;
}

@Component({
  selector: 'env-logs-table',
  templateUrl: './env-logs-table.component.html',
  styleUrls: ['./env-logs-table.component.scss'],
  imports: [GioTableWrapperModule, MatTableModule, MatSort],
})
export class EnvLogsTableComponent {
  logs = input.required<EnvLog[]>();
  pagination = input.required<Pagination>();
  tableId = input<string>('logsTable');
  ariaLabel = input<string>('Logs table');

  paginationUpdated = output<GioTableWrapperPagination>();
  logClicked = output<EnvLog>();

  readonly columns = computed<EnvLogsTableColumnDef[]>(() => [
    { id: 'timestamp', label: 'Timestamp' },
    { id: 'api', label: 'API' },
    { id: 'type', label: 'Type' },
    { id: 'application', label: 'Application' },
    { id: 'method', label: 'Method' },
    { id: 'path', label: 'Path' },
    { id: 'status', label: 'Status' },
    { id: 'responseTime', label: 'Response Time' },
    { id: 'gateway', label: 'Gateway' },
  ]);

  readonly gioTableWrapperFilters = computed(() => {
    const pagination = this.pagination();
    return {
      searchTerm: '',
      pagination: { index: pagination.page ?? 1, size: pagination.perPage ?? 10 },
    };
  });

  readonly displayedColumns = computed(() => this.columns().map((col) => col.id));

  onFiltersChanged(event: GioTableWrapperFilters) {
    const eventPagination = event.pagination;
    const currentPagination = this.pagination();
    const currentPerPage = currentPagination.perPage ?? 0;
    const currentPage = currentPagination.page ?? 0;
    if (currentPerPage !== eventPagination.size || currentPage !== eventPagination.index) {
      this.paginationUpdated.emit({ index: eventPagination.index, size: eventPagination.size });
    }
  }
}
