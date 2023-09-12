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

import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { UIRouterModule } from '@uirouter/angular';

import { NotificationsListModule } from './notifications-list.module';
import { NotificationsListComponent } from './notifications-list.component';

import { fakeNotificationTemplate } from '../../../../entities/notification/notificationTemplate.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { AjsRootScope, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';

describe('NotificationsListComponent', () => {
  let fixture: ComponentFixture<NotificationsListComponent>;
  const API_ID = 'apiId';
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('notification table test', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          GioHttpTestingModule,
          GioUiRouterTestingModule,
          NotificationsListModule,
          UIRouterModule.forRoot({
            useHash: true,
          }),
        ],
        providers: [
          { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
          { provide: AjsRootScope, useValue: null },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(NotificationsListComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    });

    it('should display an empty table', fakeAsync(async () => {
      const table = await loader.getHarness(MatTableHarness.with({ selector: '#notificationsTable' }));
      expect(await table.getCellTextByIndex()).toEqual([['Loading...']]);

      expectApiGetNotificationList([]);

      expect(await table.getCellTextByIndex()).toEqual([['No notifications to display.']]);
    }));

    it('should display a table with notifications', fakeAsync(async () => {
      const table = await loader.getHarness(MatTableHarness.with({ selector: '#notificationsTable' }));
      expect(await table.getCellTextByIndex()).toEqual([['Loading...']]);

      const notifications = [fakeNotificationTemplate({ name: 'Test name' })];
      expectApiGetNotificationList(notifications);

      expect(await table.getCellTextByIndex()).toEqual([['Test name']]);
    }));
  });

  function expectApiGetNotificationList(notifications) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings`,
        method: 'GET',
      })
      .flush(notifications);
    fixture.detectChanges();
  }
});
