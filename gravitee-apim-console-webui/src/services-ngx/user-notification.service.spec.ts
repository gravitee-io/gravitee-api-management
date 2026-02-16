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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserNotificationService } from './user-notification.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeUserNotification } from '../entities/user-notification/userNotification.fixture';

describe('UserNotification', () => {
  let httpTestingController: HttpTestingController;
  let userNotificationService: UserNotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    userNotificationService = TestBed.inject<UserNotificationService>(UserNotificationService);
  });

  describe('getNotifications', () => {
    it('should call the API', done => {
      const userNotifications = [fakeUserNotification()];

      userNotificationService.getNotifications().subscribe(response => {
        expect(response).toStrictEqual(userNotifications);
        done();
      });

      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/user/notifications` })
        .flush(userNotifications);
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      userNotificationService.delete('12345').subscribe(() => done());

      httpTestingController.expectOne({ method: 'DELETE', url: `${CONSTANTS_TESTING.org.baseURL}/user/notifications/12345` }).flush(null);
    });
  });

  describe('deleteAll', () => {
    it('should call the API', done => {
      userNotificationService.deleteAll().subscribe(() => done());

      httpTestingController.expectOne({ method: 'DELETE', url: `${CONSTANTS_TESTING.org.baseURL}/user/notifications` }).flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
