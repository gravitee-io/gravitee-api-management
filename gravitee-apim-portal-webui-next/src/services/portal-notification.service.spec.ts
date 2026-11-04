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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ConfigService } from './config.service';
import { PortalNotificationService } from './portal-notification.service';
import { fakePortalNotificationFilters, fakePortalNotificationsResponse } from '../entities/notification';
import { TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PortalNotificationService', () => {
  let service: PortalNotificationService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } }],
    });
    service = TestBed.inject(PortalNotificationService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should request a page of notifications without optional filters', done => {
      const response = fakePortalNotificationsResponse();
      service.list({ page: 2, size: 10 }).subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=2&size=10`);
      expect(req.request.method).toEqual('GET');
      req.flush(response);
    });

    it('should send read, apiId and applicationId when provided', done => {
      service.list({ page: 1, size: 20, read: false, apiId: 'api-1', applicationId: 'app-1' }).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${TESTING_BASE_URL}/user/notifications?page=1&size=20&read=false&apiId=api-1&applicationId=app-1`,
      );
      expect(req.request.method).toEqual('GET');
      req.flush(fakePortalNotificationsResponse([]));
    });
  });

  it('should get the filter options', done => {
    const filters = fakePortalNotificationFilters();
    service.getFilters().subscribe(result => {
      expect(result).toEqual(filters);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/_filters`);
    expect(req.request.method).toEqual('GET');
    req.flush(filters);
  });

  it('should mark one notification as read', done => {
    service.markAsRead('notification-1').subscribe(() => done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/notification-1/_read`);
    expect(req.request.method).toEqual('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should encode the notification id in the mark as read URL', done => {
    service.markAsRead('id/with space').subscribe(() => done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/id%2Fwith%20space/_read`);
    expect(req.request.method).toEqual('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should mark all notifications as read', done => {
    service.markAllAsRead().subscribe(() => done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/_read`);
    expect(req.request.method).toEqual('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
