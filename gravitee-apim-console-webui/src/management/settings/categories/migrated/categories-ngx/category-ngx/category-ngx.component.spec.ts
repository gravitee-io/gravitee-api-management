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
import { GioConfirmDialogHarness, GioFormFilePickerInputHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatIconHarness } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { CategoryNgxComponent } from './category-ngx.component';

import { Page } from '../../../../../../entities/page';
import { NewCategory } from '../../../../../../entities/category/NewCategory';
import { UpdateCategory } from '../../../../../../entities/category/UpdateCategory';
import { CategoriesNgxModule } from '../categories-ngx.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { Category } from '../../../../../../entities/category/Category';
import { Api as MAPIApi } from '../../../../../../entities/api';
import { fakeApi as fakeApiMAPI } from '../../../../../../entities/api/Api.fixture';
import { UpdateApi, Api as MAPIv2Api, fakeApiV2 as fakeApiMAPIv2 } from '../../../../../../entities/management-api-v2';

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
    highlightApi: 'highlight',
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
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

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
        highlightApi: null,
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
      expectGetApisByCategory(CATEGORY);
    });

    it('should initialize with category input', () => {
      expect(component.categoryDetails.getRawValue()).toEqual({
        name: CATEGORY.name,
        description: CATEGORY.description,
        page: CATEGORY.page,
        hidden: false,
        picture: ['picture_url'],
        background: ['background_url'],
        highlightApi: 'highlight',
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

  describe('Form', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
      expectGetApisByCategory(CATEGORY);
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

  describe('API List', () => {
    const APIS: MAPIApi[] = [fakeApiMAPI({ id: 'lowlight', name: 'Lowlight' }), fakeApiMAPI({ id: 'highlight', name: 'Highlight' })];
    const CAT_API_LIST: Category = { ...CATEGORY, highlightApi: 'highlight' };

    beforeEach(async () => {
      await init(CAT_API_LIST.id);
      expectGetCategory(CAT_API_LIST);
      fixture.detectChanges();

      expectPortalPagesList();
    });
    it('should show empty APIs', async () => {
      expectGetApisByCategory(CAT_API_LIST);
      const rows = await getTableRows();
      expect(await rows[0].host().then((host) => host.text())).toContain('There are no APIs for this category.');
    });
    it('should show API list', async () => {
      expectGetApisByCategory(CAT_API_LIST, APIS);
      expect(await getNameByRowIndex(0)).toEqual('Lowlight');
      expect(await getNameByRowIndex(1)).toEqual('Highlight');
    });
    it('should show highlight badge', async () => {
      expectGetApisByCategory(CAT_API_LIST, APIS);
      const rows = await getTableRows();
      expect(await rows[1].getCells().then((cells) => cells[0].getHarnessOrNull(MatIconHarness))).toBeTruthy();
    });
    it('should update Category when choosing another API to highlight', async () => {
      expectGetApisByCategory(CAT_API_LIST, APIS);
      const highlightBtn = await getActionButtonByRowIndexAndTooltip(0, 'Highlight API');
      expect(highlightBtn).toBeTruthy();
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toEqual(false);

      await highlightBtn.click();
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toEqual(true);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());

      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, highlightApi: APIS[0].id });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
    });

    it('should update Category when removing highlighted API', async () => {
      expectGetApisByCategory(CAT_API_LIST, APIS);
      const removeHighlightBtn = await getActionButtonByRowIndexAndTooltip(1, 'Remove Highlighted API');
      expect(removeHighlightBtn).toBeTruthy();
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toEqual(false);

      await removeHighlightBtn.click();
      expect(await getSaveBar().then((saveBar) => saveBar.isVisible())).toEqual(true);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());

      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, highlightApi: null });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
    });

    it('should remove API from category', async () => {
      expectGetApisByCategory(CAT_API_LIST, APIS);
      const removeApiBtn = await getActionButtonByRowIndexAndTooltip(0, 'Remove API');
      expect(removeApiBtn).toBeTruthy();
      await removeApiBtn.click();

      const removeApiDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await removeApiDialog.confirm();

      const api = fakeApiMAPIv2({ id: 'lowlight', categories: [CAT_API_LIST.key, 'other-category'] });
      expectGetApi(api);
      expectUpdateApi({ ...api, categories: ['other-category'] }, { ...api, categories: ['other-category'] });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
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

  function expectGetApisByCategory(category: Category, apis: MAPIApi[] = []) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis?category=${category.key}`).flush(apis);
  }

  function expectGetApi(api: MAPIv2Api) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`).flush(api);
  }

  function expectUpdateApi(request: UpdateApi, response: MAPIv2Api) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${response.id}`, method: 'PUT' });
    expect(req.request.body).toEqual(request);
    req.flush(response);
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
  async function getTableRows(): Promise<MatRowHarness[]> {
    return await harnessLoader.getHarness(MatTableHarness).then((table) => table.getRows());
  }
  async function getNameByRowIndex(index: number): Promise<string> {
    return await getTextByColumnNameAndRowIndex('name', index);
  }
  async function getTextByColumnNameAndRowIndex(columnName: string, index: number): Promise<string> {
    return await harnessLoader
      .getHarness(MatTableHarness)
      .then((table) => table.getRows())
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
