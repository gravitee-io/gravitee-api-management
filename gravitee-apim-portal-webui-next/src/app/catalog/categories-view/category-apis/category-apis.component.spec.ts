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

import { CategoryApisComponent } from './category-apis.component';
import { ApiCardHarness } from '../../../../components/api-card/api-card.harness';
import { BreadcrumbNavigationHarness } from '../../../../components/breadcrumb-navigation/breadcrumb-navigation.harness';
import { fakeApi, fakeApisResponse } from '../../../../entities/api/api.fixtures';
import { ApisResponse } from '../../../../entities/api/apis-response';
import { Category } from '../../../../entities/categories/categories';
import { fakeCategory } from '../../../../entities/categories/categories.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { CatalogBannerHarness } from '../../components/catalog-banner/catalog-banner.harness';

describe('CategoryApisComponent', () => {
  let fixture: ComponentFixture<CategoryApisComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const init = async (categoryId: string, categories: Category[]) => {
    await TestBed.configureTestingModule({
      imports: [CategoryApisComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryApisComponent);
    fixture.componentRef.setInput('categoryId', categoryId);
    fixture.componentRef.setInput('categories', categories);

    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('With defined current category', () => {
    beforeEach(async () => {
      await init('my-category', [fakeCategory({ id: 'my-category' })]);
      expectApiList(fakeApisResponse({ data: [fakeApi()] }), '', 'my-category');
    });
    it('should load apis', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCards).toHaveLength(1);
    });
    it('should show banner', async () => {
      expect(await harnessLoader.getHarnessOrNull(CatalogBannerHarness)).toBeTruthy();
    });
    it('should show breadcrumb', async () => {
      expect(await harnessLoader.getHarnessOrNull(BreadcrumbNavigationHarness)).toBeTruthy();
    });
  });

  describe('Without defined current category', () => {
    beforeEach(async () => {
      await init('all', [fakeCategory({ id: 'my-category' })]);
      expectApiList(fakeApisResponse({ data: [fakeApi()] }), '', '');
    });
    it('should show all APIs', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCards).toHaveLength(1);
    });
  });

  function expectApiList(apisResponse: ApisResponse = fakeApisResponse(), q: string = '', category: string = '') {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/_search?page=1&category=${category}&size=18&q=${q}`).flush(apisResponse);
  }
});
