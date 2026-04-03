/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { DEFAULT_PAGE_SIZE_OPTIONS, PaginationComponent } from '../pagination/pagination.component';

export interface TableColumn {
  id: string;
  label: string;
  type?: 'text' | 'date';
}

export interface TableAction<T> {
  id: string;
  icon: string;
  ariaLabel: string;
  color?: 'primary' | 'accent' | 'warn';
  isVisible?: (row: T) => boolean;
  isDisabled?: (row: T) => boolean;
}

@Component({
  selector: 'app-paginated-table',
  standalone: true,
  imports: [DatePipe, MatTableModule, MatIcon, RouterLink, PaginationComponent, MatButtonModule],
  templateUrl: './paginated-table.component.html',
  styleUrl: './paginated-table.component.scss',
})
export class PaginatedTableComponent<T> {
  columns = input.required<TableColumn[]>();
  rows = input.required<T[]>();
  totalElements = input.required<number>();
  currentPage = input.required<number>();
  pageSize = input.required<number>();
  pageSizeOptions = input<number[]>(DEFAULT_PAGE_SIZE_OPTIONS);
  navigable = input(true);
  actions = input<TableAction<T>[]>([]);

  pageChange = output<number>();
  pageSizeChange = output<number>();
  actionClick = output<{ actionId: string; row: T }>();

  displayedColumns = computed(() => [
    ...this.columns().map(c => c.id as string),
    ...(this.actions().length > 0 ? ['actions'] : []),
    ...(this.navigable() ? ['expand'] : []),
  ]);

  onPageChange(page: number): void {
    this.pageChange.emit(page);
  }

  onActionClick(event: Event, actionId: string, row: T): void {
    event.stopPropagation();
    this.actionClick.emit({ actionId, row });
  }
}
