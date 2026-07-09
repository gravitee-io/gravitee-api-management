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
import { MatButtonHarness } from '@angular/material/button/testing';
import {
  GioConfirmDialogHarness,
  GioFormTagsInputHarness,
  GioLicenseService,
  GioLicenseTestingModule,
  GioSaveBarHarness,
  License,
} from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { SpanHarness } from '@gravitee/ui-particles-angular/testing';
import { of } from 'rxjs';

import { PortalSettingsComponent } from './portal-settings.component';
import { PortalSettingsModule } from './portal-settings.module';
import { PortalSettingsHarness } from './portal-settings.harness';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';

describe('PortalSettingsComponent', () => {
  let fixture: ComponentFixture<PortalSettingsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let portalSettingsMock;
  let componentHarness: PortalSettingsHarness;
  const removedBannerMessage = [
    'This tech preview feature',
    "is new! We're gathering feedback on it to make it even better,",
    'so it may change as we make improvements.',
  ].join(' ');

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
    beforeEach(async () => {
      await init();
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

      const keylessPlansToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid="keyless-plans-toggle"]' }));
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
          mtls: {
            enabled: false,
          },
          analytics: {
            enabled: false,
          },
          applications: {
            membership: {
              enabled: false,
              transferOwnership: { enabled: false },
              invitations: { enabled: false },
            },
          },
          catalog: {
            fuzzySearch: { enabled: false },
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

    it('display settings form and edit Portal Next analytics toggle', async () => {
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const analyticsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-analytics' }));
      await analyticsToggle.toggle();
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.analytics.enabled).toEqual(true);
    });

    it('display settings form and edit Portal Next member mapping toggle', async () => {
      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-member-mapping' }));
      expect(await toggle.isChecked()).toBe(false);
      await toggle.toggle();
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.applications.membership.enabled).toEqual(true);
    });

    it('display settings form and edit Portal Next transfer of ownership toggle', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }));
      expect(await toggle.isChecked()).toBe(false);
      await toggle.toggle();
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.applications.membership.transferOwnership.enabled).toEqual(true);
    });

    it('display settings form and edit Portal Next invitations toggle', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-invitations' }));
      expect(await toggle.isChecked()).toBe(false);
      await toggle.toggle();
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.applications.membership.invitations.enabled).toEqual(true);
    });

    it('should disable Portal Next child toggles when Portal Next is disabled and enable them again when Portal Next is enabled', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.access.enabled = false;
      portalSettingsMock.portalNext.mtls.enabled = true;
      portalSettingsMock.portalNext.analytics.enabled = true;
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.transferOwnership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.invitations.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);

      const portalNextToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      const mtlsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-mtls' }));
      const analyticsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-analytics' }));
      const membershipToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-member-mapping' }));
      const transferOwnershipToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }),
      );
      const invitationsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-invitations' }));

      expect(await mtlsToggle.isDisabled()).toEqual(true);
      expect(await analyticsToggle.isDisabled()).toEqual(true);
      expect(await membershipToggle.isDisabled()).toEqual(true);
      expect(await transferOwnershipToggle.isDisabled()).toEqual(true);
      expect(await invitationsToggle.isDisabled()).toEqual(true);
      expect(await mtlsToggle.isChecked()).toEqual(true);
      expect(await transferOwnershipToggle.isChecked()).toEqual(true);

      await portalNextToggle.toggle();

      expect(await mtlsToggle.isDisabled()).toEqual(false);
      expect(await analyticsToggle.isDisabled()).toEqual(false);
      expect(await membershipToggle.isDisabled()).toEqual(false);
      expect(await transferOwnershipToggle.isDisabled()).toEqual(false);
      expect(await invitationsToggle.isDisabled()).toEqual(false);
    });

    it('should disable membership child toggles when application membership is disabled and enable them again when membership is enabled', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.applications.membership.enabled = false;
      portalSettingsMock.portalNext.applications.membership.transferOwnership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.invitations.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);

      const membershipToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-member-mapping' }));
      const transferOwnershipToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }),
      );
      const invitationsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-invitations' }));

      expect(await transferOwnershipToggle.isDisabled()).toEqual(true);
      expect(await invitationsToggle.isDisabled()).toEqual(true);
      expect(await transferOwnershipToggle.isChecked()).toEqual(true);
      expect(await invitationsToggle.isChecked()).toEqual(true);

      await membershipToggle.toggle();

      expect(await transferOwnershipToggle.isDisabled()).toEqual(false);
      expect(await invitationsToggle.isDisabled()).toEqual(false);
    });

    it('should keep readonly Portal Next child toggles disabled when their parents are enabled', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      portalSettingsMock.metadata.readonly = ['portal.next.mtls.enabled', 'portal.next.applications.membership.invitations.enabled'];
      expectPortalSettingsGetRequest(portalSettingsMock);

      const mtlsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-mtls' }));
      const analyticsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-analytics' }));
      const transferOwnershipToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }),
      );
      const invitationsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-invitations' }));

      expect(await mtlsToggle.isDisabled()).toEqual(true);
      expect(await analyticsToggle.isDisabled()).toEqual(false);
      expect(await transferOwnershipToggle.isDisabled()).toEqual(false);
      expect(await invitationsToggle.isDisabled()).toEqual(true);
    });

    it('should preserve Portal Next child toggle values when disabled controls are saved', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.mtls.enabled = true;
      portalSettingsMock.portalNext.analytics.enabled = true;
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.transferOwnership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.invitations.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const portalNextToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      const mtlsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-mtls' }));
      const transferOwnershipToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }),
      );

      await portalNextToggle.toggle();

      expect(await mtlsToggle.isDisabled()).toEqual(true);
      expect(await mtlsToggle.isChecked()).toEqual(true);
      expect(await transferOwnershipToggle.isDisabled()).toEqual(true);
      expect(await transferOwnershipToggle.isChecked()).toEqual(true);

      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.access.enabled).toEqual(false);
      expect(req.request.body.portalNext.mtls.enabled).toEqual(true);
      expect(req.request.body.portalNext.analytics.enabled).toEqual(true);
      expect(req.request.body.portalNext.applications.membership.enabled).toEqual(true);
      expect(req.request.body.portalNext.applications.membership.transferOwnership.enabled).toEqual(true);
      expect(req.request.body.portalNext.applications.membership.invitations.enabled).toEqual(true);
    });

    it('should preserve membership child toggle values when membership disabled controls are saved', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portalNext.applications.membership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.transferOwnership.enabled = true;
      portalSettingsMock.portalNext.applications.membership.invitations.enabled = true;
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const membershipToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-member-mapping' }));
      const transferOwnershipToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '#enable-portal-next-transfer-ownership' }),
      );
      const invitationsToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next-invitations' }));

      await membershipToggle.toggle();

      expect(await transferOwnershipToggle.isDisabled()).toEqual(true);
      expect(await transferOwnershipToggle.isChecked()).toEqual(true);
      expect(await invitationsToggle.isDisabled()).toEqual(true);
      expect(await invitationsToggle.isChecked()).toEqual(true);

      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.portalNext.applications.membership.enabled).toEqual(false);
      expect(req.request.body.portalNext.applications.membership.transferOwnership.enabled).toEqual(true);
      expect(req.request.body.portalNext.applications.membership.invitations.enabled).toEqual(true);
    });

    it('should disable Portal Next action buttons when New Developer Portal is disabled and enable them again when it is enabled', async () => {
      portalSettingsMock = fakePortalSettings();
      portalSettingsMock.portal.url = 'https://portal.example.com';
      portalSettingsMock.portalNext.access.enabled = false;
      expectPortalSettingsGetRequest(portalSettingsMock);

      const portalNextToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '#enable-portal-next' }));
      const openWebsiteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="open-website"]' }));
      const openSettingsButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="open-settings"]' }));

      expect(await openWebsiteButton.isDisabled()).toEqual(true);
      expect(await openSettingsButton.isDisabled()).toEqual(true);

      await portalNextToggle.toggle();

      expect(await openWebsiteButton.isDisabled()).toEqual(false);
      expect(await openSettingsButton.isDisabled()).toEqual(false);
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

    it('display settings form and edit Documentation field', async () => {
      const message = 'Page not found';

      portalSettingsMock = fakePortalSettings();
      expectPortalSettingsGetRequest(portalSettingsMock);
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const pageNotFoundMessageInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=pageNotFoundMessage' }));
      await pageNotFoundMessageInput.setValue(message);
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        ...portalSettingsMock,
        documentation: {
          ...portalSettingsMock.documentation,
          pageNotFoundMessage: message,
        },
      });
    });
  });
  describe('Portal next setting form', () => {
    beforeEach(async () => {
      await init();
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

    it('should not show tech preview banner in Portal Next settings', async () => {
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
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).not.toContain(removedBannerMessage);
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
    beforeEach(async () => {
      await init(['environment-integration-u'], {
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

  describe('branded senders', () => {
    beforeEach(async () => {
      await init();
    });

    it('should render the branded senders control enabled when emailing is on and not read-only', async () => {
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      expectPortalSettingsGetRequest(settings);

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toBe(false);
    });

    it('should disable branded senders when emailing is off', async () => {
      const settings = fakePortalSettings();
      settings.email.enabled = false;
      expectPortalSettingsGetRequest(settings);

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toBe(true);
    });

    it('should disable branded senders when email.branded_senders is read-only', async () => {
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      settings.metadata.readonly.push('email.branded_senders');
      expectPortalSettingsGetRequest(settings);

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toBe(true);
    });

    it('should round-trip branded senders from a populated GET through save at env scope', async () => {
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      settings.email.brandedSenders = [{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }];
      expectPortalSettingsGetRequest(settings);

      const domainsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="domains"]' }));
      await domainsInput.addTag('example.org');

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.email.brandedSenders).toEqual([
        { domains: ['example.com', 'example.org'], from: 'noreply@example.com', subject: '[Example] %s' },
      ]);
    });
  });

  describe('branded senders permissions', () => {
    it('should disable branded senders when the user lacks environment-settings update permission', async () => {
      await init([]);
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      expectPortalSettingsGetRequest(settings);

      const addConfigurationButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addConfigurationButton.isDisabled()).toBe(true);
    });
  });

  describe('branded senders reset', () => {
    const resetButton = () => loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="reset-branded-senders"]' }));

    const overriddenSettings = () => {
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      settings.email.brandedSenders = [{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }];
      settings.email.brandedSendersInherited = false;
      return settings;
    };

    it('should show the reset button when there is an environment-level override', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());

      expect(await resetButton()).not.toBeNull();
    });

    it('should hide the reset button when the value is inherited from the organization', async () => {
      await init();
      const settings = fakePortalSettings();
      settings.email.enabled = true;
      settings.email.brandedSendersInherited = true;
      expectPortalSettingsGetRequest(settings);

      expect(await resetButton()).toBeNull();
    });

    it('should hide the reset button when email.branded_senders is read-only', async () => {
      await init();
      const settings = overriddenSettings();
      settings.metadata.readonly.push('email.branded_senders');
      expectPortalSettingsGetRequest(settings);

      expect(await resetButton()).toBeNull();
    });

    it('should hide the reset button when the user lacks environment-settings update permission', async () => {
      await init([]);
      expectPortalSettingsGetRequest(overriddenSettings());

      expect(await resetButton()).toBeNull();
    });

    it('should hide the reset button when the inheritance flag is absent from the response', async () => {
      await init();
      const settings = overriddenSettings();
      delete settings.email.brandedSendersInherited;
      expectPortalSettingsGetRequest(settings);

      expect(await resetButton()).toBeNull();
    });

    it('should reset via the dedicated endpoint, not through save', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());

      await (await resetButton())!.click();

      // The reset endpoint is called; verify() (afterEach) fails if a /settings POST were issued instead.
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings/email/branded-senders/reset`);
      expect(req.request.method).toEqual('POST');
    });

    it('should show a success snackbar, re-fetch, and re-display the inherited configuration with the badge on success', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());
      const snackBarSuccess = jest.spyOn(TestBed.inject(SnackBarService), 'success');

      await (await resetButton())!.click();

      const inheritedSettings = fakePortalSettings();
      inheritedSettings.email.enabled = true;
      inheritedSettings.email.brandedSenders = [{ domains: ['org.com'], from: 'noreply@org.com', subject: '[Org] %s' }];
      inheritedSettings.email.brandedSendersInherited = true;

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings/email/branded-senders/reset`).flush(inheritedSettings);
      // The service reloads the environment settings cache, then ngOnInit re-fetches the settings.
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/portal`).flush({});
      expectPortalSettingsGetRequest(inheritedSettings);

      expect(snackBarSuccess).toHaveBeenCalled();
      // Falls back to the org-inherited configuration: badge shown, reset button gone (now inherited).
      const badge = await loader.getHarnessOrNull(SpanHarness.with({ selector: '[data-testid="branded-senders-inherited-badge"]' }));
      expect(badge).not.toBeNull();
      expect(await resetButton()).toBeNull();
    });

    it('should show an error snackbar and not re-fetch when the reset fails', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());
      const snackBarError = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      await (await resetButton())!.click();

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings/email/branded-senders/reset`)
        .flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });

      expect(snackBarError).toHaveBeenCalledWith('Boom');
      // No re-fetch on error — verify() in afterEach fails if a GET /settings or /portal were issued.
    });

    it('should confirm before resetting when the form has unsaved changes, then reset on confirm', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());

      // Unrelated unsaved edit makes the form dirty.
      const companyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=apikeyHeader' }));
      await companyInput.setValue('Unsaved edit');

      await (await resetButton())!.click();

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings/email/branded-senders/reset`);
      expect(req.request.method).toEqual('POST');
    });

    it('should not reset when the unsaved-changes confirmation is cancelled', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());

      const companyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=apikeyHeader' }));
      await companyInput.setValue('Unsaved edit');

      await (await resetButton())!.click();

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await confirmDialog.cancel();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.baseURL}/settings/email/branded-senders/reset`);
    });

    it('should still persist an empty override through save (not reset) when all configurations are removed and saved', async () => {
      await init();
      expectPortalSettingsGetRequest(overriddenSettings());

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Delete configuration"]' }));
      await deleteButton.click();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.email.brandedSenders).toEqual([]);
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
