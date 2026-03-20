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

import { Component, computed, effect, inject, input, output, signal, TemplateRef } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { NgTemplateOutlet } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { Pagination } from '../../../../../entities/management-api-v2';
import {
  GioTableWrapperFilters,
  GioTableWrapperPagination,
} from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Constants } from '../../../../../entities/Constants';

export interface LogsListColumnDef {
  id: string;
  label: string;
  template?: TemplateRef<unknown>;
}

@Component({
  selector: 'logs-list-base',
  templateUrl: './logs-list-base.component.html',
  styleUrls: ['./logs-list-base.component.scss'],
  standalone: true,
  imports: [GioTableWrapperModule, MatTableModule, MatSort, NgTemplateOutlet, MatCheckboxModule, MatMenuModule, MatButtonModule, MatIcon],
})
export class LogsListBaseComponent<T = unknown> {
  private readonly constants = inject(Constants, { optional: true });

  logs = input.required<T[]>();
  pagination = input.required<Pagination>();
  columns = input.required<LogsListColumnDef[]>();
  tableId = input<string>('logsTable');
  ariaLabel = input<string>('Logs table');
  storageKey = input<string>();

  paginationUpdated = output<GioTableWrapperPagination>();

  readonly hasColumnPicker = computed(() => !!this.storageKey());

  displayedColumnsOption: Record<string, boolean> = {};
  private readonly pickerDisplayedColumns = signal<string[]>([]);

  private readonly storageId = computed(() => {
    const key = this.storageKey();
    if (!key) {
      return null;
    }
    return `${this.constants?.org?.currentEnv?.id ?? 'default'}-${key}-logs-list-visible-columns`;
  });

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

  readonly displayedColumns = computed(() => {
    if (this.hasColumnPicker()) {
      return [...this.pickerDisplayedColumns(), 'actions'];
    }
    return this.columns().map(col => col.id);
  });

  readonly pageSizeOptions: number[] = [10, 25, 50, 100];

  constructor() {
    effect(() => {
      const id = this.storageId();
      if (!id) return;

      const stored = localStorage.getItem(id);

      if (stored) {
        try {
          const storedColumns: Record<string, boolean> = JSON.parse(stored);
          const currentIds = new Set(this.columns().map(c => c.id));

          const reconciled: Record<string, boolean> = {};
          for (const colId of currentIds) {
            reconciled[colId] = storedColumns[colId] ?? true;
          }

          this.displayedColumnsOption = reconciled;
          this.pickerDisplayedColumns.set(Object.keys(reconciled).filter(k => reconciled[k]));
          return;
        } catch {
          localStorage.removeItem(id);
        }
      }

      const allIds = this.columns().map(c => c.id);
      this.pickerDisplayedColumns.set(allIds);
      this.displayedColumnsOption = allIds.reduce(
        (acc, curr) => {
          acc[curr] = true;
          return acc;
        },
        {} as Record<string, boolean>,
      );
    });
  }

  onFiltersChanged(event: GioTableWrapperFilters) {
    const eventPagination = event.pagination;
    const currentPagination = this.pagination();
    const currentPerPage = currentPagination.perPage ?? 0;
    const currentPage = currentPagination.page ?? 0;
    if ((currentPerPage >= 0 && currentPerPage !== eventPagination.size) || (currentPage >= 0 && currentPage !== eventPagination.index)) {
      this.paginationUpdated.emit({ index: eventPagination.index, size: eventPagination.size });
    }
  }

  resetColumnOptions() {
    const applied = new Set(this.pickerDisplayedColumns());
    this.displayedColumnsOption = {};
    for (const col of this.columns()) {
      this.displayedColumnsOption[col.id] = applied.has(col.id);
    }
  }

  updateVisibleColumns() {
    const checkedColumns = Object.entries(this.displayedColumnsOption)
      .filter(([_k, v]) => v)
      .map(([k]) => k);

    if (checkedColumns.length === 0) {
      this.resetColumnOptions();
      return;
    }

    this.pickerDisplayedColumns.set(checkedColumns);
    const id = this.storageId();
    if (id) {
      localStorage.setItem(id, JSON.stringify(this.displayedColumnsOption));
    }
  }
}
