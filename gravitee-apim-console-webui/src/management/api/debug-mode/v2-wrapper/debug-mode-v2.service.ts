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
import { EMPTY, interval, Observable, of, throwError } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';

import { DEBUG_EVENT_FAILED_ERROR, DebugModeService } from '../debug-mode.service';
import { convertDebugEventToDebugResponse, DebugResponse } from '../models/DebugResponse';
import { DebugRequest } from '../models/DebugRequest';
import { EventService } from '../../../../services-ngx/event.service';
import { DebugApiService } from '../../../../services-ngx/debug-api.service';
import { DebugEvent, debugStatus } from '../models/DebugEvent';
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
    return this.sendDebugEvent(debugRequest).pipe(
      // Poll each 1s to find success event.
      switchMap(({ apiId, debugEventId }) => interval(1000).pipe(switchMap(() => this.getDebugEvent(apiId, debugEventId)))),
      switchMap((event) => {
        if (event.status === 'ERROR') {
          return throwError(() => new Error(DEBUG_EVENT_FAILED_ERROR));
        }
        if (event.status === 'SUCCESS') {
          return of(event);
        }
        return EMPTY;
      }),
      take(1),
      map((event: DebugEvent) => convertDebugEventToDebugResponse(event)),
    );
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

    return this.policyStudioService.getApiDefinition$().pipe(
      switchMap((apiDefinition) => this.apiService.get(apiDefinition.id).pipe(map((api) => ({ ...api, ...apiDefinition })))),
      switchMap((api) =>
        this.debugApiService
          .debug(api, {
            ...request,
            headers: headersAsMap,
          })
          .pipe(map((event) => ({ apiId: api.id, debugEventId: event.id }))),
      ),
    );
  }

  private getDebugEvent(apiId: string, eventId: string): Observable<DebugEvent> {
    return this.eventService.findById(apiId, eventId).pipe(
      filter((event) => event.type === 'DEBUG_API'),
      map((event) => ({
        id: event.id,
        payload: JSON.parse(event.payload),
        status: debugStatus(event.properties.api_debug_status),
      })),
    );
  }
}
