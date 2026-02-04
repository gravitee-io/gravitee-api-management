/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FacetsRequest } from './components/widget/model/request/facets-request';
import { MeasuresRequest } from './components/widget/model/request/measures-request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { RequestType } from './components/widget/model/widget/widget.model';
import { GraviteeDashboardService } from './gravitee-dashboard.service';

describe('GraviteeDashboardService', () => {
  let service: GraviteeDashboardService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GraviteeDashboardService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getWidgets', () => {
    it('should return an array of widgets', () => {
      const widgets = service.getWidgets();

      expect(widgets).toBeDefined();
      expect(Array.isArray(widgets)).toBe(true);
      expect(widgets.length).toBeGreaterThan(0);
    });

    it('should return widgets with required properties', () => {
      const widgets = service.getWidgets();

      widgets.forEach(widget => {
        expect(widget).toHaveProperty('id');
        expect(widget).toHaveProperty('title');
        expect(widget).toHaveProperty('type');
        expect(widget).toHaveProperty('layout');
        expect(widget).toHaveProperty('request');
        expect(widget.layout).toHaveProperty('cols');
        expect(widget.layout).toHaveProperty('rows');
        expect(widget.layout).toHaveProperty('x');
        expect(widget.layout).toHaveProperty('y');
        expect(widget.request).toHaveProperty('type');
      });
    });
  });

  describe('getMetrics', () => {
    it('should make POST request for measures endpoint', done => {
      const basePath = 'http://test.api';
      const endpoint = 'measures';
      const request: MeasuresRequest = {
        type: 'measures',
        timeRange: {
          from: '2025-01-01T00:00:00Z',
          to: '2025-01-31T23:59:59Z',
        },
        metrics: [],
      };
      const mockResponse: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [{ name: 'COUNT', value: 100 }],
          },
        ],
      };

      service.getMetrics(basePath, endpoint, request).subscribe(response => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${basePath}/analytics/${endpoint}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should make POST request for facets endpoint', done => {
      const basePath = 'http://test.api';
      const endpoint = 'facets';
      const request: FacetsRequest = {
        type: 'facets',
        timeRange: {
          from: '2025-01-01T00:00:00Z',
          to: '2025-01-31T23:59:59Z',
        },
        by: ['HTTP_STATUS_CODE_GROUP'],
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: ['COUNT'],
          },
        ],
      };
      const mockResponse: FacetsResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            buckets: [
              {
                key: '2xx',
                name: '2xx',
                measures: [{ name: 'COUNT', value: 1000 }],
              },
              {
                key: '4xx',
                name: '4xx',
                measures: [{ name: 'COUNT', value: 50 }],
              },
            ],
          },
        ],
      };

      service.getMetrics(basePath, endpoint, request).subscribe(response => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${basePath}/analytics/${endpoint}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should handle HTTP error responses', done => {
      const basePath = 'http://test.api';
      const endpoint = 'measures';
      const request: MeasuresRequest = {
        type: 'measures',
        timeRange: {
          from: '2025-01-01T00:00:00Z',
          to: '2025-01-31T23:59:59Z',
        },
        metrics: [],
      };
      const errorStatus = 500;
      const errorMessage = 'Internal Server Error';

      service.getMetrics(basePath, endpoint, request).subscribe({
        next: () => fail('should have failed with 500 error'),
        error: (error: Error) => {
          expect(error).toBeDefined();
          done();
        },
      });

      const req = httpTestingController.expectOne(`${basePath}/analytics/${endpoint}`);
      expect(req.request.method).toBe('POST');
      req.flush(errorMessage, { status: errorStatus, statusText: errorMessage });
    });

    it('should throw error for unsupported endpoint', () => {
      const basePath = 'http://test.api';
      const endpoint = 'unsupported' as unknown as RequestType;
      const request: MeasuresRequest = {
        type: 'measures',
        timeRange: {
          from: '2025-01-01T00:00:00Z',
          to: '2025-01-31T23:59:59Z',
        },
        metrics: [],
      };

      expect(() => {
        service.getMetrics(basePath, endpoint, request);
      }).toThrow('Endpoint unsupported not supported');
    });
  });
});
