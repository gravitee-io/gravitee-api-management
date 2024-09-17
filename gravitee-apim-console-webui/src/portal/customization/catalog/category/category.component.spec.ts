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
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';

import { CategoryCatalogComponent } from './category.component';
import { CategoryHarness } from './category.harness';

import { NewCategory } from '../../../../entities/category/NewCategory';
import { UpdateCategory } from '../../../../entities/category/UpdateCategory';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Category } from '../../../../entities/category/Category';
import { UpdateApi, Api as MAPIv2Api, fakeApiV2 as fakeMAPIv2Api } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { CategoryApi } from '../../../../entities/management-api-v2/category/categoryApi';
import { fakeCategoryApi } from '../../../../entities/management-api-v2/category/categoryApi.fixture';
import { GioApiSelectDialogHarness } from '../../../../shared/components/gio-api-select-dialog/gio-api-select-dialog.harness';
import { PortalCatalogComponent } from '../portal-catalog.component';

describe('CategoryCatalogComponent', () => {
  let component: CategoryCatalogComponent;
  let fixture: ComponentFixture<CategoryCatalogComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let router: Router;
  let componentHarness: CategoryHarness;

  const CATEGORY: Category = {
    id: 'cat',
    name: 'cat name',
    description: 'cat desc',
    key: '',
  };

  const init = async (categoryId: string) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalCatalogComponent, CategoryCatalogComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { params: of({ categoryId }) },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-category-u',
            'environment-category-d',
            'environment-category-c',
            'environment-api-u',
            'environment-api-r',
          ],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to avoid the warning
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CategoryCatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CategoryHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create', () => {
    beforeEach(async () => {
      await init('new');
      fixture.detectChanges();
    });

    it('should initialize with all input as blank', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        name: null,
        description: null,
      });
    });

    it('should be able to create', async () => {
      const spy = jest.spyOn(router, 'navigate');
      await componentHarness.getNameInput(harnessLoader).then((input) => input.setValue('Cat'));
      await componentHarness.getDescriptionInput(harnessLoader).then((input) => input.setValue('Cat desc'));

      const saveBar = await componentHarness.getSaveBar(harnessLoader);
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectPostCategory({ name: 'Cat', description: 'Cat desc' }, CATEGORY);
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith(['..', CATEGORY.id], expect.anything());
    });
  });

  describe('Update', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expect(component.mode).toEqual('edit');
      expectGetCategoryApis(CATEGORY.id);
    });

    it('should initialize with category input', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        name: CATEGORY.name,
        description: CATEGORY.description,
      });
    });

    it('should be able to update', async () => {
      await componentHarness.getNameInput(harnessLoader).then((input) => input.setValue('Cat'));
      await componentHarness.getDescriptionInput(harnessLoader).then((input) => input.setValue('Cat desc'));

      const saveBar = await componentHarness.getSaveBar(harnessLoader);
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, name: 'Cat', description: 'Cat desc' });
      expectGetCategory(CATEGORY);
      expectGetCategoryApis(CATEGORY.id);
    });
  });

  describe('Form', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();
      expectGetCategoryApis(CATEGORY.id);
    });

    it('should require name', async () => {
      const nameInput = await componentHarness.getNameInput(harnessLoader);
      expect(await nameInput.getValue()).toEqual(CATEGORY.name);
      expect(await componentHarness.getSaveBar(harnessLoader).then((saveBar) => saveBar.isVisible())).toBeFalsy();
      await nameInput.setValue('New name');
      expect(await componentHarness.getSaveBar(harnessLoader).then((saveBar) => saveBar.isVisible())).toBeTruthy();
      expect(await componentHarness.getSaveBar(harnessLoader).then((saveBar) => saveBar.isSubmitButtonInvalid())).toBeFalsy();
      await nameInput.setValue('');
      expect(await componentHarness.getSaveBar(harnessLoader).then((saveBar) => saveBar.isSubmitButtonInvalid())).toBeTruthy();
    });
  });

  describe('API List', () => {
    const APIS: CategoryApi[] = [
      fakeCategoryApi({ id: 'lowlight', name: 'Lowlight', order: 0 }),
      fakeCategoryApi({ id: 'highlight', name: 'Highlight', order: 1 }),
    ];
    const CAT_API_LIST: Category = { ...CATEGORY, highlightApi: 'highlight' };

    beforeEach(async () => {
      await init(CAT_API_LIST.id);
      expectGetCategory(CAT_API_LIST);
      fixture.detectChanges();
    });

    it('should show empty APIs', async () => {
      expectGetCategoryApis(CAT_API_LIST.id);
      const rows = await componentHarness.getTableRows(harnessLoader);
      expect(await rows[0].host().then((host) => host.text())).toContain('There are no APIs for this category.');
    });

    it('should show API list', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual('Lowlight');
      expect(await componentHarness.getNameByRowIndex(harnessLoader, 1)).toEqual('Highlight');
    });

    it('should remove API from category', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      const removeApiBtn = await componentHarness.getActionButtonByRowIndexAndTooltip(harnessLoader, 0, 'Remove API');
      expect(removeApiBtn).toBeTruthy();
      await removeApiBtn.click();
      const removeApiDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await removeApiDialog.confirm();
      const api = fakeMAPIv2Api({ id: 'lowlight', categories: [CAT_API_LIST.key, 'other-category'] });
      expectGetApi(api);
      expectUpdateApi({ ...api, categories: ['other-category'] }, { ...api, categories: ['other-category'] });
      expectGetCategory(CATEGORY);
      expectGetCategoryApis(CATEGORY.id);
    });
  });

  describe('Add API to Category', () => {
    const CAT_API_LIST: Category = { ...CATEGORY, highlightApi: 'highlight' };

    beforeEach(async () => {
      await init(CAT_API_LIST.id);
      expectGetCategory(CAT_API_LIST);
      fixture.detectChanges();

      expectGetCategoryApis(CAT_API_LIST.id);
      fixture.detectChanges();
      await componentHarness.addApiToCategory(harnessLoader);
    });

    it('should not allow user to choose API already in category', async () => {
      const apiToAdd = fakeMAPIv2Api({ id: CAT_API_LIST.id, name: CAT_API_LIST.name, categories: ['other-category', CATEGORY.key] });
      const dialog = await rootLoader.getHarness(GioApiSelectDialogHarness);
      await dialog.fillFormAndSubmit(CAT_API_LIST.name, () => {
        expectSearchApi(CAT_API_LIST.name, [apiToAdd]);
      });
      expectGetApi(apiToAdd);
      const snackbar = await rootLoader.getHarness(MatSnackBarHarness);
      expect(await snackbar.getMessage()).toEqual('API "cat name" is already defined in the category.');
    });

    it('should add API to category', async () => {
      const apiToAdd = fakeMAPIv2Api({ id: 'api-1', name: 'API 1', categories: [] });
      const dialog = await rootLoader.getHarness(GioApiSelectDialogHarness);
      await dialog.fillFormAndSubmit(CAT_API_LIST.name, () => {
        expectSearchApi(CAT_API_LIST.name, [apiToAdd]);
      });
      expectGetApi(apiToAdd);
      expectUpdateApi({ ...apiToAdd, categories: [CAT_API_LIST.key] }, { ...apiToAdd, categories: [CAT_API_LIST.key] });
      expectGetCategoryApis(CAT_API_LIST.id);
    });

    it('portal badges should be present', async () => {
      expect(await componentHarness.getBothPortalsForCategoryList()).toBeTruthy();
      expect(await componentHarness.getBothPortalsForNewCategory()).toBeTruthy();
    });
  });

  function expectGetCategory(category: Category) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories/${category.id}`).flush(category);
  }

  function expectPutCategory(category: UpdateCategory) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories/${category.id}`);
    expect(req.request.body).toEqual(category);
    req.flush(category);
  }

  function expectPostCategory(newCategory: NewCategory, category: Category) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories`, method: 'POST' });
    expect(req.request.body).toEqual(newCategory);
    req.flush(category);
  }

  function expectGetCategoryApis(categoryId: string, apis: CategoryApi[] = []) {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/categories/${categoryId}/apis?perPage=9999`)
      .flush({ data: apis, pagination: { totalCount: apis.length } });
  }

  function expectGetApi(api: MAPIv2Api) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`).flush(api);
  }

  function expectSearchApi(query: string, apis: MAPIv2Api[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=10`);

    expect(req.request.body).toEqual({ query });

    req.flush({ data: apis, pagination: { totalCount: apis.length } });
  }

  function expectUpdateApi(request: UpdateApi, response: MAPIv2Api) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${response.id}`, method: 'PUT' });
    expect(req.request.body).toEqual(request);
    req.flush(response);
  }
});
