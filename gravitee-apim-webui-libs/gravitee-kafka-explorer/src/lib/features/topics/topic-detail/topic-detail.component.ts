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

import { BadgeColor, BadgeComponent } from '../../../components/badge/badge.component';
import { DataTableComponent } from '../../../components/data-table/data-table.component';
import { ConsumerGroupSummary, DescribeTopicResponse, TopicConfig, TopicPartitionDetail } from '../../../models/kafka-cluster.model';
import { SortComparators, sortData } from '../../../utils/sort-data';

const STATE_COLORS: Record<string, BadgeColor> = {
  stable: 'success',
  empty: 'default',
  rebalancing: 'warning',
  dead: 'error',
};

@Component({
  selector: 'gke-topic-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    BadgeComponent,
    DataTableComponent,
  ],
  templateUrl: './topic-detail.component.html',
  styleUrls: ['./topic-detail.component.scss'],
})
export class TopicDetailComponent {
  topicDetail = input<DescribeTopicResponse | undefined>();
  consumerGroups = input<ConsumerGroupSummary[]>([]);
  loading = input(false);

  back = output<void>();
  browseMessages = output<void>();
  consumerGroupSelect = output<string>();

  partitionColumns = ['id', 'leader', 'replicas', 'isr', 'offline'];
  configColumns = ['name', 'value', 'source', 'readOnly', 'sensitive'];
  consumerGroupColumns = ['groupId', 'membersCount', 'totalLag', 'coordinator', 'state'];

  // Partitions sort + pagination
  partitionSortActive = signal('');
  partitionSortDirection = signal<SortDirection>('');
  partitionPage = signal(0);
  partitionPageSize = signal(25);

  private partitionComparators: SortComparators<TopicPartitionDetail> = {
    id: (a, b) => a.id - b.id,
    leader: (a, b) => (a.leader?.host ?? '').localeCompare(b.leader?.host ?? ''),
    replicas: (a, b) => (a.replicas?.length ?? 0) - (b.replicas?.length ?? 0),
    isr: (a, b) => (a.isr?.length ?? 0) - (b.isr?.length ?? 0),
    offline: (a, b) => (a.offline?.length ?? 0) - (b.offline?.length ?? 0),
  };

  partitionSortedData = computed(() =>
    sortData(this.topicDetail()?.partitions ?? [], this.partitionSortActive(), this.partitionSortDirection(), this.partitionComparators),
  );

  partitionPaginatedData = computed(() => {
    const data = this.partitionSortedData();
    const start = this.partitionPage() * this.partitionPageSize();
    return data.slice(start, start + this.partitionPageSize());
  });

  // Configuration filter + sort + pagination
  configFilter = signal('');
  configSortActive = signal('');
  configSortDirection = signal<SortDirection>('');
  configPage = signal(0);
  configPageSize = signal(25);

  configsEmpty = computed(() => (this.topicDetail()?.configs?.length ?? 0) === 0);

  configFilteredData = computed(() => {
    const data = this.topicDetail()?.configs ?? [];
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

  // Consumer Groups sort + pagination
  cgSortActive = signal('');
  cgSortDirection = signal<SortDirection>('');
  cgPage = signal(0);
  cgPageSize = signal(25);

  private cgComparators: SortComparators<ConsumerGroupSummary> = {
    groupId: (a, b) => a.groupId.localeCompare(b.groupId),
    membersCount: (a, b) => a.membersCount - b.membersCount,
    totalLag: (a, b) => a.totalLag - b.totalLag,
    coordinator: (a, b) => (a.coordinator?.id ?? 0) - (b.coordinator?.id ?? 0),
    state: (a, b) => (a.state ?? '').localeCompare(b.state ?? ''),
  };

  cgSortedData = computed(() => sortData(this.consumerGroups(), this.cgSortActive(), this.cgSortDirection(), this.cgComparators));

  cgPaginatedData = computed(() => {
    const data = this.cgSortedData();
    const start = this.cgPage() * this.cgPageSize();
    return data.slice(start, start + this.cgPageSize());
  });

  onPartitionSortChange(sort: Sort) {
    this.partitionSortActive.set(sort.active);
    this.partitionSortDirection.set(sort.direction);
    this.partitionPage.set(0);
  }

  onConfigSortChange(sort: Sort) {
    this.configSortActive.set(sort.active);
    this.configSortDirection.set(sort.direction);
    this.configPage.set(0);
  }

  onCgSortChange(sort: Sort) {
    this.cgSortActive.set(sort.active);
    this.cgSortDirection.set(sort.direction);
    this.cgPage.set(0);
  }

  onConfigFilter(value: string) {
    this.configFilter.set(value.trim().toLowerCase());
    this.configPage.set(0);
  }

  stateColor(state: string | undefined): BadgeColor {
    return STATE_COLORS[state?.toLowerCase() ?? ''] ?? 'default';
  }
}
