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
import { interval, Observable, race, timer } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';

import { DebugModeService } from '../debug-mode.service';
import { convertDebugEventToDebugResponse, DebugResponse } from '../models/DebugResponse';
import { DebugRequest } from '../models/DebugRequest';
import { EventService } from '../../../../services-ngx/event.service';
import { DebugApiService } from '../../../../services-ngx/debug-api.service';
import { DebugEvent } from '../models/DebugEvent';
import { ApiService } from '../../../../services-ngx/api.service';
import { PolicyStudioService } from '../../policy-studio-v2/policy-studio.service';
import { PolicyService } from '../../../../services-ngx/policy.service';

@Injectable({
  providedIn: 'root',
})
export class DebugModeV2Service extends DebugModeService {
  constructor(
    private readonly apiService: ApiService,
    private readonly policyStudioService: PolicyStudioService,
    private readonly debugApiService: DebugApiService,
    private readonly eventService: EventService,
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
      filter(event => event.status === 'SUCCESS'),
      take(1),
      map((event: DebugEvent) => convertDebugEventToDebugResponse(event)),
    );

    return race(maxPollingTime$, pollingEvent$);
  }

  private sendDebugEvent(request: DebugRequest): Observable<{ apiId: string; debugEventId: string }> {
    const headersAsMap = (request.headers ?? [])
      .filter(header => !!header.value)
      .reduce(
        (acc, current) => {
          acc[current.key] = acc[current.key] ? [...acc[current.key], current.value] : [current.value];
          return acc;
        },
        {} as Record<string, string[]>,
      );

    return this.policyStudioService.getApiDefinition$().pipe(
      switchMap(apiDefinition => this.apiService.get(apiDefinition.id).pipe(map(api => ({ ...api, ...apiDefinition })))),
      switchMap(api =>
        this.debugApiService
          .debug(api, {
            ...request,
            headers: headersAsMap,
          })
          .pipe(map(event => ({ apiId: api.id, debugEventId: event.id }))),
      ),
    );
  }

  private getDebugEvent(apiId: string, eventId: string): Observable<DebugEvent> {
    return this.eventService.findById(apiId, eventId).pipe(
      filter(event => event.type === 'DEBUG_API'),
      map(event => ({
        id: event.id,
        payload: JSON.parse(event.payload),
        status: event.properties.api_debug_status === 'SUCCESS' ? 'SUCCESS' : 'FAILED',
      })),
    );
  }
}
