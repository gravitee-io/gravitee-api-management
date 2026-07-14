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

import { CategoryCatalogComponent } from './category.component';
import { CategoryHarness } from './category.harness';

import { GioTestingModule } from '../../../shared/testing';
import { fakePortalCategory, PortalCategory } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { PortalCatalogComponent } from '../portal-catalog.component';
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

  describe('API List', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectListPortalCategoriesRequest(httpTestingController, [CATEGORY]);
      fixture.detectChanges();
    });

    it('should show empty APIs, API association not yet supported', async () => {
      const table = await harnessLoader.getHarness(MatTableHarness);
      const tableHost = await table.host();
      expect(await tableHost.text()).toContain('There are no APIs for this category.');
    });

    it('should have the add API button disabled', async () => {
      const addApiButton = await componentHarness.getAddApiButton(harnessLoader);
      expect(addApiButton).toBeTruthy();
      expect(await addApiButton.isDisabled()).toEqual(true);
    });
  });
});
