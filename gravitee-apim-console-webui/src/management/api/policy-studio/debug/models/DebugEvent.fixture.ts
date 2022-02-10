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
import { DebugEvent } from './DebugEvent';

export function fakeDebugEvent(attributes?: Partial<DebugEvent>): DebugEvent {
  const base: DebugEvent = {
    id: 'e9b6a4f8-f660-42a4-b6a4-f8f66072a401',
    payload: {
      request: {
        path: '/',
        method: 'GET',
        body: '{}',
        headers: {
          'initial-header': ['init'],
        },
      },
      response: {
        body: '{"headers":{"X-Gravitee-Transaction-Id":"b39875e9-7fc7-4980-9875-e97fc7b980b7","X-Gravitee-Request-Id":"303247f6-5811-4a90-b247-f658115a9033","a-header-platform":"WOW","Host":"api.gravitee.io","accept-encoding":"deflate, gzip"},"query_params":{}}',
        headers: {
          'content-length': ['246'],
          'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
          'Sozu-Id': ['01FVFJSC0K00F65RGB6AQ8K0F5'],
          'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
          'Content-Type': ['application/json'],
        },
        statusCode: 200,
      },
      debugSteps: [
        {
          policyInstanceId: 'c96188c0-c57a-4726-a188-c0c57a172604',
          policyId: 'key-less',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 46143,
          result: {
            attributes: {
              'gravitee.attribute.context-path': '/09770e92ee2112001ade3747-echo/',
              'gravitee.attribute.application': '1',
              'gravitee.attribute.api.deployed-at': 1644481512252,
              'gravitee.attribute.user-id': '127.0.0.1',
              'gravitee.attribute.plan': '9722a300-c59f-41c7-a2a3-00c59ff1c777',
              'gravitee.attribute.api': 'e9b6a4f8-f660-42a4-b6a4-f8f66072a401',
              'gravitee.attribute.resolved-path': '/',
              'gravitee.attribute.gravitee.attribute.plan.selection.rule.based': false,
            },
          },
        },
        {
          policyInstanceId: '337350ac-d157-47b2-b350-acd157e7b26b',
          policyId: 'policy-assign-attributes',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 36091,
          result: {
            attributes: {
              'gravitee.attribute.context-path': '/09770e92ee2112001ade3747-echo/',
              'gravitee.attribute.application': '1',
              'gravitee.attribute.api.deployed-at': 1644481512252,
              'gravitee.attribute.user-id': '127.0.0.1',
              'gravitee.attribute.plan': '9722a300-c59f-41c7-a2a3-00c59ff1c777',
              dev: 'gmaisse',
              'gravitee.attribute.api': 'e9b6a4f8-f660-42a4-b6a4-f8f66072a401',
              'gravitee.attribute.resolved-path': '/',
              'gravitee.attribute.gravitee.attribute.plan.selection.rule.based': false,
            },
          },
        },
        {
          policyInstanceId: 'b3cb3acc-79ea-48ea-8b3a-cc79ea48ea7e',
          policyId: 'policy-override-request-method',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 17968,
          result: {
            attributes: {
              'gravitee.attribute.context-path': '/09770e92ee2112001ade3747-echo/',
              'gravitee.attribute.application': '1',
              'gravitee.attribute.api.deployed-at': 1644481512252,
              'gravitee.attribute.user-id': '127.0.0.1',
              'gravitee.attribute.plan': '9722a300-c59f-41c7-a2a3-00c59ff1c777',
              dev: 'gmaisse',
              'gravitee.attribute.request.method': 'POST',
              'gravitee.attribute.api': 'e9b6a4f8-f660-42a4-b6a4-f8f66072a401',
              'gravitee.attribute.resolved-path': '/',
              'gravitee.attribute.gravitee.attribute.plan.selection.rule.based': false,
            },
          },
        },
        {
          policyInstanceId: '6fcb8a49-b853-4f47-8b8a-49b853df47bd',
          policyId: 'transform-headers',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 30985,
          result: {
            headers: {
              'dev-header': ['gmaisse'],
              'transfer-encoding': ['chunked'],
              'a-header-platform': ['WOW'],
              host: ['localhost:8482'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'X-Gravitee-Request-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
            },
          },
        },
        {
          policyInstanceId: '4a5265f3-0c5b-4f60-9265-f30c5b8f6041',
          policyId: 'transform-headers',
          scope: 'ON_RESPONSE',
          status: 'COMPLETED',
          duration: 34524,
          result: {
            headers: {
              'content-length': ['0'],
              'Cache-Control': ['no-cache'],
              'header-2': ['coucou'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'Sozu-Id': ['01FVHBS0T3VBPVE5RM8Q8XA8QY'],
              'X-Gravitee-Request-Id': ['f1b7ee4a-3d71-4d3b-b7ee-4a3d717d3b2b'],
            },
          },
        },
        {
          policyInstanceId: '0d47719b-6979-4ff2-8771-9b6979eff230',
          policyId: 'policy-assign-content',
          scope: 'ON_RESPONSE_CONTENT',
          status: 'COMPLETED',
          duration: 1255578,
          result: {
            headers: {
              'Cache-Control': ['no-cache'],
              'header-2': ['coucou'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'Content-Length': ['29'],
              'Sozu-Id': ['01FVHBS0T3VBPVE5RM8Q8XA8QY'],
              'X-Gravitee-Request-Id': ['f1b7ee4a-3d71-4d3b-b7ee-4a3d717d3b2b'],
            },
            body: '{ "some_content" : "content"}',
          },
        },
      ],
      initialAttributes: {
        'gravitee.attribute.context-path': '/09770e92ee2112001ade3747-echo/',
      },
      backendResponse: {
        body: '{"headers":{"X-Gravitee-Transaction-Id":"b39875e9-7fc7-4980-9875-e97fc7b980b7","X-Gravitee-Request-Id":"303247f6-5811-4a90-b247-f658115a9033","a-header-platform":"WOW","Host":"api.gravitee.io","accept-encoding":"deflate, gzip"},"query_params":{}}',
        headers: {
          'content-length': ['246'],
          'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
          'Sozu-Id': ['01FVFJSC0K00F65RGB6AQ8K0F5'],
          'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
          'Content-Type': ['application/json'],
        },
        statusCode: 200,
      },
    },
    status: 'SUCCESS',
  };

  return {
    ...base,
    ...attributes,
  };
}
