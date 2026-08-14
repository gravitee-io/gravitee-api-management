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
import { debounceTime } from 'rxjs/operators';

import { subscriptionListBreadcrumb } from './subscription-breadcrumbs';
import { BadgeComponent } from '../../../components/badge/badge.component';
import { DropdownSearchComponent } from '../../../components/dropdown-search/dropdown-search.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../components/paginated-table/table-cell.directive';
import { SearchBarComponent } from '../../../components/search-bar/search-bar.component';
import { Subscription, SubscriptionMetadata, SubscriptionReferenceType, SubscriptionStatusEnum } from '../../../entities/subscription';
import { ApplicationService } from '../../../services/application.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { areFiltersEqual, parseArrayParam, parsePageParam, parseSizeParam, toTitleCase } from '../../../utils/common.utils';

type SubscriptionFilters = {
  query: string;
  referenceTypes: SubscriptionReferenceType[] | null;
  applicationIds: string[] | undefined;
  statuses: SubscriptionStatusEnum[] | null;
  page: number;
  size: number;
};

interface SubscriptionTableRow {
  id: string;
  targetName: string;
  targetType: string;
  plan: string;
  application: string;
  startAt: string | null;
  endAt: string | null;
  status: string;
}

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [
    BadgeComponent,
    DropdownSearchComponent,
    LoaderComponent,
    MatButton,
    PaginatedTableComponent,
    ReactiveFormsModule,
    SearchBarComponent,
    TableCellDirective,
  ],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
})
export default class SubscriptionsComponent {
  private static readonly DEFAULT_PAGE_SIZE = 10;
  private static readonly DEFAULT_PAGE = 1;
  private static readonly MAX_PAGE_SIZE = 100;
  private static readonly ALL_REFERENCE_TYPES: SubscriptionReferenceType[] = ['API', 'API_PRODUCT'];

