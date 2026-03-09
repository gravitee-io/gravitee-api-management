/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, DestroyRef, effect, inject } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { merge } from 'rxjs';
import { debounceTime, map } from 'rxjs/operators';

import {
  DropdownSearchComponent,
  ResultsLoaderInput,
  ResultsLoaderOutput,
} from '../../../components/dropdown-search/dropdown-search.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { SubscriptionMetadata, SubscriptionStatusEnum } from '../../../entities/subscription';
import { ApiService } from '../../../services/api.service';
import { ApplicationService } from '../../../services/application.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { areFiltersEqual, parseArrayParam, parsePageParam, parseSizeParam, toTitleCase } from '../../../utils/common.utils';

type SubscriptionFilters = {
  apiIds: string[] | undefined;
  applicationIds: string[] | undefined;
  statuses: SubscriptionStatusEnum[] | null;
  page: number;
  size: number;
};

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [MatButton, ReactiveFormsModule, LoaderComponent, PaginatedTableComponent, DropdownSearchComponent],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
})
export default class SubscriptionsComponent {
  private static readonly DEFAULT_PAGE_SIZE = 10;
  private static readonly DEFAULT_PAGE = 1;
  private static readonly MAX_PAGE_SIZE = 100;

  private subscriptionService = inject(SubscriptionService);
  private applicationService = inject(ApplicationService);
  private apiService = inject(ApiService);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  statusOptions = this.subscriptionStatusesList.map(status => ({
    value: status,
    label: toTitleCase(status),
  }));
  tableColumns: TableColumn[] = [
    { id: 'api', label: 'Subscribed API' },
    { id: 'plan', label: 'Plan' },
    { id: 'application', label: 'Application' },
    { id: 'created_at', label: 'Created', type: 'date' },
    { id: 'status', label: 'Status' },
  ];

  // Form controls for filters (UI view of URL state)
  apiFilter = new FormControl<string[] | null>(null);
  applicationFilter = new FormControl<string[] | null>(null);
  statusFilter = new FormControl<SubscriptionStatusEnum[] | null>([]);

  private queryParams = toSignal(this.activatedRoute.queryParams, { initialValue: {} as Record<string, unknown> });

  filters = computed(() => this.parseParamsToFilters(this.queryParams()));

  pageSize = computed(() => this.filters().size);
  currentPage = computed(() => this.filters().page);

  // Available options for filters
  private availableApplicationsResource = rxResource({
    stream: () => this.applicationService.list(1, -1),
  });
  availableApplications = computed(() => this.availableApplicationsResource.value()?.data ?? []);
  applicationOptions = computed(() =>
    (this.availableApplications() ?? []).map(app => ({
      value: app.id,
      label: app.name,
    })),
  );

  isLoadingSubscriptions = computed(() => this.subscriptionsResource.isLoading());

  subscriptionsResource = rxResource({
    params: () => this.filters(),
    stream: ({ params }) => this.subscriptionService.list(params),
  });

  totalElements = computed(() => {
    const response = this.subscriptionsResource.value();
    return response?.metadata?.['paginateMetaData']?.totalElements ?? response?.data?.length ?? 0;
  });

  hasSubscriptions = computed(() => {
    const response = this.subscriptionsResource.value();
    const { apiIds, applicationIds, statuses } = this.filters();
    const isFiltered = (apiIds?.length ?? 0) > 0 || (applicationIds?.length ?? 0) > 0 || (statuses?.length ?? 0) > 0;

    if (this.isLoadingSubscriptions()) {
      return true;
    }

    if (!isFiltered) {
      return (response?.data?.length ?? 0) > 0 || this.totalElements() > 0;
    }
    return true;
  });

  rows = computed(() => {
    const response = this.subscriptionsResource.value();
    if (!response?.data) {
      return [];
    }

    return response.data.map(sub => ({
      id: sub.id,
      api: this.retrieveMetadataName(sub.api, response.metadata),
      plan: this.retrieveMetadataName(sub.plan, response.metadata),
      application: this.retrieveMetadataName(sub.application, response.metadata),
      created_at: sub.created_at ?? '',
      status: toTitleCase(sub.status),
    }));
  });

