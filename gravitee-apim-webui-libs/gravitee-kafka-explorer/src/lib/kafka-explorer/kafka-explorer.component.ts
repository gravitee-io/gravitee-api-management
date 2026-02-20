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
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { BrokersComponent } from '../brokers/brokers.component';
import { DescribeClusterResponse } from '../models/kafka-cluster.model';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-kafka-explorer',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressSpinnerModule, MatIconModule, BrokersComponent],
  templateUrl: './kafka-explorer.component.html',
  styleUrls: ['./kafka-explorer.component.scss'],
})
export class KafkaExplorerComponent implements OnInit {
  baseURL = input.required<string>();
  clusterId = input.required<string>();

  clusterInfo = signal<DescribeClusterResponse | undefined>(undefined);
  loading = signal(false);
  error = signal<string | null>(null);

  private readonly kafkaExplorerService = inject(KafkaExplorerService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.loading.set(true);
    this.error.set(null);

    this.kafkaExplorerService
      .describeCluster(this.baseURL(), this.clusterId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.clusterInfo.set(response);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(err?.error?.message || 'Failed to describe cluster');
          this.loading.set(false);
        },
      });
  }
}
