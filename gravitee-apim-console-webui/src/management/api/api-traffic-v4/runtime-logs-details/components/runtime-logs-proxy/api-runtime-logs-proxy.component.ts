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
import { Component, OnDestroy } from '@angular/core';
import { catchError, takeUntil } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiLogsV2Service } from '../../../../../../services-ngx/api-logs-v2.service';

@Component({
  selector: 'api-runtime-logs-proxy',
  templateUrl: './api-runtime-logs-proxy.component.html',
  styleUrls: ['./api-runtime-logs-proxy.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsProxyComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public connectionLog$ = this.apiLogsService
    .searchConnectionLogDetail(this.activatedRoute.snapshot.params.apiId, this.activatedRoute.snapshot.params.requestId)
    .pipe(
      catchError((err) => {
        // normally 404 is intercepted by the HttpErrorInterceptor and displayed as a snack error, but on this page, it should be dismissed.
        if (err.status === 404) {
          this.matSnackBar.dismiss();
        }
        return of(undefined);
      }),
      takeUntil(this.unsubscribe$),
    );

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly matSnackBar: MatSnackBar,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  openLogsSettings() {
    this.router.navigate(['../../runtime-logs-settings'], { relativeTo: this.activatedRoute });
  }
}
