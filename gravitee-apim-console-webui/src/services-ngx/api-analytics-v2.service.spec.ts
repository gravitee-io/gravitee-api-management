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
  fakeCountResponse,
  fakeDateHistoResponse,
  fakeGroupByResponse,
  fakeStatsResponse,
} from '../entities/management-api-v2/analytics/analyticsResponse.fixture';
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

  // ── US-05: getAnalytics — all four type variants ───────────────────────────

  describe('getAnalytics', () => {
    const FROM = 1_728_981_738_000;
    const TO = 1_729_068_138_000;
    const BASE_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;

    describe('type=COUNT', () => {
      it('should serialise COUNT params and return CountResponse', (done) => {
        const expected = fakeCountResponse();

        service.getAnalytics(apiId, { type: 'COUNT', from: FROM, to: TO }).subscribe((result) => {
          expect(result).toEqual(expected);
          done();
        });

        const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === BASE_URL);
        expect(req.request.params.get('type')).toBe('COUNT');
        expect(req.request.params.get('from')).toBe(String(FROM));
        expect(req.request.params.get('to')).toBe(String(TO));
        req.flush(expected);
      });
    });

    describe('type=STATS', () => {
      it('should serialise STATS params including field and return StatsResponse', (done) => {
        const expected = fakeStatsResponse();

        service.getAnalytics(apiId, { type: 'STATS', from: FROM, to: TO, field: 'gateway-response-time-ms' }).subscribe((result) => {
          expect(result).toEqual(expected);
          done();
        });

        const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === BASE_URL);
        expect(req.request.params.get('type')).toBe('STATS');
        expect(req.request.params.get('field')).toBe('gateway-response-time-ms');
        req.flush(expected);
      });
    });

    describe('type=GROUP_BY', () => {
      it('should serialise GROUP_BY params including optional size/order and return GroupByResponse', (done) => {
        const expected = fakeGroupByResponse();

        service
          .getAnalytics(apiId, { type: 'GROUP_BY', from: FROM, to: TO, field: 'status', size: 10, order: 'DESC' })
          .subscribe((result) => {
            expect(result).toEqual(expected);
            done();
          });

        const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === BASE_URL);
        expect(req.request.params.get('type')).toBe('GROUP_BY');
        expect(req.request.params.get('field')).toBe('status');
        expect(req.request.params.get('size')).toBe('10');
        expect(req.request.params.get('order')).toBe('DESC');
        req.flush(expected);
      });
    });

    describe('type=DATE_HISTO', () => {
      it('should serialise DATE_HISTO params including interval and return DateHistoResponse', (done) => {
        const expected = fakeDateHistoResponse();

        service
          .getAnalytics(apiId, { type: 'DATE_HISTO', from: FROM, to: TO, field: 'status', interval: 3_600_000 })
          .subscribe((result) => {
            expect(result).toEqual(expected);
            done();
          });

        const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === BASE_URL);
        expect(req.request.params.get('type')).toBe('DATE_HISTO');
        expect(req.request.params.get('field')).toBe('status');
        expect(req.request.params.get('interval')).toBe('3600000');
        req.flush(expected);
      });
    });

    it('should not include undefined optional params in the query string', (done) => {
      // GROUP_BY without optional size/order
      service.getAnalytics(apiId, { type: 'GROUP_BY', from: FROM, to: TO, field: 'status' }).subscribe(() => done());

      const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === BASE_URL);
      expect(req.request.params.has('size')).toBeFalsy();
      expect(req.request.params.has('order')).toBeFalsy();
      req.flush(fakeGroupByResponse());
    });

    // Backward-compat: assert that all legacy methods are still callable (AC 5)
    it('existing service methods are still present and callable', () => {
      expect(typeof service.getRequestsCount).toBe('function');
      expect(typeof service.getAverageConnectionDuration).toBe('function');
      expect(typeof service.getAverageMessagesPerRequest).toBe('function');
      expect(typeof service.getResponseStatusRanges).toBe('function');
      expect(typeof service.getResponseStatusOvertime).toBe('function');
      expect(typeof service.getResponseTimeOverTime).toBe('function');
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
