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
import { KafkaTopic } from '../../../models/kafka-cluster.model';
import { FileSizePipe } from '../../../pipes/file-size.pipe';

@Component({
  selector: 'gke-topics',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatSortModule, FileSizePipe, DecimalPipe, BadgeComponent, DataTableComponent],
  templateUrl: './topics.component.html',
  styleUrls: ['./topics.component.scss'],
})
export class TopicsComponent {
  topics = input<KafkaTopic[]>([]);
  totalElements = input(0);
  page = input(0);
  pageSize = input(25);
  loading = input(false);

  filterChange = output<string>();
  pageChange = output<{ page: number; pageSize: number }>();
  sortChange = output<{ active: string; direction: string }>();
  topicSelect = output<string>();

  displayedColumns = ['name', 'partitionCount', 'replicationFactor', 'underReplicatedCount', 'messageCount', 'size'];

  onSortChange(sort: Sort) {
    this.sortChange.emit({ active: sort.active, direction: sort.direction });
  }
}
