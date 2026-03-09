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
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { KAFKA_EXPLORER_BASE_URL } from '../services/kafka-explorer-config.token';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-kafka-explorer',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, MatIconModule, RouterOutlet, RouterLink, RouterLinkActive],
  providers: [KafkaExplorerStore],
  templateUrl: './kafka-explorer.component.html',
  styleUrls: ['./kafka-explorer.component.scss'],
})
export class KafkaExplorerComponent implements OnInit {
  store = inject(KafkaExplorerStore);

  private readonly baseURL = inject(KAFKA_EXPLORER_BASE_URL);
  private readonly route = inject(ActivatedRoute);
  private readonly kafkaExplorerService = inject(KafkaExplorerService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    const clusterId = this.route.parent!.snapshot.params['clusterId'];
    this.store.baseURL.set(this.baseURL);
    this.store.clusterId.set(clusterId);
    this.store.loading.set(true);
    this.store.error.set(null);

    this.kafkaExplorerService
      .describeCluster(this.baseURL, clusterId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: cluster => {
          this.store.clusterInfo.set(cluster);
          this.store.loading.set(false);
        },
        error: err => {
          this.store.error.set(err?.error?.message || 'Failed to load cluster data');
          this.store.loading.set(false);
        },
      });
  }
}
