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
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Sort, MatSortModule, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { KafkaMessage, OffsetMode } from '../../../models/kafka-cluster.model';

export interface BrowseMessagesOptions {
  partition?: number;
  offsetMode: OffsetMode;
  offsetValue?: number;
  keyFilter?: string;
  limit: number;
}

@Component({
  selector: 'gke-messages-browser',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSortModule,
    DatePipe,
  ],
  templateUrl: './messages-browser.component.html',
  styleUrls: ['./messages-browser.component.scss'],
})
export class MessagesBrowserComponent {
  topicName = input.required<string>();
  messages = input<KafkaMessage[]>([]);
  totalFetched = input(0);
  partitionCount = input(0);
  loading = input(false);

  back = output<void>();
  search = output<BrowseMessagesOptions>();

  selectedPartition = signal<number | undefined>(undefined);
  offsetMode = signal<OffsetMode>('NEWEST');
  offsetValue = signal<number | undefined>(undefined);
  keyFilter = signal('');
  limit = signal(50);

  expandedMessage = signal<KafkaMessage | null>(null);

  sortActive = signal('timestamp');
  sortDirection = signal<SortDirection>('desc');

  sortedMessages = computed(() => {
    const msgs = this.messages();
    const active = this.sortActive();
    const direction = this.sortDirection();
    if (!active || direction === '') return msgs;

    const factor = direction === 'asc' ? 1 : -1;
    return [...msgs].sort((a, b) => {
      switch (active) {
        case 'timestamp':
          return (a.timestamp - b.timestamp) * factor;
        case 'partition':
          return (a.partition - b.partition) * factor;
        case 'offset':
          return (a.offset - b.offset) * factor;
        default:
          return 0;
      }
    });
  });

  displayedColumns = ['partition', 'offset', 'timestamp', 'key', 'value'];

  get partitionOptions(): (number | undefined)[] {
    const options: (number | undefined)[] = [undefined];
    for (let i = 0; i < this.partitionCount(); i++) {
      options.push(i);
    }
    return options;
  }

  onSortChange(sort: Sort) {
    this.sortActive.set(sort.active);
    this.sortDirection.set(sort.direction);
  }

  onFetch() {
    this.sortActive.set('timestamp');
    this.sortDirection.set(this.offsetMode() === 'NEWEST' ? 'desc' : 'asc');

    this.search.emit({
      partition: this.selectedPartition(),
      offsetMode: this.offsetMode(),
      offsetValue: this.offsetValue(),
      keyFilter: this.keyFilter() || undefined,
      limit: this.limit(),
    });
  }

  onRowClick(message: KafkaMessage) {
    this.expandedMessage.set(this.expandedMessage() === message ? null : message);
  }

  truncate(value: string | null | undefined, maxLength = 80): string {
    if (!value) return '';
    return value.length > maxLength ? value.substring(0, maxLength) + '...' : value;
  }

  formatJson(value: string | null | undefined): string {
    if (!value) return '';
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }

  isJson(value: string | null | undefined): boolean {
    if (!value) return false;
    try {
      JSON.parse(value);
      return true;
    } catch {
      return false;
    }
  }
}
