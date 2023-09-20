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
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { NotificationsListModule } from './notifications-list.module';
import { NotificationsListComponent } from './notifications-list.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { User } from '../../../../entities/user';
import { NotificationSettings } from '../../../../entities/notification/notificationSettings';
import { fakeNotificationSettings } from '../../../../entities/notification/notificationSettings.fixture';

describe('NotificationsListComponent', () => {
  let fixture: ComponentFixture<NotificationsListComponent>;
  const API_ID = 'apiId';
  const currentUser = new User();
  currentUser.userPermissions = ['api-notification-u', 'api-notification-d', 'api-notification-c'];
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  describe('notification table test', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          GioHttpTestingModule,
          GioUiRouterTestingModule,
          NotificationsListModule,
          MatIconTestingModule,
          UIRouterModule.forRoot({
            useHash: true,
          }),
        ],
        providers: [
          { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
          { provide: AjsRootScope, useValue: null },
          { provide: CurrentUserService, useValue: { currentUser } },
        ],
      })
        .overrideProvider(InteractivityChecker, {
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        })
        .compileComponents();

      fixture = TestBed.createComponent(NotificationsListComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      loader = TestbedHarnessEnvironment.loader(fixture);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      fixture.detectChanges();
    });

    afterEach(() => {
      httpTestingController.verify();
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

      const notifications = [fakeNotificationSettings({ name: 'Test name' })];
      expectApiGetNotificationList(notifications);

      expect(await table.getCellTextByIndex()).toEqual([['Test name', '']]);
    }));

    it('should delete the notification', fakeAsync(async () => {
      const table = [fakeNotificationSettings({ name: 'Test name', id: 'test id' })];
      expectApiGetNotificationList(table);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Delete notification"]` }));
      await button.click();

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings/${table[0].id}`,
        method: 'DELETE',
      });
    }));
  });

  function expectApiGetNotificationList(notifactionSettings: NotificationSettings[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings`,
        method: 'GET',
      })
      .flush(notifactionSettings);
    fixture.detectChanges();
  }
});
