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
import { PortalSettings } from 'src/entities/portal/portalSettings';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioFormTagsInputHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { PortalNgComponent } from './portal-ng.component';
import { PortalNgModule } from './portal-ng.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';

describe('PortalNgComponent', () => {
  let fixture: ComponentFixture<PortalNgComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const portalSettingsMock = fakePortalSettings();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNgModule, MatIconTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-settings-u'],
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PortalNgComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    expectPortalSettingsGetRequest(portalSettingsMock);
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Portal settings form', () => {
    it('display settings form and edit Company field', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const companyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=apikeyHeader' }));
      await companyInput.setValue('Test company');
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        portal: {
          ...portalSettingsMock.portal,
          apikeyHeader: 'Test company',
        },
      });
    });

    it('display settings form and edit Console fields', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const keylessPlansToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[ng-reflect-aria-label="Keyless plans"]' }),
      );
      expect(await keylessPlansToggle.isChecked()).toBe(false);

      await keylessPlansToggle.toggle();
      expect(await keylessPlansToggle.isChecked()).toBe(true);

      const allowOriginInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labelsDictionary"]' }));
      expect(await allowOriginInput.isDisabled()).toEqual(false);
      await allowOriginInput.addTag('toto');

      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        api: {
          ...portalSettingsMock.api,
          labelsDictionary: ['test', 'toto'],
        },
        plan: {
          security: {
            ...portalSettingsMock.plan.security,
            keyless: {
              enabled: true,
            },
          },
        },
      });
    });

    it('display settings form and edit Portal fields', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const urlInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=url' }));
      await urlInput.setValue('Test api');
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        portal: {
          ...portalSettingsMock.portal,
          url: 'Test api',
        },
      });
    });

    it('display settings form and edit CORS fields', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const apikeyHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=maxAge' }));
      await apikeyHeaderInput.setValue('10');
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        cors: {
          ...portalSettingsMock.cors,
          maxAge: 10,
        },
      });
    });

    it('display settings form and edit SMTP fields', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const apikeyHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=protocol' }));
      await apikeyHeaderInput.setValue('Test protocol');
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        email: {
          ...portalSettingsMock.email,
          protocol: 'Test protocol',
        },
      });
    });
  });

  function expectPortalSettingsGetRequest(portalSettings: PortalSettings) {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/settings` }).flush(portalSettings);
  }
});
