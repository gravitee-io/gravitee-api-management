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
import { DebugResponse } from './models/DebugResponse';
import { RequestDebugStep, ResponseDebugStep } from './models/DebugStep';

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

  public debug(apiId: string, debugRequest: DebugRequest): Observable<DebugResponse> {
    const maxPollingTime$ = timer(10000);
    return this.sendDebugEvent(apiId, debugRequest).pipe(
      // Poll each 1s to find success event. Stops after 10 seconds
      switchMap((debugEventId) => interval(1000).pipe(switchMap(() => this.getDebugEvent(apiId, debugEventId)))),
      takeUntil(maxPollingTime$),
      filter((event) => event.status === 'SUCCESS'),
      take(1),
      map((event: DebugEvent) => {
        // First, create the hydrated debug steps for the REQUEST with request initial data + attributes
        const requestDebugSteps =
          event.payload.debugSteps && event.payload.debugSteps.length > 0
            ? PolicyStudioDebugService.convertRequestDebugSteps(
                event.payload.request ?? {},
                event.payload.initialAttributes ?? {},
                event.payload.debugSteps.filter((event) => event.scope === 'ON_REQUEST' || event.scope === 'ON_REQUEST_CONTENT'),
              )
            : [];

        // Then, compute response initial attributes -> either from the last REQUEST debug step or the request initial attributes
        const responseInitialAttributes =
          requestDebugSteps.length > 0
            ? requestDebugSteps[requestDebugSteps.length - 1].policyOutput.attributes
            : event.payload.initialAttributes;

        // Finally, create the hydrated debug steps for the RESPONSE with initial request data + attributes
        const responseDebugSteps =
          event.payload.debugSteps && event.payload.debugSteps.length > 0
            ? PolicyStudioDebugService.convertResponseDebugSteps(
                event.payload.backendResponse ?? {},
                responseInitialAttributes ?? {},
                event.payload.debugSteps.filter((event) => event.scope === 'ON_RESPONSE' || event.scope === 'ON_RESPONSE_CONTENT'),
              )
            : [];

        return {
          isLoading: false,
          response: event.payload.response ?? {},
          request: event.payload.request ?? {},
          backendResponse: event.payload.backendResponse ?? {},
          initialAttributes: event.payload.initialAttributes ?? {},
          requestDebugSteps,
          responseDebugSteps,
        };
      }),
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

  private static convertRequestDebugSteps(
    initialRequest: DebugEvent['payload']['request'],
    initialAttributes: DebugEvent['payload']['initialAttributes'],
    debugSteps: DebugEvent['payload']['debugSteps'],
  ): RequestDebugStep[] {
    const [firstStep, ...others] = debugSteps;

    const firstDebugStep: RequestDebugStep = {
      policyId: firstStep.policyId,
      status: firstStep.status,
      policyInstanceId: firstStep.policyInstanceId,
      scope: firstStep.scope,
      duration: firstStep.duration,
      policyOutput: {
        ...initialRequest,
        attributes: {
          ...initialAttributes,
        },
        ...firstStep.result,
      },
    };

    return others.reduce(
      (acc, currentValue) => {
        const previousStep = acc[acc.length - 1];

        return [
          ...acc,
          {
            policyId: currentValue.policyId,
            status: currentValue.status,
            policyInstanceId: currentValue.policyInstanceId,
            scope: currentValue.scope,
            duration: currentValue.duration,
            policyOutput: {
              ...previousStep.policyOutput,
              ...currentValue.result,
            },
          },
        ];
      },
      [firstDebugStep],
    );
  }

  private static convertResponseDebugSteps(
    backendResponse: DebugEvent['payload']['backendResponse'],
    initialAttributes: DebugEvent['payload']['initialAttributes'],
    debugSteps: DebugEvent['payload']['debugSteps'],
  ): ResponseDebugStep[] {
    const [firstStep, ...others] = debugSteps;

    const firstDebugStep: ResponseDebugStep = {
      policyId: firstStep.policyId,
      status: firstStep.status,
      policyInstanceId: firstStep.policyInstanceId,
      scope: firstStep.scope,
      duration: firstStep.duration,
      policyOutput: {
        ...backendResponse,
        attributes: {
          ...initialAttributes,
        },
        ...firstStep.result,
      },
    };

    return others.reduce(
      (acc, currentValue) => {
        const previousStep = acc[acc.length - 1];

        return [
          ...acc,
          {
            policyId: currentValue.policyId,
            status: currentValue.status,
            policyInstanceId: currentValue.policyInstanceId,
            scope: currentValue.scope,
            duration: currentValue.duration,
            policyOutput: {
              ...previousStep.policyOutput,
              ...currentValue.result,
            },
          },
        ];
      },
      [firstDebugStep],
    );
  }
}