  private readonly subscriptionService = inject(SubscriptionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly breadcrumbService = inject(BreadcrumbService);

  readonly subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  readonly statusOptions = this.subscriptionStatusesList.map(status => ({
    value: status,
    label: toTitleCase(status),
  }));
  readonly typeOptions = [
    { value: 'API', label: $localize`:@@subscriptionTypeApi:API` },
    { value: 'API_PRODUCT', label: $localize`:@@subscriptionTypeApiProduct:API Product` },
  ];
  readonly tableColumns: TableColumn[] = [
    { id: 'targetName', label: $localize`:@@subscriptionTargetColumn:Subscription target` },
    { id: 'targetType', label: $localize`:@@subscriptionTypeColumn:Type` },
    { id: 'plan', label: $localize`:@@subscriptionPlanColumn:Plan` },
    { id: 'application', label: $localize`:@@subscriptionApplicationColumn:Application` },
    { id: 'startAt', label: $localize`:@@subscriptionStartDateColumn:Start date`, type: 'date' },
    { id: 'endAt', label: $localize`:@@subscriptionEndDateColumn:End date`, type: 'date' },
    { id: 'status', label: $localize`:@@subscriptionStatusColumn:Status` },
  ];

  // Form controls for filters (UI view of URL state)
  readonly typeFilter = new FormControl<SubscriptionReferenceType[] | null>([]);
  readonly applicationFilter = new FormControl<string[] | null>(null);
  readonly statusFilter = new FormControl<SubscriptionStatusEnum[] | null>([]);

  private readonly queryParams = toSignal(this.activatedRoute.queryParams, { initialValue: {} as Record<string, unknown> });

  readonly filters = computed(() => this.parseParamsToFilters(this.queryParams()));

  readonly pageSize = computed(() => this.filters().size);
  readonly currentPage = computed(() => this.filters().page);

  // Available options for filters
  private readonly availableApplicationsResource = rxResource({
    stream: () => this.applicationService.list(1, -1),
  });
  readonly availableApplications = computed(() => this.availableApplicationsResource.value()?.data ?? []);
  readonly applicationOptions = computed(() =>
    (this.availableApplications() ?? []).map(app => ({
      value: app.id,
      label: app.name,
    })),
  );

  readonly isLoadingSubscriptions = computed(() => this.subscriptionsResource.isLoading());

  readonly subscriptionsResource = rxResource({
    params: () => this.filters(),
    stream: ({ params }) =>
      this.subscriptionService.list({
        ...params,
        referenceTypes: params.referenceTypes ?? SubscriptionsComponent.ALL_REFERENCE_TYPES,
      }),
  });

  readonly totalElements = computed(() => {
    const response = this.subscriptionsResource.value();
    return response?.metadata?.['paginateMetaData']?.totalElements ?? response?.data?.length ?? 0;
  });

  readonly subscriptionCountLabel = computed(() => {
    const count = this.totalElements();
    return count === 1
      ? $localize`:@@subscriptionsSingleResult:1 subscription`
      : $localize`:@@subscriptionsResultCount:${count}:count: subscriptions`;
  });

  readonly isFiltered = computed(() => {
    const { query, referenceTypes, applicationIds, statuses } = this.filters();
    return query.length > 0 || (referenceTypes?.length ?? 0) > 0 || (applicationIds?.length ?? 0) > 0 || (statuses?.length ?? 0) > 0;
  });

  readonly hasSubscriptions = computed(() => {
    const response = this.subscriptionsResource.value();
    return (response?.data?.length ?? 0) > 0 || this.totalElements() > 0;
  });

  readonly shouldShowSubscriptionsContent = computed(
    () => this.isLoadingSubscriptions() || !!this.subscriptionsResource.error() || this.hasSubscriptions() || this.isFiltered(),
  );

  readonly rows = computed<SubscriptionTableRow[]>(() => {
    const response = this.subscriptionsResource.value();
    if (!response?.data) {
      return [];
    }

    return response.data.map(subscription => this.mapSubscriptionRow(subscription, response.metadata));
  });

  constructor() {
    this.breadcrumbService.set([subscriptionListBreadcrumb()]);
    this.setupUrlSync();
  }

  retrieveMetadataName(id: string, metadata?: SubscriptionMetadata): string {
    return metadata?.[id]?.name ?? id;
  }

  onSearchTermChange(query: string): void {
    const normalizedQuery = query.trim();
    if (normalizedQuery !== this.filters().query) {
      this.updateQueryParams({ query: normalizedQuery || null, page: 1 }, true);
    }
  }

  onPageChange(page: number): void {
    this.updateQueryParams({ page });
  }

  onPageSizeChange(size: number): void {
    this.updateQueryParams({ size, page: 1 });
  }

  clearFilters(): void {
    this.updateQueryParams({
      query: null,
      referenceTypes: null,
      applicationIds: null,
      statuses: null,
      page: 1,
    });
  }

  private setupUrlSync() {
    effect(() => {
      this.applyFiltersToForm(this.filters());
    });

    merge(this.typeFilter.valueChanges, this.applicationFilter.valueChanges, this.statusFilter.valueChanges)
      .pipe(debounceTime(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.syncUrlToForm());
  }

  private applyFiltersToForm(filters: SubscriptionFilters) {
    this.typeFilter.setValue(filters.referenceTypes ?? [], { emitEvent: false });
    this.applicationFilter.setValue(filters.applicationIds ?? null, { emitEvent: false });
    this.statusFilter.setValue(filters.statuses ?? [], { emitEvent: false });
  }

  private syncUrlToForm() {
    const currentFilters = this.filters();
    const referenceTypes = this.typeFilter.value ?? [];
    const applicationIds = this.applicationFilter.value ?? [];
    const statuses = this.statusFilter.value ?? [];

    const nextFilters: SubscriptionFilters = {
      query: currentFilters.query,
      referenceTypes: referenceTypes.length ? referenceTypes : null,
      applicationIds: applicationIds.length ? applicationIds : undefined,
      statuses: statuses.length ? statuses : null,
      page: 1,
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
    const query = typeof params['query'] === 'string' ? params['query'].trim() : '';
    const referenceTypes = this.parseReferenceTypeParam(params['referenceTypes']);
    const applicationIds = parseArrayParam(params['applicationIds']);
    const statuses = this.parseStatusParam(params['statuses']);

    return {
      query,
      referenceTypes: referenceTypes.length ? referenceTypes : null,
      applicationIds: applicationIds.length ? applicationIds : undefined,
      statuses: statuses.length ? statuses : null,
      page: parsePageParam(params['page'], SubscriptionsComponent.DEFAULT_PAGE),
      size: parseSizeParam(params['size'], SubscriptionsComponent.DEFAULT_PAGE_SIZE, SubscriptionsComponent.MAX_PAGE_SIZE),
    };
  }

  private toRouterQueryParams(filters: SubscriptionFilters): Record<string, unknown> {
    return {
      query: filters.query || null,
      referenceTypes: filters.referenceTypes ?? null,
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

  private parseReferenceTypeParam(param: unknown): SubscriptionReferenceType[] {
    const values = parseArrayParam(param);
    return values.filter((value): value is SubscriptionReferenceType =>
      SubscriptionsComponent.ALL_REFERENCE_TYPES.includes(value as SubscriptionReferenceType),
    );
  }

  private mapSubscriptionRow(subscription: Subscription, metadata?: SubscriptionMetadata): SubscriptionTableRow {
    return {
      id: subscription.id,
      targetName: this.resolveTargetName(subscription, metadata),
      targetType: this.resolveTargetTypeLabel(subscription.reference_type),
      plan: this.retrieveMetadataName(subscription.plan, metadata),
      application: this.retrieveMetadataName(subscription.application, metadata),
      startAt: subscription.start_at ?? null,
      endAt: subscription.end_at ?? null,
      status: toTitleCase(subscription.status),
    };
  }

  private resolveTargetName(subscription: Subscription, metadata?: SubscriptionMetadata): string {
    const targetName = subscription.reference_id ? metadata?.[subscription.reference_id]?.name : undefined;
    if (targetName) {
      return targetName;
    }

    switch (subscription.reference_type) {
      case 'API':
        return $localize`:@@unavailableApiSubscriptionTarget:Unavailable API`;
      case 'API_PRODUCT':
        return $localize`:@@unavailableApiProductSubscriptionTarget:Unavailable API Product`;
      default:
        return $localize`:@@unavailableSubscriptionTarget:Unavailable subscription target`;
    }
  }

  private resolveTargetTypeLabel(referenceType?: SubscriptionReferenceType): string {
    switch (referenceType) {
      case 'API':
        return $localize`:@@subscriptionTypeApi:API`;
      case 'API_PRODUCT':
        return $localize`:@@subscriptionTypeApiProduct:API Product`;
      default:
        return $localize`:@@subscriptionTypeUnknown:Unknown`;
    }
  }
}
