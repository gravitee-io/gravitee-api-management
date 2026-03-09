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
import { MatCardModule } from '@angular/material/card';
import { Sort, MatSortModule, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { BadgeComponent } from '../../../components/badge/badge.component';
import { DataTableComponent } from '../../../components/data-table/data-table.component';
import { BrokerDetail, KafkaNode } from '../../../models/kafka-cluster.model';
import { FileSizePipe } from '../../../pipes/file-size.pipe';

@Component({
  selector: 'gke-brokers',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatSortModule, FileSizePipe, BadgeComponent, DataTableComponent],
  templateUrl: './brokers.component.html',
  styleUrls: ['./brokers.component.scss'],
})
export class BrokersComponent {
  nodes = input<BrokerDetail[]>([]);
  controllerId = input<number>(-1);
  clusterId = input<string>('');
  controller = input<KafkaNode | undefined>(undefined);
  totalTopics = input<number>(0);
  totalPartitions = input<number>(0);

  brokerSelect = output<number>();

  displayedColumns = ['id', 'host', 'port', 'rack', 'leaderPartitions', 'replicaPartitions', 'logDirSize'];

  sortActive = signal('');
  sortDirection = signal<SortDirection>('');

  sortedNodes = computed(() => {
    const data = this.nodes();
    const active = this.sortActive();
    const direction = this.sortDirection();
    if (!active || direction === '') return data;

    const factor = direction === 'asc' ? 1 : -1;
    return [...data].sort((a, b) => {
      switch (active) {
        case 'id':
          return (a.id - b.id) * factor;
        case 'host':
          return a.host.localeCompare(b.host) * factor;
        case 'port':
          return (a.port - b.port) * factor;
        case 'rack':
          return (a.rack ?? '').localeCompare(b.rack ?? '') * factor;
        case 'leaderPartitions':
          return ((a.leaderPartitions ?? 0) - (b.leaderPartitions ?? 0)) * factor;
        case 'replicaPartitions':
          return ((a.replicaPartitions ?? 0) - (b.replicaPartitions ?? 0)) * factor;
        case 'logDirSize':
          return ((a.logDirSize ?? 0) - (b.logDirSize ?? 0)) * factor;
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
