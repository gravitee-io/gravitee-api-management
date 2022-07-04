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
import { DebugEvent, DebugEventMetrics } from './DebugEvent';

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

  metrics?: DebugEventMetrics;
};

export const convertDebugEventToDebugResponse = (event: DebugEvent): DebugResponse => {
  // Filter out empty debug steps that are not relevant to display in the UI.
  const filteredDebugSteps = event.payload.debugSteps.filter(filterEventDebugStep);

  // First, create the hydrated debug steps for the REQUEST with request initial data + attributes
  const requestPolicyDebugSteps =
    filteredDebugSteps && filteredDebugSteps.length > 0
      ? convertRequestDebugSteps(
          event.payload.request ?? {},
          event.payload.preprocessorStep ?? {},
          filteredDebugSteps.filter((event) => event.scope === 'ON_REQUEST' || event.scope === 'ON_REQUEST_CONTENT'),
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
    filteredDebugSteps && filteredDebugSteps.length > 0
      ? convertResponseDebugSteps(
          event.payload.backendResponse ?? {},
          responsePreprocessorStep ?? {},
          filteredDebugSteps.filter((event) => event.scope === 'ON_RESPONSE' || event.scope === 'ON_RESPONSE_CONTENT'),
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
    metrics: event.payload.metrics,
  };
};

const convertRequestDebugSteps = (
  initialRequest: DebugEvent['payload']['request'],
  preprocessorStep: DebugEvent['payload']['preprocessorStep'],
  debugSteps: DebugEventDebugStepFiltered[],
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
    ...(firstStep.condition ? { condition: firstStep.condition } : {}),
    ...(firstStep.error ? { error: firstStep.error } : {}),
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
          ...(currentValue.condition ? { condition: currentValue.condition } : {}),
          ...(currentValue.error ? { error: currentValue.error } : {}),
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
  debugSteps: DebugEventDebugStepFiltered[],
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
    ...(firstStep.condition ? { condition: firstStep.condition } : {}),
    ...(firstStep.error ? { error: firstStep.error } : {}),
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
          ...(currentValue.condition ? { condition: currentValue.condition } : {}),
          ...(currentValue.error ? { error: currentValue.error } : {}),
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

type DebugEventDebugStepFiltered = DebugEvent['payload']['debugSteps'][number] & {
  status: 'COMPLETED' | 'ERROR' | 'SKIPPED';
};
const filterEventDebugStep = (step: DebugEvent['payload']['debugSteps'][number]): step is DebugEventDebugStepFiltered =>
  step.status !== 'NO_TRANSFORMATION';
