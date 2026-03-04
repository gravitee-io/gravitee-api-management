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

import { ApiAnalyticsV2Service } from './api-analytics-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeAnalyticsRequestsCount } from '../entities/management-api-v2/analytics/analyticsRequestsCount.fixture';
import { fakeAnalyticsCount } from '../entities/management-api-v2/analytics/analyticsCount.fixture';
import { fakeAnalyticsStats } from '../entities/management-api-v2/analytics/analyticsStats.fixture';
import { fakeAnalyticsGroupBy } from '../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { fakeAnalyticsDateHisto } from '../entities/management-api-v2/analytics/analyticsDateHisto.fixture';
import { fakeAnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration.fixture';
import { fakeAnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest.fixture';
import { fakeAnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges.fixture';
import { timeFrameRangesParams } from '../shared/utils/timeFrameRanges';

describe('ApiAnalyticsV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiAnalyticsV2Service;
  const apiId = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiAnalyticsV2Service>(ApiAnalyticsV2Service);
    service.setTimeRangeFilter(timeFrameRangesParams('1d'));
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getRequestsCount', () => {
    it('should call the API', (done) => {
      service.getRequestsCount(apiId).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsRequestsCount());
        done();
      });

      expectGetRequestCount();
    });
  });

  describe('getAverageConnectionDuration', () => {
    it('should call the API', (done) => {
      service.getAverageConnectionDuration(apiId).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsAverageConnectionDuration());
        done();
      });

      expectApiAnalyticsAverageConnectionDurationGetRequest();
    });
  });

  describe('getAverageMessagesPerRequest', () => {
    it('should call the API', (done) => {
      service.getAverageMessagesPerRequest(apiId).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsAverageMessagesPerRequest());
        done();
      });

      expectAverageMessagesPerRequestGetRequest();
    });
  });

  describe('getResponseStatusRanges', () => {
    it('should call the API', (done) => {
      const apiId = 'api-id';

      service.getResponseStatusRanges(apiId).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsResponseStatusRanges());
        done();
      });

      expectApiAnalyticsResponseStatusRangesGetRequest();
    });
  });

  describe('getAnalytics', () => {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;

    it('should call unified endpoint with COUNT type', (done) => {
      service.getAnalytics(apiId, { type: 'COUNT' }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsCount());
        done();
      });

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl));
      expect(req.request.params.get('type')).toBe('COUNT');
      expect(req.request.params.get('from')).toBeDefined();
      expect(req.request.params.get('to')).toBeDefined();
      req.flush(fakeAnalyticsCount());
    });

    it('should call unified endpoint with STATS type and field', (done) => {
      service.getAnalytics(apiId, { type: 'STATS', field: 'gateway-response-time-ms' }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsStats());
        done();
      });

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl));
      expect(req.request.params.get('type')).toBe('STATS');
      expect(req.request.params.get('field')).toBe('gateway-response-time-ms');
      req.flush(fakeAnalyticsStats());
    });

    it('should call unified endpoint with GROUP_BY type, field and size', (done) => {
      service.getAnalytics(apiId, { type: 'GROUP_BY', field: 'status', size: 20 }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsGroupBy());
        done();
      });

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl));
      expect(req.request.params.get('type')).toBe('GROUP_BY');
      expect(req.request.params.get('field')).toBe('status');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(fakeAnalyticsGroupBy());
    });

    it('should call unified endpoint with DATE_HISTO type, field and interval', (done) => {
      service.getAnalytics(apiId, { type: 'DATE_HISTO', field: 'status', interval: 3600000 }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsDateHisto());
        done();
      });

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl));
      expect(req.request.params.get('type')).toBe('DATE_HISTO');
      expect(req.request.params.get('field')).toBe('status');
      expect(req.request.params.get('interval')).toBe('3600000');
      req.flush(fakeAnalyticsDateHisto());
    });

    it('should use explicit from/to when provided', (done) => {
      const from = 1700000000000;
      const to = 1700086400000;
      service.getAnalytics(apiId, { type: 'COUNT', from, to }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsCount());
        done();
      });

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl));
      expect(req.request.params.get('from')).toBe('1700000000000');
      expect(req.request.params.get('to')).toBe('1700086400000');
      req.flush(fakeAnalyticsCount());
    });
  });

  function expectGetRequestCount() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics/requests-count`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeAnalyticsRequestsCount());
  }

  function expectApiAnalyticsResponseStatusRangesGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics/response-status-ranges`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeAnalyticsResponseStatusRanges());
  }

  function expectAverageMessagesPerRequestGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics/average-messages-per-request`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeAnalyticsAverageMessagesPerRequest());
  }

  function expectApiAnalyticsAverageConnectionDurationGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics/average-connection-duration`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeAnalyticsAverageConnectionDuration());
  }
});
