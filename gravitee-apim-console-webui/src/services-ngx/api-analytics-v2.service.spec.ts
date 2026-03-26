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
import { fakeAnalyticsCount } from '../entities/management-api-v2/analytics/analyticsCount.fixture';
import { fakeAnalyticsStats } from '../entities/management-api-v2/analytics/analyticsStats.fixture';
import { fakeAnalyticsGroupBy } from '../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { fakeAnalyticsDateHisto } from '../entities/management-api-v2/analytics/analyticsDateHisto.fixture';
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

  // ----- Unified endpoint tests -----

  describe('getCount', () => {
    it('should call the unified analytics endpoint with type=COUNT', (done) => {
      service.getCount(apiId).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsCount());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((req) => {
        return req.method === 'GET' && req.url.startsWith(url) && req.url.includes('type=COUNT');
      });
      req.flush(fakeAnalyticsCount());
    });
  });

  describe('getStats', () => {
    it('should call the unified analytics endpoint with type=STATS and field', (done) => {
      service.getStats(apiId, 'gateway-response-time-ms').subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsStats());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((req) => {
        return (
          req.method === 'GET' &&
          req.url.startsWith(url) &&
          req.url.includes('type=STATS') &&
          req.url.includes('field=gateway-response-time-ms')
        );
      });
      req.flush(fakeAnalyticsStats());
    });
  });

  describe('getGroupBy', () => {
    it('should call the unified analytics endpoint with type=GROUP_BY and field', (done) => {
      service.getGroupBy(apiId, 'status').subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsGroupBy());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((req) => {
        return req.method === 'GET' && req.url.startsWith(url) && req.url.includes('type=GROUP_BY') && req.url.includes('field=status');
      });
      req.flush(fakeAnalyticsGroupBy());
    });

    it('should include size parameter when provided', (done) => {
      service.getGroupBy(apiId, 'status', 5).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsGroupBy());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((req) => {
        return req.method === 'GET' && req.url.startsWith(url) && req.url.includes('type=GROUP_BY') && req.url.includes('size=5');
      });
      req.flush(fakeAnalyticsGroupBy());
    });
  });

  describe('getDateHisto', () => {
    it('should call the unified analytics endpoint with type=DATE_HISTO, field, and interval', (done) => {
      service.getDateHisto(apiId, 'status', 3600000).subscribe((result) => {
        expect(result).toEqual(fakeAnalyticsDateHisto());
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics`;
      const req = httpTestingController.expectOne((req) => {
        return (
          req.method === 'GET' &&
          req.url.startsWith(url) &&
          req.url.includes('type=DATE_HISTO') &&
          req.url.includes('field=status') &&
          req.url.includes('interval=3600000')
        );
      });
      req.flush(fakeAnalyticsDateHisto());
    });
  });

  // ----- Legacy endpoint tests -----

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
