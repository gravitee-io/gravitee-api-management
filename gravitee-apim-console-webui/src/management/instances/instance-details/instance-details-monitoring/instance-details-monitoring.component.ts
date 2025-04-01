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
import { duration } from 'moment/moment';
import { isNil, isNumber, round } from 'lodash';
import { delay, mergeMap, repeat, switchMap, takeUntil, tap } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { MonitoringData } from '../../../../entities/instance/monitoringData';
import { Instance } from '../../../../entities/instance/instance';
import { InstanceService } from '../../../../services-ngx/instance.service';

@Component({
  selector: 'instance-monitoring',
  templateUrl: './instance-details-monitoring.component.html',
  styleUrls: ['./instance-details-monitoring.component.scss'],
  standalone: false,
})
export class InstanceDetailsMonitoringComponent implements OnInit, OnDestroy {
  public instance: Instance;
  public instanceStarted: boolean;
  public monitoringData: MonitoringData;

  private monitoringPolling;
  private unsubscribe$ = new Subject<void>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly instanceService: InstanceService,
  ) {}

  ngOnInit(): void {
    this.instanceService
      .get(this.activatedRoute.snapshot.params.instanceId)
      .pipe(
        tap((instance) => {
          this.instance = instance;
          this.instanceStarted = instance.state === 'STARTED';
        }),
        switchMap((instance) => {
          if (instance.state !== 'STARTED') {
            return of();
          }
          return of({}).pipe(
            mergeMap((_) => this.instanceService.getMonitoringData(this.activatedRoute.snapshot.params.instanceId, this.instance.id)),
            tap((monitoringData) => (this.monitoringData = monitoringData)),
            delay(5000),
            repeat(),
          );
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    if (this.monitoringPolling) {
      this.monitoringPolling.unsubscribe();
    }
  }

  // Preserve original property order for the 'keyvalue' pipe
  originalOrder(): number {
    return 0;
  }

  humanizeDuration(timeInMillis) {
    return duration(-timeInMillis).humanize(true);
  }

  humanizeSize(bytes: number, precision?: number | undefined | null): string {
    if (!isNumber(bytes) || !isFinite(bytes)) {
      return '-';
    }
    if (isNil(precision)) {
      precision = 1;
    }
    const units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'];
    const mostRelevantUnitIndex = bytes === 0 ? 0 : Math.floor(Math.log(bytes) / Math.log(1024));
    const valueInSelectedUnit = bytes / Math.pow(1024, Math.floor(mostRelevantUnitIndex));
    return `${valueInSelectedUnit.toFixed(precision)} ${units[mostRelevantUnitIndex]}`;
  }

  ratio(value: number, value2: number): number {
    return value2 === 0 ? undefined : round((value / value2) * 100);
  }

  ratioLabel(value: number, value2: number): string {
    const ratio = this.ratio(value, value2);
    return ratio !== undefined ? `${ratio}%` : '-%';
  }
}
