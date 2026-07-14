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
import { MatTableHarness } from '@angular/material/table/testing';
import { of } from 'rxjs';

import { CategoryListComponent } from './category-list.component';
import { CategoryListHarness } from './category-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { EnvSettings } from '../../../entities/Constants';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { fakePortalCategory, PortalCategory } from '../../../entities/management-api-v2';
import {
  expectDeletePortalCategoryRequest,
  expectListPortalCategoriesRequest,
  expectUpdatePortalCategoryRequest,
} from '../../../services-ngx/portal-category.service.spec';

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
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot, load: () => of(snapshot) } },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, MatSlideToggleModule, CategoryListComponent],
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
      expectListPortalCategoriesRequest(httpTestingController, []);
    });
    it('should display empty message', async () => {
      const table = await harnessLoader.getHarness(MatTableHarness);
      const tableHost = await table.host();
      expect(await tableHost.text()).toContain('There are no categories for this environment.');
    });
  });

  describe('Category list', () => {
    const CATEGORIES = [
      fakePortalCategory({ id: 'cat-1', title: 'cat-1', description: 'nice cat', visible: true }),
      fakePortalCategory({ id: 'cat-2', title: 'cat-2', description: 'nice cat - second', visible: true }),
      fakePortalCategory({ id: 'cat-3', title: 'cat-3', description: 'nice cat - hidden', visible: false }),
    ];
    beforeEach(async () => {
      await init();
      expectListPortalCategoriesRequest(httpTestingController, CATEGORIES);
    });
    it('should show multiple categories', async () => {
      expect(await componentHarness.getTableRows(harnessLoader).then(rows => rows.length)).toEqual(3);
    });
    it('should show which categories are hidden', async () => {
      const rows = await componentHarness.getTableRows(harnessLoader);
      const hiddenIcon = await rows[2].getCells({ columnName: 'name' }).then(cells => cells[0].getHarnessOrNull(MatIconHarness));
      expect(hiddenIcon).toBeTruthy();
    });
    it('should show category description', async () => {
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 0)).toEqual('nice cat');
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 1)).toEqual('nice cat - second');
      expect(await componentHarness.getDescriptionByRowIndex(harnessLoader, 2)).toEqual('nice cat - hidden');
    });
  });

  describe('Actions', () => {
    const CATEGORIES: () => PortalCategory[] = () => [
      fakePortalCategory({ id: 'cat-1', title: 'cat-1', description: 'nice cat', visible: true }),
      fakePortalCategory({ id: 'cat-2', title: 'cat-2', description: 'nice cat - second', visible: true }),
      fakePortalCategory({ id: 'cat-3', title: 'cat-3', description: 'nice cat - hidden', visible: false }),
    ];
    beforeEach(async () => {
      await init();
      expectListPortalCategoriesRequest(httpTestingController, CATEGORIES());
    });

    it('should show category', async () => {
      const showCategoryButton = await componentHarness.getActionButtonByRowIndexAndTooltip(harnessLoader, 2, 'Show Category');
      expect(showCategoryButton).toBeTruthy();
      expect(await showCategoryButton.isDisabled()).toEqual(false);

      await showCategoryButton.click();
      expectUpdatePortalCategoryRequest(httpTestingController, 'cat-3', {
        title: 'cat-3',
        description: 'nice cat - hidden',
        visible: true,
      });
      expectListPortalCategoriesRequest(httpTestingController, []);
    });

    it('should delete category', async () => {
      const deleteButton = await componentHarness.getActionButtonByRowIndexAndTooltip(harnessLoader, 0, 'Delete');
      expect(deleteButton).toBeTruthy();
      expect(await deleteButton.isDisabled()).toEqual(false);

      await deleteButton.click();

      const confirmDialog = await rootLoader.getHarnessOrNull(GioConfirmDialogHarness);
      expect(confirmDialog).toBeTruthy();
      await confirmDialog.confirm();

      expectDeletePortalCategoryRequest(httpTestingController, CATEGORIES()[0].id);
      expectListPortalCategoriesRequest(httpTestingController, []);
    });
  });

  describe('Settings', () => {
    it('should load current settings', async () => {
      const settings = {
        ...DEFAULT_PORTAL_SETTINGS,
        portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, catalog: { viewMode: 'CATEGORIES' } },
      };

      await init({}, settings);
      expectListPortalCategoriesRequest(httpTestingController, []);

      const viewModeSelect = await componentHarness.getCategoryViewMode(harnessLoader);
      expect(await viewModeSelect.getValueText()).toEqual('Tiles');
    });
    it('should select Tabs catalog view mode', async () => {
      await init({}, {});
      expectListPortalCategoriesRequest(httpTestingController, []);

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
      expectListPortalCategoriesRequest(httpTestingController, []);

      const viewModeSelect = await componentHarness.getCategoryViewMode(harnessLoader);
      await viewModeSelect.clickOptions({ text: 'Tiles' });

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickReset();
      expect(await viewModeSelect.getValueText()).toEqual('Tabs (Default)');
    });
  });

  function expectGetPortalSettings(portalSettings: PortalSettings) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`).flush(portalSettings);
  }

  function expectSavePortalSettings(portalSettings: PortalSettings) {
    const request = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);

    expect(request.request.body).toEqual(portalSettings);

    request.flush(portalSettings);
  }
});
