/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { By } from '@angular/platform-browser';

import { CatalogBannerComponent } from './catalog-banner.component';
import { BannerButton } from '../../../../entities/configuration/configuration-portal-next';
import { ConfigService } from '../../../../services/config.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('CatalogBannerComponent', () => {
  let harnessLoader: HarnessLoader;
  let fixture: ComponentFixture<CatalogBannerComponent>;

  const init = async (
    params: Partial<{ userIsConnected: boolean; primaryButton: BannerButton; secondaryButton: BannerButton }> = {
      userIsConnected: false,
      primaryButton: { enabled: false },
      secondaryButton: { enabled: false },
    },
  ) => {
    const primaryButton = params.primaryButton ?? { enabled: false };
    const secondaryButton = params.secondaryButton ?? { enabled: false };
    await TestBed.configureTestingModule({
      imports: [CatalogBannerComponent],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            baseURL: TESTING_BASE_URL,
            configuration: {
              portalNext: {
                banner: {
                  enabled: true,
                  title: 'Welcome to Gravitee Developer Portal!',
                  subtitle: 'Great subtitle',
                  primaryButton,
                  secondaryButton,
                },
              },
            },
          },
        },
        {
          provide: CurrentUserService,
          useValue: {
            isUserAuthenticated: signal(params.userIsConnected),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogBannerComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  };

  it('should display both banner buttons if they are enabled and public', async () => {
    await init({
      primaryButton: { enabled: true, label: 'Primary button', visibility: 'PUBLIC' },
      secondaryButton: { enabled: true, label: 'Secondary button', visibility: 'PUBLIC' },
    });

    const primaryButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Primary button' }));
    expect(primaryButton).toBeTruthy();

    const secondaryButton = fixture.debugElement.query(By.css('.welcome-banner__actions__secondary-button'));
    expect(secondaryButton).toBeTruthy();
  });
  it('should display both banner buttons if they are enabled and private and user is connected', async () => {
    await init({
      primaryButton: { enabled: true, label: 'Primary button', visibility: 'PRIVATE' },
      secondaryButton: { enabled: true, label: 'Secondary button', visibility: 'PRIVATE' },
      userIsConnected: true,
    });

    const primaryButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Primary button' }));
    expect(primaryButton).toBeTruthy();

    const secondaryButton = fixture.debugElement.query(By.css('.welcome-banner__actions__secondary-button'));
    expect(secondaryButton).toBeTruthy();
  });
  it('should not display banner buttons if they are enabled and private and user is not connected', async () => {
    await init({
      primaryButton: { enabled: true, label: 'Primary button', visibility: 'PRIVATE' },
      secondaryButton: { enabled: true, label: 'Secondary button', visibility: 'PRIVATE' },
      userIsConnected: false,
    });

    const primaryButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Primary button' }));
    expect(primaryButton).toBeNull();

    const secondaryButton = fixture.debugElement.query(By.css('.welcome-banner__actions__secondary-button'));
    expect(secondaryButton).toBeNull();
  });
  it('should not display banner buttons if they are disabled and public', async () => {
    await init({
      primaryButton: { enabled: false, label: 'Primary button', visibility: 'PUBLIC' },
      secondaryButton: { enabled: false, label: 'Secondary button', visibility: 'PUBLIC' },
    });

    const primaryButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Primary button' }));
    expect(primaryButton).toBeNull();

    const secondaryButton = fixture.debugElement.query(By.css('.welcome-banner__actions__secondary-button'));
    expect(secondaryButton).toBeNull();
  });
});
