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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4 } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-portal-proxy-endpoints',
  template: require('./api-backend-services.component.html'),
  styles: [require('./api-backend-services.component.scss')],
})
export class ApiBackendServicesComponent implements OnInit, OnDestroy {
  public apiV2: ApiV2;
  public apiV4: ApiV4;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams, private readonly apiService: ApiV2Service) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api: Api) => {
          if (api?.definitionVersion !== 'V4') {
            this.apiV2 = api as ApiV2;
          } else {
            this.apiV4 = api as ApiV4;
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
