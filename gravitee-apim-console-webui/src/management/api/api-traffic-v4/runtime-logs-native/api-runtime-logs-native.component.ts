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

import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { EMPTY, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { isEqual } from 'lodash';
import moment from 'moment';

import { NATIVE_CONNECTION_STATUSES } from './api-runtime-logs-native.models';
import { ApiRuntimeLogsNativeListComponent } from './components/api-runtime-logs-native-list/api-runtime-logs-native-list.component';

import { ApiNativeLogsV2Service } from '../../../../services-ngx/api-native-logs-v2.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import {
  ApiV4,
  NativeApiLog,
  NativeApiLogsParam,
  NativeApiLogsResponse,
  NativeConnectionStatus,
} from '../../../../entities/management-api-v2';
import { GioTableWrapperPagination } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Application } from '../../../../entities/application/Application';
import { customTimeFrames, timeFrameRangesParams, timeFrames } from '../../../../shared/utils/timeFrameRanges';
import {
  GioSelectSearchComponent,
  ResultsLoaderInput,
  ResultsLoaderOutput,
  SelectOption,
} from '../../../../shared/components/gio-select-search/gio-select-search.component';
import { GioTimeframeComponent, TimeframeValue } from '../../../../shared/components/gio-timeframe/gio-timeframe.component';
import { ActionButtonsDirective } from '../../api-navigation/api-navigation-header/action-buttons.directive';
import { ApiNavigationModule } from '../../api-navigation/api-navigation.module';

const CUSTOM_PERIOD = 'custom';

interface NativeLogFiltersForm {
  timeframe: FormControl<TimeframeValue | null>;
  applicationIds: FormControl<string[]>;
  planIds: FormControl<string[]>;
  connectionStatuses: FormControl<NativeConnectionStatus[]>;
}

interface SearchInput {
  page: number;
  perPage: number;
}

