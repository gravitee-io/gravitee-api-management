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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { EnvironmentLogsService, SearchLogsResponse } from './environment-logs.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing/gio-testing.module';

describe('EnvironmentLogsService', () => {
  let service: EnvironmentLogsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(EnvironmentLogsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('searchLogs', () => {
    it('should POST to /logs/search with default pagination', done => {
      const mockResponse: SearchLogsResponse = {
        data: [
          {
            apiId: 'api-1',
            timestamp: '2025-01-15T10:30:00Z',
            id: 'log-1',
            requestId: 'req-1',
            method: 'GET',
            status: 200,
            requestEnded: true,
            gatewayResponseTime: 42,
            gateway: 'Gateway 1',
            uri: '/my-api',
          },
        ],
        pagination: {
          page: 1,
          perPage: 10,
          pageCount: 1,
          pageItemsCount: 1,
          totalCount: 1,
        },
      };

      service.searchLogs().subscribe(response => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=1&perPage=10`,
      });
      expect(req.request.body).toEqual(
        expect.objectContaining({
          timeRange: expect.objectContaining({
            from: expect.any(String),
            to: expect.any(String),
          }),
        }),
      );
      req.flush(mockResponse);
    });

    it('should POST to /logs/search with custom pagination', done => {
      const mockResponse: SearchLogsResponse = {
        data: [],
        pagination: {
          page: 3,
          perPage: 25,
          pageCount: 0,
          pageItemsCount: 0,
          totalCount: 0,
        },
      };

      service.searchLogs({ page: 3, perPage: 25 }).subscribe(response => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=3&perPage=25`,
      });
      expect(req.request.body).toEqual(
        expect.objectContaining({
          timeRange: expect.objectContaining({
            from: expect.any(String),
            to: expect.any(String),
          }),
        }),
      );
      req.flush(mockResponse);
    });
  });
});