  constructor() {
    this.setupUrlSync();
  }

  apiResultsLoader = ({ searchTerm, page }: ResultsLoaderInput) =>
    this.apiService.search(page, 'all', searchTerm ?? '', 10).pipe(
      map(
        (response): ResultsLoaderOutput => ({
          data: (response.data ?? []).map(api => ({ value: api.id, label: api.name })),
          hasNextPage: (response.metadata?.pagination?.current_page ?? 1) < (response.metadata?.pagination?.total_pages ?? 1),
        }),
      ),
    );

  retrieveMetadataName(id: string, metadata?: SubscriptionMetadata): string {
    return metadata?.[id]?.name ?? id;
  }

  onPageChange(page: number): void {
    this.updateQueryParams({ page });
  }

  onPageSizeChange(size: number): void {
    this.updateQueryParams({ size, page: 1 });
  }

  clearFilters(): void {
    this.updateQueryParams({
      apiIds: null,
      applicationIds: null,
      statuses: null,
      page: 1,
    });
  }

  private setupUrlSync() {
    effect(() => {
      this.applyFiltersToForm(this.filters());
    });

    merge(this.apiFilter.valueChanges, this.applicationFilter.valueChanges, this.statusFilter.valueChanges)
      .pipe(debounceTime(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.syncUrlToForm());
  }

  private applyFiltersToForm(filters: SubscriptionFilters) {
    this.apiFilter.setValue(filters.apiIds ?? null, { emitEvent: false });
    this.applicationFilter.setValue(filters.applicationIds ?? null, { emitEvent: false });
    this.statusFilter.setValue(filters.statuses ?? [], { emitEvent: false });
  }

  private syncUrlToForm() {
    const currentFilters = this.filters();
    const apiIds = this.apiFilter.value ?? [];
    const applicationIds = this.applicationFilter.value ?? [];
    const statuses = this.statusFilter.value ?? [];

    const nextFilters: SubscriptionFilters = {
      apiIds: apiIds.length ? apiIds : undefined,
      applicationIds: applicationIds.length ? applicationIds : undefined,
      statuses: statuses.length ? statuses : null,
      page: currentFilters.page,
      size: currentFilters.size,
    };

    if (!areFiltersEqual(currentFilters, nextFilters)) {
      this.updateQueryParams(this.toRouterQueryParams(nextFilters), true);
    }
  }

  private updateQueryParams(queryParams: Record<string, unknown>, replaceUrl = false): void {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl,
    });
  }

  private parseParamsToFilters(params: Record<string, unknown>): SubscriptionFilters {
    const apiIds = parseArrayParam(params['apiIds']);
    const applicationIds = parseArrayParam(params['applicationIds']);
    const statuses = this.parseStatusParam(params['statuses']);

    return {
      apiIds: apiIds.length ? apiIds : undefined,
      applicationIds: applicationIds.length ? applicationIds : undefined,
      statuses: statuses.length ? statuses : null,
      page: parsePageParam(params['page'], SubscriptionsComponent.DEFAULT_PAGE),
      size: parseSizeParam(params['size'], SubscriptionsComponent.DEFAULT_PAGE_SIZE, SubscriptionsComponent.MAX_PAGE_SIZE),
    };
  }

  private toRouterQueryParams(filters: SubscriptionFilters): Record<string, unknown> {
    return {
      apiIds: filters.apiIds ?? null,
      applicationIds: filters.applicationIds ?? null,
      statuses: filters.statuses ?? null,
      page: filters.page,
      size: filters.size,
    };
  }

  private parseStatusParam(param: unknown): SubscriptionStatusEnum[] {
    const values = parseArrayParam(param);
    return values.filter((v): v is SubscriptionStatusEnum => Object.values(SubscriptionStatusEnum).includes(v as SubscriptionStatusEnum));
  }
}
