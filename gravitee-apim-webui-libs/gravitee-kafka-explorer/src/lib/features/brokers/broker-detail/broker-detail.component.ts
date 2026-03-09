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
import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Sort, MatSortModule, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { BadgeComponent } from '../../../components/badge/badge.component';
import { DataTableComponent } from '../../../components/data-table/data-table.component';
import { BrokerLogDirEntry, DescribeBrokerResponse, TopicConfig } from '../../../models/kafka-cluster.model';
import { FileSizePipe } from '../../../pipes/file-size.pipe';
import { SortComparators, sortData } from '../../../utils/sort-data';

@Component({
  selector: 'gke-broker-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    FileSizePipe,
    BadgeComponent,
    DataTableComponent,
  ],
  templateUrl: './broker-detail.component.html',
  styleUrls: ['./broker-detail.component.scss'],
})
export class BrokerDetailComponent {
  brokerDetail = input<DescribeBrokerResponse | undefined>();
  loading = input(false);

  back = output<void>();

  logDirColumns = ['path', 'error', 'topics', 'partitions', 'size'];
  configColumns = ['name', 'value', 'source', 'readOnly', 'sensitive'];

  // Log Directories sort + pagination
  logDirSortActive = signal('');
  logDirSortDirection = signal<SortDirection>('');
  logDirPage = signal(0);
  logDirPageSize = signal(25);

  private logDirComparators: SortComparators<BrokerLogDirEntry> = {
    path: (a, b) => a.path.localeCompare(b.path),
    error: (a, b) => (a.error ?? '').localeCompare(b.error ?? ''),
    topics: (a, b) => a.topics - b.topics,
    partitions: (a, b) => a.partitions - b.partitions,
    size: (a, b) => a.size - b.size,
  };

  logDirSortedData = computed(() =>
    sortData(this.brokerDetail()?.logDirEntries ?? [], this.logDirSortActive(), this.logDirSortDirection(), this.logDirComparators),
  );

  logDirPaginatedData = computed(() => {
    const data = this.logDirSortedData();
    const start = this.logDirPage() * this.logDirPageSize();
    return data.slice(start, start + this.logDirPageSize());
  });

  // Configuration filter + sort + pagination
  configFilter = signal('');
  configSortActive = signal('');
  configSortDirection = signal<SortDirection>('');
  configPage = signal(0);
  configPageSize = signal(25);

  configsEmpty = computed(() => (this.brokerDetail()?.configs?.length ?? 0) === 0);

  configFilteredData = computed(() => {
    const data = this.brokerDetail()?.configs ?? [];
    const filter = this.configFilter();
    if (!filter) return data;
    return data.filter(
      (c: TopicConfig) =>
        c.name.toLowerCase().includes(filter) || c.value.toLowerCase().includes(filter) || c.source.toLowerCase().includes(filter),
    );
  });

  private configComparators: SortComparators<TopicConfig> = {
    name: (a, b) => a.name.localeCompare(b.name),
    value: (a, b) => a.value.localeCompare(b.value),
    source: (a, b) => a.source.localeCompare(b.source),
    readOnly: (a, b) => Number(a.readOnly) - Number(b.readOnly),
    sensitive: (a, b) => Number(a.sensitive) - Number(b.sensitive),
  };

  configSortedData = computed(() =>
    sortData(this.configFilteredData(), this.configSortActive(), this.configSortDirection(), this.configComparators),
  );

  configPaginatedData = computed(() => {
    const data = this.configSortedData();
    const start = this.configPage() * this.configPageSize();
    return data.slice(start, start + this.configPageSize());
  });

  onLogDirSortChange(sort: Sort) {
    this.logDirSortActive.set(sort.active);
    this.logDirSortDirection.set(sort.direction);
    this.logDirPage.set(0);
  }

  onConfigSortChange(sort: Sort) {
    this.configSortActive.set(sort.active);
    this.configSortDirection.set(sort.direction);
    this.configPage.set(0);
  }

  onConfigFilter(value: string) {
    this.configFilter.set(value.trim().toLowerCase());
    this.configPage.set(0);
  }
}
