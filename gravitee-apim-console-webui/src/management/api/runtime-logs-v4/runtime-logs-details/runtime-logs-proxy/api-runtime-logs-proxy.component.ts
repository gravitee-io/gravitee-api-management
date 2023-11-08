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
import { Component, Inject, OnDestroy } from '@angular/core';
import { StateParams } from '@uirouter/core';
import { catchError, takeUntil } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { StateService } from '@uirouter/angular';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiLogsV2Service } from '../../../../../services-ngx/api-logs-v2.service';

@Component({
  selector: 'api-runtime-logs-proxy',
  template: require('./api-runtime-logs-proxy.component.html'),
  styles: [require('./api-runtime-logs-proxy.component.scss')],
})
export class ApiRuntimeLogsProxyComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public connectionLog$ = this.apiLogsService.searchConnectionLogDetail(this.ajsStateParams.apiId, this.ajsStateParams.requestId).pipe(
    catchError(() => {
      return of(undefined);
    }),
    takeUntil(this.unsubscribe$),
  );

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiLogsService: ApiLogsV2Service,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  openLogsSettings() {
    return this.ajsState.go('management.apis.runtimeLogs-settings');
  }
}
