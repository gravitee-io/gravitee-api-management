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
import { MatCardHarness } from '@angular/material/card/testing';
import { MatTabGroupHarness, MatTabHarness } from '@angular/material/tabs/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { CatalogComponent } from './catalog.component';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { fakeApi, fakeApisResponse } from '../../entities/api/api.fixtures';
import { ApisResponse } from '../../entities/api/apis-response';
import { Categories } from '../../entities/categories/categories';
import { fakeCategoriesResponse, fakeCategory } from '../../entities/categories/categories.fixture';
import { BannerButton } from '../../entities/configuration/configuration-portal-next';
import { ConfigService } from '../../services/config.service';
import { CurrentUserService } from '../../services/current-user.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    params: Partial<{
      apisResponse: ApisResponse;
      page: number;
      size: number;
      query: string;
      categoryId: string;
      categoriesResponse: Categories;
      userIsConnected: boolean;
      primaryButton: BannerButton;
      secondaryButton: BannerButton;
    }> = {
      apisResponse: fakeApisResponse(),
      page: 1,
      size: 18,
      query: '',
      categoryId: '',
      categoriesResponse: fakeCategoriesResponse(),
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
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();

    expectApiList(params.apisResponse, params.page, params.size, params.query, params.categoryId);
    expectCategoriesList(params.categoriesResponse);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('populated api list', () => {
    beforeEach(async () => {
      await init({
        apisResponse: fakeApisResponse({
          data: [
            fakeApi({
              id: '1',
              name: 'Test title',
              version: 'v.1.2',
              description:
                'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
            }),
          ],
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 2,
            },
          },
        }),
      });
    });

    it('should render banner text', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('app-banner')?.textContent).toContain('Welcome to Gravitee Developer Portal!');
    });

    it('should show API list', async () => {
      const apiCard = await harnessLoader.getHarness(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(await apiCard.getTitle()).toEqual('Test title');
      expect(await apiCard.getDescription()).toEqual(
        'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
      );
      expect(await apiCard.getVersion()).toEqual('v.1.2');
    });

    it('should call second page after scrolled event', async () => {
      const apiCard = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(apiCard.length).toEqual(1);
      expect(await apiCard[0].getTitle()).toEqual('Test title');

      document.getElementsByClassName('api-list__container')[0].dispatchEvent(new Event('scrolled'));
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api', name: 'second page api', version: '24' })],
          metadata: {
            pagination: {
              current_page: 3,
              total_pages: 3,
            },
          },
        }),
        3,
        9,
        '',
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(allHarnesses.length).toEqual(2);

      const secondPageApi = await harnessLoader.getHarnessOrNull(ApiCardHarness.with({ selector: '[ng-reflect-id="second-page-api"]' }));
      expect(secondPageApi).toBeTruthy();
    });

    it('should call API list with search query', async () => {
      const apiCard = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(apiCard.length).toEqual(1);
      expect(await apiCard[0].getTitle()).toEqual('Test title');

      document.getElementsByClassName('api-list__container')[0].dispatchEvent(new Event('scrolled'));
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api', name: 'second page api', version: '24' })],
          metadata: {
            pagination: {
              current_page: 3,
              total_pages: 5,
            },
          },
        }),
        3,
        9,
        '',
      );
      fixture.detectChanges();
    });

    it('should not call page if on last page', async () => {
      const apiCard = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCard.length).toEqual(1);

      document.getElementsByClassName('api-list__container')[0].dispatchEvent(new Event('scrolled'));
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api' })],
          metadata: {
            pagination: {
              current_page: 3,
              total_pages: 3,
            },
          },
        }),
        3,
        9,
        '',
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(allHarnesses.length).toEqual(2);

      document.getElementsByClassName('api-list__container')[0].dispatchEvent(new Event('scrolled'));
      fixture.detectChanges();

      httpTestingController.expectNone(`${TESTING_BASE_URL}/apis?page=3&size=9`);
    });
  });

  describe('empty component', () => {
    beforeEach(async () => {
      await init({ apisResponse: fakeApisResponse({ data: [] }) });
    });

    it('should show empty API list', async () => {
      const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
      expect(noApiCard).toBeTruthy();
      expect(await noApiCard.getText()).toContain(`Sorry, there are no APIs listed yet.`);
    });
  });

  describe('Category filters', () => {
    const CATEGORY_1 = fakeCategory({ id: 'category-1', name: 'Category 1' });
    const CATEGORY_2 = fakeCategory({ id: 'category-2', name: 'Category 2' });
    describe('With no categories', () => {
      beforeEach(async () => {
        await init({ categoriesResponse: fakeCategoriesResponse({ data: [] }) });
      });
      it('should not show filters if no categories', async () => {
        const categoryTabs = await harnessLoader.getHarnessOrNull(MatTabGroupHarness);
        expect(categoryTabs).toBeNull();
      });
    });

    describe('With no filter defined in params', () => {
      beforeEach(async () => {
        await init({ categoriesResponse: fakeCategoriesResponse({ data: [CATEGORY_1, CATEGORY_2] }) });
      });
      it('should categories + All as filters', async () => {
        const tabs = await harnessLoader.getAllHarnesses(MatTabHarness);
        expect(tabs).toHaveLength(3);
        expect(await tabs[0].getLabel()).toEqual('All');
        expect(await tabs[1].getLabel()).toEqual(CATEGORY_1.name);
        expect(await tabs[2].getLabel()).toEqual(CATEGORY_2.name);
      });
      it('should navigate to category', async () => {
        const category1Tab = await harnessLoader.getHarness(MatTabHarness.with({ label: CATEGORY_1.name }));
        await category1Tab.select();

        expect(routerNavigateSpy).toHaveBeenCalledWith([''], {
          queryParams: { filter: CATEGORY_1.id, query: undefined },
          relativeTo: expect.anything(),
        });
      });
    });

    describe('With specified filter in params', () => {
      beforeEach(async () => {
        await init({ categoryId: CATEGORY_2.id, categoriesResponse: fakeCategoriesResponse({ data: [CATEGORY_1, CATEGORY_2] }) });
      });

      it('should navigate to All', async () => {
        const allTab = await harnessLoader.getHarness(MatTabHarness.with({ label: 'All' }));
        await allTab.select();

        expect(routerNavigateSpy).toHaveBeenCalledWith([''], {
          queryParams: { filter: '', query: undefined },
          relativeTo: expect.anything(),
        });
      });
      it('should have category selected if query defined', async () => {
        const category2Tab = await harnessLoader.getHarness(MatTabHarness.with({ label: CATEGORY_2.name }));
        expect(await category2Tab.isSelected()).toEqual(true);
      });
    });
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

  function expectApiList(
    apisResponse: ApisResponse = fakeApisResponse(),
    page: number = 1,
    size: number = 18,
    q: string = '',
    category: string = '',
  ) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/apis/_search?page=${page}&category=${category}&size=${size}&q=${q}`)
      .flush(apisResponse);
  }

  function expectCategoriesList(categoriesResponse = fakeCategoriesResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/categories?size=-1`).flush(categoriesResponse);
  }
});
