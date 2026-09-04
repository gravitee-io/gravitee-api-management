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

  it('should compose subscription form from navigation item content and resolved options', () => {
    const apiId = 'api-123';
    const navigationItemId = 'nav-item-1';
    const gmdContent = '<gmd-select fieldkey="country" options="France,Spain"></gmd-select>';
    const resolvedOptions = { country: ['France', 'Spain'] };

    service.getSubscriptionForm(apiId).subscribe(form => {
      expect(form).toEqual({ gmdContent, resolvedOptions });
    });

    const listReq = httpMock.expectOne(`${baseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`);
    expect(listReq.request.method).toBe('GET');
    listReq.flush([{ id: navigationItemId }]);

    const contentReq = httpMock.expectOne(`${baseURL}/portal-navigation-items/${navigationItemId}/content`);
    expect(contentReq.request.method).toBe('GET');
    contentReq.flush({ type: 'GRAVITEE_MARKDOWN', content: gmdContent });

    const optionsReq = httpMock.expectOne(`${baseURL}/apis/${apiId}/subscription-form`);
    expect(optionsReq.request.method).toBe('GET');
    optionsReq.flush({ resolvedOptions });
  });

  it('should return null when no SUBSCRIPTION_FORM navigation item exists', () => {
    const apiId = 'api-no-form';

    service.getSubscriptionForm(apiId).subscribe(form => {
      expect(form).toBeNull();
    });

    const listReq = httpMock.expectOne(`${baseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`);
    listReq.flush([]);
  });

  it('should return null when the navigation item content call responds 404', () => {
    const apiId = 'api-123';
    const navigationItemId = 'nav-item-1';

    service.getSubscriptionForm(apiId).subscribe(form => {
      expect(form).toBeNull();
    });

    httpMock.expectOne(`${baseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`).flush([{ id: navigationItemId }]);
    httpMock
      .expectOne(`${baseURL}/portal-navigation-items/${navigationItemId}/content`)
      .flush(null, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne(`${baseURL}/apis/${apiId}/subscription-form`).flush({ resolvedOptions: {} });
  });

  it('should return null when the resolved-options call responds 404', () => {
    const apiId = 'api-404';
    const navigationItemId = 'nav-item-1';

    service.getSubscriptionForm(apiId).subscribe(form => {
      expect(form).toBeNull();
    });

    httpMock.expectOne(`${baseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`).flush([{ id: navigationItemId }]);
    httpMock
      .expectOne(`${baseURL}/portal-navigation-items/${navigationItemId}/content`)
      .flush({ type: 'GRAVITEE_MARKDOWN', content: 'hello' });
    httpMock.expectOne(`${baseURL}/apis/${apiId}/subscription-form`).flush(null, { status: 404, statusText: 'Not Found' });
  });
});
