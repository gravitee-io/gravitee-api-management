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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { License } from '@gravitee/ui-particles-angular';

import { GioLicenseBannerModule } from './gio-license-banner.module';
import { GioLicenseBannerHarness } from './gio-license-banner.harness';

import { GioTestingModule } from '../../testing';

const onRequestUpgrade = jest.fn();

@Component({
  template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
  standalone: false,
})
class TestComponent {
  public onRequestUpgrade = onRequestUpgrade;
  public license: License;
  public isOEM: boolean;
}

describe('GioLicenseBannerModule', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioTestingModule, GioLicenseBannerModule],
    });
    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it.each([
    ['missing', null],
    [
      'oss',
      {
        tier: 'oss',
        packs: [],
        features: [],
        scope: 'PLATFORM',
      },
    ],
  ])('should display default information for %s license', async (_: string, license: License) => {
    fixture.componentInstance.license = license;
    fixture.componentInstance.isOEM = false;
    fixture.detectChanges();

    const harness = await loader.getHarness(GioLicenseBannerHarness);
    expect(await harness.getTitle()).toEqual('This configuration requires an enterprise license');
    expect(await harness.getBody()).toEqual(
      'Request a license to unlock enterprise functionality, such as support for connecting to event brokers, connecting to APIs via Websocket and Server-Sent Events, and publishing Webhooks.',
    );
    expect(await harness.buttonIsVisible()).toEqual(true);
    await harness.clickButton();
    expect(onRequestUpgrade).toHaveBeenCalled();
  });

  it('should display information for EE', async () => {
    fixture.componentInstance.license = {
      tier: 'universe',
      packs: [],
      features: [],
      scope: 'PLATFORM',
    };
    fixture.componentInstance.isOEM = false;
    fixture.detectChanges();

    const harness = await loader.getHarness(GioLicenseBannerHarness);
    expect(await harness.getTitle()).toEqual('This configuration requires a license upgrade');
    expect(await harness.getBody()).toEqual(
      'Your organization’s license does not support some features used in this API. Request an upgrade to enable the selected features.',
    );

    expect(await harness.buttonIsVisible()).toEqual(true);
    await harness.clickButton();
    expect(onRequestUpgrade).toHaveBeenCalled();
  });

  it.each([
    {
      tier: 'universe',
      packs: [],
      features: [],
      scope: 'ORGANIZATION',
    },
    {
      tier: 'oss',
      packs: [],
      features: [],
      scope: 'ORGANIZATION',
    },
  ])('should display information for Cloud', async (license: License) => {
    fixture.componentInstance.license = license;
    fixture.componentInstance.isOEM = false;
    fixture.detectChanges();

    const harness = await loader.getHarness(GioLicenseBannerHarness);
    expect(await harness.getTitle()).toEqual('This configuration requires a license upgrade');
    expect(await harness.getBody()).toEqual(
      'Your organization’s license does not support some features used in this API. Request an upgrade to enable the selected features.',
    );
    expect(await harness.buttonIsVisible()).toEqual(false);
  });

  it('should display information for OEM', async () => {
    fixture.componentInstance.license = {
      tier: 'universe',
      packs: [],
      features: ['oem-customization'],
      scope: 'ORGANIZATION',
    };
    fixture.componentInstance.isOEM = true;
    fixture.detectChanges();

    const harness = await loader.getHarness(GioLicenseBannerHarness);
    expect(await harness.getTitle()).toEqual('This configuration requires a license upgrade');
    expect(await harness.getBody()).toEqual(
      'Your platform license does not support this feature. Please contact your platform administrator to find out more.',
    );
    expect(await harness.buttonIsVisible()).toEqual(false);
  });
});
