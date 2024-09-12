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
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { MatCardHarness } from '@angular/material/card/testing';

import { CategoryComponent } from './category.component';

import { Page } from '../../../../entities/page';
import { NewCategory } from '../../../../entities/category/NewCategory';
import { UpdateCategory } from '../../../../entities/category/UpdateCategory';
import { CategoriesModule } from '../categories.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Category } from '../../../../entities/category/Category';
import { UpdateApi, Api as MAPIv2Api, fakeApiV2 as fakeMAPIv2Api } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { CategoryApi } from '../../../../entities/management-api-v2/category/categoryApi';
import { fakeCategoryApi } from '../../../../entities/management-api-v2/category/categoryApi.fixture';
import { UpdateCategoryApi } from '../../../../entities/management-api-v2/category/updateCategoryApi';
import { GioApiSelectDialogHarness } from '../../../../shared/components/gio-api-select-dialog/gio-api-select-dialog.harness';
import { EnvSettings } from '../../../../entities/Constants';
import { EnvironmentSettingsService } from '../../../../services-ngx/environment-settings.service';

describe('CategoryComponent', () => {
  let component: CategoryComponent;
  let fixture: ComponentFixture<CategoryComponent>;
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

  const DEFAULT_PORTAL_SETTINGS = {
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

  const init = async (categoryId: string, snapshot: Partial<EnvSettings> = DEFAULT_PORTAL_SETTINGS) => {
    await TestBed.configureTestingModule({
      declarations: [CategoryComponent],
      imports: [NoopAnimationsModule, GioTestingModule, CategoriesModule],
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
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to avoid the warning
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CategoryComponent);
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
      expectGetCategoryApis(CATEGORY.id);
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
      expectGetCategoryApis(CATEGORY.id);
    });
    it('should be able to change picture', async () => {
      const picturePicker = await getPicturePicker();
      await picturePicker.dropFiles([new File([''], 'cat', { type: 'png' })]);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, picture_url: undefined, picture: 'data:application/octet-stream;base64,' });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
    });
    it('should be able to change background', async () => {
      const backgroundPicker = await getBackgroundPicker();
      await backgroundPicker.dropFiles([new File([''], 'cat', { type: 'png' })]);
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());
      expectGetCategory(CATEGORY);
      expectPutCategory({ ...CATEGORY, background_url: undefined, background: 'data:application/octet-stream;base64,' });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
    });
  });

  describe('Form', () => {
    beforeEach(async () => {
      await init(CATEGORY.id);
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
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
    const APIS: CategoryApi[] = [
      fakeCategoryApi({ id: 'lowlight', name: 'Lowlight', order: 0 }),
      fakeCategoryApi({ id: 'highlight', name: 'Highlight', order: 1 }),
    ];
    const CAT_API_LIST: Category = { ...CATEGORY, highlightApi: 'highlight' };

    beforeEach(async () => {
      await init(CAT_API_LIST.id);
      expectGetCategory(CAT_API_LIST);
      fixture.detectChanges();

      expectPortalPagesList();
    });
    it('should show empty APIs', async () => {
      expectGetCategoryApis(CAT_API_LIST.id);
      const rows = await getTableRows();
      expect(await rows[0].host().then((host) => host.text())).toContain('There are no APIs for this category.');
    });
    it('should show API list', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      expect(await getNameByRowIndex(0)).toEqual('Lowlight');
      expect(await getNameByRowIndex(1)).toEqual('Highlight');
    });
    it('should show highlight badge', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      const rows = await getTableRows();
      expect(await rows[1].getCells().then((cells) => cells[0].getHarnessOrNull(MatIconHarness))).toBeTruthy();
    });
    it('should update Category when choosing another API to highlight', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
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
      expectGetCategoryApis(CATEGORY.id);
    });

    it('should update Category when removing highlighted API', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
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
      expectGetCategoryApis(CATEGORY.id);
    });

    it('should remove API from category', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      const removeApiBtn = await getActionButtonByRowIndexAndTooltip(0, 'Remove API');
      expect(removeApiBtn).toBeTruthy();
      await removeApiBtn.click();

      const removeApiDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await removeApiDialog.confirm();

      const api = fakeMAPIv2Api({ id: 'lowlight', categories: [CAT_API_LIST.key, 'other-category'] });
      expectGetApi(api);
      expectUpdateApi({ ...api, categories: ['other-category'] }, { ...api, categories: ['other-category'] });

      expectGetCategory(CATEGORY);
      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
    });

    it('should move API up', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      const moveUpApiBtn = await getActionButtonByRowIndexAndTooltip(0, 'Move Up');
      expect(await moveUpApiBtn.isDisabled()).toBeTruthy();

      const moveDownApiBtn = await getActionButtonByRowIndexAndTooltip(0, 'Move Down');
      expect(await moveDownApiBtn.isDisabled()).toBeFalsy();
      await moveDownApiBtn.click();

      expectPostCategoryApi(CAT_API_LIST.id, APIS[0].id, { order: 1 });

      expectGetCategoryApis(CAT_API_LIST.id, APIS);
    });

    it('should move API down', async () => {
      expectGetCategoryApis(CAT_API_LIST.id, APIS);
      const moveUpApiBtn = await getActionButtonByRowIndexAndTooltip(1, 'Move Up');
      expect(await moveUpApiBtn.isDisabled()).toBeFalsy();
      await moveUpApiBtn.click();

      const moveDownApiBtn = await getActionButtonByRowIndexAndTooltip(1, 'Move Down');
      expect(await moveDownApiBtn.isDisabled()).toBeTruthy();

      expectPostCategoryApi(CAT_API_LIST.id, APIS[1].id, { order: 0 });

      expectGetCategoryApis(CAT_API_LIST.id, APIS);
    });
  });

  describe('Add API to Category', () => {
    const CAT_API_LIST: Category = { ...CATEGORY, highlightApi: 'highlight' };

    beforeEach(async () => {
      await init(CAT_API_LIST.id);
      expectGetCategory(CAT_API_LIST);
      fixture.detectChanges();

      expectPortalPagesList();
      expectGetCategoryApis(CAT_API_LIST.id);
      fixture.detectChanges();
      await addApiToCategory();
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
  });

  describe('Applies to both portal badge', () => {
    it('should show the badge in both headers if portal-next is enabled', async () => {
      await init(CATEGORY.id, { portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, access: { enabled: true } } });
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
      fixture.detectChanges();

      const matCards = await harnessLoader.getAllHarnesses(MatCardHarness);
      expect(matCards).toHaveLength(2);

      expect(await matCards[0].getText()).toContain('Applies to both portals');
      expect(await matCards[1].getText()).toContain('Applies to both portals');
    });
    it('should not show the badge in both headers if portal-next is disabled', async () => {
      await init(CATEGORY.id, { portalNext: { ...DEFAULT_PORTAL_SETTINGS.portalNext, access: { enabled: false } } });
      expectGetCategory(CATEGORY);
      fixture.detectChanges();

      expectPortalPagesList();
      expectGetCategoryApis(CATEGORY.id);
      fixture.detectChanges();

      const matCards = await harnessLoader.getAllHarnesses(MatCardHarness);
      expect(matCards).toHaveLength(2);

      const generalCardText = await matCards[0].getText();
      expect(generalCardText.includes('Applies to both portals')).toEqual(false);

      const apisCardText = await matCards[1].getText();
      expect(apisCardText.includes('Applies to both portals')).toEqual(false);
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

  function expectPostCategoryApi(
    categoryId: string,
    apiId: string,
    updateCategoryApi: UpdateCategoryApi,
    responseCategoryApi = fakeCategoryApi(),
  ) {
    responseCategoryApi.id = apiId;
    responseCategoryApi.order = updateCategoryApi.order;
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/categories/${categoryId}/apis/${apiId}`,
      method: 'POST',
    });
    expect(req.request.body).toEqual(updateCategoryApi);
    req.flush(responseCategoryApi);
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
  async function addApiToCategory(): Promise<void> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.add-button' })).then((btn) => btn.click());
  }
});
