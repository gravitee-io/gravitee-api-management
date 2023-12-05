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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { of } from 'rxjs';

import { NotificationSettingsListModule } from './notification-settings-list.module';
import { NotificationSettingsListServices } from './notification-settings-list.component';

import { fakeNotifier } from '../../entities/notification/notifier.fixture';
import { fakeNotificationSettings } from '../../entities/notification/notificationSettings.fixture';
import { CurrentUserService } from '../../ajs-upgraded-providers';
import { GioHttpTestingModule } from '../../shared/testing';
import { User } from '../../entities/user';
import { ApplicationNotificationSettingsService } from '../../services-ngx/application-notification-settings.service';
import { NotificationSettings } from '../../entities/notification/notificationSettings';

@Component({
  selector: 'test-component',
  template: ` <notification-settings-list
    [notificationSettingsListServices]="notificationSettingsListServices"
  ></notification-settings-list>`,
})
class TestComponent {
  notificationSettingsListServices: NotificationSettingsListServices;
}

describe('NotificationsSettingsListComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  const currentUser = new User();
  currentUser.userPermissions = ['api-notification-u', 'api-notification-d', 'api-notification-c'];
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  describe('notification table test', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [NoopAnimationsModule, GioHttpTestingModule, NotificationSettingsListModule, MatIconTestingModule, MatCardModule],
        providers: [{ provide: CurrentUserService, useValue: { currentUser } }, ApplicationNotificationSettingsService],
      })
        .overrideProvider(InteractivityChecker, {
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
            isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        })
        .compileComponents();

      fixture = TestBed.createComponent(TestComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      loader = TestbedHarnessEnvironment.loader(fixture);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should display an empty table', async () => {
      fixture.componentInstance.notificationSettingsListServices = {
        reference: { referenceType: 'API' as const, referenceId: '123' },
        getList: () => of([]),
        getNotifiers: () => of([]),
        create: (notificationSettings) => of(notificationSettings),
        delete: () => of([fakeNotificationSettings({ name: 'Test name' })]),
      };
      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#notificationsTable' }));

      expect(await table.getCellTextByIndex()).toEqual([['No notifications to display.']]);
    });

    it('should display a table with notifications', async () => {
      fixture.componentInstance.notificationSettingsListServices = {
        reference: { referenceType: 'API' as const, referenceId: '123' },
        getList: () => of([fakeNotificationSettings({ name: 'Test name' })]),
        getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
        create: (notificationSettings) => of(notificationSettings),
        delete: () => of([fakeNotificationSettings({ name: 'Test name' })]),
      };

      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#notificationsTable' }));

      expect(await table.getCellTextByIndex()).toEqual([['Test name', 'Notifier A', '']]);
    });

    it('should delete the notification', async () => {
      let deletedNotificationIdToExpect: string | undefined;
      fixture.componentInstance.notificationSettingsListServices = {
        reference: { referenceType: 'API' as const, referenceId: '123' },
        getList: () => of([fakeNotificationSettings({ name: 'Test name' })]),
        getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
        create: (notificationSettings) => of(notificationSettings),
        delete: (notificationId) => {
          deletedNotificationIdToExpect = notificationId;
          return of([fakeNotificationSettings({ name: 'Test name' })]);
        },
      };
      fixture.detectChanges();

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Delete notification"]` }));
      await button.click();

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      expect(deletedNotificationIdToExpect).toEqual('f7889b1c-2b4c-435d-889b-1c2b4c235da9');
    });

    it('should add the notification', async () => {
      let newNotificationToExpect: NotificationSettings | undefined;
      fixture.componentInstance.notificationSettingsListServices = {
        reference: { referenceType: 'API' as const, referenceId: '123' },
        getList: () => of([fakeNotificationSettings({ name: 'Test name' })]),
        getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
        create: (notificationSettings) => {
          newNotificationToExpect = notificationSettings;
          return of(notificationSettings);
        },
        delete: () => of([fakeNotificationSettings({ name: 'Test name' })]),
      };
      fixture.detectChanges();

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Add notification"]` }));
      expect(await button.isDisabled()).toBeFalsy();
      await button.click();

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Test notification');

      const notifierGroupSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=notifier]' }));
      await notifierGroupSelect.clickOptions({ text: 'Notifier A' });

      await submitButton.click();

      expect(newNotificationToExpect.name).toEqual('Test notification');
    });
  });
});
