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
import { Observable } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';

import { DebugRequest } from './models/DebugRequest';
import { DebugEvent } from './models/DebugEvent';

import { ApiService } from '../../../../services-ngx/api.service';
import { DebugApiService } from '../../../../services-ngx/debug-api.service';
import { EventService } from '../../../../services-ngx/event.service';


@Injectable({
  providedIn: 'root',
})
export class PolicyStudioDebugService {
  constructor(
    private readonly apiService: ApiService,
    private readonly debugApiService: DebugApiService,
    private readonly eventService: EventService,
  ) {}

  public sendDebugEvent(apiId: string, request: DebugRequest): Observable<string> {
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

  public getDebugEvent(apiId: string, eventId: string): Observable<DebugEvent> {
    return this.eventService.findById(apiId, eventId).pipe(
      filter((event) => event.type === 'DEBUG_API'),
      map((event) => ({
        id: event.id,
        payload: event.payload,
        status: event.properties.api_debug_status === 'SUCCESS' ? 'SUCCESS' : 'FAILED',
      })),
    );
  }
}
