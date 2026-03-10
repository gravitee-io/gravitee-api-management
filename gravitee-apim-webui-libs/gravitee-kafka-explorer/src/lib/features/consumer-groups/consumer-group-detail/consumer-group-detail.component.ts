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
import { ConsumerGroupMember, ConsumerGroupOffset, DescribeConsumerGroupResponse } from '../../../models/kafka-cluster.model';
import { SortComparators, sortData } from '../../../utils/sort-data';
import { consumerGroupStateColor } from '../consumer-group-state';

@Component({
  selector: 'gke-consumer-group-detail',
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
  templateUrl: './consumer-group-detail.component.html',
  styleUrls: ['./consumer-group-detail.component.scss'],
})
export class ConsumerGroupDetailComponent {
  groupDetail = input<DescribeConsumerGroupResponse | undefined>();
  loading = input(false);

  back = output<void>();
  topicSelect = output<string>();

  memberColumns = ['memberId', 'clientId', 'host', 'assignments'];
  offsetColumns = ['topic', 'partition', 'committedOffset', 'endOffset', 'lag'];
  stateColor = consumerGroupStateColor;

  // Members sort + pagination
  memberSortActive = signal('');
  memberSortDirection = signal<SortDirection>('');
  memberPage = signal(0);
  memberPageSize = signal(25);

  private memberComparators: SortComparators<ConsumerGroupMember> = {
    memberId: (a, b) => a.memberId.localeCompare(b.memberId),
    clientId: (a, b) => a.clientId.localeCompare(b.clientId),
    host: (a, b) => a.host.localeCompare(b.host),
    assignments: (a, b) => (a.assignments?.length ?? 0) - (b.assignments?.length ?? 0),
  };

  memberSortedData = computed(() =>
    sortData(this.groupDetail()?.members ?? [], this.memberSortActive(), this.memberSortDirection(), this.memberComparators),
  );

  memberPaginatedData = computed(() => {
    const data = this.memberSortedData();
    const start = this.memberPage() * this.memberPageSize();
    return data.slice(start, start + this.memberPageSize());
  });

  // Offsets sort + pagination
  offsetSortActive = signal('');
  offsetSortDirection = signal<SortDirection>('');
  offsetPage = signal(0);
  offsetPageSize = signal(25);

  private offsetComparators: SortComparators<ConsumerGroupOffset> = {
    topic: (a, b) => a.topic.localeCompare(b.topic),
    partition: (a, b) => a.partition - b.partition,
    committedOffset: (a, b) => a.committedOffset - b.committedOffset,
    endOffset: (a, b) => a.endOffset - b.endOffset,
    lag: (a, b) => a.lag - b.lag,
  };

  offsetSortedData = computed(() =>
    sortData(this.groupDetail()?.offsets ?? [], this.offsetSortActive(), this.offsetSortDirection(), this.offsetComparators),
  );

  offsetPaginatedData = computed(() => {
    const data = this.offsetSortedData();
    const start = this.offsetPage() * this.offsetPageSize();
    return data.slice(start, start + this.offsetPageSize());
  });

  onMemberSortChange(sort: Sort) {
    this.memberSortActive.set(sort.active);
    this.memberSortDirection.set(sort.direction);
    this.memberPage.set(0);
  }

  onOffsetSortChange(sort: Sort) {
    this.offsetSortActive.set(sort.active);
    this.offsetSortDirection.set(sort.direction);
    this.offsetPage.set(0);
  }

  countDistinctTopics(offsets: ConsumerGroupOffset[]): number {
    return new Set(offsets.map(o => o.topic)).size;
  }
}
