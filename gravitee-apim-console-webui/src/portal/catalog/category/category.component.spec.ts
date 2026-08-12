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

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakePortalCategory,
  fakePortalNavigationApi,
  fakePortalNavigationItemsResponse,
  PortalCategory,
  PortalNavigationApi,
  PortalNavigationItemApiSummary,
  UpdateApiPortalNavigationItem,
} from '../../../entities/management-api-v2';
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

  const init = async (categoryId: string) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalCatalogComponent, CategoryCatalogComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { params: of({ categoryId }), snapshot: { params: { categoryId } } },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-category-u',
            'environment-category-d',
            'environment-category-c',
            'environment-documentation-u',
            'environment-documentation-r',
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
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CategoryHarness);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  const expectGetPortalNavigationItems = (items: PortalNavigationApi[] = []) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR`,
    });
    req.flush(fakePortalNavigationItemsResponse({ items }));
  };

  const expectGetPortalNavigationItemsWithApis = (
    items: PortalNavigationApi[] = [],
    apisById: Record<string, PortalNavigationItemApiSummary> = {},
  ) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR&includes=apis`,
    });
    req.flush(fakePortalNavigationItemsResponse({ items, metadata: { apis: apisById } }));
  };

  const expectUpdateNavigationItem = (navItemId: string, expectedBody: UpdateApiPortalNavigationItem, response: PortalNavigationApi) => {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${navItemId}`,
    });
    expect(req.request.body).toEqual(expectedBody);
    req.flush(response);
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
});
