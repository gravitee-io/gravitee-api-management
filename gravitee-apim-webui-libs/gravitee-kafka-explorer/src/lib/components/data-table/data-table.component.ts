/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'gke-data-table',
  standalone: true,
  imports: [MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule, MatProgressBarModule],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTableComponent {
  filterable = input(false);
  filterPlaceholder = input('Filter...');
  filterChange = output<string>();

  loading = input(false);

  empty = input(false);
  emptyMessage = input('No data available');

  paginated = input(false);
  totalElements = input(0);
  page = input(0);
  pageSize = input(25);
  pageSizeOptions = input([10, 25, 50, 100]);
  pageChange = output<{ page: number; pageSize: number }>();

  onFilterChange(event: Event) {
    this.filterChange.emit((event.target as HTMLInputElement).value);
  }
}
