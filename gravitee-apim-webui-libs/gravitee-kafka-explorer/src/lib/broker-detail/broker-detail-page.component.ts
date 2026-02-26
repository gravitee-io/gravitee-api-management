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
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { BrokerDetailComponent } from './broker-detail.component';
import { DescribeBrokerResponse } from '../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-broker-detail-page',
  standalone: true,
  imports: [BrokerDetailComponent],
  templateUrl: './broker-detail-page.component.html',
})
export class BrokerDetailPageComponent implements OnInit {
  store = inject(KafkaExplorerStore);
  private readonly service = inject(KafkaExplorerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  brokerDetail = signal<DescribeBrokerResponse | undefined>(undefined);
  loading = signal(false);

  ngOnInit() {
    const brokerId = Number(this.route.snapshot.params['brokerId']);
    this.loading.set(true);
    this.service
      .describeBroker(this.store.baseURL(), this.store.clusterId(), brokerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: detail => {
          this.brokerDetail.set(detail);
          this.loading.set(false);
        },
        error: err => {
          this.store.error.set(err?.error?.message ?? 'Failed to load broker details');
          this.loading.set(false);
        },
      });
  }

  onBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
