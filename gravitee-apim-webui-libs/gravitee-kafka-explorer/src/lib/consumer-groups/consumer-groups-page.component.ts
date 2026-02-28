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
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { ConsumerGroupsComponent } from './consumer-groups.component';
import { ListConsumerGroupsResponse } from '../models/kafka-cluster.model';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';
import { KafkaExplorerService } from '../services/kafka-explorer.service';

@Component({
  selector: 'gke-consumer-groups-page',
  standalone: true,
  imports: [ConsumerGroupsComponent],
  templateUrl: './consumer-groups-page.component.html',
})
export class ConsumerGroupsPageComponent implements OnInit {
  store = inject(KafkaExplorerStore);
  private readonly service = inject(KafkaExplorerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  consumerGroupsPage = signal<ListConsumerGroupsResponse | undefined>(undefined);
  consumerGroupsLoading = signal(false);

  private currentFilter = '';
  private currentPage = 0;
  private currentPageSize = 25;
  private readonly filterSubject = new Subject<string>();

  ngOnInit() {
    this.loadConsumerGroups(this.currentFilter, this.currentPage, this.currentPageSize);

    this.filterSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(filter => {
      this.currentFilter = filter;
      this.currentPage = 0;
      this.loadConsumerGroups(filter, 0, this.currentPageSize);
    });
  }

  onFilterChange(filter: string) {
    this.filterSubject.next(filter);
  }

  onGroupSelect(groupId: string) {
    this.router.navigate([groupId], { relativeTo: this.route });
  }

  onPageChange(event: { page: number; pageSize: number }) {
    this.currentPage = event.page;
    this.currentPageSize = event.pageSize;
    this.loadConsumerGroups(this.currentFilter, event.page, event.pageSize);
  }

  private loadConsumerGroups(nameFilter: string, page: number, pageSize: number) {
    this.consumerGroupsLoading.set(true);
    this.service
      .listConsumerGroups(this.store.baseURL(), this.store.clusterId(), nameFilter || undefined, page + 1, pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.consumerGroupsPage.set(response);
          this.consumerGroupsLoading.set(false);
        },
        error: err => {
          this.store.error.set(err?.error?.message ?? 'Failed to load consumer groups');
          this.consumerGroupsLoading.set(false);
        },
      });
  }
}
