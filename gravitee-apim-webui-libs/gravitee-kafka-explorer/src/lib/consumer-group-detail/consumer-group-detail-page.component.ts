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

import { ConsumerGroupDetailComponent } from './consumer-group-detail.component';
import { DescribeConsumerGroupResponse } from '../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-consumer-group-detail-page',
  standalone: true,
  imports: [ConsumerGroupDetailComponent],
  templateUrl: './consumer-group-detail-page.component.html',
})
export class ConsumerGroupDetailPageComponent implements OnInit {
  store = inject(KafkaExplorerStore);
  private readonly service = inject(KafkaExplorerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  groupDetail = signal<DescribeConsumerGroupResponse | undefined>(undefined);
  loading = signal(false);

  ngOnInit() {
    const groupId = this.route.snapshot.params['groupId'];
    this.loading.set(true);
    this.service
      .describeConsumerGroup(this.store.baseURL(), this.store.clusterId(), groupId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: detail => {
          this.groupDetail.set(detail);
          this.loading.set(false);
        },
        error: err => {
          this.store.error.set(err?.error?.message ?? 'Failed to load consumer group details');
          this.loading.set(false);
        },
      });
  }

  onBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  onTopicSelect(topicName: string) {
    this.router.navigate(['../../topics', topicName], { relativeTo: this.route });
  }
}
