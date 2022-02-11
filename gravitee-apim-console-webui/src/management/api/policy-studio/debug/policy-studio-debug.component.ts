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
import { StateParams } from '@uirouter/core';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { DebugRequest } from './models/DebugRequest';
import { DebugResponse } from './models/DebugResponse';
import { PolicyStudioDebugService } from './policy-studio-debug.service';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { PolicyListItem } from '../../../../entities/policy';

@Component({
  selector: 'policy-studio-debug',
  template: require('./policy-studio-debug.component.html'),
  styles: [require('./policy-studio-debug.component.scss')],
})
export class PolicyStudioDebugComponent implements OnInit {
  public debugResponse: DebugResponse;
  public tryItDisplay = false;
  public listPolicies: PolicyListItem[];

  private apiId: string;
  private unsubscribe$ = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly policyStudioDebugService: PolicyStudioDebugService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.policyStudioDebugService
      .listPolicies()
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((listPolicies) => {
          this.listPolicies = listPolicies;
        }),
      )
      .subscribe();
    this.apiId = this.ajsStateParams.apiId;

    this.tryItDisplay = this.ajsStateParams.tryItDisplay;
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onRequestSubmitted(debugRequest: DebugRequest) {
    this.debugResponse = {
      isLoading: true,
      request: {},
      response: {},
      responseDebugSteps: [],
      backendResponse: {},
      requestDebugSteps: [],
      initialAttributes: {},
    };

    this.policyStudioDebugService
      .debug(this.apiId, debugRequest)
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError(() => {
          this.snackBarService.error('Unable to try the request, please try again');
          this.debugResponse = {
            isLoading: false,
            request: {},
            response: {},
            responseDebugSteps: [],
            backendResponse: {},
            requestDebugSteps: [],
            initialAttributes: {},
          };
          return EMPTY;
        }),
      )
      .subscribe((debugResponse) => {
        this.debugResponse = debugResponse;
      });
  }
}
