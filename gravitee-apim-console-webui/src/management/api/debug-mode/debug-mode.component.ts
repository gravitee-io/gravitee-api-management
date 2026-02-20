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
import { EMPTY, Subject, Subscription, timer } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { DebugRequest } from './models/DebugRequest';
import { DebugResponse } from './models/DebugResponse';
import { DEBUG_EVENT_FAILED_ERROR, DEBUG_EVENT_FAILED_MESSAGE, DebugModeService } from './debug-mode.service';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PolicyListItem } from '../../../entities/policy';

@Component({
  selector: 'debug-mode',
  templateUrl: './debug-mode.component.html',
  styleUrls: ['./debug-mode.component.scss'],
  standalone: false,
})
export class DebugModeComponent implements OnInit, OnDestroy {
  public debugResponse: DebugResponse;
  public listPolicies: PolicyListItem[];
  public showLongRequestWarning = false;

  private unsubscribe$ = new Subject<boolean>();
  private cancelRequest$ = new Subject<void>();
  private longRequestWarningSubscription?: Subscription;

  constructor(
    private readonly debugModeService: DebugModeService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.debugModeService
      .listPolicies()
      .pipe(
        tap(listPolicies => {
          this.listPolicies = listPolicies;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.clearLongRequestWarningTimer();
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onRequestSubmitted(debugRequest: DebugRequest) {
    this.showLongRequestWarning = false;
    this.startLongRequestWarningTimer();
    this.debugResponse = {
      isLoading: true,
      executionMode: null,
      request: {},
      response: {},
      responsePolicyDebugSteps: [],
      backendResponse: {},
      preprocessorStep: {},
      requestPolicyDebugSteps: [],
      requestDebugSteps: {},
      responseDebugSteps: {},
    };

    this.debugModeService
      .debug(debugRequest)
      .pipe(
        catchError((error: Error) => {
          this.snackBarService.error(
            error?.message === DEBUG_EVENT_FAILED_ERROR ? DEBUG_EVENT_FAILED_MESSAGE : 'Unable to try the request, please try again',
          );
          this.clearLongRequestWarningTimer();
          this.showLongRequestWarning = false;
          this.debugResponse = null;
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
        takeUntil(this.cancelRequest$),
      )
      .subscribe(debugResponse => {
        this.clearLongRequestWarningTimer();
        this.showLongRequestWarning = false;
        this.debugResponse = debugResponse;
      });
  }

  onRequestCancelled() {
    this.cancelRequest$.next();
    this.clearLongRequestWarningTimer();
    this.showLongRequestWarning = false;
    this.debugResponse = null;
  }

  private startLongRequestWarningTimer() {
    this.clearLongRequestWarningTimer();
    this.longRequestWarningSubscription = timer(20_000)
      .pipe(takeUntil(this.unsubscribe$), takeUntil(this.cancelRequest$))
      .subscribe(() => {
        if (this.debugResponse?.isLoading) {
          this.showLongRequestWarning = true;
        }
      });
  }

  private clearLongRequestWarningTimer() {
    this.longRequestWarningSubscription?.unsubscribe();
    this.longRequestWarningSubscription = undefined;
  }
}
