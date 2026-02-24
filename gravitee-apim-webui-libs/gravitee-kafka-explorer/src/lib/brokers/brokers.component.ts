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
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';

import { BrokerDetail } from '../models/kafka-cluster.model';

@Component({
  selector: 'gke-brokers',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule],
  templateUrl: './brokers.component.html',
  styleUrls: ['./brokers.component.scss'],
})
export class BrokersComponent {
  nodes = input<BrokerDetail[]>([]);
  controllerId = input<number>(-1);

  displayedColumns = ['id', 'host', 'port', 'rack', 'leaderPartitions', 'replicaPartitions', 'logDirSize'];

  formatBytes(bytes: number | null): string {
    if (bytes === null || bytes === undefined) return '-';
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
