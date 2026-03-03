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
import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { BrowseMessagesOptions, MessagesBrowserComponent, TailMessagesOptions } from './messages/messages-browser.component';
import { KafkaMessage } from '../../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../../services/kafka-explorer.service';

@Component({
  selector: 'gke-messages-page',
  standalone: true,
  imports: [MessagesBrowserComponent],
  templateUrl: './messages-page.component.html',
})
export class MessagesPageComponent implements OnInit, OnDestroy {
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
  tailActive = signal(false);
  private tailSubscription: Subscription | null = null;

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

  onStartTail(options: TailMessagesOptions) {
    this.tailActive.set(true);
    this.messages.set([]);
    this.totalFetched.set(0);

    this.tailSubscription = this.service
      .tailMessages(this.store.baseURL(), this.store.clusterId(), this.topicName, {
        partition: options.partition,
        keyFilter: options.keyFilter,
        valueFilter: options.valueFilter,
        maxMessages: options.maxMessages,
        durationSeconds: options.durationSeconds,
      })
      .subscribe({
        next: message => {
          const current = this.messages();
          const updated = [message, ...current].slice(0, options.maxMessages);
          this.messages.set(updated);
          this.totalFetched.set(this.totalFetched() + 1);
        },
        error: (err: unknown) => {
          this.tailActive.set(false);
          this.tailSubscription = null;
          const message = err instanceof Error ? err.message : 'Tail stream failed';
          this.store.error.set(message);
        },
        complete: () => {
          this.tailActive.set(false);
          this.tailSubscription = null;
        },
      });
  }

  onStopTail() {
    this.tailSubscription?.unsubscribe();
    this.tailSubscription = null;
    this.tailActive.set(false);
  }

  ngOnDestroy() {
    this.onStopTail();
  }

  private fetchMessages(options: BrowseMessagesOptions) {
    this.loading.set(true);
    this.service
      .browseMessages(this.store.baseURL(), this.store.clusterId(), this.topicName, {
        partition: options.partition,
        offsetMode: options.offsetMode,
        offsetValue: options.offsetValue,
        keyFilter: options.keyFilter,
        valueFilter: options.valueFilter,
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
