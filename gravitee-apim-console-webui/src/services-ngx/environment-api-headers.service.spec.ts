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
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { EnvironmentApiHeadersService } from './environment-api-headers.service';

import { CONSTANTS_TESTING } from '../shared/testing';
import { fakeApiPortalHeaders } from '../entities/api-portal-headers/api-portal-headers.fixture';
import { ApiPortalHeader } from '../entities/apiPortalHeader';
import { Constants } from '../entities/Constants';

describe('environment api headers service', () => {
  let httpTestingController: HttpTestingController;
  let environmentApiHeadersService: EnvironmentApiHeadersService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    environmentApiHeadersService = TestBed.inject(EnvironmentApiHeadersService);
  });

  it('should be created', () => {
    expect(environmentApiHeadersService).toBeTruthy();
  });

  describe('getApiHeaders', () => {
    it('should call the API', (done) => {
      const fakeHeaders = [fakeApiPortalHeaders()];
      environmentApiHeadersService.getApiHeaders().subscribe((headers: ApiPortalHeader[]) => {
        expect(headers).toMatchObject(fakeHeaders);
        done();
      });
      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` })
        .flush(fakeHeaders);
    });
  });

  describe('createApiHeader', () => {
    it('should call the API', (done) => {
      const headerDialogResult: {
        name: string;
        value: string;
      } = { name: 'test-name', value: 'test-value' };
      environmentApiHeadersService.createApiHeader(headerDialogResult).subscribe(() => {
        done();
      });
      const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` });
      expect(req.request.body).toEqual(headerDialogResult);
      req.flush(null);

      httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` }).flush(null);
    });
  });

  describe('updateApiHeader', () => {
    it('should call the API', (done) => {
      const apiPortalHeader: ApiPortalHeader = fakeApiPortalHeaders();
      environmentApiHeadersService.updateApiHeader(apiPortalHeader).subscribe(() => {
        done();
      });
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/${apiPortalHeader.id}`,
      });
      expect(req.request.body).toEqual({
        name: 'test-name',
        value: 'test-value',
        order: 1,
      });
      req.flush(null);

      httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` }).flush(null);
    });
  });

  describe('deleteApiHeader', () => {
    it('should call the API', (done) => {
      const apiPortalHeader: ApiPortalHeader = fakeApiPortalHeaders();
      environmentApiHeadersService.deleteApiHeader(apiPortalHeader).subscribe(() => {
        done();
      });
      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/${apiPortalHeader.id}`,
      });
      req.flush(null);

      httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` }).flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
