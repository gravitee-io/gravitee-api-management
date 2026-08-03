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
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatTableHarness } from '@angular/material/table/testing';

import { CategoryListComponent } from './category-list.component';
import { CategoryListHarness } from './category-list.harness';

import { GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
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

  const init = async (permissions: string[] = ['environment-category-u', 'environment-category-d', 'environment-category-c']) => {
    await TestBed.configureTestingModule({
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, CategoryListComponent],
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
});
