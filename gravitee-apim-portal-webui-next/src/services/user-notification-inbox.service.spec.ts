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

import { UserNotificationInboxService } from './user-notification-inbox.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('UserNotificationInboxService', () => {
  let service: UserNotificationInboxService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(UserNotificationInboxService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('loads the unread count from the notifications API', () => {
    service.refresh();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=1&size=1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      data: [{ id: 'n1', title: 'Subscription Accepted', message: 'Accepted', created_at: '2026-09-10T08:00:00Z' }],
      metadata: { pagination: { current_page: 1, total: 4 } },
    });

    expect(service.unreadCount()).toBe(4);
  });
});
