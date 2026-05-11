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
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { NATIVE_STATUS_META } from '../../api-runtime-logs-native.models';
import { FormatDurationPipe } from '../../../../../../shared/pipes/format-duration.pipe';
import { GioTableWrapperModule } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import {
  GioTableWrapperFilters,
  GioTableWrapperPagination,
} from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { NativeApiLog, Pagination, Plan } from '../../../../../../entities/management-api-v2';
import { Application } from '../../../../../../entities/application/Application';

const DISPLAYED_COLUMNS = ['timestamp', 'application', 'plan', 'clientIdentifier', 'connectionStatus', 'duration'];

@Component({
  selector: 'api-runtime-logs-native-list',
  templateUrl: './api-runtime-logs-native-list.component.html',
  styleUrls: ['./api-runtime-logs-native-list.component.scss'],
  standalone: true,
  imports: [GioTableWrapperModule, MatTableModule, MatIconModule, DatePipe, FormatDurationPipe],
})
export class ApiRuntimeLogsNativeListComponent {
  logs = input.required<NativeApiLog[]>();
  pagination = input.required<Pagination>();
  applications = input<Application[]>([]);
  plans = input<Plan[]>([]);

  paginationUpdated = output<GioTableWrapperPagination>();
  pageSizeOptions: number[] = [10, 25, 50, 100];
  displayedColumns = DISPLAYED_COLUMNS;
  protected readonly statusMeta = NATIVE_STATUS_META;

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

  private readonly applicationsById = computed(() => new Map(this.applications().map(a => [a.id, a.name] as const)));
  private readonly plansById = computed(() => new Map(this.plans().map(p => [p.id, p.name] as const)));

  onFiltersChanged(event: GioTableWrapperFilters) {
    const eventPagination = event.pagination;
    const currentPagination = this.pagination();
    // Suppress the initial mount emission when pagination has not been hydrated yet.
    const sizeChanged = currentPagination.perPage != null && currentPagination.perPage !== eventPagination.size;
    const pageChanged = currentPagination.page != null && currentPagination.page !== eventPagination.index;
    if (sizeChanged || pageChanged) {
      this.paginationUpdated.emit({ index: eventPagination.index, size: eventPagination.size });
    }
  }

  applicationName(id?: string): string {
    if (!id) return '';
    return this.applicationsById().get(id) ?? '';
  }

  planName(id?: string): string {
    if (!id) return '';
    return this.plansById().get(id) ?? '';
  }
}
