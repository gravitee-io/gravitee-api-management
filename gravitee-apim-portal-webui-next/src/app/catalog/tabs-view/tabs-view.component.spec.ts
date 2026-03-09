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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTabGroupHarness, MatTabHarness } from '@angular/material/tabs/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { TabsViewComponent } from './tabs-view.component';
import { fakeApisResponse } from '../../../entities/api/api.fixtures';
import { ApisResponse } from '../../../entities/api/apis-response';
import { Category } from '../../../entities/categories/categories';
import { fakeCategory } from '../../../entities/categories/categories.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('TabsViewComponent', () => {
  let fixture: ComponentFixture<TabsViewComponent>;
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
    fixture.componentRef.setInput('categories', params.categories ?? []);

    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();

    expectApiList(params.apisResponse, params.page, params.size, params.query, params.categoryId);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
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

        expect(routerNavigateSpy).toHaveBeenCalledWith([], {
          queryParams: { filter: CATEGORY_1.id, query: '' },
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

        expect(routerNavigateSpy).toHaveBeenCalledWith([], {
          queryParams: { filter: '', query: '' },
          relativeTo: expect.anything(),
        });
      });
      it('should have category selected if query defined', async () => {
        const category2Tab = await harnessLoader.getHarness(MatTabHarness.with({ label: CATEGORY_2.name }));
        expect(await category2Tab.isSelected()).toEqual(true);
      });
    });

    describe('when clicking on a category tab', () => {
      const CATEGORY_1 = fakeCategory({ id: 'category-1', name: 'Category 1' });
      const CATEGORY_2 = fakeCategory({ id: 'category-2', name: 'Category 2' });

      it('should navigate to the new category when switching from another category', async () => {
        await init({
          categoryId: CATEGORY_1.id,
          categories: [CATEGORY_1, CATEGORY_2],
          query: '',
        });
        routerNavigateSpy.mockClear();

        const matTabGroup = await harnessLoader.getHarness(MatTabGroupHarness);
        const selectedTab = await matTabGroup.getSelectedTab();
        expect(await selectedTab.getLabel()).toBe(CATEGORY_1.name);

        const category2Tab = await harnessLoader.getHarness(MatTabHarness.with({ label: CATEGORY_2.name }));
        await category2Tab.select();

        expect(routerNavigateSpy).toHaveBeenCalledTimes(1);
        expect(routerNavigateSpy).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: {
            filter: CATEGORY_2.id,
            query: '',
          },
        });
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
