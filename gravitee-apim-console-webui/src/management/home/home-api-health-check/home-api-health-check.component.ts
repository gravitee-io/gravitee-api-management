/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { get, has, isEmpty, isEqual, isNumber } from 'lodash';
import { BehaviorSubject, combineLatest, merge, Observable, of, Subject } from 'rxjs';
import {
  delay,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  mergeMap,
  scan,
  startWith,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { HealthAvailabilityTimeFrameOption } from './health-availability-time-frame/health-availability-time-frame.component';

import { Api, ApiMetrics, ApiOrigin, ApiState } from '../../../entities/api';
import { PagedResult } from '../../../entities/pagedResult';
import { ApiService } from '../../../services-ngx/api.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioQuickTimeRangeComponent } from '../components/gio-quick-time-range/gio-quick-time-range.component';

export type ApisTableDS = {
  id: string;
  name: string;
  version: string;
  contextPath: string;
  tags: string;
  owner: string;
  ownerEmail: string;
  picture: string;
  state: ApiState;
  origin: ApiOrigin;
  availability$: Observable<
    | {
        type: 'not-configured';
      }
    | {
        type: 'no-data';
      }
    | {
        type: 'configured';
        healthCheckAvailability: number;
        healthAvailabilityTimeFrame: HealthAvailabilityTimeFrameOption;
      }
  >;
};

@Component({
  selector: 'home-api-health-check',
  templateUrl: './home-api-health-check.component.html',
  styleUrls: ['./home-api-health-check.component.scss'],
  standalone: false,
})
export class HomeApiHealthCheckComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'states', 'availability', 'actions'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS[] = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  isLoadingData = true;
  timeFrameControl = new UntypedFormControl('1m', Validators.required);

  allApisHCStatus: { inError: number; inWarning: number; isLoading: boolean };

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private refreshAvailability$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiService,
  ) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        debounceTime(200),
        distinctUntilChanged(isEqual),
        tap(({ pagination, searchTerm, sort }) => {
          // Change url params
          this.router.navigate(['.'], {
            relativeTo: this.activatedRoute,
            queryParams: {
              q: searchTerm,
              page: pagination.index,
              size: pagination.size,
              order: toOrder(sort),
            },
          });
        }),
        switchMap(({ pagination, searchTerm, sort }) =>
          this.apiService
            .list(searchTerm, toOrder(sort), pagination.index, pagination.size)
            .pipe(catchError(() => of(new PagedResult<Api>()))),
        ),
        tap((apisPage) => {
          this.apisTableDS = this.toApisTableDS(apisPage);
          this.apisTableDSUnpaginatedLength = apisPage.page.total_elements;
          this.isLoadingData = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.initFilters();

    // On timeFrame change restart all HC check
    this.timeFrameControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.checkAllApisHCStatus());

    this.checkAllApisHCStatus();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  onViewHealthCheckClicked(api: ApisTableDS) {
    this.router.navigate(['../../', 'apis', api.id, 'v2', 'healthcheck-dashboard'], {
      relativeTo: this.activatedRoute,
    });
  }

  onRefreshClicked() {
    this.refreshAvailability$.next();
    this.checkAllApisHCStatus();
  }

  onOnlyHCConfigured() {
    this.filters = { ...this.filters, searchTerm: 'has_health_check:true' };
    this.filters$.next(this.filters);
  }

  checkAllApisHCStatus() {
    const loadPage$ = new BehaviorSubject(1);
    this.allApisHCStatus = { inError: 0, inWarning: 0, isLoading: true };

    loadPage$
      .pipe(
        mergeMap((page) => this.apiService.list('has_health_check:true', undefined, page, 100)),
        delay(100),
        map((apisResult) => {
          const hasNextPage = apisResult.page.total_pages > apisResult.page.current;
          if (hasNextPage) {
            loadPage$.next(apisResult.page.current + 1);
          }

          return {
            apis: apisResult.data,
            hasNextPage,
          };
        }),
        mergeMap(({ apis, hasNextPage }) => {
          const getApisHealth: Observable<{ apiMetrics?: ApiMetrics; isLastApiHealth: boolean }>[] = apis
            .filter((a) => a.healthcheck_enabled)
            .map((api, index, array) =>
              this.apiService.apiHealth(api.id, 'availability').pipe(
                map((apiMetrics) => ({ apiMetrics, isLastApiHealth: !hasNextPage && array.length - 1 <= index })),
                delay(100),
              ),
            );
          const emptyDefaultReturn = { apiMetrics: undefined, isLastApiHealth: !hasNextPage };

          return isEmpty(getApisHealth) ? of(emptyDefaultReturn) : merge(...getApisHealth);
        }),
        scan(
          (acc, curr: { apiMetrics?: ApiMetrics; isLastApiHealth: boolean }) => {
            const availability = get(curr.apiMetrics, `global.${this.timeFrameControl.value}`);

            if (availability && availability >= 0) {
              if (availability <= 80) {
                acc.inError++;
              } else if (availability <= 95) {
                acc.inWarning++;
              }
            }
            acc.isLoading = !curr.isLastApiHealth;
            return acc;
          },
          { inError: 0, inWarning: 0, isLoading: true },
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (allApisHCStatus) => {
          this.allApisHCStatus = allApisHCStatus;
        },
      });
  }

  private initFilters() {
    const initialSearchValue = this.activatedRoute.snapshot.queryParams?.q ?? this.filters.searchTerm;
    const initialPageNumber = this.activatedRoute.snapshot.queryParams?.page
      ? Number(this.activatedRoute.snapshot.queryParams.page)
      : this.filters.pagination.index;
    const initialPageSize = this.activatedRoute.snapshot.queryParams?.size
      ? Number(this.activatedRoute.snapshot.queryParams.size)
      : this.filters.pagination.size;
    const initialSort = toSort(this.activatedRoute.snapshot.queryParams?.order, this.filters.sort);
    this.filters = {
      searchTerm: initialSearchValue,
      sort: initialSort,
      pagination: {
        ...this.filters$.value.pagination,
        index: initialPageNumber,
        size: initialPageSize,
      },
    };
    this.filters$.next(this.filters);
  }

  private toApisTableDS(api: PagedResult<Api>): ApisTableDS[] {
    if (api.page.total_elements === 0) {
      return [];
    }

    return api.data.map(
      (api) =>
        ({
          id: api.id,
          name: api.name,
          version: api.version,
          contextPath: api.context_path,
          tags: api.tags.join(', '),
          owner: api?.owner?.displayName,
          ownerEmail: api?.owner?.email,
          picture: api.picture_url,
          state: api.state,
          lifecycleState: api.lifecycle_state,
          workflowBadge: this.getWorkflowBadge(api),
          healthcheck_enabled: api.healthcheck_enabled,
          origin: api.definition_context.origin,
          availability$: this.getAvailability$(api),
        }) as ApisTableDS,
    );
  }

  private getWorkflowBadge(api) {
    const state = api.lifecycle_state === 'DEPRECATED' ? api.lifecycle_state : api.workflow_state;
    const toReadableState = {
      DEPRECATED: { text: 'Deprecated', class: 'gio-badge-error' },
      DRAFT: { text: 'Draft', class: 'gio-badge-primary' },
      IN_REVIEW: { text: 'In Review', class: 'gio-badge-error' },
      REQUEST_FOR_CHANGES: { text: 'Need changes', class: 'gio-badge-error' },
    };
    return toReadableState[state];
  }

  private getAvailability$(api: Api): ApisTableDS['availability$'] {
    if (!api.healthcheck_enabled) {
      return of({
        type: 'not-configured' as const,
      });
    }

    return this.refreshAvailability$.pipe(
      switchMap(() =>
        combineLatest([
          this.apiService.apiHealth(api.id, 'availability'),
          this.timeFrameControl.valueChanges.pipe(startWith(this.timeFrameControl.value)) as Observable<string>,
        ]),
      ),
      switchMap(([healthAvailability, timeFrame]) => {
        const currentTimeFrameRangesParams = GioQuickTimeRangeComponent.getTimeFrameRangesParams(timeFrame);
        return combineLatest([
          of(healthAvailability),
          of(timeFrame),
          this.apiService.apiHealthAverage(api.id, {
            from: currentTimeFrameRangesParams.from,
            to: currentTimeFrameRangesParams.to,
            interval: currentTimeFrameRangesParams.interval,
            type: 'AVAILABILITY',
          }),
        ]);
      }),
      map(([healthAvailability, timeFrame, healthAvailabilityTimeFrame]) => {
        const healthCheckAvailability = get(healthAvailability, `global.${timeFrame}`);
        if (
          !healthCheckAvailability ||
          !isNumber(healthCheckAvailability) ||
          !healthAvailabilityTimeFrame ||
          !has(healthAvailabilityTimeFrame, 'values[0].buckets[0].data')
        ) {
          return {
            type: 'no-data' as const,
          };
        }

        return {
          type: 'configured' as const,
          healthCheckAvailability: healthCheckAvailability / 100,
          healthAvailabilityTimeFrame: {
            data: healthAvailabilityTimeFrame.values[0].buckets[0].data,
            timestamp: {
              start: healthAvailabilityTimeFrame.timestamp.from,
              interval: healthAvailabilityTimeFrame.timestamp.interval,
            },
          },
        };
      }),
    );
  }
}
