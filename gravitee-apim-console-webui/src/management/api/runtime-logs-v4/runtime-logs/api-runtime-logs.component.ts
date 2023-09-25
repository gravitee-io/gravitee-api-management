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

import { Component, Inject } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { map, shareReplay } from 'rxjs/operators';
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angular';

import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-runtime-logs',
  template: require('./api-runtime-logs.component.html'),
  styles: [require('./api-runtime-logs.component.scss')],
})
export class ApiRuntimeLogsComponent {
  private api$ = this.apiService.get(this.ajsStateParams.apiId).pipe(shareReplay(1));
  apiLogs$ = this.apiLogsService.searchConnectionLogs(this.ajsStateParams.apiId);
  isMessageApi$ = this.api$.pipe(map((api: ApiV4) => api?.type === 'MESSAGE'));
  apiLogsEnabled$ = this.api$.pipe(map(ApiRuntimeLogsComponent.isLogEnabled));

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly apiService: ApiV2Service,
  ) {}

  paginationUpdated(event: PageEvent) {
    this.apiLogs$ = this.apiLogsService.searchConnectionLogs(this.ajsStateParams.apiId, event.pageIndex + 1, event.pageSize);
  }

  openLogsSettings() {
    return this.ajsState.go('management.apis.runtimeLogs-settings');
  }

  private static isLogEnabled = (api: ApiV4) => {
    return api.analytics.enabled && (api.analytics.logging?.mode?.endpoint === true || api.analytics.logging?.mode?.entrypoint === true);
  };
}
