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

import { EnvironmentNotificationSettingsService } from './environment-notification-settings.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeNotifier } from '../entities/notification/notifier.fixture';
import { fakeNotificationSettings } from '../entities/notification/notificationSettings.fixture';

describe('EnvironmentNotificationSettingsService', () => {
  let httpTestingController: HttpTestingController;
  let notificationSettingsNewService: EnvironmentNotificationSettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    notificationSettingsNewService = TestBed.inject<EnvironmentNotificationSettingsService>(EnvironmentNotificationSettingsService);
  });

  describe('delete', () => {
    it('should call the API', done => {
      notificationSettingsNewService.delete('456').subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/notificationsettings/456`,
        })
        .flush(null);
    });
  });

  describe('get notifiers', () => {
    it('should call the API', done => {
      const notifier = [fakeNotifier({ id: 'notifier-a', name: 'Notifier A' })];

      notificationSettingsNewService.getNotifiers().subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/notifiers`,
        })
        .flush(notifier);
    });
  });

  describe('get notification settings', () => {
    it('should call the API', done => {
      const notificationSettings = [fakeNotificationSettings({ name: 'Test name', id: 'test id' })];

      notificationSettingsNewService.getAll().subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/notificationsettings`,
        })
        .flush(notificationSettings);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const fakeNewNotificationSettings = {
        name: 'test name',
        notifier: 'test notifier',
        config_type: 'test config_type',
        hooks: [],
        referenceType: 'test reference_type',
        referenceId: 'test reference_id',
      };

      notificationSettingsNewService.create(fakeNewNotificationSettings).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/notificationsettings`,
      );
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(fakeNewNotificationSettings);

      req.flush(fakeNewNotificationSettings);
    });
  });
});
