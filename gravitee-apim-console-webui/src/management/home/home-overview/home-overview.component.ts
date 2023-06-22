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
import { Observable, Subject } from 'rxjs';
import { FormControl, Validators } from '@angular/forms';
import { distinctUntilChanged, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';

import { AnalyticsStatsResponse } from '../../../entities/analytics/analyticsResponse';
import { AnalyticsService } from '../../../services-ngx/analytics.service';
import { GioQuickTimeRangeComponent, TimeRangeParams } from '../widgets/gio-quick-time-range/gio-quick-time-range.component';

@Component({
  selector: 'home-overview',
  template: require('./home-overview.component.html'),
  styles: [require('./home-overview.component.scss')],
})
export class HomeOverviewComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  loading = false;

  constructor(private readonly statsService: AnalyticsService) {}

  requestStats: AnalyticsStatsResponse;
  timeRangeControl = new FormControl('1m', Validators.required);

  ngOnInit(): void {
    this.timeRangeControl.valueChanges
      .pipe(
        startWith(this.timeRangeControl.value),
        distinctUntilChanged(),
        switchMap((timeRange) => this.getRequestStats(GioQuickTimeRangeComponent.getTimeFrameRangesParams(timeRange))),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  getRequestStats(val: TimeRangeParams): Observable<AnalyticsStatsResponse> {
    this.loading = true;
    return this.statsService.getStats({ field: 'response-time', interval: val.interval, from: val.from, to: val.to }).pipe(
      tap((data) => {
        this.loading = false;
        this.requestStats = data;
      }),
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  refresh() {
    const timeRange = this.timeRangeControl.value;
    if (timeRange.from && timeRange.to) this.getRequestStats(timeRange).subscribe();
  }
}
