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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatTableHarness } from '@angular/material/table/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { CategoryCatalogComponent } from './category.component';
import { CategoryHarness } from './category.harness';
import { AddApiToCategoryDialogHarness } from './add-api-to-category-dialog/add-api-to-category-dialog.harness';
import { AddApiProductToCategoryDialogHarness } from './add-api-product-to-category-dialog/add-api-product-to-category-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakePortalCategory,
  fakePortalNavigationApi,
  fakePortalNavigationApiProduct,
  fakePortalNavigationItemsResponse,
  PortalCategory,
  PortalNavigationApi,
  PortalNavigationApiProduct,
  PortalNavigationItemApiSummary,
  UpdateApiPortalNavigationItem,
  UpdateApiProductPortalNavigationItem,
} from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { PortalCatalogComponent } from '../portal-catalog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import {
  expectCreatePortalCategoryRequest,
  expectListPortalCategoriesRequest,
  expectUpdatePortalCategoryRequest,
} from '../../../services-ngx/portal-category.service.spec';

describe('CategoryCatalogComponent', () => {
  let component: CategoryCatalogComponent;
  let fixture: ComponentFixture<CategoryCatalogComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let router: Router;
  let componentHarness: CategoryHarness;

  const CATEGORY: PortalCategory = fakePortalCategory({ id: 'cat', title: 'cat title', description: 'cat desc', visible: true });
  const DEFAULT_PERMISSIONS = [
    'environment-category-u',
    'environment-category-d',
    'environment-category-c',
    'environment-documentation-u',
    'environment-documentation-r',
  ];

  const init = async (categoryId: string, permissions = DEFAULT_PERMISSIONS) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalCatalogComponent, CategoryCatalogComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { params: of({ categoryId }), snapshot: { params: { categoryId } } },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
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
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CategoryHarness);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  const expectGetPortalNavigationItems = (items: (PortalNavigationApi | PortalNavigationApiProduct)[] = []) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR`,
    });
    req.flush(fakePortalNavigationItemsResponse({ items }));
  };

  const expectGetPortalNavigationItemsWithApis = (
    items: (PortalNavigationApi | PortalNavigationApiProduct)[] = [],
    apisById: Record<string, PortalNavigationItemApiSummary> = {},
  ) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR&includes=apis`,
    });
    req.flush(fakePortalNavigationItemsResponse({ items, metadata: { apis: apisById } }));
  };

  const expectUpdateNavigationItem = (
    navItemId: string,
    expectedBody: UpdateApiPortalNavigationItem | UpdateApiProductPortalNavigationItem,
    response: PortalNavigationApi | PortalNavigationApiProduct,
  ) => {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${navItemId}`,
    });
    expect(req.request.body).toEqual(expectedBody);
    req.flush(response);
  };

  const expectSearchApiProducts = (ids: string[], products: ApiProduct[]) => {
    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search?page=1&perPage=100`,
    });
    expect(req.request.body).toEqual({ ids });
    req.flush({ data: products, pagination: { totalCount: products.length } });
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
        title: null,
        description: null,
        visible: true,
      });
    });

    it('should be able to create', async () => {
      const spy = jest.spyOn(router, 'navigate');
      await componentHarness.getTitleInput(harnessLoader).then(input => input.setValue('Cat'));
      await componentHarness.getDescriptionInput(harnessLoader).then(input => input.setValue('Cat desc'));

      const saveBar = await componentHarness.getSaveBar(harnessLoader);
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectCreatePortalCategoryRequest(httpTestingController, { title: 'Cat', description: 'Cat desc', visible: true }, CATEGORY);
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith(['..', CATEGORY.id], expect.anything());
    });
  });

  describe('Update', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
      expectGetPortalNavigationItemsWithApis([]);

      expect(component.mode).toEqual('edit');
    });

    it('should initialize with category input', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        title: CATEGORY.title,
        description: CATEGORY.description,
        visible: CATEGORY.visible,
      });
    });

    it('should be able to update', async () => {
      await componentHarness.getTitleInput(harnessLoader).then(input => input.setValue('Cat'));
      await componentHarness.getDescriptionInput(harnessLoader).then(input => input.setValue('Cat desc'));

      const saveBar = await componentHarness.getSaveBar(harnessLoader);
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectUpdatePortalCategoryRequest(httpTestingController, CATEGORY.id, { title: 'Cat', description: 'Cat desc', visible: true });
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
    });
  });

  describe('Form', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
      expectGetPortalNavigationItemsWithApis([]);
    });

    it('should require title', async () => {
      const titleInput = await componentHarness.getTitleInput(harnessLoader);
      expect(await titleInput.getValue()).toEqual(CATEGORY.title);
      expect(await componentHarness.getSaveBar(harnessLoader).then(saveBar => saveBar.isVisible())).toBeFalsy();
      await titleInput.setValue('New title');
      expect(await componentHarness.getSaveBar(harnessLoader).then(saveBar => saveBar.isVisible())).toBeTruthy();
      expect(await componentHarness.getSaveBar(harnessLoader).then(saveBar => saveBar.isSubmitButtonInvalid())).toBeFalsy();
      await titleInput.setValue('');
      expect(await componentHarness.getSaveBar(harnessLoader).then(saveBar => saveBar.isSubmitButtonInvalid())).toBeTruthy();
    });

    it('should toggle visible', async () => {
      const visibleToggle = await componentHarness.getVisibleToggle(harnessLoader);
      expect(await visibleToggle.isChecked()).toEqual(true);
      await visibleToggle.toggle();
      expect(await visibleToggle.isChecked()).toEqual(false);
    });
  });

  describe('Not found', () => {
    it('should redirect to the category list and show an error when the category does not exist', async () => {
      await init('unknown-id');
      const navigateSpy = jest.spyOn(router, 'navigate');
      const snackBarErrorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);

      expect(snackBarErrorSpy).toHaveBeenCalledWith('Category not found');
      expect(navigateSpy).toHaveBeenCalledWith(['..', '..'], expect.anything());
    });
  });

  describe('Navigation item loading', () => {
    it('should recover the API and API Product lists after the initial request fails', async () => {
      await init(CATEGORY.id);
      const errorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();

      const failedRequest = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR&includes=apis`,
      });
      failedRequest.flush({ message: 'Navigation items unavailable' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(errorSpy).toHaveBeenCalledWith('Navigation items unavailable');
      expect(await componentHarness.getApiProductTable(harnessLoader)).toBeTruthy();

      const navigationItem = fakePortalNavigationApi({
        id: 'nav-api-1',
        apiId: 'api-1',
        title: 'Planets API',
        categoryIds: [],
      });
      const addApiButton = await componentHarness.getAddApiButton(harnessLoader);
      await addApiButton!.click();
      expectGetPortalNavigationItems([navigationItem]);

      const dialog = await rootLoader.getHarness(AddApiToCategoryDialogHarness);
      await dialog.fillFormAndSubmit(navigationItem.title);

      const updatedNavigationItem = { ...navigationItem, categoryIds: [CATEGORY.id] };
      expectUpdateNavigationItem(
        navigationItem.id,
        {
          type: 'API',
          title: navigationItem.title,
          published: navigationItem.published,
          visibility: navigationItem.visibility,
          parentId: navigationItem.parentId,
          order: navigationItem.order,
          apiId: navigationItem.apiId,
          categoryIds: [CATEGORY.id],
        },
        updatedNavigationItem,
      );
      expectGetPortalNavigationItemsWithApis([updatedNavigationItem], {
        [navigationItem.id]: { id: navigationItem.apiId, name: navigationItem.title, apiVersion: '1.0', contextPath: '/planets' },
      });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual(navigationItem.title);
    });
  });

  describe('API List', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
    });

    it('should show empty APIs when none are assigned to the category', async () => {
      expectGetPortalNavigationItemsWithApis([]);
      fixture.detectChanges();

      const table = await harnessLoader.getHarness(MatTableHarness);
      const tableHost = await table.host();
      expect(await tableHost.text()).toContain('There are no APIs for this category.');
    });

    it('should have the add API button enabled', async () => {
      expectGetPortalNavigationItemsWithApis([]);
      fixture.detectChanges();

      const addApiButton = await componentHarness.getAddApiButton(harnessLoader);
      expect(addApiButton).toBeTruthy();
      expect(await addApiButton!.isDisabled()).toEqual(false);
    });

    it('should list APIs assigned to the category', async () => {
      const navItem = fakePortalNavigationApi({
        id: 'nav-api-1',
        apiId: 'api-1',
        title: 'Planets API',
        categoryIds: [CATEGORY.id],
      });

      expectGetPortalNavigationItemsWithApis([navItem], {
        'nav-api-1': { id: 'api-1', name: 'Planets API', apiVersion: '1.0', contextPath: '/planets' },
      });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual('Planets API');
      expect(await componentHarness.getTextByColumnNameAndRowIndex(harnessLoader, 'version', 0)).toEqual('1.0');
      expect(await componentHarness.getTextByColumnNameAndRowIndex(harnessLoader, 'contextPath', 0)).toEqual('/planets');
    });

    it('should still list an API assigned to the category after it was unpublished', async () => {
      const navItem = fakePortalNavigationApi({
        id: 'nav-api-1',
        apiId: 'api-1',
        title: 'Planets API',
        categoryIds: [CATEGORY.id],
        published: false,
      });

      expectGetPortalNavigationItemsWithApis([navItem], {
        'nav-api-1': { id: 'api-1', name: 'Planets API', apiVersion: '1.0', contextPath: '/planets' },
      });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual('Planets API');
    });

    describe('Add API to Category', () => {
      beforeEach(async () => {
        expectGetPortalNavigationItemsWithApis([]);
        fixture.detectChanges();
      });

      it('should add the selected API to the category', async () => {
        const navItem = fakePortalNavigationApi({
          id: 'nav-api-1',
          apiId: 'api-1',
          title: 'Planets API',
          categoryIds: [],
        });

        const successSpy = jest.spyOn(TestBed.inject(SnackBarService), 'success');

        const addApiButton = await componentHarness.getAddApiButton(harnessLoader);
        await addApiButton!.click();
        expectGetPortalNavigationItems([navItem]);

        const dialog = await rootLoader.getHarness(AddApiToCategoryDialogHarness);
        await dialog.fillFormAndSubmit('Planets API');

        const updatedNavItem = { ...navItem, categoryIds: [CATEGORY.id] };
        expectUpdateNavigationItem(
          navItem.id,
          {
            type: 'API',
            title: navItem.title,
            published: navItem.published,
            visibility: navItem.visibility,
            parentId: navItem.parentId,
            order: navItem.order,
            apiId: navItem.apiId,
            categoryIds: [CATEGORY.id],
          },
          updatedNavItem,
        );

        expect(successSpy).toHaveBeenCalledWith('API [Planets API] has been added to the category.');

        expectGetPortalNavigationItemsWithApis([updatedNavItem], {
          'nav-api-1': { id: 'api-1', name: 'Planets API', apiVersion: '1.0', contextPath: '/planets' },
        });
      });

      it('should not offer APIs already assigned to the category', async () => {
        const alreadyInCategory = fakePortalNavigationApi({
          id: 'nav-api-1',
          apiId: 'api-1',
          title: 'Already In Category',
          categoryIds: [CATEGORY.id],
        });
        const selectable = fakePortalNavigationApi({
          id: 'nav-api-2',
          apiId: 'api-2',
          title: 'Selectable API',
          categoryIds: [],
        });

        const addApiButton = await componentHarness.getAddApiButton(harnessLoader);
        await addApiButton!.click();
        expectGetPortalNavigationItems([alreadyInCategory, selectable]);

        const dialog = await rootLoader.getHarness(AddApiToCategoryDialogHarness);
        expect(await dialog.getOptionLabels()).toEqual(['Selectable API']);
      });
    });

    describe('Remove API from Category', () => {
      const navItem = fakePortalNavigationApi({
        id: 'nav-api-1',
        apiId: 'api-1',
        title: 'Planets API',
        categoryIds: [CATEGORY.id],
      });

      beforeEach(async () => {
        expectGetPortalNavigationItemsWithApis([navItem], {
          'nav-api-1': { id: 'api-1', name: 'Planets API', apiVersion: '1.0', contextPath: '/planets' },
        });
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
      });

      it('should remove the API from the category after confirmation', async () => {
        const successSpy = jest.spyOn(TestBed.inject(SnackBarService), 'success');

        const removeBtn = await componentHarness.getRemoveApiButtonByRowIndex(harnessLoader, 0);
        expect(removeBtn).toBeTruthy();
        await removeBtn!.click();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectUpdateNavigationItem(
          navItem.id,
          {
            type: 'API',
            title: navItem.title,
            published: navItem.published,
            visibility: navItem.visibility,
            parentId: navItem.parentId,
            order: navItem.order,
            apiId: navItem.apiId,
            categoryIds: [],
          },
          { ...navItem, categoryIds: [] },
        );

        expect(successSpy).toHaveBeenCalledWith(`'Planets API' removed successfully`);

        expectGetPortalNavigationItemsWithApis([]);
      });

      it('should not remove the API when cancelling the confirmation', async () => {
        const removeBtn = await componentHarness.getRemoveApiButtonByRowIndex(harnessLoader, 0);
        await removeBtn!.click();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.cancel();

        expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual('Planets API');
      });
    });
  });

  describe('API Product List', () => {
    const commerceProduct: ApiProduct = {
      id: 'product-1',
      name: 'Commerce Product',
      version: '1.0',
    };

    beforeEach(async () => {
      await init(CATEGORY.id);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
    });

    it('should show an empty state when no API Products are assigned to the category', async () => {
      expectGetPortalNavigationItemsWithApis([]);
      fixture.detectChanges();

      const table = await componentHarness.getApiProductTable(harnessLoader);
      expect(await table.host().then(host => host.text())).toContain('There are no API Products for this category.');
    });

    it('should list an assigned API Product with its name and version', async () => {
      const navigationItem = fakePortalNavigationApiProduct({
        id: 'nav-product-1',
        apiProductId: commerceProduct.id,
        categoryIds: [CATEGORY.id],
      });

      expectGetPortalNavigationItemsWithApis([navigationItem]);
      expectSearchApiProducts([commerceProduct.id], [commerceProduct]);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getApiProductNameByRowIndex(harnessLoader, 0)).toEqual(commerceProduct.name);
      expect(await componentHarness.getApiProductTextByColumnNameAndRowIndex(harnessLoader, 'version', 0)).toEqual(commerceProduct.version);
    });

    it('should search each API Product id only once', async () => {
      const firstNavigationItem = fakePortalNavigationApiProduct({
        id: 'nav-product-1',
        apiProductId: commerceProduct.id,
        categoryIds: [CATEGORY.id],
      });
      const secondNavigationItem = fakePortalNavigationApiProduct({
        id: 'nav-product-2',
        apiProductId: commerceProduct.id,
        categoryIds: [CATEGORY.id],
      });

      expectGetPortalNavigationItemsWithApis([firstNavigationItem, secondNavigationItem]);
      expectSearchApiProducts([commerceProduct.id], [commerceProduct]);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getApiProductNameByRowIndex(harnessLoader, 0)).toEqual(commerceProduct.name);
      expect(await componentHarness.getApiProductNameByRowIndex(harnessLoader, 1)).toEqual(commerceProduct.name);
    });

    it('should still list an assigned API Product after it was unpublished', async () => {
      const navigationItem = fakePortalNavigationApiProduct({
        id: 'nav-product-1',
        apiProductId: commerceProduct.id,
        categoryIds: [CATEGORY.id],
        published: false,
      });

      expectGetPortalNavigationItemsWithApis([navigationItem]);
      expectSearchApiProducts([commerceProduct.id], [commerceProduct]);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await componentHarness.getApiProductNameByRowIndex(harnessLoader, 0)).toEqual(commerceProduct.name);
    });

    it('should show an error when assigned API Products cannot be loaded', () => {
      const navigationItem = fakePortalNavigationApiProduct({
        apiProductId: commerceProduct.id,
        categoryIds: [CATEGORY.id],
      });
      const errorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      expectGetPortalNavigationItemsWithApis([navigationItem]);
      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search?page=1&perPage=100`,
      });
      req.flush({ message: 'Products unavailable' }, { status: 500, statusText: 'Server Error' });

      expect(errorSpy).toHaveBeenCalledWith('Products unavailable');
    });

    describe('Add API Product to Category', () => {
      beforeEach(() => {
        expectGetPortalNavigationItemsWithApis([]);
        fixture.detectChanges();
      });

      it('should add the selected API Product while retaining its existing categories', async () => {
        const navigationItem = fakePortalNavigationApiProduct({
          id: 'nav-product-1',
          apiProductId: commerceProduct.id,
          title: 'Commerce documentation',
          published: true,
          categoryIds: ['existing-category'],
        });
        const successSpy = jest.spyOn(TestBed.inject(SnackBarService), 'success');

        const addButton = await componentHarness.getAddApiProductButton(harnessLoader);
        await addButton!.click();
        expectGetPortalNavigationItems([navigationItem]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);

        const dialog = await rootLoader.getHarness(AddApiProductToCategoryDialogHarness);
        await dialog.fillFormAndSubmit(commerceProduct.name);

        const updatedNavigationItem = { ...navigationItem, categoryIds: ['existing-category', CATEGORY.id] };
        expectUpdateNavigationItem(
          navigationItem.id,
          {
            type: 'API_PRODUCT',
            title: navigationItem.title,
            published: navigationItem.published,
            visibility: navigationItem.visibility,
            parentId: navigationItem.parentId,
            order: navigationItem.order,
            categoryIds: ['existing-category', CATEGORY.id],
          },
          updatedNavigationItem,
        );

        expect(successSpy).toHaveBeenCalledWith(`API Product [${commerceProduct.name}] has been added to the category.`);
        expectGetPortalNavigationItemsWithApis([updatedNavigationItem]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);
      });

      it('should offer only published API Products not yet assigned to the category', async () => {
        const assigned = fakePortalNavigationApiProduct({
          apiProductId: 'assigned-product',
          published: true,
          categoryIds: [CATEGORY.id],
        });
        const unpublished = fakePortalNavigationApiProduct({
          apiProductId: 'unpublished-product',
          published: false,
          categoryIds: [],
        });
        const selectable = fakePortalNavigationApiProduct({
          apiProductId: commerceProduct.id,
          published: true,
          categoryIds: [],
        });

        const addButton = await componentHarness.getAddApiProductButton(harnessLoader);
        await addButton!.click();
        expectGetPortalNavigationItems([assigned, unpublished, selectable]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);

        const dialog = await rootLoader.getHarness(AddApiProductToCategoryDialogHarness);
        expect(await dialog.getOptionLabels()).toEqual(['Commerce Product (1.0)']);
      });

      it('should prevent opening multiple dialogs while API Product candidates are loading', async () => {
        const navigationItem = fakePortalNavigationApiProduct({
          apiProductId: commerceProduct.id,
          published: true,
          categoryIds: [],
        });
        const addButton = await componentHarness.getAddApiProductButton(harnessLoader);

        await addButton!.click();
        expect(await addButton!.isDisabled()).toBe(true);
        await addButton!.click();
        expectGetPortalNavigationItems([navigationItem]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);

        const dialog = await rootLoader.getHarness(AddApiProductToCategoryDialogHarness);
        await dialog.cancel();
        fixture.detectChanges();

        expect(await addButton!.isDisabled()).toBe(false);
      });

      it('should show a load-specific error and re-enable the button when candidates cannot be loaded', async () => {
        const errorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
        const addButton = await componentHarness.getAddApiProductButton(harnessLoader);

        await addButton!.click();
        const request = httpTestingController.expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR`,
        });
        request.flush({}, { status: 500, statusText: 'Server Error' });
        fixture.detectChanges();

        expect(errorSpy).toHaveBeenCalledWith('Unable to load API Products');
        expect(await addButton!.isDisabled()).toBe(false);
      });

      it('should show an error when the API Product update fails', async () => {
        const navigationItem = fakePortalNavigationApiProduct({
          apiProductId: commerceProduct.id,
          title: 'Commerce documentation',
          published: true,
          categoryIds: [],
        });
        const errorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

        const addButton = await componentHarness.getAddApiProductButton(harnessLoader);
        await addButton!.click();
        expectGetPortalNavigationItems([navigationItem]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);

        const dialog = await rootLoader.getHarness(AddApiProductToCategoryDialogHarness);
        await dialog.fillFormAndSubmit(commerceProduct.name);

        const request = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${navigationItem.id}`,
        });
        request.flush({ message: 'Update rejected' }, { status: 400, statusText: 'Bad Request' });

        expect(errorSpy).toHaveBeenCalledWith('Update rejected');
      });
    });

    describe('Remove API Product from Category', () => {
      const navigationItem = fakePortalNavigationApiProduct({
        id: 'nav-product-1',
        apiProductId: commerceProduct.id,
        title: 'Commerce documentation',
        published: false,
        categoryIds: ['another-category', CATEGORY.id],
      });

      beforeEach(async () => {
        expectGetPortalNavigationItemsWithApis([navigationItem]);
        expectSearchApiProducts([commerceProduct.id], [commerceProduct]);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
      });

      it('should remove only the current category after confirmation', async () => {
        const successSpy = jest.spyOn(TestBed.inject(SnackBarService), 'success');

        const removeButton = await componentHarness.getRemoveApiProductButtonByRowIndex(harnessLoader, 0);
        await removeButton!.click();
        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectUpdateNavigationItem(
          navigationItem.id,
          {
            type: 'API_PRODUCT',
            title: navigationItem.title,
            published: false,
            visibility: navigationItem.visibility,
            parentId: navigationItem.parentId,
            order: navigationItem.order,
            categoryIds: ['another-category'],
          },
          { ...navigationItem, categoryIds: ['another-category'] },
        );

        expect(successSpy).toHaveBeenCalledWith(`'${commerceProduct.name}' removed successfully`);
        expectGetPortalNavigationItemsWithApis([]);
      });

      it('should not remove the API Product when confirmation is cancelled', async () => {
        const removeButton = await componentHarness.getRemoveApiProductButtonByRowIndex(harnessLoader, 0);
        await removeButton!.click();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.cancel();

        expect(await componentHarness.getApiProductNameByRowIndex(harnessLoader, 0)).toEqual(commerceProduct.name);
      });

      it('should show an error when the API Product removal fails', async () => {
        const errorSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
        const removeButton = await componentHarness.getRemoveApiProductButtonByRowIndex(harnessLoader, 0);
        await removeButton!.click();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        const request = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${navigationItem.id}`,
        });
        request.flush({ message: 'Removal rejected' }, { status: 400, statusText: 'Bad Request' });

        expect(errorSpy).toHaveBeenCalledWith('Removal rejected');
      });
    });
  });

  describe('Permissions', () => {
    it('should hide API and API Product update actions without documentation update permission', async () => {
      await init(CATEGORY.id, ['environment-category-u', 'environment-documentation-r']);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
      expectGetPortalNavigationItemsWithApis([]);
      fixture.detectChanges();

      expect(await componentHarness.getAddApiButton(harnessLoader)).toBeNull();
      expect(await componentHarness.getAddApiProductButton(harnessLoader)).toBeNull();
    });
  });
});
