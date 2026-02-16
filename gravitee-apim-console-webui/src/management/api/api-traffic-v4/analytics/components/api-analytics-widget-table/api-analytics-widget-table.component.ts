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
import { Component, computed, input, output, Signal, signal } from '@angular/core';
import { DecimalPipe, NgTemplateOutlet, PercentPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';

import { GioTableWrapperFilters } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioTableWrapperModule } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

type ColumnDataType = 'number' | 'string' | 'percentage';

export interface ApiAnalyticsWidgetTableRowData {
  // Column name and value pairs
  [columnName: string]: unknown;
}

export type ApiAnalyticsWidgetTableDataColumn = {
  name: string;
  label: string;
  isSortable?: boolean;
  dataType: ColumnDataType;
};

@Component({
  selector: 'api-analytics-widget-table',
  imports: [DecimalPipe, GioTableWrapperModule, MatTableModule, MatSortModule, PercentPipe, NgTemplateOutlet],
  templateUrl: './api-analytics-widget-table.component.html',
  styleUrl: './api-analytics-widget-table.component.scss',
})
export class ApiAnalyticsWidgetTableComponent {
  columns = input.required<ApiAnalyticsWidgetTableDataColumn[]>();
  data = input.required<ApiAnalyticsWidgetTableRowData[]>();
  firstColumnClickable = input<boolean>(false);
  keyIdentifier = input<string | undefined>(undefined);

  firstColumnCellClick = output<ApiAnalyticsWidgetTableRowData>();

  displayedColumns = computed(() => this.columns().map(column => column.name));
  totalLength: Signal<number> = computed(() => this.data()?.length || 0);

  filteredTableData: Signal<ApiAnalyticsWidgetTableRowData[]> = computed(() => {
    const currentData = this.data();
    if (!currentData || currentData.length === 0) {
      return [];
    }
    const filtered = gioTableFilterCollection(currentData, this.tableFilters());
    return filtered.filteredCollection;
  });

  tableFilters = signal<GioTableWrapperFilters>({
    pagination: { index: 1, size: 5 },
    searchTerm: '',
    sort: {
      direction: 'desc',
    },
  });

  onFirstColumnActivate(rowData: ApiAnalyticsWidgetTableRowData) {
    if (!rowData) {
      return;
    }
    if (rowData['unknown']) {
      return;
    }
    this.firstColumnCellClick.emit(rowData);
  }
}
