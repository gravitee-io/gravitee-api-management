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
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { ConsumerGroupOffset, DescribeConsumerGroupResponse } from '../models/kafka-cluster.model';

@Component({
  selector: 'gke-consumer-group-detail',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatIconModule, MatButtonModule, MatProgressBarModule],
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

  countDistinctTopics(offsets: ConsumerGroupOffset[]): number {
    return new Set(offsets.map(o => o.topic)).size;
  }
}
