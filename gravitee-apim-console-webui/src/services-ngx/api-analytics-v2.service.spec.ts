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

  describe('getV4Analytics', () => {
    const from = 1000;
    const to = 2000;
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;

    const urlIncludes = (r: { url: string }, type: string, field?: string) => {
      if (!r.url.startsWith(baseUrl)) return false;
      if (!r.url.includes(`type=${type}`)) return false;
      if (field != null && !r.url.includes(`field=${encodeURIComponent(field)}`)) return false;
      return true;
    };

    it('should call API with COUNT type and return count', (done) => {
      service.getV4Analytics(apiId, { type: 'COUNT', from, to }).subscribe((result) => {
        expect(result).toEqual({ type: 'COUNT', count: 42 });
        done();
      });

      const req = httpTestingController.expectOne((r) => urlIncludes(r, 'COUNT'));
      expect(req.request.url).toContain(`from=${from}`);
      expect(req.request.url).toContain(`to=${to}`);
      req.flush({ type: 'COUNT', count: 42 });
    });

    it('should call API with STATS type and field', (done) => {
      const statsResponse = {
        type: 'STATS',
        count: 10,
        min: 1,
        max: 100,
        avg: 50.5,
        sum: 505,
      };
      service
        .getV4Analytics(apiId, { type: 'STATS', from, to, field: 'gateway-response-time-ms' })
        .subscribe((result) => {
          expect(result).toEqual(statsResponse);
          done();
        });

      const req = httpTestingController.expectOne((r) => urlIncludes(r, 'STATS', 'gateway-response-time-ms'));
      req.flush(statsResponse);
    });

    it('should call API with GROUP_BY type, field and size', (done) => {
      const groupByResponse = { type: 'GROUP_BY', values: { '200': 80, '404': 10 }, metadata: {} };
      service
        .getV4Analytics(apiId, { type: 'GROUP_BY', from, to, field: 'status', size: 10 })
        .subscribe((result) => {
          expect(result).toEqual(groupByResponse);
          done();
        });

      const req = httpTestingController.expectOne((r) => urlIncludes(r, 'GROUP_BY', 'status') && r.url.includes('size=10'));
      req.flush(groupByResponse);
    });

    it('should call API with DATE_HISTO type, field and interval', (done) => {
      const dateHistoResponse = {
        type: 'DATE_HISTO',
        timestamp: [1000, 2000],
        values: [{ field: 'status', buckets: [5, 10], metadata: {} }],
      };
      service
        .getV4Analytics(apiId, { type: 'DATE_HISTO', from, to, field: 'status', interval: 3600000 })
        .subscribe((result) => {
          expect(result).toEqual(dateHistoResponse);
          done();
        });

      const req = httpTestingController.expectOne(
        (r) => urlIncludes(r, 'DATE_HISTO', 'status') && r.url.includes('interval=3600000'),
      );
      req.flush(dateHistoResponse);
    });

    it('should propagate error on HTTP failure', (done) => {
      service.getV4Analytics(apiId, { type: 'COUNT', from, to }).subscribe({
        next: () => fail('should have failed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      });

      const req = httpTestingController.expectOne((r) => urlIncludes(r, 'COUNT'));
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
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
