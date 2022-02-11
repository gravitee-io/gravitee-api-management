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
import { interval, Observable, timer } from 'rxjs';
import { filter, map, switchMap, take, takeUntil } from 'rxjs/operators';

import { DebugRequest } from './models/DebugRequest';
import { DebugEvent } from './models/DebugEvent';
import { convertDebugEventToDebugResponse, DebugResponse } from './models/DebugResponse';

import { ApiService } from '../../../../services-ngx/api.service';
import { DebugApiService } from '../../../../services-ngx/debug-api.service';
import { EventService } from '../../../../services-ngx/event.service';
import { PolicyListItem } from '../../../../entities/policy/policyListItem';
import { PolicyService } from '../../../../services-ngx/policy.service';

@Injectable({
  providedIn: 'root',
})
export class PolicyStudioDebugService {
  constructor(
    private readonly apiService: ApiService,
    private readonly debugApiService: DebugApiService,
    private readonly eventService: EventService,
    private readonly policyService: PolicyService,
  ) {}

  public debug(apiId: string, debugRequest: DebugRequest): Observable<DebugResponse> {
    const maxPollingTime$ = timer(10000);
    return this.sendDebugEvent(apiId, debugRequest).pipe(
      // Poll each 1s to find success event. Stops after 10 seconds
      switchMap((debugEventId) => interval(1000).pipe(switchMap(() => this.getDebugEvent(apiId, debugEventId)))),
      takeUntil(maxPollingTime$),
      filter((event) => event.status === 'SUCCESS'),
      take(1),
      map((event: DebugEvent) => convertDebugEventToDebugResponse(event)),
    );
  }

  private sendDebugEvent(apiId: string, request: DebugRequest): Observable<string> {
    const headersAsMap = (request.headers ?? [])
      .filter((header) => !!header.value)
      .reduce((acc, current) => {
        acc[current.name] = acc[current.name] ? [...acc[current.name], current.value] : [current.value];
        return acc;
      }, {} as Record<string, string[]>);

    return this.apiService.get(apiId).pipe(
      switchMap((api) =>
        this.debugApiService.debug(api, {
          ...request,
          headers: headersAsMap,
        }),
      ),
      map((event) => event.id),
    );
  }

  private getDebugEvent(apiId: string, eventId: string): Observable<DebugEvent> {
    return this.eventService.findById(apiId, eventId).pipe(
      filter((event) => event.type === 'DEBUG_API'),
      map((event) => ({
        id: event.id,
        payload: JSON.parse(event.payload),
        status: event.properties.api_debug_status === 'SUCCESS' ? 'SUCCESS' : 'FAILED',
      })),
    );
  }

  public listPolicies(): Observable<PolicyListItem[]> {
    return this.policyService.list({ expandIcon: true, withoutResource: true });
  }
}
