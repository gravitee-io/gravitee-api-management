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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConsoleSettingsComponent } from './console-settings';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { ConsoleSettings } from '../../../entities/consoleSettings';

describe('ConsoleSettingsComponent', () => {
  let fixture: ComponentFixture<ConsoleSettingsComponent>;
  let loader: HarnessLoader;

  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ConsoleSettingsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

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
