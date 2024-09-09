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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { PortalBannerComponent } from './portal-banner.component';
import { PortalBannerHarness } from './portal-banner.harness';

import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('DeveloperPortalBannerComponent', () => {
  let fixture: ComponentFixture<PortalBannerComponent>;
  let componentHarness: PortalBannerHarness;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const PORTAL_SETTINGS = fakePortalSettings();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, NoopAnimationsModule, PortalBannerComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-settings-u'],
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(PortalBannerComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalBannerHarness);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const getSettings = () => {
    const requests = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
    });

    requests.flush(PORTAL_SETTINGS);
    fixture.detectChanges();
  };

  it('should render None radio button selected and not render featured banner elements', async () => {
    getSettings();
    await componentHarness.disableBanner();
    fixture.detectChanges();

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    const request = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
    });

    request.flush({});
    expect(request.request.body).toEqual({
      ...PORTAL_SETTINGS,
      portalNext: {
        ...PORTAL_SETTINGS.portalNext,
        banner: {
          ...PORTAL_SETTINGS.portalNext.banner,
          enabled: false,
          primaryButton: {
            enabled: false,
            label: '',
            target: '',
            type: 'EXTERNAL',
            visibility: 'PUBLIC',
          },
          secondaryButton: {
            enabled: false,
            label: '',
            target: '',
            type: 'EXTERNAL',
            visibility: 'PUBLIC',
          },
        },
      },
    });
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/portal` }).flush({});
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(PORTAL_SETTINGS);
  });

  it('should render Featured banner radio button selected and render featured banner elements', async () => {
    const testTitle = 'Test Title';
    const testSubtitle = 'Test Subtitle';

    getSettings();
    await componentHarness.enableBanner();
    fixture.detectChanges();

    await componentHarness.setTitle(testTitle);
    await componentHarness.setSubtitle(testSubtitle);

    const title = await componentHarness.getTitle();
    const subtitle = await componentHarness.getSubtitle();

    expect(title).toBe(testTitle);
    expect(subtitle).toBe(testSubtitle);

    const primaryButtonText = await componentHarness.getPrimaryButtonTextInput();
    expect(await primaryButtonText.getValue()).toEqual('');
    await primaryButtonText.setValue('Primary button');

    const primaryButtonEnableToggle = await componentHarness.getPrimaryButtonEnableToggle();
    expect(await primaryButtonEnableToggle.isChecked()).toEqual(false);
    await primaryButtonEnableToggle.toggle();

    const currentPrimaryButtonVisibility = await componentHarness.getPrimaryButtonVisibilityValue();
    expect(currentPrimaryButtonVisibility).toEqual('PUBLIC');
    await componentHarness.setPrimaryButtonVisibility('PRIVATE');

    const primaryButtonTarget = await componentHarness.getPrimaryButtonTargetInput();
    expect(await primaryButtonTarget.getValue()).toEqual('');
    await primaryButtonTarget.setValue('cats-rule');

    const secondaryButtonText = await componentHarness.getSecondaryButtonTextInput();
    expect(await secondaryButtonText.getValue()).toEqual('');
    await secondaryButtonText.setValue('Secondary button');

    const secondaryButtonEnableToggle = await componentHarness.getSecondaryButtonEnableToggle();
    expect(await secondaryButtonEnableToggle.isChecked()).toEqual(false);
    await secondaryButtonEnableToggle.toggle();

    const currentSecondaryButtonVisibility = await componentHarness.getSecondaryButtonVisibilityValue();
    expect(currentSecondaryButtonVisibility).toEqual('PUBLIC');
    await componentHarness.setSecondaryButtonVisibility('PRIVATE');

    const secondaryButtonTarget = await componentHarness.getSecondaryButtonTargetInput();
    expect(await secondaryButtonTarget.getValue()).toEqual('');
    await secondaryButtonTarget.setValue('dogs-drool');

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    const request = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
    });

    expect(request.request.body).toEqual({
      ...PORTAL_SETTINGS,
      portalNext: {
        ...PORTAL_SETTINGS.portalNext,
        banner: {
          enabled: true,
          title: testTitle,
          subtitle: testSubtitle,
          primaryButton: {
            enabled: true,
            label: 'Primary button',
            target: 'cats-rule',
            type: 'EXTERNAL',
            visibility: 'PRIVATE',
          },
          secondaryButton: {
            enabled: true,
            label: 'Secondary button',
            target: 'dogs-drool',
            type: 'EXTERNAL',
            visibility: 'PRIVATE',
          },
        },
      },
    });
    request.flush({});

    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/portal` }).flush({});
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(PORTAL_SETTINGS);
  });

  it('should require text + target when primary button is enabled', async () => {
    getSettings();

    const primaryButtonEnableToggle = await componentHarness.getPrimaryButtonEnableToggle();
    expect(await primaryButtonEnableToggle.isChecked()).toEqual(false);
    await primaryButtonEnableToggle.toggle();

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

    const primaryButtonText = await componentHarness.getPrimaryButtonTextInput();
    await primaryButtonText.setValue('Primary button');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

    const primaryButtonTarget = await componentHarness.getPrimaryButtonTargetInput();
    await primaryButtonTarget.setValue('cats-rule');

    await componentHarness.setPrimaryButtonVisibility('PRIVATE');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    const request = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
    });
    expect(request.request.body).toEqual({
      ...PORTAL_SETTINGS,
      portalNext: {
        ...PORTAL_SETTINGS.portalNext,
        banner: {
          ...PORTAL_SETTINGS.portalNext.banner,
          enabled: true,
          primaryButton: {
            enabled: true,
            label: 'Primary button',
            target: 'cats-rule',
            type: 'EXTERNAL',
            visibility: 'PRIVATE',
          },
          secondaryButton: {
            enabled: false,
            label: '',
            target: '',
            type: 'EXTERNAL',
            visibility: 'PUBLIC',
          },
        },
      },
    });
    request.flush({});

    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/portal` }).flush({});
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(PORTAL_SETTINGS);
  });
});
