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
import { Injectable } from '@angular/core';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { interval, Observable, race, timer } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { ApiEventsV2Service } from '../../../../services-ngx/api-events-v2.service';
import { PolicyService } from '../../../../services-ngx/policy.service';
import { DebugRequest } from '../models/DebugRequest';
import { convertDebugEventToDebugResponse, DebugResponse } from '../models/DebugResponse';
import { DebugApiV2Service } from '../../../../services-ngx/debug-api-v2.service';
import { DebugModeService } from '../debug-mode.service';
import { DebugEvent } from '../models/DebugEvent';

@Injectable({
  providedIn: 'root',
})
export class DebugModeV4Service extends DebugModeService {
  constructor(
    private readonly debugApiService: DebugApiV2Service,
    private readonly eventService: ApiEventsV2Service,
    private readonly activatedRoute: ActivatedRoute,
    policyService: PolicyService,
  ) {
    super(policyService);
  }

  public debug(debugRequest: DebugRequest): Observable<DebugResponse> {
    const maxPollingTime$ = timer(10000).pipe(
      map<number, DebugResponse>(() => ({
        isLoading: false,
        reachedTimeout: true,
        executionMode: null,
        request: {},
        response: {},
        responsePolicyDebugSteps: [],
        backendResponse: {},
        requestPolicyDebugSteps: [],
        preprocessorStep: {},
        requestDebugSteps: {},
        responseDebugSteps: {},
      })),
    );

    const pollingEvent$ = this.sendDebugEvent(debugRequest).pipe(
      // Poll each 1s to find success event. Stops after 10 seconds
      switchMap(({ apiId, debugEventId }) => interval(1000).pipe(switchMap(() => this.getDebugEvent(apiId, debugEventId)))),
      filter((event) => event.status === 'SUCCESS'),
      take(1),
      map((event: DebugEvent) => convertDebugEventToDebugResponse(event)),
    );

    return race(maxPollingTime$, pollingEvent$);
  }

  private sendDebugEvent(request: DebugRequest): Observable<{ apiId: string; debugEventId: string }> {
    const headersAsMap = (request.headers ?? [])
      .filter((header) => !!header.value)
      .reduce(
        (acc, current) => {
          acc[current.key] = acc[current.key] ? [...acc[current.key], current.value] : [current.value];
          return acc;
        },
        {} as Record<string, string[]>,
      );

    const apiId = this.activatedRoute.snapshot.params.apiId;

    return this.debugApiService
      .debug(
        {
          ...request,
          headers: headersAsMap,
        },
        apiId,
      )
      .pipe(map((event) => ({ apiId: apiId, debugEventId: event.id })));
  }

  private getDebugEvent(apiId: string, eventId: string): Observable<DebugEvent> {
    return this.eventService.findById(apiId, eventId).pipe(
      filter((event) => event.type === 'DEBUG_API'),
      map((event) => ({
        id: event.id,
        payload: JSON.parse(event.payload),
        status: event.properties.API_DEBUG_STATUS === 'SUCCESS' ? 'SUCCESS' : 'FAILED',
      })),
    );
  }
}
