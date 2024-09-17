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
import {
  GioFormTagsInputHarness,
  GioLicenseService,
  GioLicenseTestingModule,
  GioSaveBarHarness,
  License,
} from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { of } from 'rxjs';

import { PortalSettingsComponent } from './portal-settings.component';
import { PortalSettingsModule } from './portal-settings.module';
import { PortalSettingsHarness } from './portal-settings.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';

describe('PortalSettingsComponent', () => {
  let fixture: ComponentFixture<PortalSettingsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let portalSettingsMock;
  let componentHarness: PortalSettingsHarness;

  const init = async (
    permissions: GioTestingPermission = ['environment-settings-u'],
    licence: License = {
      tier: 'galaxy',
      packs: ['', ''],
      features: [''],
      isExpired: false,
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalSettingsModule, MatIconTestingModule, GioLicenseTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [...permissions],
        },
        {
          provide: GioLicenseService,
          useValue: {
            openDialog: jest.fn(),
            getLicense$: () => of(licence),
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
    fixture = TestBed.createComponent(PortalSettingsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalSettingsHarness);

    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Portal settings form', () => {
    beforeEach(() => {
      init();
    });

    it('display settings form and edit Company field', async () => {
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
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
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
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
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
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

    it('display settings form and edit Portal Next fields', async () => {
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      await toggle.toggle();
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        portalNext: {
          access: {
            enabled: false,
          },
          banner: {
            ...portalSettingsMock.portalNext.banner,
            enabled: true,
            title: 'testTitle',
            subtitle: 'testSubtitle',
          },
        },
      });
    });

    it('display settings form and edit CORS fields', async () => {
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
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
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
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
  describe('Portal next setting form', () => {
    beforeEach(() => {
      init();
    });
    it('should show portal next settings if settings have "access.enabled = true"', async () => {
      portalSettingsMock = fakePortalSettings({
        portalNext: {
          access: { enabled: true },
          banner: {
            enabled: true,
            title: 'testTitle',
            subtitle: 'testSubtitle',
          },
        },
      });
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      expect(await toggle.isChecked()).toBe(true);
    });

    it('should not show portal next settings if settings does not include: "access.enabled"', async () => {
      portalSettingsMock = fakePortalSettings({
        portalNext: {
          access: { enabled: false },
          banner: {
            enabled: false,
            title: 'testTitle',
            subtitle: 'testSubtitle',
          },
        },
      });
      expectPortalSettingsGetRequest(portalSettingsMock);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      expect(await toggle.isChecked()).toBe(false);
    });
  });

  describe('Portal next setting form', () => {
    beforeEach(() => {
      init(['environment-integration-u'], {
        tier: 'oss',
        packs: [],
        features: [],
      });
    });

    it('should not show portal next settings if license is oss', async () => {
      portalSettingsMock = fakePortalSettings({
        portalNext: {
          access: { enabled: true },
          banner: {
            enabled: false,
            title: 'testTitle',
            subtitle: 'testSubtitle',
          },
        },
      });
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      expect(fixture.nativeElement.querySelector('.portal__form_card')).toBeNull();
    });

    it('applies to both portals is present', async () => {
      const appliesToBothPortals = 'Applies to both portals';
      portalSettingsMock = fakePortalSettings({
        portalNext: {
          access: { enabled: true },
        },
      });
      expectPortalSettingsGetRequest(portalSettingsMock);
      expect(await componentHarness.getBadgeWarningText()).toEqual(appliesToBothPortals);
      expect(await componentHarness.getBadgeIcon()).toBeTruthy();
    });

    it('applies to both portals is absent', async () => {
      portalSettingsMock = fakePortalSettings({
        portalNext: {
          access: { enabled: false },
        },
      });
      expectPortalSettingsGetRequest(portalSettingsMock);

      const badgeWarning = await componentHarness.getBadgeWarningText().catch(() => null);
      const badgeIcon = await componentHarness.getBadgeIcon().catch(() => null);

      expect(badgeWarning).toBeNull();
      expect(badgeIcon).toBeNull();
    });
  });

  function expectPortalSettingsGetRequest(portalSettings: PortalSettings) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(portalSettings);
  }
});
