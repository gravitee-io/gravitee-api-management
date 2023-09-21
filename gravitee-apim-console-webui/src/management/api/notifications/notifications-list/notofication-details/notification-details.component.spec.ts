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

import { NotificationDetailsComponent } from './notification-details.component';
import { NotificationDetailsModule } from './notification-details.module';

import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { GioUiRouterTestingModule } from '../../../../../shared/testing/gio-uirouter-testing-module';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { fakeHooks } from '../../../../../entities/notification/hooks.fixture';
import { fakeNotificationSettings } from '../../../../../entities/notification/notificationSettings.fixture';
import { Notifier } from '../../../../../entities/notification/notifier';
import { fakeNotifier } from '../../../../../entities/notification/notifier.fixture';
import { NotificationSettings } from '../../../../../entities/notification/notificationSettings';
import { Hooks } from '../../../../../entities/notification/hooks';

describe('NotificationDetailsComponent', () => {
  let fixture: ComponentFixture<NotificationDetailsComponent>;
  const API_ID = 'apiId';
  const NOTIFICATION_ID = 'f7889b1c-2b4c-435d-889b-1c2b4c235da9';
  const currentUser = new User();
  currentUser.userPermissions = ['api-notification-u', 'api-notification-d', 'api-notification-c'];
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        GioUiRouterTestingModule,
        NotificationDetailsModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, notificationId: NOTIFICATION_ID } },
        { provide: AjsRootScope, useValue: null },
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

    fixture = TestBed.createComponent(NotificationDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display all checkboxes', async () => {
    const hooks = [fakeHooks()];
    const notifications = [fakeNotificationSettings({ name: 'Test name' })];
    const notifier = [fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })];

    expectApiGetHooks(hooks);
    expectApiGetNotifiers(notifier);
    expectApiGetSingleNotificationList(notifications);

    const groupCheckbox = await loader.getAllHarnesses(MatCheckboxHarness);
    expect(groupCheckbox.length).toEqual(1);

    const testCheckbox = await loader.getHarness(MatCheckboxHarness.with({ selector: `[ng-reflect-name="test_id"]` }));
    expect(await testCheckbox.isChecked()).toEqual(false);
  });

  it('should update a checkbox', async () => {
    const hooks = [fakeHooks()];
    const notifications = [fakeNotificationSettings({ name: 'Test name' })];
    const notifier = [fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })];

    expectApiGetHooks(hooks);
    expectApiGetNotifiers(notifier);
    expectApiGetSingleNotificationList(notifications);

    const testCheckbox = await loader.getHarness(MatCheckboxHarness.with({ selector: `[ng-reflect-name="test_id"]` }));
    expect(await testCheckbox.isChecked()).toEqual(false);

    await testCheckbox.check();

    expect(await testCheckbox.isChecked()).toEqual(true);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

    await saveBar.clickSubmit();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings/${NOTIFICATION_ID}`);

    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual({
      id: 'f7889b1c-2b4c-435d-889b-1c2b4c235da9',
      name: 'Test name',
      referenceType: 'API',
      referenceId: 'f1ddf4b5-c23a-33a7-87bf-28ec0a1d9db9',
      notifier: 'default-email',
      hooks: ['test_id'],
      useSystemProxy: false,
      config_type: 'GENERIC',
      config: null,
    });
  });

  it('should update an email', async () => {
    const hooks = [fakeHooks()];
    const notifications = [fakeNotificationSettings({ name: 'Test name' })];
    const notifier = [fakeNotifier({ id: 'default-email', name: 'Notifier A', type: 'Notifier-type' })];

    expectApiGetHooks(hooks);
    expectApiGetNotifiers(notifier);
    expectApiGetSingleNotificationList(notifications);

    const notifierInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=notifier]' }));
    await notifierInput.setValue('test@email.test');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

    await saveBar.clickSubmit();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings/${NOTIFICATION_ID}`);

    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual({
      id: 'f7889b1c-2b4c-435d-889b-1c2b4c235da9',
      name: 'Test name',
      referenceType: 'API',
      referenceId: 'f1ddf4b5-c23a-33a7-87bf-28ec0a1d9db9',
      notifier: 'default-email',
      hooks: ['notifier'],
      useSystemProxy: false,
      config_type: 'GENERIC',
      config: 'test@email.test',
    });
  });

  function expectApiGetHooks(hooks: Hooks[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/hooks`,
        method: 'GET',
      })
      .flush(hooks);
    fixture.detectChanges();
  }

  function expectApiGetNotifiers(notifier: Notifier[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notifiers`,
        method: 'GET',
      })
      .flush(notifier);
    fixture.detectChanges();
  }

  function expectApiGetSingleNotificationList(notifactionSettings: NotificationSettings[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/notificationsettings`,
        method: 'GET',
      })
      .flush(notifactionSettings);
    fixture.detectChanges();
  }
});
