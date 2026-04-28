/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { AnalyticsDashboardService } from './analytics-dashboard.service';
import { fakeDashboard, fakeAnalyticsDashboardsResponse } from '../entities/analytics-dashboard/analytics-dashboard.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AnalyticsDashboardService', () => {
  let service: AnalyticsDashboardService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(AnalyticsDashboardService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should_return_dashboards_with_default_page_and_size', done => {
      const response = fakeAnalyticsDashboardsResponse();

      service.list().subscribe(result => {
        expect(result).toMatchObject(response);
        done();
      });

      const req = httpTestingController.expectOne(
        r => r.url === `${TESTING_BASE_URL}/analytics/dashboards` && r.params.get('page') === '1' && r.params.get('size') === '10',
      );
      expect(req.request.method).toEqual('GET');
      req.flush(response);
    });

    it('should_return_dashboards_with_specified_page_and_size', done => {
      const response = fakeAnalyticsDashboardsResponse();

      service.list(2, 20).subscribe(result => {
        expect(result).toMatchObject(response);
        done();
      });

      const req = httpTestingController.expectOne(
        r => r.url === `${TESTING_BASE_URL}/analytics/dashboards` && r.params.get('page') === '2' && r.params.get('size') === '20',
      );
      expect(req.request.method).toEqual('GET');
      req.flush(response);
    });
  });

  describe('getById', () => {
    it('should_return_single_dashboard', done => {
      const dashboard = fakeDashboard({ id: 'my-dashboard' });

      service.getById('my-dashboard').subscribe(result => {
        expect(result).toMatchObject(dashboard);
        done();
      });

      const req = httpTestingController.expectOne(r => r.url === `${TESTING_BASE_URL}/analytics/dashboards/my-dashboard`);
      expect(req.request.method).toEqual('GET');
      req.flush(dashboard);
    });
  });
});
