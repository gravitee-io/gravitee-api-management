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
import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { Sort, MatSortModule, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { BadgeComponent } from '../../../components/badge/badge.component';
import { DataTableComponent } from '../../../components/data-table/data-table.component';
import { ConsumerGroupSummary } from '../../../models/kafka-cluster.model';
import { consumerGroupStateColor } from '../consumer-group-state';

@Component({
  selector: 'gke-consumer-groups',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatSortModule, MatTooltipModule, DecimalPipe, BadgeComponent, DataTableComponent],
  templateUrl: './consumer-groups.component.html',
  styleUrls: ['./consumer-groups.component.scss'],
})
export class ConsumerGroupsComponent {
  consumerGroups = input<ConsumerGroupSummary[]>([]);
  totalElements = input(0);
  page = input(0);
  pageSize = input(25);
  loading = input(false);

  filterChange = output<string>();
  pageChange = output<{ page: number; pageSize: number }>();
  groupSelect = output<string>();

  displayedColumns = ['groupId', 'state', 'membersCount', 'numTopics', 'coordinator', 'totalLag'];

  stateColor = consumerGroupStateColor;

  sortActive = signal('');
  sortDirection = signal<SortDirection>('');

  sortedConsumerGroups = computed(() => {
    const data = this.consumerGroups();
    const active = this.sortActive();
    const direction = this.sortDirection();
    if (!active || direction === '') return data;

    const factor = direction === 'asc' ? 1 : -1;
    return [...data].sort((a, b) => {
      switch (active) {
        case 'groupId':
          return a.groupId.localeCompare(b.groupId) * factor;
        case 'state':
          return (a.state ?? '').localeCompare(b.state ?? '') * factor;
        case 'membersCount':
          return (a.membersCount - b.membersCount) * factor;
        case 'numTopics':
          return (a.numTopics - b.numTopics) * factor;
        case 'coordinator':
          return ((a.coordinator?.id ?? 0) - (b.coordinator?.id ?? 0)) * factor;
        case 'totalLag':
          return (a.totalLag - b.totalLag) * factor;
        default:
          return 0;
      }
    });
  });

  onSortChange(sort: Sort) {
    this.sortActive.set(sort.active);
    this.sortDirection.set(sort.direction);
  }
}
