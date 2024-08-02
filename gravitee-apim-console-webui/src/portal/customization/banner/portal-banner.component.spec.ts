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

describe('DeveloperPortalBannerComponent', () => {
  let fixture: ComponentFixture<PortalBannerComponent>;
  let componentHarness: PortalBannerHarness;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const PORTAL_SETTINGS = fakePortalSettings();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, NoopAnimationsModule, PortalBannerComponent],
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
          enabled: true,
          title: testTitle,
          subtitle: testSubtitle,
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
});
