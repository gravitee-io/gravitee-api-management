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
import { fakeAnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration.fixture';
import { fakeAnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest.fixture';
import { fakeAnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges.fixture';
import {
  fakeAnalyticsCount,
  fakeAnalyticsDateHisto,
  fakeAnalyticsGroupBy,
  fakeAnalyticsStats,
} from '../entities/management-api-v2/analytics/analyticsUnified.fixture';
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
    it('should call the unified endpoint with type=COUNT', (done) => {
      service.getAnalytics(apiId, { type: 'COUNT' }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsCount({ count: 42 }));
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
      expect(req.request.params.get('type')).toBeNull(); // params built via URLSearchParams in the URL
      expect(req.request.url).toContain('type=COUNT');
      req.flush(fakeAnalyticsCount({ count: 42 }));
    });

    it('should include field and interval params for DATE_HISTO', (done) => {
      service.getAnalytics(apiId, { type: 'DATE_HISTO', field: 'status', interval: 3_600_000 }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsDateHisto());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
      expect(req.request.url).toContain('type=DATE_HISTO');
      expect(req.request.url).toContain('field=status');
      expect(req.request.url).toContain('interval=3600000');
      req.flush(fakeAnalyticsDateHisto());
    });

    it('should include field param for STATS', (done) => {
      service
        .getAnalytics(apiId, { type: 'STATS', field: 'gateway-response-time-ms' })
        .subscribe((result) => {
          expect(result).toEqual(fakeAnalyticsStats({ count: 10, avg: 120.5 }));
          done();
        });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
      expect(req.request.url).toContain('type=STATS');
      expect(req.request.url).toContain('field=gateway-response-time-ms');
      req.flush(fakeAnalyticsStats({ count: 10, avg: 120.5 }));
    });

    it('should include field and size params for GROUP_BY', (done) => {
      service.getAnalytics(apiId, { type: 'GROUP_BY', field: 'status', size: 5 }).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsGroupBy({ values: { '200': 100 } }));
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
      expect(req.request.url).toContain('type=GROUP_BY');
      expect(req.request.url).toContain('field=status');
      expect(req.request.url).toContain('size=5');
      req.flush(fakeAnalyticsGroupBy({ values: { '200': 100 } }));
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
