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

import { AnalyticsService } from './analytics.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { AnalyticsCountResponse, AnalyticsGroupByResponse, AnalyticsStatsResponse } from '../entities/analytics/analyticsResponse';
import { AnalyticsRequestParam } from '../entities/analytics/analyticsRequestParam';

describe('AnalyticsService', () => {
  let httpTestingController: HttpTestingController;
  let analyticsService: AnalyticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    analyticsService = TestBed.inject<AnalyticsService>(AnalyticsService);
  });

  describe('getStats', () => {
    it('should call the API for STATS analytics', done => {
      const fromTimestamp = 1677339613055;
      const toTimestamp = 1679931613055;
      const interval = 86400000;
      const field = 'response-time';

      const fakeAnalytics: AnalyticsStatsResponse = {
        count: 44079.0,
        min: 0.0,
        max: 338.0,
        avg: 0.991243,
        sum: 43693.0,
        rps: 0.017005786,
        rpm: 1.0203471,
        rph: 61.22083,
      };
      const params: AnalyticsRequestParam = {
        field,
        interval,
        from: fromTimestamp,
        to: toTimestamp,
      };
      analyticsService.getGroupBy(params).subscribe(response => {
        expect(response).toMatchObject(fakeAnalytics);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=${field}&interval=${interval}&from=${fromTimestamp}&to=${toTimestamp}`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(fakeAnalytics);
    });

    it('should call the API for GROUP_BY analytics', done => {
      const fromTimestamp = 1677339613055;
      const toTimestamp = 1679931613055;
      const interval = 86400000;
      const field = 'api';

      const fakeAnalytics: AnalyticsGroupByResponse = {
        values: {
          blue: 12,
          yellow: 5,
        },
        metadata: {
          blue: {
            name: 'BLUE',
            order: '1',
          },
          yellow: {
            name: 'YELLOW',
            order: '2',
          },
        },
      };
      const params: AnalyticsRequestParam = {
        field,
        interval,
        from: fromTimestamp,
        to: toTimestamp,
      };
      analyticsService.getGroupBy(params).subscribe(response => {
        expect(response).toMatchObject(fakeAnalytics);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=${field}&interval=${interval}&from=${fromTimestamp}&to=${toTimestamp}`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(fakeAnalytics);
    });

    it('should call the API for COUNT analytics', done => {
      const fromTimestamp = 1677339613055;
      const toTimestamp = 1679931613055;
      const interval = 86400000;
      const field = 'application';

      const fakeAnalytics: AnalyticsCountResponse = {
        count: 1234,
      };
      const params: AnalyticsRequestParam = {
        field,
        interval,
        from: fromTimestamp,
        to: toTimestamp,
      };
      analyticsService.getCount(params).subscribe(response => {
        expect(response).toMatchObject(fakeAnalytics);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/analytics?type=count&field=${field}&interval=${interval}&from=${fromTimestamp}&to=${toTimestamp}`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(fakeAnalytics);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
