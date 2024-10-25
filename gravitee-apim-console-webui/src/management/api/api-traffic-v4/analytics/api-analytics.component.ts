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
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';

import { ApiAnalyticsMessageComponent } from './api-analytics-message/api-analytics-message.component';
import { ApiAnalyticsProxyComponent } from './api-analytics-proxy/api-analytics-proxy.component';

import { onlyApiV4Filter } from '../../../../util/apiFilter.operator';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-analytics',
  standalone: true,
  imports: [CommonModule, ApiAnalyticsMessageComponent, ApiAnalyticsProxyComponent],
  template: `
    @if (api$ | async; as api) {
      @switch (api.type) {
        @case ('MESSAGE') {
          <api-analytics-message/>
        }
        @case ('PROXY') {
          <api-analytics-proxy/>
        }
      }
    }
  `,
})
export class ApiAnalyticsComponent {
  private readonly apiService = inject(ApiV2Service);
  private readonly activatedRoute = inject(ActivatedRoute);

  api$ = this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter());
}
