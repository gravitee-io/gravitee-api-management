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

import { RequestDebugStep, DebugSteps, RequestPolicyDebugStep, ResponsePolicyDebugStep, ResponseDebugStep } from './DebugStep';
import { DebugEvent } from './DebugEvent';

export type DebugResponse = {
  isLoading: boolean;
  reachedTimeout?: boolean;

  request: {
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };

  preprocessorStep: {
    attributes?: Record<string, boolean | number | string>;
    headers?: Record<string, string[]>;
  };

  requestPolicyDebugSteps: RequestPolicyDebugStep[];
  requestDebugSteps: DebugSteps<RequestDebugStep>;

  backendResponse: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  responsePolicyDebugSteps: ResponsePolicyDebugStep[];
  responseDebugSteps: DebugSteps<ResponseDebugStep>;

  response: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
};

export const convertDebugEventToDebugResponse = (event: DebugEvent): DebugResponse => {
  // First, create the hydrated debug steps for the REQUEST with request initial data + attributes
  const requestPolicyDebugSteps =
    event.payload.debugSteps && event.payload.debugSteps.length > 0
      ? convertRequestDebugSteps(
          event.payload.request ?? {},
          event.payload.preprocessorStep ?? {},
          event.payload.debugSteps.filter((event) => event.scope === 'ON_REQUEST' || event.scope === 'ON_REQUEST_CONTENT'),
        )
      : [];

  const requestInputDebugStep: RequestDebugStep = {
    id: 'request-input',
    status: undefined,
    duration: 0,
    output: {
      ...event.payload.request,
      ...event.payload.preprocessorStep,
    },
    stage: undefined,
  };

  const requestOutputDebugStep = requestPolicyDebugSteps.reduce(
    (acc, current) => {
      const status = acc.status !== 'ERROR' ? current.status : acc.status;
      const duration = acc.duration + current.duration;
      return {
        ...acc,
        status,
        duration,
        output: {
          ...acc.output,
          ...current.output,
        },
      };
    },
    { ...requestInputDebugStep, id: 'request-output' },
  );

  // Then, compute response initial attributes and headers -> either from the last REQUEST debug step or the request initial attributes
  const responsePreprocessorStep: DebugEvent['payload']['preprocessorStep'] =
    requestPolicyDebugSteps.length > 0
      ? {
          attributes: requestPolicyDebugSteps[requestPolicyDebugSteps.length - 1].output.attributes,
          headers: requestPolicyDebugSteps[requestPolicyDebugSteps.length - 1].output.headers,
        }
      : event.payload.preprocessorStep;

  // Finally, create the hydrated debug steps for the RESPONSE with initial request data + attributes
  const responsePolicyDebugSteps =
    event.payload.debugSteps && event.payload.debugSteps.length > 0
      ? convertResponseDebugSteps(
          event.payload.backendResponse ?? {},
          responsePreprocessorStep ?? {},
          event.payload.debugSteps.filter((event) => event.scope === 'ON_RESPONSE' || event.scope === 'ON_RESPONSE_CONTENT'),
        )
      : [];

  const responseInputDebugStep: RequestDebugStep = {
    id: 'response-input',
    status: undefined,
    duration: 0,
    output: {
      attributes: requestOutputDebugStep.output.attributes,
      ...(event.payload.backendResponse ?? {}),
    },
    stage: undefined,
  };

  const responseOutputDebugStep = responsePolicyDebugSteps.reduce(
    (acc, current) => {
      const status = acc.status !== 'ERROR' ? current.status : acc.status;
      const duration = acc.duration + current.duration;
      return {
        ...acc,
        status,
        duration,
        output: {
          ...acc.output,
          ...current.output,
        },
      };
    },
    { ...responseInputDebugStep, id: 'response-output' },
  );

  return {
    isLoading: false,
    response: event.payload.response ?? {},
    request: event.payload.request ?? {},
    backendResponse: event.payload.backendResponse ?? {},
    preprocessorStep: event.payload.preprocessorStep ?? {},
    requestPolicyDebugSteps,
    responsePolicyDebugSteps,
    requestDebugSteps: {
      input: requestInputDebugStep,
      output: requestOutputDebugStep,
    },
    responseDebugSteps: {
      input: responseInputDebugStep,
      output: responseOutputDebugStep,
    },
  };
};

const convertRequestDebugSteps = (
  initialRequest: DebugEvent['payload']['request'],
  preprocessorStep: DebugEvent['payload']['preprocessorStep'],
  debugSteps: DebugEvent['payload']['debugSteps'],
): RequestPolicyDebugStep[] => {
  if (debugSteps.length === 0) {
    return [];
  }

  const [firstStep, ...others] = debugSteps;

  const firstDebugStep: RequestPolicyDebugStep = {
    id: uniqueId(),
    policyId: firstStep.policyId,
    status: firstStep.status,
    policyInstanceId: firstStep.policyInstanceId,
    scope: firstStep.scope,
    duration: firstStep.duration,
    stage: firstStep.stage,
    output: {
      ...initialRequest,
      ...preprocessorStep,
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
          output: {
            ...previousStep.output,
            ...currentValue.result,
          },
          stage: currentValue.stage,
        },
      ];
    },
    [firstDebugStep],
  );
};

const convertResponseDebugSteps = (
  backendResponse: DebugEvent['payload']['backendResponse'],
  preprocessorStep: DebugEvent['payload']['preprocessorStep'],
  debugSteps: DebugEvent['payload']['debugSteps'],
): ResponsePolicyDebugStep[] => {
  if (debugSteps.length === 0) {
    return [];
  }

  const [firstStep, ...others] = debugSteps;

  const firstDebugStep: ResponsePolicyDebugStep = {
    id: uniqueId(),
    policyId: firstStep.policyId,
    status: firstStep.status,
    policyInstanceId: firstStep.policyInstanceId,
    scope: firstStep.scope,
    duration: firstStep.duration,
    output: {
      ...backendResponse,
      ...preprocessorStep,
      ...firstStep.result,
    },
    stage: firstStep.stage,
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
          output: {
            ...previousStep.output,
            ...currentValue.result,
          },
          stage: currentValue.stage,
        },
      ];
    },
    [firstDebugStep],
  );
};
