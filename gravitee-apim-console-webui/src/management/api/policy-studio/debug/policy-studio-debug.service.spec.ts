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
import { HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { PolicyStudioDebugService } from './policy-studio-debug.service';
import { fakeDebugEvent } from './models/DebugEvent.fixture';
import { RequestDebugStep, ResponseDebugStep } from './models/DebugStep';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeApi } from '../../../../entities/api/Api.fixture';

describe('PolicyStudioDebugService', () => {
  let httpTestingController: HttpTestingController;
  let policyStudioDebugService: PolicyStudioDebugService;
  const api = fakeApi();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    policyStudioDebugService = TestBed.inject<PolicyStudioDebugService>(PolicyStudioDebugService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('debug', () => {
    it('should call the API', fakeAsync((done) => {
      const mockDebugEvent = fakeDebugEvent();
      const eventId = 'eventId';

      const expectedRequestDebugSteps: RequestDebugStep[] = [
        {
          id: expect.any(String),
          policyInstanceId: 'c96188c0-c57a-4726-a188-c0c57a172604',
          policyId: 'key-less',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 46143,
          policyOutput: {
            path: '/',
            method: 'GET',
            body: '{}',
            headers: {
              'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
              'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
            },
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
          id: expect.any(String),
          policyInstanceId: '337350ac-d157-47b2-b350-acd157e7b26b',
          policyId: 'policy-assign-attributes',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 36091,
          policyOutput: {
            path: '/',
            method: 'GET',
            body: '{}',
            headers: {
              'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
              'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
            },
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
          id: expect.any(String),
          policyInstanceId: 'b3cb3acc-79ea-48ea-8b3a-cc79ea48ea7e',
          policyId: 'policy-override-request-method',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 17968,
          policyOutput: {
            path: '/',
            method: 'GET',
            body: '{}',
            headers: {
              'X-Gravitee-Request-Id': ['303247f6-5811-4a90-b247-f658115a9033'],
              'X-Gravitee-Transaction-Id': ['b39875e9-7fc7-4980-9875-e97fc7b980b7'],
            },
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
          id: expect.any(String),
          policyInstanceId: '6fcb8a49-b853-4f47-8b8a-49b853df47bd',
          policyId: 'transform-headers',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 30985,
          policyOutput: {
            path: '/',
            method: 'GET',
            body: '{}',
            headers: {
              'dev-header': ['gmaisse'],
              'transfer-encoding': ['chunked'],
              'a-header-platform': ['WOW'],
              host: ['localhost:8482'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'X-Gravitee-Request-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
            },
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
          duration: 17968,
          id: expect.any(String),
          policyId: 'transform-headers',
          policyInstanceId: 'b3cb3acc-79ea-48ea-8b3a-cc79ea48e666',
          policyOutput: {
            attributes: {
              dev: 'gmaisse',
              'gravitee.attribute.api': 'e9b6a4f8-f660-42a4-b6a4-f8f66072a401',
              'gravitee.attribute.api.deployed-at': 1644481512252,
              'gravitee.attribute.application': '1',
              'gravitee.attribute.context-path': '/09770e92ee2112001ade3747-echo/',
              'gravitee.attribute.gravitee.attribute.plan.selection.rule.based': false,
              'gravitee.attribute.plan': '9722a300-c59f-41c7-a2a3-00c59ff1c777',
              'gravitee.attribute.request.method': 'POST',
              'gravitee.attribute.resolved-path': '/',
              'gravitee.attribute.user-id': '127.0.0.1',
            },
            body: '{}',
            'error.contentType': 'application/json',
            'error.key': 'POLICY_ERROR',
            'error.message': 'Error message',
            'error.status': '400',
            headers: {
              'X-Gravitee-Request-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'a-header-platform': ['WOW'],
              'dev-header': ['gmaisse'],
              host: ['localhost:8482'],
              'transfer-encoding': ['chunked'],
            },
            method: 'GET',
            path: '/',
          },
          scope: 'ON_REQUEST',
          status: 'ERROR',
        },
      ];

      const expectedResponseDebugSteps: ResponseDebugStep[] = [
        {
          id: expect.any(String),
          policyInstanceId: '4a5265f3-0c5b-4f60-9265-f30c5b8f6041',
          policyId: 'transform-headers',
          scope: 'ON_RESPONSE',
          status: 'COMPLETED',
          duration: 34524,
          policyOutput: {
            body: '{"headers":{"X-Gravitee-Transaction-Id":"b39875e9-7fc7-4980-9875-e97fc7b980b7","X-Gravitee-Request-Id":"303247f6-5811-4a90-b247-f658115a9033","a-header-platform":"WOW","Host":"api.gravitee.io","accept-encoding":"deflate, gzip"},"query_params":{}}',
            statusCode: 200,
            headers: {
              'content-length': ['0'],
              'Cache-Control': ['no-cache'],
              'header-2': ['coucou'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'Sozu-Id': ['01FVHBS0T3VBPVE5RM8Q8XA8QY'],
              'X-Gravitee-Request-Id': ['f1b7ee4a-3d71-4d3b-b7ee-4a3d717d3b2b'],
            },
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
          id: expect.any(String),
          policyInstanceId: '0d47719b-6979-4ff2-8771-9b6979eff230',
          policyId: 'policy-assign-content',
          scope: 'ON_RESPONSE_CONTENT',
          status: 'COMPLETED',
          duration: 1255578,
          policyOutput: {
            statusCode: 200,
            headers: {
              'Cache-Control': ['no-cache'],
              'header-2': ['coucou'],
              'X-Gravitee-Transaction-Id': ['8a0b25f7-bb56-4a0b-8b25-f7bb564a0b99'],
              'Content-Length': ['29'],
              'Sozu-Id': ['01FVHBS0T3VBPVE5RM8Q8XA8QY'],
              'X-Gravitee-Request-Id': ['f1b7ee4a-3d71-4d3b-b7ee-4a3d717d3b2b'],
            },
            body: '{ "some_content" : "content"}',
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
      ];

      policyStudioDebugService
        .debug(api.id, {
          method: 'GET',
          body: '',
          headers: [],
          path: '',
        })
        .subscribe((response) => {
          expect(response.preprocessorStep).toStrictEqual(mockDebugEvent.payload.preprocessorStep);
          expect(response.backendResponse).toStrictEqual(mockDebugEvent.payload.backendResponse);
          expect(response.request).toStrictEqual(mockDebugEvent.payload.request);
          expect(response.response).toStrictEqual(mockDebugEvent.payload.response);
          expect(response.isLoading).toStrictEqual(false);
          expect(response.requestDebugSteps).toStrictEqual(expectedRequestDebugSteps);
          expect(response.responseDebugSteps).toStrictEqual(expectedResponseDebugSteps);
          done();
        });

      expectSendDebugEvent(eventId);
      tick(1000);
      expectGetDebugEvent(eventId, false);
      tick(1000);
      expectGetDebugEvent(eventId, true);
      tick(1000);
    }));
  });

  function expectSendDebugEvent(eventId: string) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/_debug`,
      })
      .flush({ id: eventId });
  }

  function expectGetDebugEvent(eventId: string, success: boolean) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          api_debug_status: success ? 'SUCCESS' : 'FAILED',
        },
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});
