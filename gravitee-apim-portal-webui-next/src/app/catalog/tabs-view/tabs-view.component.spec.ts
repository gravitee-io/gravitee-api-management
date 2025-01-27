/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatTabGroupHarness, MatTabHarness } from '@angular/material/tabs/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { TabsViewComponent } from './tabs-view.component';
import { ApiCardHarness } from '../../../components/api-card/api-card.harness';
import { fakeApi, fakeApisResponse } from '../../../entities/api/api.fixtures';
import { ApisResponse } from '../../../entities/api/apis-response';
import { Category } from '../../../entities/categories/categories';
import { fakeCategory } from '../../../entities/categories/categories.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('TabsViewComponent', () => {
  let fixture: ComponentFixture<TabsViewComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const initBase = async (
    params: Partial<{
      page: number;
      size: number;
      query: string;
      categoryId: string;
      categories: Category[];
    }> = {
      page: 1,
      size: 18,
      query: '',
      categoryId: '',
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [TabsViewComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({ filter: params.categoryId }) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TabsViewComponent);
    fixture.componentRef.setInput('categories', params.categories);

    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();
  };
  const init = async (
    params: Partial<{
      apisResponse: ApisResponse;
      page: number;
      size: number;
      query: string;
      categoryId: string;
      categories: Category[];
    }> = {
      apisResponse: fakeApisResponse(),
      page: 1,
      size: 18,
      query: '',
      categoryId: '',
      categories: [],
    },
  ) => {
    await initBase(params);
    expectApiList(params.apisResponse, params.page, params.size, params.query, params.categoryId);
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
    describe('when no results', () => {
      beforeEach(async () => {
        await init({ apisResponse: fakeApisResponse({ data: [] }) });
      });

      it('should show empty API list', async () => {
        const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
        expect(noApiCard).toBeTruthy();
        expect(await noApiCard.getText()).toContain(`Sorry, there are no APIs listed yet.`);
      });
    });

    describe('when error occurs', () => {
      it('should show empty API list if no search params', async () => {
        await initBase();
        httpTestingController
          .expectOne(`${TESTING_BASE_URL}/apis/_search?page=1&category=&size=18&q=`)
          .flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
        fixture.detectChanges();

        const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
        expect(noApiCard).toBeTruthy();
        expect(await noApiCard.getText()).toContain(`Sorry, there are no APIs listed yet.`);
      });

      it('should show empty API list if search params in request', async () => {
        await initBase({ categoryId: 'my-category' });
        // expectCategoriesList(fakeCategoriesResponse({ data: [fakeCategory({ id: 'my-category' })] }));
        httpTestingController
          .expectOne(`${TESTING_BASE_URL}/apis/_search?page=1&category=my-category&size=18&q=`)
          .flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
        fixture.detectChanges();

        const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
        expect(noApiCard).toBeTruthy();
        expect(await noApiCard.getText()).toContain(`Your search didn't return any APIs`);
      });
    });
  });

  describe('Category filters', () => {
    const CATEGORY_1 = fakeCategory({ id: 'category-1', name: 'Category 1' });
    const CATEGORY_2 = fakeCategory({ id: 'category-2', name: 'Category 2' });
    describe('With no categories', () => {
      beforeEach(async () => {
        await init({ categories: [] });
      });
      it('should not show filters if no categories', async () => {
        const categoryTabs = await harnessLoader.getHarnessOrNull(MatTabGroupHarness);
        expect(categoryTabs).toBeNull();
      });
    });

    describe('With no filter defined in params', () => {
      beforeEach(async () => {
        await init({ categories: [CATEGORY_1, CATEGORY_2] });
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
        await init({ categoryId: CATEGORY_2.id, categories: [CATEGORY_1, CATEGORY_2] });
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
});
