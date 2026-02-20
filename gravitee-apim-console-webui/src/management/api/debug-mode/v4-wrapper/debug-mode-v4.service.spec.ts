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
import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { DebugModeV4Service } from './debug-mode-v4.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { RequestPolicyDebugStep, ResponsePolicyDebugStep } from '../models/DebugStep';
import { fakeDebugEvent } from '../models/DebugEvent.fixture';
import { fakeApiV4 } from '../../../../entities/management-api-v2';
import { DEBUG_EVENT_FAILED_ERROR } from '../debug-mode.service';

describe('DebugModeV4Service', () => {
  let httpTestingController: HttpTestingController;
  let debugModeV4Service: DebugModeV4Service;
  const api = fakeApiV4();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: api.id } } } }],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    debugModeV4Service = TestBed.inject<DebugModeV4Service>(DebugModeV4Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('debug', () => {
    it('should call the API', fakeAsync(() => {
      const mockDebugEvent = fakeDebugEvent();
      const eventId = 'eventId';

      const expectedRequestDebugSteps: RequestPolicyDebugStep[] = [
        {
          id: expect.any(String),
          policyInstanceId: 'c96188c0-c57a-4726-a188-c0c57a172604',
          policyId: 'key-less',
          scope: 'ON_REQUEST',
          status: 'COMPLETED',
          duration: 46143,
          stage: 'PLAN',
          output: {
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
          stage: 'API',
          output: {
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
          stage: 'API',
          output: {
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
          status: 'SKIPPED',
          duration: 30985,
          stage: 'API',
          output: {
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
          stage: 'API',
          output: {
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
          error: {
            contentType: 'application/json',
            key: 'POLICY_ERROR',
            message: 'Error message',
            status: 400,
          },
        },
      ];

      const expectedResponseDebugSteps: ResponsePolicyDebugStep[] = [
        {
          id: expect.any(String),
          policyInstanceId: '4a5265f3-0c5b-4f60-9265-f30c5b8f6041',
          policyId: 'transform-headers',
          scope: 'ON_RESPONSE',
          status: 'COMPLETED',
          duration: 34524,
          stage: 'API',
          output: {
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
          stage: 'API',
          output: {
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

      let done = false;
      debugModeV4Service
        .debug({
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
          expect(response.requestPolicyDebugSteps).toStrictEqual(expectedRequestDebugSteps);
          expect(response.responsePolicyDebugSteps).toStrictEqual(expectedResponseDebugSteps);
          done = true;
        });

      expectSendDebugEvent(eventId);
      tick(1000);
      expectGetDebugEvent(eventId, 'DEBUGGING');
      tick(1000);
      expectGetDebugEvent(eventId, 'SUCCESS');
      tick(1000);

      expect(done).toEqual(true);
    }));

    it('should keep polling beyond 10 seconds and not auto-complete', fakeAsync(() => {
      const eventId = 'eventId';
      let done = false;
      debugModeV4Service
        .debug({
          method: 'GET',
          body: '',
          headers: [],
          path: '',
        })
        .subscribe(() => {
          done = true;
        });

      expectSendDebugEvent(eventId);

      for (let i = 0; i < 11; i++) {
        tick(1000);
        expectGetDebugEvent(eventId, 'DEBUGGING');
      }

      expect(done).toEqual(false);
      discardPeriodicTasks();
    }));

    it('should stop polling and throw an error when debug event status is error', fakeAsync(() => {
      const eventId = 'eventId';
      let error: Error;

      debugModeV4Service
        .debug({
          method: 'GET',
          body: '',
          headers: [],
          path: '',
        })
        .subscribe({
          error: (err) => {
            error = err;
          },
        });

      expectSendDebugEvent(eventId);
      tick(1000);
      expectGetDebugEvent(eventId, 'ERROR');
      tick();

      expect(error.message).toEqual(DEBUG_EVENT_FAILED_ERROR);

      tick(5000);
      httpTestingController.expectNone({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/events/${eventId}`,
      });
    }));
  });

  function expectSendDebugEvent(eventId: string) {
    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/debug`,
      })
      .flush({ id: eventId });
  }

  function expectGetDebugEvent(eventId: string, status: 'SUCCESS' | 'ERROR' | 'DEBUGGING') {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          API_DEBUG_STATUS: status,
        },
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});
