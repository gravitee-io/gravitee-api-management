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

import { NotificationSettingsService } from './notification-settings.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';

describe('NotificationSettings', () => {
  let httpTestingController: HttpTestingController;
  let notificationSettingsService: NotificationSettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    notificationSettingsService = TestBed.inject<NotificationSettingsService>(NotificationSettingsService);
  });

  describe('delete', () => {
    it('should call the API', (done) => {
      notificationSettingsService.delete('123', '456').subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/apis/123/notificationsettings/456`,
        })
        .flush(null);
    });
  });
});
