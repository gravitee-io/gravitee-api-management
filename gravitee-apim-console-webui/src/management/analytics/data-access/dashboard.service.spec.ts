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

  describe('sort by labels', () => {
    it('should sort by labels ascending', done => {
      service.list(undefined, { active: 'labels', direction: 'asc' }, 1, 100).subscribe(result => {
        const labels = result.data.map(d => d.labels);
        // Items without labels (empty string) should come first in ascending order
        const withoutLabels = labels.filter(l => !l || Object.keys(l).length === 0);
        const withLabels = labels.filter(l => l && Object.keys(l).length > 0);
        expect(withoutLabels.length + withLabels.length).toBe(labels.length);

        // Verify items with labels are sorted
        const labelStrings = withLabels.map(l =>
          Object.entries(l)
            .map(([k, v]) => `${k}:${v}`)
            .sort()
            .join(','),
        );
        for (let i = 1; i < labelStrings.length; i++) {
          expect(labelStrings[i] >= labelStrings[i - 1]).toBeTruthy();
        }
        done();
      });
    });

    it('should sort by labels descending', done => {
      service.list(undefined, { active: 'labels', direction: 'desc' }, 1, 100).subscribe(result => {
        const labels = result.data.map(d => d.labels);
        const labelStrings = labels
          .filter(l => l && Object.keys(l).length > 0)
          .map(l =>
            Object.entries(l)
              .map(([k, v]) => `${k}:${v}`)
              .sort()
              .join(','),
          );
        for (let i = 1; i < labelStrings.length; i++) {
          expect(labelStrings[i] <= labelStrings[i - 1]).toBeTruthy();
        }
        done();
      });
    });

    it('should handle items without labels when sorting', done => {
      service.list(undefined, { active: 'labels', direction: 'asc' }, 1, 100).subscribe(result => {
        expect(result.data.length).toBeGreaterThan(0);
        // Should not throw an error and should return all items
        const itemsWithLabels = result.data.filter(d => d.labels && Object.keys(d.labels).length > 0);
        const itemsWithoutLabels = result.data.filter(d => !d.labels || Object.keys(d.labels).length === 0);
        expect(itemsWithLabels.length).toBeGreaterThan(0);
        expect(itemsWithoutLabels.length).toBeGreaterThan(0);
        done();
      });
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
    it('should extract name, labels and widgets from template initialConfig', () => {
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

      expect(result.name).toBe('Dashboard from Template');
      expect(result.labels).toEqual({ Focus: 'HTTP' });
      expect(result.widgets).toHaveLength(1);
      // Should NOT contain template-specific fields
      expect(result).not.toHaveProperty('previewImage');
      expect(result).not.toHaveProperty('shortDescription');
      expect(result).not.toHaveProperty('description');
    });

    it('should fallback to template name if initialConfig.name is not set', () => {
      const template: DashboardTemplate = {
        id: 'tpl-2',
        name: 'Fallback Name',
        shortDescription: 'Short desc',
        description: 'Long desc',
        previewImage: 'assets/preview.png',
        initialConfig: {
          labels: { Theme: 'AI' },
        },
      };

      const result = service.toCreateDashboard(template);

      expect(result.name).toBe('Fallback Name');
      expect(result.labels).toEqual({ Theme: 'AI' });
      expect(result.widgets).toEqual([]);
    });
  });
});

