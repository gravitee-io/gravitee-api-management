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
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, Subject } from 'rxjs';
import { UntypedFormControl, Validators } from '@angular/forms';
import { switchMap, takeUntil, tap } from 'rxjs/operators';

import { AnalyticsService } from '../../../services-ngx/analytics.service';
import { GioQuickTimeRangeComponent, TimeRangeParams } from '../components/gio-quick-time-range/gio-quick-time-range.component';
import { TopApisData } from '../components/gio-top-apis-table/gio-top-apis-table.component';
import { RequestStats } from '../components/gio-request-stats/gio-request-stats.component';
import { ApiResponseStatusData } from '../components/gio-api-response-status/gio-api-response-status.component';
import { ApiStateData } from '../components/gio-api-state/gio-api-state.component';
import { ApiLifecycleStateData } from '../components/gio-api-lifecycle-state/gio-api-lifecycle-state.component';

@Component({
  selector: 'home-overview',
  templateUrl: './home-overview.component.html',
  styleUrls: ['./home-overview.component.scss'],
})
export class HomeOverviewComponent implements OnInit, OnDestroy {
  loading = false;

  private fetchAnalyticsRequest$ = new Subject<TimeRangeParams>();
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  constructor(
    private readonly statsService: AnalyticsService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {}

  topApis?: TopApisData;
  requestStats?: RequestStats;
  apiResponseStatus?: ApiResponseStatusData;
  apiState?: ApiStateData;
  apiLifecycleState?: ApiLifecycleStateData;
  apiNb?: number;
  applicationNb?: number;

  timeRangeControl = new UntypedFormControl('1m', Validators.required);
  timeRangeParams: TimeRangeParams;

  ngOnInit(): void {
    // Quick Summary
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => {
          this.apiNb = undefined;
          this.applicationNb = undefined;
        }),
        switchMap((val) =>
          forkJoin([
            this.statsService.getCount({
              field: 'application',
              interval: val.interval,
              from: val.from,
              to: val.to,
            }),
            this.statsService.getCount({
              field: 'api',
              interval: val.interval,
              from: val.from,
              to: val.to,
            }),
          ]),
        ),
        tap(([applicationNb, apiNb]) => {
          this.apiNb = apiNb.count;
          this.applicationNb = applicationNb.count;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // API lifecycle state
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => (this.apiLifecycleState = undefined)),
        switchMap((val) =>
          this.statsService.getGroupBy({
            field: 'lifecycle_state',
            interval: val.interval,
            from: val.from,
            to: val.to,
          }),
        ),
        tap((data) => (this.apiLifecycleState = data)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // API state
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => (this.apiState = undefined)),
        switchMap((val) =>
          this.statsService.getGroupBy({
            field: 'state',
            interval: val.interval,
            from: val.from,
            to: val.to,
          }),
        ),
        tap((data) => (this.apiState = data)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // API response status
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => (this.apiResponseStatus = undefined)),
        switchMap((val) =>
          this.statsService.getGroupBy({
            field: 'status',
            interval: val.interval,
            from: val.from,
            to: val.to,
            ranges: '100:199;200:299;300:399;400:499;500:599',
          }),
        ),
        tap((data) => (this.apiResponseStatus = data)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // Top APIs
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => (this.topApis = undefined)),
        switchMap((val) => this.statsService.getGroupBy({ field: 'api', interval: val.interval, from: val.from, to: val.to })),
        tap((data) => (this.topApis = data)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // Request Stats
    this.fetchAnalyticsRequest$
      .pipe(
        tap(() => (this.requestStats = undefined)),
        switchMap((val) => this.statsService.getStats({ field: 'response-time', interval: val.interval, from: val.from, to: val.to })),
        tap((data) => (this.requestStats = data)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // Fetch Analytics when timeRange change
    this.timeRangeControl.valueChanges
      .pipe(
        tap(() => this.fetchAnalyticsRequest()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.changeDetectorRef.markForCheck());

    // First fetch
    this.fetchAnalyticsRequest();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  fetchAnalyticsRequest() {
    const timeRange = this.timeRangeControl.value;
    this.timeRangeParams = GioQuickTimeRangeComponent.getTimeFrameRangesParams(timeRange);
    this.fetchAnalyticsRequest$.next(this.timeRangeParams);
  }
}
