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
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatChipInputHarness, MatChipListHarness } from '@angular/material/chips/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ConsoleSettingsComponent } from './console-settings';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { ConsoleSettings } from '../../../entities/consoleSettings';

describe('ConsoleSettingsComponent', () => {
  let fixture: ComponentFixture<ConsoleSettingsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ConsoleSettingsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should loading when no setting is provided', async () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings/`);

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

      const activateSupportCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'Activate Support' }));
      expect(await activateSupportCheckbox.isDisabled()).toEqual(false);
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

      const activateSupportCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'Activate Support' }));
      await activateSupportCheckbox.check();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

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

      const userCreationCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'Allow User Registration' }));
      const automaticValidationCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: /^Enable automatic validation/ }));

      expect(await automaticValidationCheckbox.isDisabled()).toEqual(false);
      await userCreationCheckbox.uncheck();
      expect(await automaticValidationCheckbox.isDisabled()).toEqual(true);

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

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

  describe('theme', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        theme: {
          name: undefined,
          logo: 'The logo',
          loader: '',
        },
        metadata: {
          readonly: ['theme.name', 'theme.logo', 'theme.loader'],
        },
      });

      const nameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Name' }));
      expect(await nameFormField.isDisabled()).toEqual(true);

      const logoFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Logo' }));
      expect(await logoFormField.isDisabled()).toEqual(true);

      const loaderFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Loader' }));
      expect(await loaderFormField.isDisabled()).toEqual(true);
    });

    it('should save theme settings', async () => {
      expectConsoleSettingsGetRequest({
        theme: {
          name: undefined,
          logo: 'The logo',
          loader: '',
          css: 'red style',
        },
      });

      const nameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Name' }));
      await (await nameFormField.getControl(MatInputHarness)).setValue('New name');

      const logoFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Logo' }));
      await (await logoFormField.getControl(MatInputHarness)).setValue('');

      const loaderFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Loader' }));
      await (await loaderFormField.getControl(MatInputHarness)).setValue('New loader');

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

      expectConsoleSettingsSendRequest({
        theme: {
          name: 'New name',
          logo: '',
          loader: 'New loader',
          css: 'red style',
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
          readonly: ['scheduler.tasks', 'scheduler.notifications'],
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

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

      expectConsoleSettingsSendRequest({
        scheduler: {
          tasks: 666,
          notifications: null,
        },
      });
    });
  });

  describe('alert', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        alert: {
          enabled: false,
        },
        metadata: {
          readonly: ['alert.enabled'],
        },
      });

      const enableAlertingCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'Enable Alerting' }));
      expect(await enableAlertingCheckbox.isDisabled()).toEqual(true);
    });

    it('should save alert settings', async () => {
      expectConsoleSettingsGetRequest({
        alert: {
          enabled: false,
        },
      });

      const enableAlertingCheckbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'Enable Alerting' }));
      await enableAlertingCheckbox.check();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

      expectConsoleSettingsSendRequest({
        alert: {
          enabled: true,
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
          readonly: ['cors.allowOrigin', 'cors.allowMethods', 'cors.allowHeaders', 'cors.exposedHeaders', 'cors.maxAge'],
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
      expect(await (await allowOriginFormField.getControl(MatChipListHarness)).getChips()).toEqual([]);
      // Add valid RegExp
      const allowOriginChipListInput = await allowOriginFormField.getControl(MatChipInputHarness);
      await allowOriginChipListInput.setValue('(Valid|RegExp)');
      await allowOriginChipListInput.blur();

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
      const allowHeadersChipList = await allowHeadersFormField.getControl(MatChipListHarness);
      const allowHeadersChipListInput = await allowHeadersChipList.getInput();
      // Add new custom header
      await allowHeadersChipListInput.setValue('x-ray');
      await allowHeadersChipListInput.blur();
      // Remove x-foo header
      await (await allowHeadersChipList.getChips({ text: 'x-foo' }))[0].remove();
      // Check headers
      const allowHeadersChips = await Promise.all(await (await allowHeadersChipList.getChips()).map((c) => c.getText()));
      expect(allowHeadersChips).toEqual(['x-bar', 'x-ray']);

      // # Exposed-Headers
      const exposedHeadersFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Exposed-Headers' }));
      const exposedHeadersChipList = await exposedHeadersFormField.getControl(MatChipListHarness);
      const exposedHeadersAutocomplete = await exposedHeadersFormField.getControl(MatAutocompleteHarness);
      // Select `Cache-Control` with autocomplete
      await exposedHeadersAutocomplete.focus();
      await exposedHeadersAutocomplete.selectOption({ text: 'Cache-Control' });
      // Check headers
      const exposedHeadersChips = await Promise.all(await (await exposedHeadersChipList.getChips()).map((c) => c.getText()));
      expect(exposedHeadersChips).toEqual(['Cache-Control']);

      // # Max age
      const maxAgeFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Max age/ }));
      await (await maxAgeFormField.getControl(MatInputHarness)).setValue('666');

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

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
      expect(await (await allowOriginFormField.getControl(MatChipListHarness)).getChips()).toEqual([]);
      // Add `*` and confirm dialog
      const allowOriginChipListInput = await allowOriginFormField.getControl(MatChipInputHarness);

      await allowOriginChipListInput.setValue('*');
      await allowOriginChipListInput.blur();
      const dialogOne = await rootLoader.getHarness(MatDialogHarness);
      expect(await dialogOne.getId()).toEqual('allowAllOriginsConfirmDialog');
      await dialogOne.close();

      await allowOriginChipListInput.setValue('*');
      await allowOriginChipListInput.blur();
      const dialogTwo = await rootLoader.getHarness(MatDialogHarness);
      await (await dialogTwo.getHarness(MatButtonHarness.with({ text: /^Yes,/ }))).click();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await saveButton.click();

      expectConsoleSettingsSendRequest({
        cors: {
          allowOrigin: ['*'],
        },
      });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectConsoleSettingsSendRequest(consoleSettingsPayload: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings/`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toMatchObject(consoleSettingsPayload);
  }

  function expectConsoleSettingsGetRequest(consoleSettingsResponse: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings/`);
    expect(req.request.method).toEqual('GET');
    req.flush(consoleSettingsResponse);
  }
});
