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

import { ApiLogsV2Service } from './api-logs-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ApiLogsResponse, fakeAggregatedMessageLog, fakeConnectionLog, fakePagedResult } from '../entities/management-api-v2';

describe('ApiLogsV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiPlanV2Service: ApiLogsV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiPlanV2Service = TestBed.inject<ApiLogsV2Service>(ApiLogsV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('searchConnectionLogs', () => {
    it.each<any | jest.DoneCallback>([
      { queryParams: {}, queryURL: '?page=1&perPage=10' },
      { queryParams: { page: 2, perPage: 12 }, queryURL: '?page=2&perPage=12' },
      { queryParams: { page: 2, perPage: 12, from: 222 }, queryURL: '?page=2&perPage=12&from=222' },
      { queryParams: { page: 2, perPage: 12, to: 333 }, queryURL: '?page=2&perPage=12&to=333' },
      { queryParams: { page: 2, perPage: 12, from: 222, to: 333 }, queryURL: '?page=2&perPage=12&from=222&to=333' },
      { queryParams: { from: 222, to: 333 }, queryURL: '?page=1&perPage=10&from=222&to=333' },
      { queryParams: { applicationIds: '1,2' }, queryURL: '?page=1&perPage=10&applicationIds=1,2' },
      { queryParams: { planIds: '1,2' }, queryURL: '?page=1&perPage=10&planIds=1,2' },
      { queryParams: { applicationIds: '1,2', planIds: '1,2' }, queryURL: '?page=1&perPage=10&applicationIds=1,2&planIds=1,2' },
      { queryParams: { methods: 'GET,POST' }, queryURL: '?page=1&perPage=10&methods=GET,POST' },
      { queryParams: { statuses: '200,202' }, queryURL: '?page=1&perPage=10&statuses=200,202' },
      { queryParams: { errorKeys: 'API_KEY_MISSING' }, queryURL: '?page=1&perPage=10&errorKeys=API_KEY_MISSING' },
      { queryParams: { errorKeys: 'KEY_1,KEY_2' }, queryURL: '?page=1&perPage=10&errorKeys=KEY_1,KEY_2' },
    ])('call the service with: $queryParams should call API with: $queryURL', ({ queryParams, queryURL }: any, done: jest.DoneCallback) => {
      const fakeResponse: ApiLogsResponse = {
        data: [fakeConnectionLog({ apiId: API_ID })],
      };

      apiPlanV2Service.searchConnectionLogs(API_ID, queryParams).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual(fakeResponse.data);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs${queryURL}`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });

  describe('searchMessageLogs', () => {
    const REQUEST_ID = 'request-id';

    it('should call the API', done => {
      const fakeResponse = fakePagedResult([fakeAggregatedMessageLog()]);

      apiPlanV2Service.searchMessageLogs(API_ID, REQUEST_ID).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual(fakeResponse.data);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}/messages?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });
  describe('searchErrorKeys', () => {
    it('should call the API to fetch error keys without time range', done => {
      const fakeErrorKeys = ['KEY_1', 'KEY_2'];

      apiPlanV2Service.searchErrorKeys(API_ID).subscribe(response => {
        expect(response).toEqual(fakeErrorKeys);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/error-keys`);

      expect(req.request.method).toEqual('GET');
      req.flush(fakeErrorKeys);
    });

    it('should call the API to fetch error keys with from and to params', done => {
      const fakeErrorKeys = ['KEY_1', 'KEY_2'];
      const from = 1000000;
      const to = 2000000;

      apiPlanV2Service.searchErrorKeys(API_ID, from, to).subscribe(response => {
        expect(response).toEqual(fakeErrorKeys);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/error-keys?from=${from}&to=${to}`,
      );

      expect(req.request.method).toEqual('GET');
      req.flush(fakeErrorKeys);
    });
  });
});
