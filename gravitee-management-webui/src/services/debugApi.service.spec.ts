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
import { IHttpBackendService } from 'angular';

import { DebugApiService } from './debugApi.service';

import { fakeEvent } from '../entities/event/event.fixture';
import { setupAngularJsTesting } from '../../jest.setup.js';

setupAngularJsTesting();

describe('DebugApiService', () => {
  let debugApiService: DebugApiService;
  let $httpBackend: IHttpBackendService;

  beforeEach(inject((_debugApiService_, _$httpBackend_) => {
    debugApiService = _debugApiService_;
    $httpBackend = _$httpBackend_;
  }));

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('debug', () => {
    it('calls the endpoint', (done) => {
      const request = {
        path: '/planets',
        method: 'POST',
        headers: {},
        body: '{"key": "value"}',
      };

      const responseEvent = fakeEvent();
      $httpBackend
        .expectPOST('https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/apiId/_debug', {
          request,
        })
        .respond(responseEvent);

      const api = {
        id: 'apiId',
      };
      debugApiService
        .debug(api, request)
        .then((result) => {
          expect(result).toEqual(responseEvent);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });

    it('removes http proxy of some specific endpoints', (done) => {
      const request = {
        path: '/planets',
        method: 'POST',
        headers: {},
        body: '{"key": "value"}',
      };

      const api = {
        id: 'apiId',
        proxy: {
          endpoints: [
            {
              proxy: {
                useSystemProxy: false,
              },
            },
            {
              proxy: {
                useSystemProxy: false,
                host: 'localhost',
                port: 3000,
              },
            },
            {
              proxy: {
                useSystemProxy: true,
              },
            },
          ],
        },
      };

      const responseEvent = fakeEvent();
      $httpBackend
        .expectPOST('https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/apiId/_debug', {
          request,
          proxy: {
            endpoints: [
              {},
              {
                proxy: {
                  useSystemProxy: false,
                  host: 'localhost',
                  port: 3000,
                },
              },
              {
                proxy: {
                  useSystemProxy: true,
                },
              },
            ],
          },
        })
        .respond(responseEvent);

      debugApiService
        .debug(api, request)
        .then((result) => {
          expect(result).toEqual(responseEvent);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });
});
