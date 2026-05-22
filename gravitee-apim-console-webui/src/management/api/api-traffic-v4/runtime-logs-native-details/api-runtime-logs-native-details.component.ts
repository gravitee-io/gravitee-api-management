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
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Params, RouterLink } from '@angular/router';
import { AsyncPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  DEFAULT_NATIVE_LOGS_PERIOD,
  NATIVE_STATUS_META,
  isNativeConnectionErrored,
} from '../runtime-logs-native/api-runtime-logs-native.models';
import { ApiNativeLogsV2Service } from '../../../../services-ngx/api-native-logs-v2.service';
import { FormatDurationPipe } from '../../../../shared/pipes/format-duration.pipe';
import { NativeApiLog } from '../../../../entities/management-api-v2';
import { timeFrameRangesParams } from '../../../../shared/utils/timeFrameRanges';

type LogState = { kind: 'ok'; log: NativeApiLog } | { kind: 'not-found' } | { kind: 'load-failed' };

@Component({
  selector: 'api-runtime-logs-native-details',
  templateUrl: './api-runtime-logs-native-details.component.html',
  styleUrls: ['./api-runtime-logs-native-details.component.scss'],
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatCardModule, MatIconModule, GioBannerModule, GioIconsModule, FormatDurationPipe],
})
export class ApiRuntimeLogsNativeDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly logsService = inject(ApiNativeLogsV2Service);

  private readonly apiId = this.route.snapshot.params.apiId as string;
  protected readonly requestId = this.route.snapshot.params.requestId as string;
  private readonly range = resolveTimeWindow(this.route.snapshot.queryParams);

  protected readonly backQueryParams = this.route.snapshot.queryParams;

  protected readonly statusMeta = NATIVE_STATUS_META;

  protected readonly state$: Observable<LogState> = this.logsService
    .getConnectionLog(this.apiId, this.requestId, this.range.from, this.range.to)
    .pipe(
      map(log => ({ kind: 'ok', log }) as LogState),
      catchError((err: HttpErrorResponse) => of({ kind: err.status === 404 ? 'not-found' : 'load-failed' } as LogState)),
    );

  protected readonly isErrored = isNativeConnectionErrored;
}

function resolveTimeWindow(qp: Params): { from: number; to: number } {
  const fromQp = qp.from ? Number(qp.from) : NaN;
  const toQp = qp.to ? Number(qp.to) : NaN;
  if (Number.isFinite(fromQp) && Number.isFinite(toQp)) return { from: fromQp, to: toQp };
  const { from, to } = timeFrameRangesParams(DEFAULT_NATIVE_LOGS_PERIOD);
  return { from, to };
}
