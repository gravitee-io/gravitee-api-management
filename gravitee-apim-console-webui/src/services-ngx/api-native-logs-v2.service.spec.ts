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
import { firstValueFrom } from 'rxjs';

import { ApiNativeLogsV2Service } from './api-native-logs-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeNativeApiLog, fakeNativeApiLogsSummary, NativeApiLogsParam, NativeApiLogsResponse } from '../entities/management-api-v2';

interface Row {
  queryParams: NativeApiLogsParam;
  queryURL: string;
}

describe('ApiNativeLogsV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiNativeLogsV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiNativeLogsV2Service>(ApiNativeLogsV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const cases: Row[] = [
    { queryParams: {}, queryURL: '?page=1&perPage=10' },
    { queryParams: { page: 2, perPage: 12 }, queryURL: '?page=2&perPage=12' },
    { queryParams: { from: 222, to: 333 }, queryURL: '?page=1&perPage=10&from=222&to=333' },
    { queryParams: { applicationIds: '1,2' }, queryURL: '?page=1&perPage=10&applicationIds=1,2' },
    { queryParams: { planIds: '1,2' }, queryURL: '?page=1&perPage=10&planIds=1,2' },
    {
      queryParams: { connectionStatuses: 'CONNECTED,CONNECTION_ERROR' },
      queryURL: '?page=1&perPage=10&connectionStatuses=CONNECTED,CONNECTION_ERROR',
    },
    {
      queryParams: { from: 222, to: 333, applicationIds: '1,2', planIds: '3,4', connectionStatuses: 'CONNECTED' },
      queryURL: '?page=1&perPage=10&from=222&to=333&applicationIds=1,2&planIds=3,4&connectionStatuses=CONNECTED',
    },
  ];

  describe('searchConnectionLogs', () => {
    it.each(cases)('call the service with: $queryParams should call API with: $queryURL', async ({ queryParams, queryURL }) => {
      const fakeResponse: NativeApiLogsResponse = {
        data: [fakeNativeApiLog({ apiId: API_ID })],
      };

      const responsePromise = firstValueFrom(service.searchConnectionLogs(API_ID, queryParams));

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native${queryURL}`,
          method: 'GET',
        })
        .flush(fakeResponse);

      const response = await responsePromise;
      expect(response.data).toEqual(fakeResponse.data);
    });
  });

  describe('searchSummary', () => {
    it('calls GET /logs/native/summary with from and to params', async () => {
      const fakeResponse = fakeNativeApiLogsSummary();

      const responsePromise = firstValueFrom(service.searchSummary(API_ID, 1000, 2000));

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`,
          method: 'GET',
        })
        .flush(fakeResponse);

      const response = await responsePromise;
      expect(response.countByConnectionStatus).toEqual(fakeResponse.countByConnectionStatus);
    });
  });
});
