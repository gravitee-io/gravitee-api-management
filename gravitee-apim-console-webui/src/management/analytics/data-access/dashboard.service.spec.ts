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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { DashboardService } from './dashboard.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { DashboardTemplate } from './templates/dashboard-template.model';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    service = TestBed.inject(DashboardService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('list', () => {
    it('should GET dashboards with page and perPage query params', done => {
      const mockResponse = {
        data: [
          { id: '1', name: 'Dashboard 1', createdBy: 'Admin', createdAt: '2025-01-01', lastModified: '2025-01-01', labels: {}, widgets: [] },
          { id: '2', name: 'Dashboard 2', createdBy: 'Admin', createdAt: '2025-01-01', lastModified: '2025-01-01', labels: {}, widgets: [] },
        ],
        pagination: { page: 1, perPage: 10, totalCount: 2, pageItemsCount: 2, pageCount: 1 },
        links: {},
      };

      service.list(1, 10).subscribe(result => {
        expect(result.data).toHaveLength(2);
        expect(result.data[0].name).toBe('Dashboard 1');
        expect(result.pagination.totalCount).toBe(2);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/analytics/dashboards?page=1&perPage=10`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should pass correct pagination params', done => {
      const mockResponse = { data: [], pagination: { page: 3, perPage: 5, totalCount: 0, pageItemsCount: 0, pageCount: 0 }, links: {} };

      service.list(3, 5).subscribe(result => {
        expect(result.data).toHaveLength(0);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/analytics/dashboards?page=3&perPage=5`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('create', () => {
    it('should POST to the correct URL with the dashboard payload', done => {
      const payload = { name: 'Test Dashboard', labels: { env: 'prod' }, widgets: [] };
      const mockResponse = { id: 'new-id', name: 'Test Dashboard', createdBy: 'Admin', createdAt: '2025-01-01', lastModified: '2025-01-01', labels: { env: 'prod' }, widgets: [] };

      service.create(payload).subscribe(result => {
        expect(result).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/analytics/dashboards`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockResponse);
    });
  });

  describe('getById', () => {
    it('should GET the correct URL with the dashboard id', done => {
      const mockResponse = { id: 'abc-123', name: 'My Dashboard', createdBy: 'Admin', createdAt: '2025-01-01', lastModified: '2025-01-01', labels: {}, widgets: [] };

      service.getById('abc-123').subscribe(result => {
        expect(result).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/analytics/dashboards/abc-123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('toCreateDashboard', () => {
    it('should use template name with locale date for dashboard name', () => {
      const template: DashboardTemplate = {
        id: 'tpl-1',
        name: 'Template Name',
        shortDescription: 'Short desc',
        description: 'Long desc',
        previewImage: 'assets/preview.png',
        initialConfig: {
          name: 'Dashboard from Template',
          labels: { Focus: 'HTTP' },
          widgets: [{ id: 'w1', title: 'Widget 1', type: 'stats', layout: { cols: 1, rows: 1, x: 0, y: 0 }, request: { type: 'measures', metrics: [] } }],
        },
      };

      const result = service.toCreateDashboard(template);

      expect(result.name).toContain('Template Name - ');
      expect(result.labels).toEqual({ Focus: 'HTTP' });
      expect(result.widgets).toHaveLength(1);
      // Should NOT contain template-specific fields
      expect(result).not.toHaveProperty('previewImage');
      expect(result).not.toHaveProperty('shortDescription');
      expect(result).not.toHaveProperty('description');
    });

    it('should inject default timeRange on widget requests', () => {
      const template: DashboardTemplate = {
        id: 'tpl-2',
        name: 'Fallback Name',
        shortDescription: 'Short desc',
        description: 'Long desc',
        previewImage: 'assets/preview.png',
        initialConfig: {
          labels: { Theme: 'AI' },
          widgets: [{ id: 'w1', title: 'Widget', type: 'stats', layout: { cols: 1, rows: 1, x: 0, y: 0 }, request: { type: 'measures', metrics: [] } }],
        },
      };

      const result = service.toCreateDashboard(template);

      expect(result.widgets[0].request.timeRange).toBeDefined();
      expect(result.widgets[0].request.timeRange.from).toBeDefined();
      expect(result.widgets[0].request.timeRange.to).toBeDefined();
    });

    it('should inject default interval on time-series widget requests', () => {
      const template: DashboardTemplate = {
        id: 'tpl-3',
        name: 'Time Series',
        shortDescription: 'Short desc',
        description: 'Long desc',
        previewImage: 'assets/preview.png',
        initialConfig: {
          labels: {},
          widgets: [{ id: 'w1', title: 'Widget', type: 'line', layout: { cols: 1, rows: 1, x: 0, y: 0 }, request: { type: 'time-series', metrics: [], by: [] } }],
        },
      };

      const result = service.toCreateDashboard(template);

      expect((result.widgets[0].request as any).interval).toBe(10000);
    });

    it('should handle template with no widgets', () => {
      const template: DashboardTemplate = {
        id: 'tpl-4',
        name: 'Empty',
        shortDescription: 'Short desc',
        description: 'Long desc',
        previewImage: 'assets/preview.png',
        initialConfig: {
          labels: {},
        },
      };

      const result = service.toCreateDashboard(template);

      expect(result.widgets).toEqual([]);
    });
  });
});
