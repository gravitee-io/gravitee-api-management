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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ConfigService } from './config.service';
import { PortalService } from './portal.service';
import { ApiInformation } from '../entities/api/api-information';
import { PortalPage } from '../entities/portal/portal-page';

describe('PortalService', () => {
  let service: PortalService;
  let httpMock: HttpTestingController;

  const baseURL = 'http://localhost:3000';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PortalService, { provide: ConfigService, useValue: { baseURL } }],
    });

    service = TestBed.inject(PortalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET api informations for a given API id', () => {
    const apiId = 'api-123';
    const mockResponse: ApiInformation[] = [{ key: 'version', value: '1.0.0' } as unknown as ApiInformation];

    service.getApiInformations(apiId).subscribe(data => {
      expect(data).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${baseURL}/apis/${apiId}/informations`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should GET portal homepage with expands=CONTENT by default', () => {
    const mockPages: PortalPage[] = [
      {
        id: 'home-1',
        type: 'GRAVITEE_MARKDOWN',
        content: 'Hello',
        context: 'HOME',
        published: true,
      } as PortalPage,
    ];

    service.getPortalHomepages('CONTENT').subscribe(data => {
      expect(data).toEqual(mockPages);
    });

    const req = httpMock.expectOne(`${baseURL}/portal-pages?type=HOMEPAGE&expands=CONTENT`);
    expect(req.request.method).toBe('GET');
    req.flush({ pages: mockPages });
  });

  it('should GET portal homepage without expands=CONTENT by default', () => {
    const mockPages: PortalPage[] = [
      {
        id: 'home-1',
        type: 'GRAVITEE_MARKDOWN',
        content: 'Hello',
        context: 'HOME',
        published: true,
      } as PortalPage,
    ];

    service.getPortalHomepages().subscribe(data => {
      expect(data).toEqual(mockPages);
    });

    const req = httpMock.expectOne(`${baseURL}/portal-pages?type=HOMEPAGE`);
    expect(req.request.method).toBe('GET');
    req.flush({ pages: mockPages });
  });
});
