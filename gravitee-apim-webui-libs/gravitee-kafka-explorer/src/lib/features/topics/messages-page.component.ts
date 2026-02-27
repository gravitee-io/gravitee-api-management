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

import { BrowseMessagesOptions, MessagesBrowserComponent } from './messages/messages-browser.component';
import { KafkaMessage } from '../../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../../services/kafka-explorer.service';

@Component({
  selector: 'gke-messages-page',
  standalone: true,
  imports: [MessagesBrowserComponent],
  templateUrl: './messages-page.component.html',
})
export class MessagesPageComponent implements OnInit {
  store = inject(KafkaExplorerStore);
  private readonly service = inject(KafkaExplorerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  topicName = '';
  partitionCount = signal(0);
  messages = signal<KafkaMessage[]>([]);
  totalFetched = signal(0);
  loading = signal(false);

  ngOnInit() {
    this.topicName = this.route.snapshot.params['topicName'];

    // Fetch topic info to get partition count
    this.service
      .describeTopic(this.store.baseURL(), this.store.clusterId(), this.topicName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: detail => this.partitionCount.set(detail.partitions.length),
      });

    // Initial fetch with defaults
    this.fetchMessages({ offsetMode: 'NEWEST', limit: 50 });
  }

  onSearch(options: BrowseMessagesOptions) {
    this.fetchMessages(options);
  }

  onBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  private fetchMessages(options: BrowseMessagesOptions) {
    this.loading.set(true);
    this.service
      .browseMessages(this.store.baseURL(), this.store.clusterId(), this.topicName, {
        partition: options.partition,
        offsetMode: options.offsetMode,
        offsetValue: options.offsetValue,
        keyFilter: options.keyFilter,
        limit: options.limit,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.messages.set(result.data);
          this.totalFetched.set(result.totalFetched);
          this.loading.set(false);
        },
        error: err => {
          this.store.error.set(err?.error?.message ?? 'Failed to browse messages');
          this.loading.set(false);
        },
      });
  }
}
