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
import { uniqueId } from 'lodash';

import { RequestDebugStep, ResponseDebugStep } from './DebugStep';
import { DebugEvent } from './DebugEvent';

export type DebugResponse = {
  isLoading: boolean;

  request: {
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  initialAttributes: Record<string, boolean | number | string>;
  requestDebugSteps: RequestDebugStep[];

  backendResponse: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  responseDebugSteps: ResponseDebugStep[];

  response: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
};

export const convertDebugEventToDebugResponse = (event: DebugEvent) => {
  // First, create the hydrated debug steps for the REQUEST with request initial data + attributes
  const requestDebugSteps =
    event.payload.debugSteps && event.payload.debugSteps.length > 0
      ? convertRequestDebugSteps(
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
      ? convertResponseDebugSteps(
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
};

const convertRequestDebugSteps = (
  initialRequest: DebugEvent['payload']['request'],
  initialAttributes: DebugEvent['payload']['initialAttributes'],
  debugSteps: DebugEvent['payload']['debugSteps'],
): RequestDebugStep[] => {
  if (debugSteps.length === 0) {
    return [];
  }

  const [firstStep, ...others] = debugSteps;

  const firstDebugStep: RequestDebugStep = {
    id: uniqueId(),
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
          id: uniqueId(),
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
};

const convertResponseDebugSteps = (
  backendResponse: DebugEvent['payload']['backendResponse'],
  initialAttributes: DebugEvent['payload']['initialAttributes'],
  debugSteps: DebugEvent['payload']['debugSteps'],
): ResponseDebugStep[] => {
  if (debugSteps.length === 0) {
    return [];
  }

  const [firstStep, ...others] = debugSteps;

  const firstDebugStep: ResponseDebugStep = {
    id: uniqueId(),
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
          id: uniqueId(),
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
};
