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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { UIRouterModule } from '@uirouter/angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { Component } from '@angular/core';
import { of } from 'rxjs';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { NotificationSettingsDetailsServices } from './notification-settings-details.component';

import { User } from '../../../../entities/user';
import { GioHttpTestingModule } from '../../../../shared/testing';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeHooks } from '../../../../entities/notification/hooks.fixture';
import { fakeNotificationSettings } from '../../../../entities/notification/notificationSettings.fixture';
import { fakeNotifier } from '../../../../entities/notification/notifier.fixture';
import { NotificationSettings } from '../../../../entities/notification/notificationSettings';
import { NotificationSettingsListModule } from '../notification-settings-list.module';

@Component({
  selector: 'test-component',
  template: ` <notification-settings-details
    [notificationSettingsDetailsServices]="notificationSettingsDetailsServices"
  ></notification-settings-details>`,
})
class TestComponent {
  notificationSettingsDetailsServices: NotificationSettingsDetailsServices;
}

describe('NotificationSettingsDetailsComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  const API_ID = 'apiId';
  const NOTIFICATION_ID = 'f7889b1c-2b4c-435d-889b-1c2b4c235da9';
  const currentUser = new User();
  currentUser.userPermissions = ['api-notification-u', 'api-notification-d', 'api-notification-c'];
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        GioUiRouterTestingModule,
        NotificationSettingsListModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, notificationId: NOTIFICATION_ID } },
        { provide: UIRouterState, useValue: { $current: { parent: { name: 'test' } } } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
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
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display all checkboxes', async () => {
    fixture.componentInstance.notificationSettingsDetailsServices = {
      reference: { referenceType: 'API' as const, referenceId: '123' },
      getHooks: () => of([fakeHooks()]),
      getSingleNotificationSetting: () => of(fakeNotificationSettings({ name: 'Test name' })),
      getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
      update: (updatedNotification) => of(updatedNotification),
    };
    fixture.detectChanges();

    const groupCheckbox = await loader.getAllHarnesses(MatCheckboxHarness);
    expect(groupCheckbox.length).toEqual(1);

    const testCheckbox = await loader.getHarness(MatCheckboxHarness.with({ selector: `[ng-reflect-name="test_id"]` }));
    expect(await testCheckbox.isChecked()).toEqual(false);
  });

  it('should update a checkbox', async () => {
    let updateToExpect: NotificationSettings | undefined;
    fixture.componentInstance.notificationSettingsDetailsServices = {
      reference: { referenceType: 'API' as const, referenceId: '123' },
      getHooks: () => of([fakeHooks()]),
      getSingleNotificationSetting: () => of(fakeNotificationSettings({ name: 'Test name' })),
      getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
      update: (updatedNotification) => {
        updateToExpect = updatedNotification;
        return of(updatedNotification);
      },
    };
    fixture.detectChanges();

    const testCheckbox = await loader.getHarness(MatCheckboxHarness.with({ selector: `[ng-reflect-name="test_id"]` }));
    expect(await testCheckbox.isChecked()).toEqual(false);

    await testCheckbox.check();

    expect(await testCheckbox.isChecked()).toEqual(true);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

    await saveBar.clickSubmit();

    expect(updateToExpect.hooks).toEqual(['test_id']);
  });

  it('should update an email', async () => {
    let updateToExpect: NotificationSettings | undefined;
    fixture.componentInstance.notificationSettingsDetailsServices = {
      reference: { referenceType: 'API' as const, referenceId: '123' },
      getHooks: () => of([fakeHooks()]),
      getSingleNotificationSetting: () => of(fakeNotificationSettings({ name: 'Test name' })),
      getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })]),
      update: (updatedNotification) => {
        updateToExpect = updatedNotification;
        return of(updatedNotification);
      },
    };
    fixture.detectChanges();

    const notifierInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=notifier]' }));
    await notifierInput.setValue('test@email.test');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

    await saveBar.clickSubmit();

    expect(updateToExpect.config).toEqual('test@email.test');
  });

  it('should have "use system proxy" checkbox when notifier type is webhook', async () => {
    fixture.componentInstance.notificationSettingsDetailsServices = {
      reference: { referenceType: 'API' as const, referenceId: '123' },
      getHooks: () => of([fakeHooks()]),
      getSingleNotificationSetting: () => of(fakeNotificationSettings({ name: 'Test name' })),
      getNotifiers: () => of([fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'WEBHOOK' })]),
      update: (updatedNotification) => {
        return of(updatedNotification);
      },
    };
    fixture.detectChanges();

    const useSystemProxySlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=useSystemProxy]' }));
    expect(await useSystemProxySlideToggle.isChecked()).toEqual(false);
  });
});
