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

import { Component, computed, inject, input, output, signal, effect } from '@angular/core';
import { GioAvatarModule } from '@gravitee/ui-particles-angular';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { FormsModule } from '@angular/forms';

import { GioTableWrapperModule } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { ApiType, ConnectionLog, Pagination } from '../../../../../../entities/management-api-v2';
import {
  GioTableWrapperFilters,
  GioTableWrapperPagination,
} from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTooltipOnEllipsisModule } from '../../../../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';
import { ApiUtils } from '../../../../../../util/api.util';
import { Constants } from '../../../../../../entities/Constants';

@Component({
  selector: 'api-runtime-logs-list',
  templateUrl: './api-runtime-logs-list.component.html',
  styleUrls: ['./api-runtime-logs-list.component.scss'],
  standalone: true,
  imports: [
    GioAvatarModule,
    GioTableWrapperModule,
    MatIcon,
    MatTableModule,
    MatSort,
    MatTooltipModule,
    RouterLink,
    MatButtonModule,
    DatePipe,
    GioTooltipOnEllipsisModule,
    MatCheckboxModule,
    MatMenuModule,
    FormsModule,
  ],
})
export class ApiRuntimeLogsListComponent {
  constants = inject(Constants);
  logs = input.required<ConnectionLog[]>();
  pagination = input.required<Pagination>();
  apiType = input.required<ApiType>();
  readonly gioTableWrapperFilters = computed(() => {
    const pagination = this.pagination();
    return {
      searchTerm: '',
      pagination: {
        index: pagination.page ?? 1,
        size: pagination.perPage ?? 10,
      },
    };
  });
  columnsAvailable = computed(() => {
    const type = this.apiType();
    return [
      { name: 'timestamp', label: 'Timestamp' },
      { name: 'method', label: 'Method' },
      { name: 'status', label: 'Status' },
      ...(type === 'MCP_PROXY'
        ? [
            { name: 'mcpMethod', label: 'MCP Method' },
            { name: 'mcpError', label: 'MCP Error' },
          ]
        : []),
      { name: 'URI', label: 'URI' },
      { name: 'application', label: 'Application' },
      { name: 'plan', label: 'Plan' },
      { name: 'responseTime', label: 'Response time' },
      ...(type === 'MESSAGE' ? [] : [{ name: 'endpoint', label: 'Endpoint reached' }]),
      { name: 'issues', label: 'Issues' },
      { name: 'actions', label: 'Actions' },
    ];
  });
  displayedColumns = signal<string[]>([]);
  displayedColumnsOption: Record<string, boolean> = {};

  paginationUpdated = output<GioTableWrapperPagination>();
  pageSizeOptions: number[] = [10, 25, 50, 100];

  constructor() {
    effect(() => {
      if (localStorage.getItem(`${this.constants.org.currentEnv.id}-${this.apiType}-logs-list-visible-columns`)) {
        const storedColumns: Record<string, boolean> = JSON.parse(
          localStorage.getItem(`${this.constants.org.currentEnv.id}-${this.apiType}-logs-list-visible-columns`),
        );
        this.displayedColumnsOption = storedColumns;
        const displayedColumns = Object.keys(storedColumns).filter(key => storedColumns[key]);
        this.displayedColumns.set(displayedColumns);
      } else {
        this.displayedColumns.set(this.columnsAvailable().map(c => c.name));
        this.displayedColumnsOption = this.displayedColumns().reduce(
          (acc, curr) => {
            acc[curr] = true;
            return acc;
          },
          {} as Record<string, boolean>,
        );
      }
    });
  }

  onFiltersChanged(event: GioTableWrapperFilters) {
    const eventPagination = event.pagination;
    const currentPagination = this.pagination();
    if (
      (currentPagination.perPage >= 0 && currentPagination.perPage !== eventPagination.size) ||
      (currentPagination.page >= 0 && currentPagination.page !== eventPagination.index)
    ) {
      this.paginationUpdated.emit({ index: eventPagination.index, size: eventPagination.size });
    }
  }

  getMcpErrorLabel(error?: string): string {
    return ApiUtils.getMcpErrorLabel(error);
  }

  protected updateVisibleColumns() {
    const checkedColumns = Object.entries(this.displayedColumnsOption)
      .filter(([_k, v]) => v)
      .map(([k]) => k);
    this.displayedColumns.set(checkedColumns);
    localStorage.setItem(
      `${this.constants.org.currentEnv.id}-${this.apiType}-logs-list-visible-columns`,
      JSON.stringify(this.displayedColumnsOption),
    );
  }
}
