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
import { Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { Sort, MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { BadgeComponent } from '../../../components/badge/badge.component';
import { DataTableComponent } from '../../../components/data-table/data-table.component';
import { ConsumerGroupSummary } from '../../../models/kafka-cluster.model';
import { consumerGroupStateColor } from '../consumer-group-state';

@Component({
  selector: 'gke-consumer-groups',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatSortModule, DecimalPipe, BadgeComponent, DataTableComponent],
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
  sortChange = output<{ active: string; direction: string }>();
  groupSelect = output<string>();

  displayedColumns = ['groupId', 'state', 'membersCount', 'numTopics', 'coordinator', 'totalLag'];

  stateColor = consumerGroupStateColor;

  onSortChange(sort: Sort) {
    this.sortChange.emit({ active: sort.active, direction: sort.direction });
  }
}
