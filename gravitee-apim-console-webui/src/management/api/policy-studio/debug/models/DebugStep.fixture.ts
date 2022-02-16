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

import { merge } from 'lodash';

import { RequestPolicyDebugStep } from './DebugStep';

export function fakeRequestDebugStep(attributes?: Partial<RequestPolicyDebugStep>): RequestPolicyDebugStep {
  const base: RequestPolicyDebugStep = {
    id: 'fake-step',
    policyInstanceId: 'c96188c0-c57a-4726-a188-c0c57a172604',
    policyId: 'key-less',
    scope: 'ON_REQUEST',
    status: 'COMPLETED',
    duration: 46143,
    output: {
      headers: {
        'content-length': ['246'],
        'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
        'Sozu-Id': ['01FVFJSC0K00F65RGB6AQ8K0F5'],
        'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
        'Content-Type': ['application/json'],
      },
      body: `{
    "foo": "bar"
}`,
      pathParameters: {
        retrieverPetGenders: 'true',
        retrievePetSize: 'false',
        defaultPetName: 'Bobby',
      },
      method: 'GET',
      path: '/fake/api',
    },
  };

  return merge(base, attributes);
}

export function fakeErrorRequestDebugStep(attributes?: Partial<RequestPolicyDebugStep>): RequestPolicyDebugStep {
  const base: RequestPolicyDebugStep = {
    id: 'fake-step',
    policyInstanceId: 'c96188c0-c57a-4726-a188-c0c57a172604',
    policyId: 'key-less',
    scope: 'ON_REQUEST',
    status: 'COMPLETED',
    duration: 46143,
    output: {
      body: '{}',
      'error.contentType': 'application/json',
      'error.key': 'POLICY_ERROR',
      'error.message': 'Error message',
      'error.status': '400',
    },
  };

  return merge(base, attributes);
}
