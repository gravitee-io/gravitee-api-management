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
import { Component, DestroyRef, inject } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';

import { ApiProxyRequestLogOverviewComponent } from './components/api-proxy-request-log-overview/api-proxy-request-log-overview.component';
import { ApiProxyRequestMetricOverviewComponent } from './components/api-proxy-request-metric-overview/api-proxy-request-metric-overview.component';

import { ApiRuntimeLogsConnectionLogDetailsModule, ApiRuntimeLogsDetailsEmptyStateModule } from '../components';
import { ApiLogsV2Service } from '../../../../../../services-ngx/api-logs-v2.service';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'api-runtime-logs-proxy',
  templateUrl: './api-runtime-logs-proxy.component.html',
  styleUrls: ['./api-runtime-logs-proxy.component.scss'],
  imports: [
    RouterModule,
    MatButtonModule,
    MatCardModule,
    ApiRuntimeLogsConnectionLogDetailsModule,
    ApiRuntimeLogsDetailsEmptyStateModule,
    MatIconModule,
    MatExpansionModule,
    MatListModule,
    ApiProxyRequestLogOverviewComponent,
    ApiProxyRequestMetricOverviewComponent,
  ],
})
export class ApiRuntimeLogsProxyComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiLogsService = inject(ApiLogsV2Service);
  private readonly apiAnalyticsService = inject(ApiAnalyticsV2Service);
  private readonly matSnackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;
  private readonly requestId = this.activatedRoute.snapshot.params.requestId;

  public connectionLog = toSignal(
    this.apiLogsService.searchConnectionLogDetail(this.apiId, this.requestId).pipe(
      catchError((err) => {
        // normally 404 is intercepted by the HttpErrorInterceptor and displayed as a snack error, but on this page, it should be dismissed.
        if (err.status === 404) {
          this.matSnackBar.dismiss();
        }
        return of(undefined);
      }),
      takeUntilDestroyed(this.destroyRef),
    ),
  );

  public apiMetricsDetail = toSignal(this.apiAnalyticsService.getApiMetricsDetail(this.apiId, this.requestId));
}
