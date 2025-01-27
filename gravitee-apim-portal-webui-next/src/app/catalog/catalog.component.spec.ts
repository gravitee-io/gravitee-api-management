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
import { HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { CatalogComponent } from './catalog.component';
import { fakeApisResponse } from '../../entities/api/api.fixtures';
import { ApisResponse } from '../../entities/api/apis-response';
import { fakeCategoriesResponse } from '../../entities/categories/categories.fixture';
import { BannerButton } from '../../entities/configuration/configuration-portal-next';
import { ConfigService } from '../../services/config.service';
import { CurrentUserService } from '../../services/current-user.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const initBase = async (
    params: Partial<{
      page: number;
      size: number;
      query: string;
      categoryId: string;
      userIsConnected: boolean;
      primaryButton: BannerButton;
      secondaryButton: BannerButton;
    }> = {
      page: 1,
      size: 18,
      query: '',
      categoryId: '',
      userIsConnected: false,
      primaryButton: { enabled: false },
      secondaryButton: { enabled: false },
    },
  ) => {
    const primaryButton = params.primaryButton ?? { enabled: false };
    const secondaryButton = params.secondaryButton ?? { enabled: false };
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({ filter: params.categoryId }) },
        },
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

    fixture = TestBed.createComponent(CatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  };
  const init = async (
    params: Partial<{
      userIsConnected: boolean;
      primaryButton: BannerButton;
      secondaryButton: BannerButton;
    }> = {
      userIsConnected: false,
      primaryButton: { enabled: false },
      secondaryButton: { enabled: false },
    },
  ) => {
    await initBase(params);
    expectCategoriesList(fakeCategoriesResponse());
    fixture.detectChanges();

    expectApiList(fakeApisResponse(), 1, 18, '');
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Banner', () => {
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

  function expectApiList(apisResponse: ApisResponse = fakeApisResponse(), page: number = 1, size: number = 18, category: string = '') {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/apis/_search?page=${page}&category=${category}&size=${size}&q=`)
      .flush(apisResponse);
  }

  function expectCategoriesList(categoriesResponse = fakeCategoriesResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/categories?size=-1`).flush(categoriesResponse);
  }
});
