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

import { DescribeTopicResponse } from '../models/kafka-cluster.model';

@Component({
  selector: 'gke-topic-detail',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatIconModule, MatButtonModule, MatProgressBarModule],
  templateUrl: './topic-detail.component.html',
  styleUrls: ['./topic-detail.component.scss'],
})
export class TopicDetailComponent {
  topicDetail = input<DescribeTopicResponse | undefined>();
  loading = input(false);

  back = output<void>();

  partitionColumns = ['id', 'leader', 'replicas', 'isr', 'offline'];
  configColumns = ['name', 'value', 'source', 'readOnly', 'sensitive'];
}
