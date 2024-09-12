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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { By } from '@angular/platform-browser';

import { CategoriesModule } from './categories.module';
import { CategoriesComponent } from './categories.component';

import { Category } from '../../../entities/category/Category';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { EnvSettings } from '../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { UpdateCategory } from '../../../entities/category/UpdateCategory';

describe('CategoriesComponent', () => {
  let fixture: ComponentFixture<CategoriesComponent>;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const DEFAULT_PORTAL_SETTINGS = {
    portal: {
      url: 'url',
      entrypoint: 'entrypoint',
      apikeyHeader: 'apiKeyHeader',
      support: {
        enabled: true,
      },
      apis: {
        categoryMode: {
          enabled: true,
        },
        tilesMode: {
          enabled: true,
        },
        apiHeaderShowTags: {
          enabled: true,
        },
        apiHeaderShowCategories: {
          enabled: true,
        },
      },
      analytics: {
        enabled: true,
      },
      rating: {
        enabled: true,
        comment: { mandatory: true },
      },
      userCreation: {
        enabled: true,
        automaticValidation: { enabled: true },
      },
      uploadMedia: { enabled: true, maxSizeInOctet: 10 },
    },
    portalNext: {
      access: {
        enabled: true,
      },
      banner: {
        enabled: true,
        title: 'testTitle',
        subtitle: 'testSubtitle',
      },
    },
  };

  const init = async (snapshot: Partial<EnvSettings> = DEFAULT_PORTAL_SETTINGS) => {
    await TestBed.configureTestingModule({
      declarations: [CategoriesComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-category-u', 'environment-category-d', 'environment-category-c'],
        },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot } },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, CategoriesModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // Allows to choose a day in the calendar
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CategoriesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('No categories', () => {
    beforeEach(async () => {
      await init();
      expectGetCategoriesList();
    });
    it('should display empty message', async () => {
      const tableRows = await getTableRows();
      expect(await tableRows[0].host().then((host) => host.text())).toContain('There are no categories for this environment.');
    });
  });

  describe('Category mode', () => {
    it('should be checked if settings mark it as enabled', async () => {
      await init();
      expectGetCategoriesList();
      const toggle = await getToggle();
      expect(await toggle.isChecked()).toEqual(true);
    });

    it('should not be checked if settings mark it as not activated', async () => {
      await init({
        portal: { ...DEFAULT_PORTAL_SETTINGS.portal, apis: { ...DEFAULT_PORTAL_SETTINGS.portal.apis, categoryMode: { enabled: false } } },
      });
      expectGetCategoriesList();
      const toggle = await getToggle();
      expect(await toggle.isChecked()).toEqual(false);
    });
  });

  describe('Category list', () => {
    beforeEach(async () => {
      await init();
      expectGetCategoriesList([
        { id: 'cat-1', name: 'cat-1', key: 'cat-1', order: 1, hidden: false, description: 'nice cat', totalApis: 1 },
        { id: 'cat-2', name: 'cat-2', key: 'cat-2', order: 3, hidden: false, description: 'nice cat - out of order', totalApis: 10 },
        { id: 'cat-3', name: 'cat-3', key: 'cat-3', order: 2, hidden: true, description: 'nice cat - hidden', totalApis: 0 },
      ]);
    });
    it('should show multiple categories', async () => {
      expect(await getTableRows().then((rows) => rows.length)).toEqual(3);
    });
    it('should sort categories by order', async () => {
      expect(await getNameByRowIndex(0)).toEqual('cat-1');
      expect(await getNameByRowIndex(1)).toEqual('cat-3');
      expect(await getNameByRowIndex(2)).toEqual('cat-2');
    });
    it('should show which categories are hidden', async () => {
      const rows = await getTableRows();
      const hiddenIcon = await rows[1].getCells({ columnName: 'name' }).then((cells) => cells[0].getHarnessOrNull(MatIconHarness));
      expect(hiddenIcon).toBeTruthy();
    });
    it('should show api count', async () => {
      expect(await getApiCountByRowIndex(0)).toEqual('1');
      expect(await getApiCountByRowIndex(1)).toEqual('0');
      expect(await getApiCountByRowIndex(2)).toEqual('10');
    });
    it('should show category description', async () => {
      expect(await getDescriptionByRowIndex(0)).toEqual('nice cat');
      expect(await getDescriptionByRowIndex(1)).toEqual('nice cat - hidden');
      expect(await getDescriptionByRowIndex(2)).toEqual('nice cat - out of order');
    });
  });

  describe('Actions', () => {
    const CATEGORIES = () => [
      { id: 'cat-1', name: 'cat-1', key: 'cat-1', order: 1, hidden: false, description: 'nice cat', totalApis: 1 },
      { id: 'cat-2', name: 'cat-2', key: 'cat-2', order: 3, hidden: false, description: 'nice cat - out of order', totalApis: 10 },
      { id: 'cat-3', name: 'cat-3', key: 'cat-3', order: 2, hidden: true, description: 'nice cat - hidden', totalApis: 0 },
    ];
    beforeEach(async () => {
      await init();
      expectGetCategoriesList(CATEGORIES());
    });
    it('should hide category', async () => {
      const hideCategoryButton = await getActionButtonByRowIndexAndTooltip(0, 'Hide Category');
      expect(hideCategoryButton).toBeTruthy();
      expect(await hideCategoryButton.isDisabled()).toEqual(false);

      await hideCategoryButton.click();
      const updatedCategory = { ...CATEGORIES()[0], hidden: true };
      expectPutCategory(updatedCategory);
      expectGetCategoriesList();
    });

    it('should show category', async () => {
      const showCategoryButton = await getActionButtonByRowIndexAndTooltip(1, 'Show Category');
      expect(showCategoryButton).toBeTruthy();
      expect(await showCategoryButton.isDisabled()).toEqual(false);

      await showCategoryButton.click();
      const updatedCategory = { ...CATEGORIES()[2], hidden: false };
      expectPutCategory(updatedCategory);
      expectGetCategoriesList();
    });

    it('should move category down', async () => {
      const moveDownButton = await getActionButtonByRowIndexAndTooltip(0, 'Move Down');
      expect(moveDownButton).toBeTruthy();
      expect(await moveDownButton.isDisabled()).toEqual(false);
      await moveDownButton.click();

      expectPutCategoryList([
        { ...CATEGORIES()[2], order: 1 },
        { ...CATEGORIES()[0], order: 2 },
      ]);
      expectGetCategoriesList();
    });
    it('should move category up', async () => {
      const moveUpButton = await getActionButtonByRowIndexAndTooltip(2, 'Move Up');
      expect(moveUpButton).toBeTruthy();
      expect(await moveUpButton.isDisabled()).toEqual(false);
      await moveUpButton.click();

      expectPutCategoryList([
        { ...CATEGORIES()[1], order: 2 },
        { ...CATEGORIES()[2], order: 3 },
      ]);
      expectGetCategoriesList();
    });
    it('should not allow first element to be moved up', async () => {
      const moveUpButton = await getActionButtonByRowIndexAndTooltip(0, 'Move Up');
      expect(await moveUpButton.isDisabled()).toEqual(true);
    });
    it('should not allow last element to be moved up', async () => {
      const moveDownButton = await getActionButtonByRowIndexAndTooltip(2, 'Move Down');
      expect(await moveDownButton.isDisabled()).toEqual(true);
    });
    it('should delete category', async () => {
      const deleteButton = await getActionButtonByRowIndexAndTooltip(0, 'Delete');
      expect(deleteButton).toBeTruthy();
      expect(await deleteButton.isDisabled()).toEqual(false);

      await deleteButton.click();

      const confirmDialog = await rootLoader.getHarnessOrNull(GioConfirmDialogHarness);
      expect(confirmDialog).toBeTruthy();
      await confirmDialog.confirm();

      expectDeleteCategory(CATEGORIES()[0].id);
      expectGetCategoriesList();
    });
  });

  describe('Applies to both portals badge', () => {
    it('should show badge when portal next is enabled', async () => {
      await init({ ...DEFAULT_PORTAL_SETTINGS, portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, access: { enabled: true } } });
      expectGetCategoriesList([
        { id: 'cat-1', name: 'cat-1', key: 'cat-1', order: 1, hidden: false, description: 'nice cat', totalApis: 1 },
      ]);
      fixture.detectChanges();

      const title = fixture.debugElement.query(By.css('.title'));
      const element: HTMLDivElement = title.nativeElement;
      expect(element.innerHTML).toContain('Applies to both portals');
    });
    it('should not show badge when portal next is disabled', async () => {
      await init({ ...DEFAULT_PORTAL_SETTINGS, portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, access: { enabled: false } } });
      expectGetCategoriesList([
        { id: 'cat-1', name: 'cat-1', key: 'cat-1', order: 1, hidden: false, description: 'nice cat', totalApis: 1 },
      ]);
      fixture.detectChanges();

      const title = fixture.debugElement.query(By.css('.title'));
      const element: HTMLDivElement = title.nativeElement;
      const innerHTML = element.innerHTML;
      expect(innerHTML.includes('Applies to both portals')).toEqual(false);
    });
  });

  function expectGetCategoriesList(list: Category[] = []) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories?include=total-apis`).flush(list);
  }
  function expectPutCategory(category: UpdateCategory) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories/${category.id}`);
    expect(req.request.body).toEqual(category);
    req.flush(category);
  }

  function expectPutCategoryList(categoryList: UpdateCategory[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories`);
    expect(req.request.body).toEqual(categoryList);
    req.flush(categoryList);
  }

  function expectDeleteCategory(categoryId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories/${categoryId}`,
        method: 'DELETE',
      })
      .flush({});
  }

  async function getToggle(): Promise<MatSlideToggleHarness> {
    return await harnessLoader.getHarness(MatSlideToggleHarness);
  }
  async function getTableRows(): Promise<MatRowHarness[]> {
    return await harnessLoader.getHarness(MatTableHarness).then((table) => table.getRows());
  }

  async function getNameByRowIndex(index: number): Promise<string> {
    return await getTextByColumnNameAndRowIndex('name', index);
  }

  async function getApiCountByRowIndex(index: number): Promise<string> {
    return await getTextByColumnNameAndRowIndex('count', index);
  }
  async function getDescriptionByRowIndex(index: number): Promise<string> {
    return await getTextByColumnNameAndRowIndex('description', index);
  }
  async function getTextByColumnNameAndRowIndex(columnName: string, index: number): Promise<string> {
    return await getTableRows()
      .then((rows) => rows[index])
      .then((row) => row.getCellTextByIndex({ columnName }).then((cell) => cell[0]));
  }

  async function getActionButtonByRowIndexAndTooltip(rowIndex: number, tooltipText: string): Promise<MatButtonHarness | null> {
    return await getTableRows()
      .then((rows) => rows[rowIndex].getCells({ columnName: 'actions' }))
      .then((cells) => cells[0])
      .then((actionCell) => actionCell.getHarnessOrNull(MatButtonHarness.with({ selector: `[mattooltip="${tooltipText}"]` })));
  }
});
