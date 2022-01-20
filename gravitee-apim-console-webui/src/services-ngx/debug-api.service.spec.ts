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
import { TestBed } from '@angular/core/testing';

import { DebugApiService } from './debug-api.service';

import { fakeEvent } from '../entities/event/event.fixture';
import { setupAngularJsTesting } from '../../jest.setup.js';
import { GioHttpTestingModule } from '../shared/testing';

setupAngularJsTesting();

describe('DebugApiService', () => {
  let httpTestingController: HttpTestingController;
  let debugApiService: DebugApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    debugApiService = TestBed.inject<DebugApiService>(DebugApiService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('debug', () => {
    it('calls the endpoint', (done) => {
      const api = {
        id: 'apiId',
      };
      const request = {
        path: '/planets',
        method: 'POST',
        headers: {},
        body: '{"key": "value"}',
      };
      const responseEvent = fakeEvent();

      debugApiService.debug(api, request).subscribe((result) => {
        expect(result).toEqual(responseEvent);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/apiId/_debug',
      });
      expect(req.request.body).toEqual({ request });
      req.flush(responseEvent);
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

      debugApiService.debug(api, request).subscribe((result) => {
        expect(result).toEqual(responseEvent);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/apiId/_debug',
      });
      expect(req.request.body).toEqual({
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
      });
      req.flush(responseEvent);
    });
  });
});
