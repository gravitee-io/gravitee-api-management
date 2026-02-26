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

import { TopicDetailComponent } from './topic-detail.component';
import { DescribeTopicResponse } from '../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-topic-detail-page',
  standalone: true,
  imports: [TopicDetailComponent],
  templateUrl: './topic-detail-page.component.html',
})
export class TopicDetailPageComponent implements OnInit {
  store = inject(KafkaExplorerStore);
  private readonly service = inject(KafkaExplorerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  topicDetail = signal<DescribeTopicResponse | undefined>(undefined);
  loading = signal(false);

  ngOnInit() {
    const topicName = this.route.snapshot.params['topicName'];
    this.loading.set(true);
    this.service
      .describeTopic(this.store.baseURL(), this.store.clusterId(), topicName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: detail => {
          this.topicDetail.set(detail);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
