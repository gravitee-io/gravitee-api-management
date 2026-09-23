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

import { UserNotificationService } from './user-notification.service';
import { fakePortalNotificationsResponse } from '../entities/notification/portal-notification.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('UserNotificationService', () => {
  let service: UserNotificationService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(UserNotificationService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('lists current user notifications with pagination', done => {
    const response = fakePortalNotificationsResponse();

    service.list(2, 25).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=2&size=25`);
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('marks a notification as read', done => {
    service.markAsRead('notification-1').subscribe(() => done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/notification-1`);
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
  });

  it('deletes all notifications', done => {
    service.deleteAll().subscribe(() => done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications`);
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
  });
});
