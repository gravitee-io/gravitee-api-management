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
import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-creation-v4-confirmation',
  template: require('./api-creation-v4-confirmation.component.html'),
  styles: [require('./api-creation-v4-confirmation.component.scss')],
})
export class ApiCreationV4ConfirmationComponent implements OnInit {
  private unsubscribe$: Subject<void> = new Subject<void>();
  public api: Api;
  constructor(
    @Inject(UIRouterState) readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiV2Service: ApiV2Service,
  ) {}

  navigate(urlState: string) {
    return this.ajsState.go(urlState, { apiId: this.ajsStateParams.apiId }, { reload: true });
  }

  ngOnInit(): void {
    this.apiV2Service
      .get(this.ajsStateParams.apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((api) => {
        this.api = api;
      });
  }
}
