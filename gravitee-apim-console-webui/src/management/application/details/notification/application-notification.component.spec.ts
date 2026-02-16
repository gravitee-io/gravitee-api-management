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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApplicationNotificationComponent } from './application-notification.component';
import { ApplicationNotificationModule } from './application-notification.module';

import { NotificationAddDialogHarness, NotificationEditDialogHarness, NotificationListHarness } from '../../../../components/notification';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeNotifier } from '../../../../entities/notification/notifier.fixture';
import { fakeNotificationSettings, fakePortalNotificationSettings } from '../../../../entities/notification/notificationSettings.fixture';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { fakeHooks } from '../../../../entities/notification/hooks.fixture';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Application } from '../../../../entities/application/Application';
import { Metadata } from '../../../../entities/metadata/metadata';
import { fakeMetadata } from '../../../../entities/metadata/metadata.fixture';
import { fakeApplication } from '../../../../entities/application/Application.fixture';

describe('AppNotificationComponent', () => {
  const APPLICATION_ID = 'an-app-id';
  const EMAIL_NOTIFICATION_ID = 'email-notification-id';
  const WEBHOOK_NOTIFICATION_ID = 'webhook-notification-id';
  const NOTIFIER_EMAIL_ID = 'default-email';
  const NOTIFIER_EMAIL_NAME = 'Default Mail Notifier';
  const NOTIFIER_WEBHOOK_ID = 'default-webhook';
  const NOTIFIER_WEBHOOK_NAME = 'Default Webhook Notifier';

  const PORTAL_NOTIFICATION = fakePortalNotificationSettings({
    referenceType: 'APPLICATION',
    referenceId: APPLICATION_ID,
  });
  const EMAIL_NOTIFICATION = fakeNotificationSettings({
    id: EMAIL_NOTIFICATION_ID,
    notifier: NOTIFIER_EMAIL_ID,
    referenceType: 'APPLICATION',
    referenceId: APPLICATION_ID,
    name: 'Default Mail Notifications',
    hooks: ['APIKEY_EXPIRED', 'API_UPDATED'],
  });
  const WEBHOOK_NOTIFICATION = fakeNotificationSettings({
    id: WEBHOOK_NOTIFICATION_ID,
    notifier: NOTIFIER_WEBHOOK_ID,
    referenceType: 'APPLICATION',
    referenceId: APPLICATION_ID,
    name: 'Default Webhook Notifications',
    hooks: ['APIKEY_EXPIRED', 'API_UPDATED'],
  });

  const fakeSnackBarService = {
    success: jest.fn(),
  };

  let fixture: ComponentFixture<ApplicationNotificationComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  let httpTestingController: HttpTestingController;
  let permissionsService: GioPermissionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, ApplicationNotificationModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } },
        { provide: SnackBarService, useValue: fakeSnackBarService },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to avoid the warning
          isTabbable: () => true,
        },
      })
      .compileComponents();

    permissionsService = TestBed.inject(GioPermissionService);

    initComponent();
  });

  function initComponent(permissions = ['application-notification-c', 'application-notification-d', 'application-notification-u']) {
    permissionsService._setPermissions(permissions);
    fixture = TestBed.createComponent(ApplicationNotificationComponent);
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    expectNotificationSettingsRequest();
    expectNotifiersRequest();
    expectGetApplication(fakeApplication());
    expectMetadataList();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display notifications', async () => {
    const table = await loader.getHarness(NotificationListHarness);
    expect(await table.rows()).toEqual([
      {
        name: PORTAL_NOTIFICATION.name,
        notifier: '',
        subscribedEvents: '0 events',
        actions: '',
      },
      {
        name: EMAIL_NOTIFICATION.name,
        notifier: NOTIFIER_EMAIL_NAME,
        subscribedEvents: '2 events',
        actions: '',
      },
      {
        name: WEBHOOK_NOTIFICATION.name,
        notifier: NOTIFIER_WEBHOOK_NAME,
        subscribedEvents: '2 events',
        actions: '',
      },
    ]);
  });

  describe('Add new notification', () => {
    it('should add new notification', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid=add-notification]' }));
      await addButton.click();

      const dialog = await rootLoader.getHarness(NotificationAddDialogHarness);
      await dialog.fillForm('New Notification', NOTIFIER_WEBHOOK_NAME).then(() => dialog.confirm());

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings`,
      });
      expect(req.request.body).toEqual({
        name: 'New Notification',
        notifier: NOTIFIER_WEBHOOK_ID,
        referenceType: 'APPLICATION',
        referenceId: APPLICATION_ID,
        config_type: 'GENERIC',
        hooks: [],
      });
    });

    it('should display a success notification, refresh the list and open edit dialog', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid=add-notification]' }));
      await addButton.click();

      const dialog = await rootLoader.getHarness(NotificationAddDialogHarness);
      await dialog.fillForm('New Notification', NOTIFIER_WEBHOOK_NAME).then(() => dialog.confirm());

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings`,
      });
      req.flush({ id: 'new-notification-id', ...req.request.body });
      expectNotificationSettingsRequest([PORTAL_NOTIFICATION, EMAIL_NOTIFICATION, WEBHOOK_NOTIFICATION, req.request.body]);

      // Check that the snackbar has been displayed
      expect(fakeSnackBarService.success).toHaveBeenCalledWith('Notification created successfully');

      // Check that the table has been refreshed
      const table = await loader.getHarness(NotificationListHarness);
      expect(await table.rows()).toEqual([
        {
          name: PORTAL_NOTIFICATION.name,
          notifier: '',
          subscribedEvents: '0 events',
          actions: '',
        },
        {
          name: EMAIL_NOTIFICATION.name,
          notifier: NOTIFIER_EMAIL_NAME,
          subscribedEvents: '2 events',
          actions: '',
        },
        {
          name: WEBHOOK_NOTIFICATION.name,
          notifier: NOTIFIER_WEBHOOK_NAME,
          subscribedEvents: '2 events',
          actions: '',
        },
        {
          name: 'New Notification',
          notifier: NOTIFIER_WEBHOOK_NAME,
          subscribedEvents: '0 events',
          actions: '',
        },
      ]);

      // Check the edit dialog has been opened
      expectHooksRequest();
      expect(await rootLoader.getHarness(NotificationEditDialogHarness)).not.toBeNull();
    });

    it('should not show Add button if the user is not allowed', async () => {
      initComponent([]);

      const addButton = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=add-notification]' }));
      expect(addButton).toBeNull();
    });
  });

  describe('Delete a notification', () => {
    it('should delete notification', async () => {
      const table = await loader.getHarness(NotificationListHarness);
      await table.deleteRow(1);

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/${EMAIL_NOTIFICATION_ID}`,
      });
    });

    it('should display a success notification and refresh the list', async () => {
      const table = await loader.getHarness(NotificationListHarness);
      await table.deleteRow(1);

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/${EMAIL_NOTIFICATION_ID}`,
        })
        .flush([]);
      expectNotificationSettingsRequest([PORTAL_NOTIFICATION]);

      // Check that the snackbar has been displayed
      expect(fakeSnackBarService.success).toHaveBeenCalledWith('"Default Mail Notifications" has been deleted');

      // Check that the table has been refreshed
      expect(await table.rows()).toEqual([
        {
          name: 'Portal Notification',
          notifier: '',
          subscribedEvents: '0 events',
          actions: '',
        },
      ]);
    });

    it('should not show Delete button on Portal Notification row', async () => {
      const table = await loader.getHarness(NotificationListHarness);
      const addButton = await table.getDeleteButton(0);
      expect(addButton).toBeNull();
    });

    it('should not show Delete button if the user is not allowed', async () => {
      initComponent([]);

      const table = await loader.getHarness(NotificationListHarness);
      const addButton = await table.getDeleteButton(1);
      expect(addButton).toBeNull();
    });
  });

  describe('Edit a notification', () => {
    describe('using Portal', () => {
      it('should open edit modal', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(0);

        expectHooksRequest();
        expectNotificationSettingsRequest([PORTAL_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);

        // Notifier config is not available
        const notifierConfigInput = await dialog.getNotifierConfigInput();
        expect(notifierConfigInput).toBeNull();

        // UseSystemProxy is not available
        const useSystemProxyToggle = await dialog.getUseSystemProxyToggle();
        expect(useSystemProxyToggle).toBeNull();

        // Selected Hooks are checked
        const hooks = await dialog.getAllHooks();
        expect(hooks).toEqual(
          expect.arrayContaining([
            { name: 'APIKEY_EXPIRED', checked: false },
            { name: 'API_UPDATED', checked: false },
            { name: 'APIKEY_REVOKED', checked: false },
            { name: 'API_STARTED', checked: false },
            { name: 'API_STOPPED', checked: false },
          ]),
        );
      });

      it('should edit the notification', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(0);

        expectHooksRequest();
        expectNotificationSettingsRequest([PORTAL_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);
        await dialog.getHookCheckbox('APIKEY_EXPIRED').then(checkbox => checkbox.uncheck());
        await dialog.getHookCheckbox('API_UPDATED').then(checkbox => checkbox.uncheck());
        await dialog.getHookCheckbox('API_STARTED').then(checkbox => checkbox.check());
        await dialog.save();

        const req = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/`,
        });
        expect(req.request.body).toEqual({
          ...PORTAL_NOTIFICATION,
          hooks: ['API_STARTED'],
        });
      });

      it('should not show Edit button if the user is not allowed', async () => {
        initComponent([]);

        const table = await loader.getHarness(NotificationListHarness);

        // Portal Notification
        expect(await table.getEditButton(0)).toBeNull();

        // Other Notification
        expect(await table.getEditButton(1)).toBeNull();
      });
    });

    describe('using Email Notifier', () => {
      it('should open edit modal', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(1);

        expectHooksRequest();
        expectNotificationSettingsRequest([EMAIL_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);

        // Notifier config is available
        const notifierConfigInput = await dialog.getNotifierConfigInput();
        expect(notifierConfigInput).not.toBeNull();

        // UseSystemProxy is not available
        const useSystemProxyToggle = await dialog.getUseSystemProxyToggle();
        expect(useSystemProxyToggle).toBeNull();

        // Selected Hooks are checked
        const hooks = await dialog.getAllHooks();
        expect(hooks).toEqual(
          expect.arrayContaining([
            { name: 'APIKEY_EXPIRED', checked: true },
            { name: 'API_UPDATED', checked: true },
            { name: 'APIKEY_REVOKED', checked: false },
            { name: 'API_STARTED', checked: false },
            { name: 'API_STOPPED', checked: false },
          ]),
        );
      });

      it('should edit the notification', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(1);

        expectHooksRequest();
        expectNotificationSettingsRequest([EMAIL_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);
        await dialog.fillConfig('new-config');
        await dialog.getHookCheckbox('APIKEY_EXPIRED').then(checkbox => checkbox.toggle());
        await dialog.getHookCheckbox('API_STOPPED').then(checkbox => checkbox.check());
        await dialog.save();

        const req = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/${EMAIL_NOTIFICATION_ID}`,
        });
        expect(req.request.body).toEqual({
          ...EMAIL_NOTIFICATION,
          config: 'new-config',
          useSystemProxy: false,
          hooks: ['API_UPDATED', 'API_STOPPED'],
        });
      });
    });

    describe('using Webhook Notifier', () => {
      it('should open edit modal', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(2);

        expectHooksRequest();
        expectNotificationSettingsRequest([WEBHOOK_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);

        // Notifier config is available
        const notifierConfigInput = await dialog.getNotifierConfigInput();
        expect(notifierConfigInput).not.toBeNull();

        // UseSystemProxy is available
        const useSystemProxyToggle = await dialog.getUseSystemProxyToggle();
        expect(useSystemProxyToggle).not.toBeNull();

        // Selected Hooks are checked
        const hooks = await dialog.getAllHooks();
        expect(hooks).toEqual(
          expect.arrayContaining([
            { name: 'APIKEY_EXPIRED', checked: true },
            { name: 'API_UPDATED', checked: true },
            { name: 'APIKEY_REVOKED', checked: false },
            { name: 'API_STARTED', checked: false },
            { name: 'API_STOPPED', checked: false },
          ]),
        );
      });

      it('should edit the notification', async () => {
        const table = await loader.getHarness(NotificationListHarness);
        await table.editRow(2);

        expectHooksRequest();
        expectNotificationSettingsRequest([WEBHOOK_NOTIFICATION]);

        const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);
        await dialog.fillConfig('new-config');
        await dialog.toggleUseSystemProxy();
        await dialog.getHookCheckbox('API_UPDATED').then(checkbox => checkbox.uncheck());
        await dialog.getHookCheckbox('APIKEY_REVOKED').then(checkbox => checkbox.check());
        await dialog.save();

        const req = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/${WEBHOOK_NOTIFICATION_ID}`,
        });
        expect(req.request.body).toEqual({
          ...WEBHOOK_NOTIFICATION,
          config: 'new-config',
          useSystemProxy: true,
          hooks: ['APIKEY_EXPIRED', 'APIKEY_REVOKED'],
        });
      });
    });

    it.each([
      {
        index: 0,
        id: '',
      },
      {
        index: 1,
        id: EMAIL_NOTIFICATION_ID,
      },
      {
        index: 2,
        id: WEBHOOK_NOTIFICATION_ID,
      },
    ])('should display a success notification and refresh the list', async ({ index, id }) => {
      const table = await loader.getHarness(NotificationListHarness);
      await table.editRow(index);

      expectHooksRequest();
      expectNotificationSettingsRequest([PORTAL_NOTIFICATION, EMAIL_NOTIFICATION, WEBHOOK_NOTIFICATION]);

      const dialog = await rootLoader.getHarness(NotificationEditDialogHarness);
      await dialog.getHookCheckbox('APIKEY_EXPIRED').then(checkbox => checkbox.uncheck());
      await dialog.save();

      httpTestingController
        .expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings/${id}`,
        })
        .flush([]);

      // Refresh the list
      expectNotificationSettingsRequest([PORTAL_NOTIFICATION]);

      // Check that the snackbar has been displayed
      expect(fakeSnackBarService.success).toHaveBeenCalledWith('Notification saved successfully');

      // Check that the table has been refreshed
      expect(await table.rows()).toEqual([
        {
          name: 'Portal Notification',
          notifier: '',
          subscribedEvents: '0 events',
          actions: '',
        },
      ]);
    });
  });

  function expectHooksRequest(
    response = [
      fakeHooks({ id: 'APIKEY_EXPIRED', label: 'API-Key expired', category: 'API KEY' }),
      fakeHooks({ id: 'APIKEY_REVOKED', label: 'API-Key revoked', category: 'API KEY' }),
      fakeHooks({ id: 'API_STARTED', label: 'API Started', category: 'LIFECYCLE' }),
      fakeHooks({ id: 'API_UPDATED', label: 'API Updated', category: 'LIFECYCLE' }),
      fakeHooks({ id: 'API_STOPPED', label: 'API Stopped', category: 'LIFECYCLE' }),
    ],
  ) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/hooks`);
    if (!req.cancelled) req.flush(response);
  }

  function expectNotificationSettingsRequest(response = [PORTAL_NOTIFICATION, EMAIL_NOTIFICATION, WEBHOOK_NOTIFICATION]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notificationsettings`);
    if (!req.cancelled) req.flush(response);
  }

  function expectNotifiersRequest() {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/notifiers`);
    req.flush([
      fakeNotifier({ id: NOTIFIER_EMAIL_ID, type: 'EMAIL', name: NOTIFIER_EMAIL_NAME }),
      fakeNotifier({ id: NOTIFIER_WEBHOOK_ID, type: 'WEBHOOK', name: NOTIFIER_WEBHOOK_NAME }),
    ]);
  }

  function expectGetApplication(application: Application) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' })
      .flush(application);
  }

  function expectMetadataList(list: Metadata[] = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/metadata/`, method: 'GET' })
      .flush(list);
  }
});
