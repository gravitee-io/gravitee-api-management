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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioFormFilePickerInputHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { CategoryNgxComponent } from './category-ngx.component';

import { Page } from '../../../../../../entities/page';
import { NewCategory } from '../../../../../../entities/category/NewCategory';
import { UpdateCategory } from '../../../../../../entities/category/UpdateCategory';
import { CategoriesNgxModule } from '../categories-ngx.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { Category } from '../../../../../../entities/category/Category';

describe('CategoryNgxComponent', () => {
  let component: CategoryNgxComponent;
  let fixture: ComponentFixture<CategoryNgxComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let router: Router;

  const CATEGORY: Category = {
    id: 'cat',
    name: 'cat name',
    description: 'cat desc',
    key: 'cat-key',
    page: 'page-1',
    hidden: false,
    picture_url: 'picture_url',
    background_url: 'background_url',
  };

  const init = async (categoryId: string) => {
    await TestBed.configureTestingModule({
      declarations: [CategoryNgxComponent],
      imports: [NoopAnimationsModule, GioTestingModule, CategoriesNgxModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { params: of({ categoryId }) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryNgxComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
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
      expectPortalPagesList();
      fixture.detectChanges();
    });
    it('should initialize with all input as blank', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        name: null,
        description: null,
        page: null,
        hidden: false,
        picture: [],
        background: [],
      });
    });
    it('should be able to create', async () => {
      const spy = jest.spyOn(router, 'navigate');
      await getNameInput().then((input) => input.setValue('Cat'));
      await getDescriptionInput().then((input) => input.setValue('Cat desc'));

      const select = await getSelectPage();
      await select.open();
      await getSelectPage().then((select) => select.clickOptions({ text: 'Page 1' }));
      await getHideCategoryToggle().then((toggle) => toggle.toggle());

      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectPostCategory({ name: 'Cat', description: 'Cat desc', page: 'page-1', hidden: true }, CATEGORY);
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith(['..', CATEGORY.id], expect.anything());
    });
  });

  describe('Update', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
      expect(component.mode).toEqual('edit');
    });

    it('should initialize with category input', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        name: CATEGORY.name,
        description: CATEGORY.description,
        page: CATEGORY.page,
        hidden: false,
        picture: ['picture_url'],
        background: ['background_url'],
      });
    });
    it('should be able to update', async () => {
      await getNameInput().then((input) => input.setValue('Cat'));
      await getDescriptionInput().then((input) => input.setValue('Cat desc'));

      const select = await getSelectPage();
      await select.open();
      await getSelectPage().then((select) => select.clickOptions({ text: '-- Select documentation page --' }));
      await getHideCategoryToggle().then((toggle) => toggle.toggle());

      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, name: 'Cat', description: 'Cat desc', page: undefined, hidden: true });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
    });
    it('should be able to change picture', async () => {
      const picturePicker = await getPicturePicker();
      await picturePicker.dropFiles([new File([''], 'cat', { type: 'png' })]);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, picture_url: undefined, picture: 'data:application/octet-stream;base64,' });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
    });
    it('should be able to change background', async () => {
      const backgroundPicker = await getBackgroundPicker();
      await backgroundPicker.dropFiles([new File([''], 'cat', { type: 'png' })]);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, background_url: undefined, background: 'data:application/octet-stream;base64,' });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
    });
  });

  // Form
  describe('Form', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
    });
    it('should require name', async () => {
      const nameInput = await getNameInput();
      expect(await nameInput.getValue()).toEqual(CATEGORY.name);

      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toBeFalsy();
      await nameInput.setValue('New name');
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toBeTruthy();
      expect(await getSaveBar().then((saveBar) => saveBar.isSubmitButtonInvalid())).toBeFalsy();

      await nameInput.setValue('');
      expect(await getSaveBar().then((saveBar) => saveBar.isSubmitButtonInvalid())).toBeTruthy();
    });
    it('should unselect documentation page', async () => {
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toBeFalsy();

      const select = await getSelectPage();
      await select.open();
      await getSelectPage().then((select) => select.clickOptions({ text: '-- Select documentation page --' }));
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toBeTruthy();
      expect(await getSaveBar().then((saveBar) => saveBar.isSubmitButtonInvalid())).toBeFalsy();
    });
  });

  function expectPortalPagesList(pages: Page[] = [{ id: 'page-1', name: 'Page 1' }]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/portal/pages?type=MARKDOWN&published=true`).flush(pages);
  }

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

  // Access components
  async function getNameInput(): Promise<MatInputHarness> {
    return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  }
  async function getDescriptionInput(): Promise<MatInputHarness> {
    return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  }
  async function getSelectPage(): Promise<MatSelectHarness> {
    return await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName="page"]' }));
  }
  async function getHideCategoryToggle(): Promise<MatSlideToggleHarness> {
    return await harnessLoader.getHarness(MatSlideToggleHarness);
  }
  async function getSaveBar(): Promise<GioSaveBarHarness> {
    return await rootLoader.getHarness(GioSaveBarHarness);
  }
  async function getPicturePicker(): Promise<GioFormFilePickerInputHarness> {
    return await harnessLoader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
  }
  async function getBackgroundPicker(): Promise<GioFormFilePickerInputHarness> {
    return await harnessLoader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }));
  }
});
