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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { ApiLogsResponse, fakePagedResult } from '../entities/management-api-v2';
import { fakeConnectionLog } from '../entities/management-api-v2/log/connectionLog.fixture';
import { fakeMessageLog } from '../entities/management-api-v2/log/messageLog.fixture';

describe('ApiLogsV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiPlanV2Service: ApiLogsV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiPlanV2Service = TestBed.inject<ApiLogsV2Service>(ApiLogsV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('searchConnectionLogs', () => {
    it('should call the API', (done) => {
      const fakeResponse: ApiLogsResponse = {
        data: [fakeConnectionLog({ apiId: API_ID })],
      };

      apiPlanV2Service.searchConnectionLogs(API_ID).subscribe((apiPlansResponse) => {
        expect(apiPlansResponse.data).toEqual(fakeResponse.data);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });

  describe('searchMessageLogs', () => {
    const REQUEST_ID = 'request-id';

    it('should call the API', (done) => {
      const fakeResponse = fakePagedResult([fakeMessageLog()]);

      apiPlanV2Service.searchMessageLogs(API_ID, REQUEST_ID).subscribe((apiPlansResponse) => {
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
});
