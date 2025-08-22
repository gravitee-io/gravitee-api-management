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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { CategoryListComponent } from './category-list.component';
import { CategoryListHarness } from './category-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Category } from '../../../entities/category/Category';
import { EnvSettings } from '../../../entities/Constants';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { CategoriesModule } from '../../../management/settings/categories/categories.module';
import { UpdateCategory } from '../../../entities/category/UpdateCategory';
import { PortalSettings } from '../../../entities/portal/portalSettings';

describe('CategoryListComponent', () => {
  let fixture: ComponentFixture<CategoryListComponent>;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let componentHarness: CategoryListHarness;
  const DEFAULT_ENV_SETTINGS = {
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
  };

  const DEFAULT_PORTAL_SETTINGS: PortalSettings = {
    portalNext: {
      access: {
        enabled: true,
      },
      banner: {},
      catalog: {
        viewMode: 'TABS',
      },
    },
  };

  const init = async (
    snapshot: Partial<EnvSettings> = DEFAULT_ENV_SETTINGS,
    portalSettings: Partial<PortalSettings> = DEFAULT_PORTAL_SETTINGS,
  ) => {
    await TestBed.configureTestingModule({
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-category-u',
            'environment-category-d',
            'environment-category-c',
            'environment-settings-r',
            'environment-settings-u',
          ],
        },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot } },
      ],
      imports: [
        NoopAnimationsModule,
        GioTestingModule,
        CategoriesModule,
        MatIconTestingModule,
        MatSlideToggleModule,
        CategoryListComponent,
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CategoryListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CategoryListHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();

    expectGetPortalSettings({ ...DEFAULT_PORTAL_SETTINGS, ...portalSettings });
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
      const tableRows = await componentHarness.getTableRows(harnessLoader);
      expect(await tableRows[0].host().then((host) => host.text())).toContain('There are no categories for this environment.');
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
      expect(await componentHarness.getTableRows(harnessLoader).then((rows) => rows.length)).toEqual(3);
    });
    it('should sort categories by order', async () => {
      expect(await componentHarness.getNameByRowIndex(harnessLoader, 0)).toEqual('cat-1');
      expect(await componentHarness.getNameByRowIndex(harnessLoader, 1)).toEqual('cat-3');
      expect(await componentHarness.getNameByRowIndex(harnessLoader, 2)).toEqual('cat-2');
    });
    it('should show which categories are hidden', async () => {
      const rows = await componentHarness.getTableRows(harnessLoader);
      const hiddenIcon = await rows[1].getCells({ columnName: 'name' }).then((cells) => cells[0].getHarnessOrNull(MatIconHarness));
      expect(hiddenIcon).toBeTruthy();
    });
    it('should show api count', async () => {
      expect(await componentHarness.getApiCountByRowIndex(harnessLoader, 0)).toEqual('1');
      expect(await componentHarness.getApiCountByRowIndex(harnessLoader, 1)).toEqual('0');
      expect(await componentHarness.getApiCountByRowIndex(harnessLoader, 2)).toEqual('10');
    });
    it('should show category description', async () => {
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 0)).toEqual('nice cat');
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 1)).toEqual('nice cat - hidden');
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 2)).toEqual('nice cat - out of order');
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

    it('should show category', async () => {
      const showCategoryButton = await componentHarness.getActionButtonByRowIndexAndTooltip(harnessLoader, 1, 'Show Category');
      expect(showCategoryButton).toBeTruthy();
      expect(await showCategoryButton.isDisabled()).toEqual(false);

      await showCategoryButton.click();
      const updatedCategory = { ...CATEGORIES()[2], hidden: false };
      expectPutCategory(updatedCategory);
      expectGetCategoriesList();
    });

    it('should delete category', async () => {
      expect(await componentHarness.getBothPortalsBadge()).toBeTruthy();
      const deleteButton = await componentHarness.getActionButtonByRowIndexAndTooltip(harnessLoader, 0, 'Delete');
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

  describe('Settings', () => {
    it('should load current settings', async () => {
      const settings = {
        ...DEFAULT_PORTAL_SETTINGS,
        portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, catalog: { viewMode: 'CATEGORIES' } },
      };

      await init({}, settings);
      expectGetCategoriesList();

      const viewModeSelect = await componentHarness.getCategoryViewMode(harnessLoader);
      expect(await viewModeSelect.getValueText()).toEqual('Tiles');
    });
    it('should select Tabs catalog view mode', async () => {
      await init({}, {});
      expectGetCategoriesList();

      const viewModeSelect = await componentHarness.getCategoryViewMode(harnessLoader);
      expect(await viewModeSelect.getValueText()).toEqual('Tabs (Default)');

      await viewModeSelect.clickOptions({ text: 'Tiles' });

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toEqual(true);

      await saveBar.clickSubmit();
      expectGetPortalSettings(DEFAULT_PORTAL_SETTINGS);

      const newSettings = {
        ...DEFAULT_PORTAL_SETTINGS,
        portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, catalog: { viewMode: 'CATEGORIES' } },
      };

      expectSavePortalSettings(newSettings);
    });
    it('should reset settings', async () => {
      await init({}, {});
      expectGetCategoriesList();

      const viewModeSelect = await componentHarness.getCategoryViewMode(harnessLoader);
      await viewModeSelect.clickOptions({ text: 'Tiles' });

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickReset();
      expect(await viewModeSelect.getValueText()).toEqual('Tabs (Default)');
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

  function expectDeleteCategory(categoryId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories/${categoryId}`,
        method: 'DELETE',
      })
      .flush({});
  }

  function expectGetPortalSettings(portalSettings: PortalSettings) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`).flush(portalSettings);
  }

  function expectSavePortalSettings(portalSettings: PortalSettings) {
    const request = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);

    expect(request.request.body).toEqual(portalSettings);

    request.flush(portalSettings);
  }
});
