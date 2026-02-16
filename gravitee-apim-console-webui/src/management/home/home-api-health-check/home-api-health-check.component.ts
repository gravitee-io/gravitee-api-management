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
import { Component, OnDestroy, OnInit, signal, WritableSignal } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { get, has, isEmpty, isEqual, isNumber } from 'lodash';
import { BehaviorSubject, combineLatest, merge, Observable, of, Subject } from 'rxjs';
import {
  catchError,
  debounceTime,
  delay,
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

import { ApiMetrics } from '../../../entities/api';
import { ApiService } from '../../../services-ngx/api.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioQuickTimeRangeComponent } from '../components/gio-quick-time-range/gio-quick-time-range.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { apiSortByParamFromString, ApisResponse, ApiState, ApiV2, ApiV4, GenericApi, Origin } from '../../../entities/management-api-v2';

type ApisTableDS = {
  id: string;
  definitionVersion: GenericApi['definitionVersion'];
  name: string;
  version: string;
  tags: string;
  owner: string;
  ownerEmail: string;
  picture: string;
  state: ApiState;
  origin: Origin;
  lifecycleState: GenericApi['lifecycleState'];
  workflowBadge: { text: string; class: string };
  healthcheck_enabled: boolean;
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

type HCStatus = { inError: number; inWarning: number; isLoading: boolean };

@Component({
  selector: 'home-api-health-check',
  templateUrl: './home-api-health-check.component.html',
  styleUrls: ['./home-api-health-check.component.scss'],
  standalone: false,
})
export class HomeApiHealthCheckComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'states', 'availability', 'actions'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: WritableSignal<ApisTableDS[]> = signal([]);
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  isLoadingData = true;
  timeFrameControl = new UntypedFormControl('1m', Validators.required);

  allApisHCStatus: WritableSignal<HCStatus | null> = signal(null);

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private refreshAvailability$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiService,
    private readonly apiServiceV2: ApiV2Service,
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
          this.apiServiceV2
            .search(
              { query: searchTerm, definitionVersions: ['V2', 'V4'] },
              apiSortByParamFromString(sort),
              pagination.index,
              pagination.size,
            )
            .pipe(catchError(() => of({ data: [] } as ApisResponse))),
        ),
        tap(apisPage => {
          this.apisTableDS.set(this.toApisTableDS(apisPage));
          this.apisTableDSUnpaginatedLength = apisPage?.pagination?.totalCount ?? 0;
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

  path2dashboard(api: ApisTableDS): string[] | undefined {
    if (!api.healthcheck_enabled) {
      return undefined;
    }
    switch (api.definitionVersion) {
      case 'V2':
        return ['../../', 'apis', api.id, 'v2', 'healthcheck-dashboard'];
      case 'V4':
        return ['../../', 'apis', api.id, 'v4', 'health-check-dashboard'];
      default:
        return undefined;
    }
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
    this.allApisHCStatus.set({ inError: 0, inWarning: 0, isLoading: true });

    loadPage$
      .pipe(
        mergeMap((page: number) => this.apiServiceV2.search({ query: 'has_health_check:true' }, null, page, 100)),
        delay(100),
        map((apisResult: ApisResponse) => {
          const hasNextPage = (apisResult?.pagination?.pageCount ?? 0) > (apisResult?.pagination?.page ?? 0);
          if (hasNextPage) {
            loadPage$.next(apisResult?.pagination?.page + 1);
          }

          return {
            apis: apisResult.data,
            hasNextPage,
          };
        }),
        mergeMap(({ apis, hasNextPage }) => {
          const getApisHealth: Observable<{ apiMetrics?: ApiMetrics; isLastApiHealth: boolean }>[] = apis.map((api, index, array) =>
            this.apiService.apiHealth(api.id, 'availability').pipe(
              map(apiMetrics => ({ apiMetrics, isLastApiHealth: !hasNextPage && array.length - 1 <= index })),
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
        next: allApisHCStatus => {
          this.allApisHCStatus.set(allApisHCStatus);
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

  private toApisTableDS(apiR: ApisResponse): ApisTableDS[] {
    return apiR.data.flatMap(api => {
      switch (api.definitionVersion) {
        case 'FEDERATED':
          return [];
        case 'V1':
          return [];
        case 'V2':
          return [this.v2toApisTableDS(api)];
        case 'V4':
          return [this.v4toApisTableDS(api)];
      }
    });
  }

  private v2toApisTableDS(api: ApiV2): ApisTableDS {
    return {
      id: api.id,
      name: api.name,
      definitionVersion: api.definitionVersion,
      version: api.apiVersion,
      tags: api.tags.join(', '),
      owner: api?.primaryOwner.displayName,
      ownerEmail: api?.primaryOwner.email,
      state: api.state,
      lifecycleState: api.lifecycleState,
      workflowBadge: this.getWorkflowBadge(api),
      availability$: this.getAvailability$(api.id, this.healthcheckEnabledApiV2(api)),
      picture: api._links.pictureUrl,
      healthcheck_enabled: this.healthcheckEnabledApiV2(api),
      origin: api.originContext.origin,
    } satisfies ApisTableDS;
  }

  private v4toApisTableDS(api: ApiV4): ApisTableDS {
    return {
      id: api.id,
      name: api.name,
      definitionVersion: api.definitionVersion,
      version: api.apiVersion,
      tags: api.tags?.join(', ') ?? '',
      owner: api.primaryOwner.displayName,
      ownerEmail: api.primaryOwner.email,
      state: api.state,
      lifecycleState: api.lifecycleState,
      workflowBadge: this.getWorkflowBadge(api),
      picture: api._links.pictureUrl,
      healthcheck_enabled: this.healthcheckEnabled(api),
      origin: api.originContext.origin,
      availability$: this.getAvailability$(api.id, this.healthcheckEnabled(api)),
    } satisfies ApisTableDS;
  }

  private getWorkflowBadge(api: ApiV2 | ApiV4): ApisTableDS['workflowBadge'] {
    const toReadableState = {
      DEPRECATED: { text: 'Deprecated', class: 'gio-badge-error' },
      DRAFT: { text: 'Draft', class: 'gio-badge-primary' },
      IN_REVIEW: { text: 'In Review', class: 'gio-badge-error' },
      REQUEST_FOR_CHANGES: { text: 'Need changes', class: 'gio-badge-error' },
    };
    return toReadableState?.[api.lifecycleState] ?? null;
  }

  private healthcheckEnabled(api: ApiV4): boolean {
    const enabledInEndpoints: boolean = api.endpointGroups?.some(endpointGroup =>
      endpointGroup?.endpoints?.some(endpoint => endpoint?.services?.healthCheck?.enabled),
    );
    const enabledInServices: boolean = api.endpointGroups?.some(endpointGroup => endpointGroup?.services?.healthCheck?.enabled);

    return enabledInServices || enabledInEndpoints;
  }

  private healthcheckEnabledApiV2(api: ApiV2): boolean {
    return api?.services?.healthCheck?.enabled ?? false;
  }

  private getAvailability$(apiId: string, enabled: boolean = true): ApisTableDS['availability$'] {
    if (!enabled) {
      return of({
        type: 'not-configured' as const,
      });
    }

    return this.refreshAvailability$.pipe(
      switchMap(() =>
        combineLatest([
          this.apiService.apiHealth(apiId, 'availability'),
          this.timeFrameControl.valueChanges.pipe(startWith(this.timeFrameControl.value)) as Observable<string>,
        ]),
      ),
      switchMap(([healthAvailability, timeFrame]) => {
        const currentTimeFrameRangesParams = GioQuickTimeRangeComponent.getTimeFrameRangesParams(timeFrame);
        return combineLatest([
          of(healthAvailability),
          of(timeFrame),
          this.apiService.apiHealthAverage(apiId, {
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
