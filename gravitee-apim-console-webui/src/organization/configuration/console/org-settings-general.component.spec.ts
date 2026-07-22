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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioFormTagsInputHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';

import { OrgSettingsGeneralComponent } from './org-settings-general.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ConsoleSettingsComponent', () => {
  let fixture: ComponentFixture<OrgSettingsGeneralComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, MatIconTestingModule],
      providers: [{ provide: GioTestingPermissionProvider, useValue: ['organization-settings-u'] }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(OrgSettingsGeneralComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should loading when no setting is provided', async () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);

    expect(fixture.componentInstance.isLoading).toBeTruthy();
  });

  describe('management', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        management: {
          title: 'Title',
          userCreation: {
            enabled: false,
          },
        },
        metadata: {
          readonly: ['management.title'],
        },
      });

      const titleFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Title' }));
      expect(await titleFormField.isDisabled()).toEqual(true);

      const activateSupportSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'support' }));
      expect(await activateSupportSlideToggle.isDisabled()).toEqual(false);
    });

    it('should save management settings', async () => {
      expectConsoleSettingsGetRequest({
        management: {
          title: 'Title',
          support: {
            enabled: false,
          },
          userCreation: {
            enabled: false,
          },
        },
      });

      const titleFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Title' }));
      const titleInput = await titleFormField.getControl(MatInputHarness);
      await titleInput?.setValue('New Title');

      const activateSupportSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'support' }));
      await activateSupportSlideToggle.check();

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        management: {
          title: 'New Title',
          support: {
            enabled: true,
          },
          userCreation: {
            enabled: false,
          },
        },
      });
    });

    it('should disable automaticValidation when userCreation is not checked', async () => {
      expectConsoleSettingsGetRequest({
        management: {
          userCreation: {
            enabled: true,
          },
          automaticValidation: {
            enabled: true,
          },
        },
      });

      const titleFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Title' }));
      const titleInput = await titleFormField.getControl(MatInputHarness);
      await titleInput?.setValue('New Title');

      const userCreationSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'userCreation' }));
      const automaticValidationSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'automaticValidation' }));

      expect(await automaticValidationSlideToggle.isDisabled()).toEqual(false);
      await userCreationSlideToggle.toggle();

      // expect automaticValidation SlideToggle not to be visible
      expect(await loader.getAllHarnesses(MatSlideToggleHarness.with({ name: 'automaticValidation' }))).toEqual([]);

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        management: {
          userCreation: {
            enabled: false,
          },
          automaticValidation: {
            enabled: true,
          },
        },
      });
    });
  });

  describe('scheduler', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        scheduler: {
          tasks: undefined,
          notifications: 0,
        },
        metadata: {
          readonly: ['console.scheduler.tasks', 'console.scheduler.notifications'],
        },
      });

      const taskFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Tasks/ }));
      expect(await taskFormField.isDisabled()).toEqual(true);

      const notificationFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Notifications/ }));
      expect(await notificationFormField.isDisabled()).toEqual(true);
    });

    it('should save scheduler settings', async () => {
      expectConsoleSettingsGetRequest({
        scheduler: {
          tasks: undefined,
          notifications: 0,
        },
      });

      const taskFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Tasks/ }));
      await (await taskFormField.getControl(MatInputHarness)).setValue('666');

      const notificationFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Notifications/ }));
      await (await notificationFormField.getControl(MatInputHarness)).setValue('');

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        scheduler: {
          tasks: 666,
          notifications: null,
        },
      });
    });
  });

  describe('cors', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        cors: {
          allowOrigin: undefined,
          allowMethods: ['GET', 'POST'],
          allowHeaders: ['a', 'b'],
          exposedHeaders: [],
          maxAge: 42,
        },
        metadata: {
          readonly: [
            'http.api.management.cors.allow-origin',
            'http.api.management.cors.allow-methods',
            'http.api.management.cors.allow-headers',
            'http.api.management.cors.exposed-headers',
            'http.api.management.cors.max-age',
          ],
        },
      });

      const allowOriginFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Allow-Origin' }));
      expect(await allowOriginFormField.isDisabled()).toEqual(true);

      const allowMethodsFormField = await loader.getHarness(
        MatFormFieldHarness.with({ floatingLabelText: 'Access-Control-Allow-Methods' }),
      );
      expect(await allowMethodsFormField.isDisabled()).toEqual(true);

      const allowHeadersFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Allow-Headers' }));
      expect(await allowHeadersFormField.isDisabled()).toEqual(true);

      const exposedHeadersFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Exposed-Headers' }));
      expect(await exposedHeadersFormField.isDisabled()).toEqual(true);

      const maxAgeFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Max age/ }));
      expect(await maxAgeFormField.isDisabled()).toEqual(true);
    });

    it('should save cors settings', async () => {
      expectConsoleSettingsGetRequest({
        cors: {
          allowOrigin: undefined,
          allowMethods: ['GET', 'POST'],
          allowHeaders: ['x-foo', 'x-bar'],
          exposedHeaders: [],
          maxAge: 42,
        },
      });

      // # Allow-Origin
      const allowOriginFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Allow-Origin' }));
      expect(await (await allowOriginFormField.getControl(GioFormTagsInputHarness)).getTags()).toEqual([]);
      // Add valid RegExp
      const allowOriginTagsInput = await allowOriginFormField.getControl(GioFormTagsInputHarness);
      await allowOriginTagsInput.addTag('(Valid|RegExp)', 'blur');

      // # Access-Control-Allow-Methods
      const allowMethodsFormField = await loader.getHarness(
        MatFormFieldHarness.with({ floatingLabelText: 'Access-Control-Allow-Methods' }),
      );
      // Open select and select DELETE option
      const allowMethodsSelect = await allowMethodsFormField.getControl(MatSelectHarness);
      await allowMethodsSelect.open();
      await (await allowMethodsSelect.getOptions({ text: 'DELETE' }))[0].click();
      await allowMethodsSelect.close();
      // Check selected options
      expect(await allowMethodsSelect.getValueText()).toEqual('GET, DELETE, POST');

      // # Allow-Headers
      const allowHeadersFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Allow-Headers' }));
      const allowHeadersTagsInput = await allowHeadersFormField.getControl(GioFormTagsInputHarness);
      // Add new custom header
      await allowHeadersTagsInput.addTag('x-ray');
      // Remove x-foo header
      await allowHeadersTagsInput.removeTag('x-foo');
      // Check headers
      expect(await allowHeadersTagsInput.getTags()).toEqual(['x-bar', 'x-ray']);

      // # Exposed-Headers
      const exposedHeadersFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Exposed-Headers' }));
      const exposedHeadersFormTags = await exposedHeadersFormField.getControl(GioFormTagsInputHarness);
      const exposedHeadersAutocomplete = await exposedHeadersFormTags.getMatAutocompleteHarness();
      // Select `Cache-Control` with autocomplete
      await exposedHeadersAutocomplete.focus();
      await exposedHeadersAutocomplete.selectOption({ text: 'Cache-Control' });
      // Check headers
      expect(await exposedHeadersFormTags.getTags()).toEqual(['Cache-Control']);

      // # Max age
      const maxAgeFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Max age/ }));
      await (await maxAgeFormField.getControl(MatInputHarness)).setValue('666');

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        cors: {
          allowOrigin: ['(Valid|RegExp)'],
          allowMethods: ['GET', 'DELETE', 'POST'],
          allowHeaders: ['x-bar', 'x-ray'],
          exposedHeaders: ['Cache-Control'],
          maxAge: 666,
        },
      });
    });

    it('should open confirm dialog for the addition of a Allow-Origin with `*`', async () => {
      expectConsoleSettingsGetRequest({
        cors: {
          allowOrigin: undefined,
        },
      });

      // # Allow-Origin
      const allowOriginFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Allow-Origin' }));
      expect(await (await allowOriginFormField.getControl(GioFormTagsInputHarness)).getTags()).toEqual([]);
      // Add `*` and confirm dialog
      const allowOriginChipListInput = await allowOriginFormField.getControl(GioFormTagsInputHarness);

      await allowOriginChipListInput.addTag('*');
      const dialogOne = await rootLoader.getHarness(MatDialogHarness);
      expect(await dialogOne.getId()).toEqual('allowAllOriginsConfirmDialog');
      await dialogOne.close();

      await allowOriginChipListInput.addTag('*');
      const dialogTwo = await rootLoader.getHarness(MatDialogHarness);
      await (await dialogTwo.getHarness(MatButtonHarness.with({ text: /^Yes,/ }))).click();

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        cors: {
          allowOrigin: ['*'],
        },
      });
    });
  });

  describe('email', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        email: {
          enabled: true,
        },
        metadata: {
          readonly: [
            'email.enabled',
            'email.host',
            'email.port',
            'email.username',
            'email.password',
            'email.protocol',
            'email.subject',
            'email.from',
            'email.properties.auth',
            'email.properties.startTlsEnable',
            'email.properties.sslTrust',
          ],
        },
      });

      const emailEnabledEnableSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailEnabled' }));
      expect(await emailEnabledEnableSlideToggle.isDisabled()).toEqual(true);

      await Promise.all(
        ['Host', 'Port', 'Username', 'Password', 'Protocol', 'Subject', 'From', 'SSL Trust'].map(async floatingLabelText => {
          const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText }));

          expect(await emailFormField.isDisabled()).toEqual(true);
        }),
      );

      const propertiesAuthSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailPropertiesAuth' }));
      expect(await propertiesAuthSlideToggle.isDisabled()).toEqual(true);

      const propertiesStartTlsEnableSlideToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ name: 'emailPropertiesStartTlsEnable' }),
      );
      expect(await propertiesStartTlsEnableSlideToggle.isDisabled()).toEqual(true);
    });

    it('should save email settings', async () => {
      expectConsoleSettingsGetRequest({
        email: {
          enabled: true,
          host: 'Host',
          port: 42,
          username: 'Username',
          password: 'Password',
          protocol: 'Protocol',
          subject: 'Subject',
          from: 'From',
          properties: { auth: true, startTlsEnable: false, sslTrust: undefined },
        },
      });

      await Promise.all(
        ['Host', 'Username', 'Password', 'Protocol', 'Subject', 'From', 'SSL Trust'].map(async floatingLabelText => {
          const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText }));

          await (await emailFormField.getControl(MatInputHarness)).setValue(`New ${floatingLabelText}`);
        }),
      );

      const propertiesAuthSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailPropertiesAuth' }));
      await propertiesAuthSlideToggle.uncheck();

      const propertiesStartTlsEnableSlideToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ name: 'emailPropertiesStartTlsEnable' }),
      );
      await propertiesStartTlsEnableSlideToggle.check();

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        email: {
          enabled: true,
          host: 'New Host',
          port: 42,
          username: 'New Username',
          password: 'New Password',
          protocol: 'New Protocol',
          subject: 'New Subject',
          from: 'NewFrom',
          properties: { auth: false, startTlsEnable: true, sslTrust: 'New SSL Trust' },
        },
      });
    });

    it('should disable all email settings when email.enabled is not checked', async () => {
      expectConsoleSettingsGetRequest({
        email: {
          enabled: true,
        },
        metadata: {
          readonly: ['email.host', 'email.port', 'email.properties.auth'],
        },
      });

      const emailEnabledEnableSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailEnabled' }));
      await emailEnabledEnableSlideToggle.uncheck();

      // expect all email settings to be not visible
      await Promise.all(
        ['Host', 'Port', 'Username', 'Password', 'Protocol', 'Subject', 'From', 'SSL Trust'].map(async floatingLabelText => {
          const isEmailFormFieldVisible = await loader
            .getHarness(MatFormFieldHarness.with({ floatingLabelText }))
            .then(() => true)
            .catch(() => false);

          expect(isEmailFormFieldVisible).toEqual(false);
        }),
      );

      const isPropertiesAuthSlideToggleVisible = await loader
        .getHarness(MatSlideToggleHarness.with({ name: 'emailPropertiesAuth' }))
        .then(() => true)
        .catch(() => false);
      expect(await isPropertiesAuthSlideToggleVisible).toEqual(false);

      const isPropertiesStartTlsEnableSlideToggleVisible = await loader
        .getHarness(MatSlideToggleHarness.with({ name: 'emailPropertiesStartTlsEnable' }))
        .then(() => true)
        .catch(() => false);
      expect(await isPropertiesStartTlsEnableSlideToggleVisible).toEqual(false);

      await emailEnabledEnableSlideToggle.check();

      // Expect all email settings to be enabled except for read-only settings
      await Promise.all(
        ['Username', 'Password', 'Protocol', 'Subject', 'From', 'SSL Trust'].map(async floatingLabelText => {
          const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText }));

          expect(await emailFormField.isDisabled()).toEqual(false);
        }),
      );
      await Promise.all(
        ['Host', 'Port'].map(async floatingLabelText => {
          const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText }));

          expect(await emailFormField.isDisabled()).toEqual(true);
        }),
      );

      const propertiesAuthSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailPropertiesAuth' }));
      expect(await propertiesAuthSlideToggle.isDisabled()).toEqual(true);

      const propertiesStartTlsEnableSlideToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ name: 'emailPropertiesStartTlsEnable' }),
      );
      expect(await propertiesStartTlsEnableSlideToggle.isDisabled()).toEqual(false);
    });

    it('should enable and disable the branded senders control together with email.enabled', async () => {
      expectConsoleSettingsGetRequest({
        email: { enabled: true },
        metadata: { readonly: [] },
      });

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toEqual(false);

      const emailEnabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailEnabled' }));
      await emailEnabledSlideToggle.uncheck();
      expect(await addConfigurationButton.isDisabled()).toEqual(true);

      await emailEnabledSlideToggle.check();
      expect(await addConfigurationButton.isDisabled()).toEqual(false);
    });

    it('should keep branded senders disabled when email.branded_senders is read-only, even after toggling email on', async () => {
      expectConsoleSettingsGetRequest({
        email: { enabled: true },
        metadata: { readonly: ['email.branded_senders'] },
      });

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toEqual(true);

      // Toggling email off then on must not re-enable a system-provided (read-only) setting: this guards the
      // camelCase form-path vs. snake_case parameter-key divergence in the enable/disable sync.
      const emailEnabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emailEnabled' }));
      await emailEnabledSlideToggle.uncheck();
      await emailEnabledSlideToggle.check();
      expect(await addConfigurationButton.isDisabled()).toEqual(true);
    });

    it('should explain the lock when email.branded_senders is read-only', async () => {
      expectConsoleSettingsGetRequest({
        email: { enabled: true },
        metadata: { readonly: ['email.branded_senders'] },
      });

      // Disabling alone is indistinguishable from missing ENVIRONMENT_SETTINGS[U]; the reason has to be shown.
      const lock = await loader.getHarnessOrNull(MatIconHarness.with({ selector: '[data-testid="branded-senders-system-lock"]' }));
      expect(lock).not.toBeNull();
    });

    it('should not show the system lock when branded senders are merely disabled', async () => {
      expectConsoleSettingsGetRequest({ email: { enabled: false } });

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toEqual(true);
      expect(await loader.getHarnessOrNull(MatIconHarness.with({ selector: '[data-testid="branded-senders-system-lock"]' }))).toBeNull();
    });

    it('should disable branded senders on initial load when email is disabled', async () => {
      expectConsoleSettingsGetRequest({
        email: { enabled: false },
        metadata: { readonly: [] },
      });

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toEqual(true);
    });

    it('should still explain the lock when the setting is read-only and email is disabled', async () => {
      // The host collapses both conditions into one `disabled` flag; the lock tracks only the read-only one, so
      // their intersection must still be explained rather than falling back to a bare disabled section.
      expectConsoleSettingsGetRequest({
        email: { enabled: false },
        metadata: { readonly: ['email.branded_senders'] },
      });

      expect(
        await loader.getHarnessOrNull(MatIconHarness.with({ selector: '[data-testid="branded-senders-system-lock"]' })),
      ).not.toBeNull();
    });

    it('should round-trip branded senders from a populated GET through save', async () => {
      expectConsoleSettingsGetRequest({
        email: {
          enabled: true,
          brandedSenders: [{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }],
        },
        metadata: { readonly: [] },
      });

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      await (await domainsField.getControl(GioFormTagsInputHarness))!.addTag('example.org');

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      expectConsoleSettingsSendRequest({
        email: {
          brandedSenders: [{ domains: ['example.com', 'example.org'], from: 'noreply@example.com', subject: '[Example] %s' }],
        },
      });
    });

    it('should surface a snackbar error when saving fails', async () => {
      const snackBarService = TestBed.inject(SnackBarService);
      const errorSpy = jest.spyOn(snackBarService, 'error');

      expectConsoleSettingsGetRequest({
        email: {
          enabled: true,
          brandedSenders: [{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }],
        },
        metadata: { readonly: [] },
      });

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      await (await domainsField.getControl(GioFormTagsInputHarness))!.addTag('example.org');

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
      req.flush({ message: 'Payload too large' }, { status: 400, statusText: 'Bad Request' });

      expect(errorSpy).toHaveBeenCalledWith('Payload too large');
    });
  });

  describe('permissions', () => {
    beforeEach(() => {
      // The root beforeEach grants organization-settings-u; re-create the component without it to exercise the
      // read-only path (the previously-created instance and its HTTP controller are discarded by the reset).
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, MatIconTestingModule],
        providers: [{ provide: GioTestingPermissionProvider, useValue: [] }],
      })
        .overrideProvider(InteractivityChecker, {
          useValue: {
            isFocusable: () => true,
          },
        })
        .compileComponents();

      fixture = TestBed.createComponent(OrgSettingsGeneralComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    it('should disable the form including branded senders when lacking organization-settings-u', async () => {
      expectConsoleSettingsGetRequest({ email: { enabled: true }, metadata: { readonly: [] } });

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toEqual(true);

      // This is the state the padlock exists to be told apart from. Showing it here would assert "the system
      // configured this" to someone who is merely unprivileged — the original ambiguity, now confidently wrong.
      expect(await loader.getHarnessOrNull(MatIconHarness.with({ selector: '[data-testid="branded-senders-system-lock"]' }))).toBeNull();
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectConsoleSettingsSendRequest(consoleSettingsPayload: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toMatchObject(consoleSettingsPayload);
  }

  function expectConsoleSettingsGetRequest(consoleSettingsResponse: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    expect(req.request.method).toEqual('GET');
    req.flush(consoleSettingsResponse);
  }
});