@Component({
  selector: 'api-runtime-logs-native',
  templateUrl: './api-runtime-logs-native.component.html',
  styleUrls: ['./api-runtime-logs-native.component.scss'],
  standalone: true,
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    GioBannerModule,
    GioTimeframeComponent,
    GioSelectSearchComponent,
    ApiRuntimeLogsNativeListComponent,
    ActionButtonsDirective,
    ApiNavigationModule,
  ],
})
export class ApiRuntimeLogsNativeComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiService = inject(ApiV2Service);
  private readonly logsService = inject(ApiNativeLogsV2Service);
  private readonly planService = inject(ApiPlanV2Service);
  private readonly applicationService = inject(ApplicationService);

  protected readonly connectionStatusOptions: SelectOption[] = NATIVE_CONNECTION_STATUSES.map(s => ({ value: s.value, label: s.label }));
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  private readonly api$ = this.apiService
    .get(this.activatedRoute.snapshot.params.apiId)
    .pipe(shareReplay({ bufferSize: 1, refCount: true }));
  protected readonly isReportingDisabled$ = this.api$.pipe(map((api: ApiV4) => api.analytics?.reporterMetricsEnabled === false));

  protected readonly plans$ = this.planService
    .list(this.activatedRoute.snapshot.params.apiId, undefined, undefined, undefined, undefined, 1, 9999)
    .pipe(
      map(response => response.data ?? []),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  protected readonly planOptions$: Observable<SelectOption[]> = this.plans$.pipe(
    map(plans => plans.map(p => ({ value: p.id, label: p.name }))),
  );

  protected readonly logs$ = new ReplaySubject<NativeApiLogsResponse>(1);
  protected readonly rowApplications$ = new ReplaySubject<Application[]>(1);
  protected readonly loading = signal(true);

  private readonly searchTrigger$ = new Subject<SearchInput>();

  protected readonly applicationsResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    const perPage = 25;
    return this.applicationService.list(undefined, input.searchTerm || undefined, undefined, input.page, perPage).pipe(
      map(paged => {
        const data: SelectOption[] = (paged.data ?? []).map(app => ({ value: app.id, label: app.name }));
        const total = paged.page?.total_elements;
        const hasNextPage = total != null ? input.page * perPage < total : data.length === perPage;
        return { data, hasNextPage };
      }),
    );
  };

  protected readonly form = new FormGroup<NativeLogFiltersForm>({
    timeframe: new FormControl<TimeframeValue | null>({ period: '', from: null, to: null }),
    applicationIds: new FormControl<string[]>([], { nonNullable: true }),
    planIds: new FormControl<string[]>([], { nonNullable: true }),
    connectionStatuses: new FormControl<NativeConnectionStatus[]>([], { nonNullable: true }),
  });

  ngOnInit(): void {
    this.hydrateFormFromQueryParams();

    // Each trigger runs as its own inner observable with catchError + finalize so a failed
    // request never terminates the outer Subject. Without inner isolation, one HTTP error
    // would silently kill all subsequent filter / pagination / refresh interactions.
    this.searchTrigger$
      .pipe(
        switchMap(({ page, perPage }) => {
          this.loading.set(true);
          const params = this.toQueryParams(page, perPage);
          return this.logsService.searchConnectionLogs(this.activatedRoute.snapshot.params.apiId, params).pipe(
            switchMap(response => this.resolveRowApplications(response)),
            tap(response => {
              this.logs$.next(response);
              this.router.navigate(['.'], { relativeTo: this.activatedRoute, queryParams: params, queryParamsHandling: '' });
            }),
            catchError(() => EMPTY),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.searchTrigger$.next({ page: this.currentPage(), perPage: this.currentPerPage() });

    this.form.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      // Custom timeframe needs explicit Apply via gio-timeframe (apply event); preset emits here.
      const tf = this.form.controls.timeframe.value;
      if (tf?.period === CUSTOM_PERIOD) return;
      this.searchTrigger$.next({ page: 1, perPage: this.currentPerPage() });
    });
  }

  protected paginationUpdated(event: GioTableWrapperPagination) {
    this.searchTrigger$.next({ page: event.index, perPage: event.size });
  }

  protected refresh() {
    this.searchTrigger$.next({ page: this.currentPage(), perPage: this.currentPerPage() });
  }

  protected applyCustomTimeframe() {
    this.searchTrigger$.next({ page: 1, perPage: this.currentPerPage() });
  }

  private currentPage(): number {
    return +(this.activatedRoute.snapshot.queryParams.page ?? 1);
  }

  private currentPerPage(): number {
    return +(this.activatedRoute.snapshot.queryParams.perPage ?? 10);
  }

  // Returns the original response so the search pipeline can flow through a single switchMap.
  // Name resolution is non-essential — degrade gracefully on failure so log rows still render with raw IDs.
  private resolveRowApplications(response: NativeApiLogsResponse): Observable<NativeApiLogsResponse> {
    const ids = uniqueIds(response.data);
    if (ids.length === 0) {
      this.rowApplications$.next([]);
      return of(response);
    }
    return this.applicationService.findByIds(ids, 1, ids.length).pipe(
      tap(paged => this.rowApplications$.next(paged.data ?? [])),
      map(() => response),
      catchError(() => {
        this.rowApplications$.next([]);
        return of(response);
      }),
    );
  }

  private toQueryParams(page: number, perPage: number): NativeApiLogsParam & { period?: string } {
    const value = this.form.getRawValue();
    const tf = value.timeframe;
    const { from, to } = !tf?.period
      ? {}
      : tf.period === CUSTOM_PERIOD
        ? { from: tf.from?.valueOf(), to: tf.to?.valueOf() }
        : timeFrameRangesParams(tf.period);
    return {
      page,
      perPage,
      period: tf?.period || undefined,
      from,
      to,
      applicationIds: value.applicationIds?.length ? value.applicationIds.join(',') : undefined,
      planIds: value.planIds?.length ? value.planIds.join(',') : undefined,
      connectionStatuses: value.connectionStatuses?.length ? value.connectionStatuses.join(',') : undefined,
    };
  }

  private hydrateFormFromQueryParams() {
    const qp = this.activatedRoute.snapshot.queryParams;
    const period = qp?.period ?? '';
    const fromQp = qp?.from ? moment(Number(qp.from)) : null;
    const toQp = qp?.to ? moment(Number(qp.to)) : null;
    this.form.patchValue(
      {
        timeframe: { period, from: period === CUSTOM_PERIOD ? fromQp : null, to: period === CUSTOM_PERIOD ? toQp : null },
        applicationIds: qp?.applicationIds ? qp.applicationIds.split(',') : [],
        planIds: qp?.planIds ? qp.planIds.split(',') : [],
        connectionStatuses: qp?.connectionStatuses ? (qp.connectionStatuses.split(',') as NativeConnectionStatus[]) : [],
      },
      { emitEvent: false },
    );
  }
}

function uniqueIds(rows: NativeApiLog[] | undefined): string[] {
  if (!rows?.length) return [];
  return Array.from(new Set(rows.map(r => r.applicationId).filter((id): id is string => !!id)));
}
