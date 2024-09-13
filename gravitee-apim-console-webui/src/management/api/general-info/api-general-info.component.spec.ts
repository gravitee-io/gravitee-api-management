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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import {
  GioFormFilePickerInputHarness,
  GioFormTagsInputHarness,
  GioSaveBarHarness,
  LICENSE_CONFIGURATION_TESTING,
} from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { ApiGeneralInfoModule } from './api-general-info.module';
import { ApiGeneralInfoComponent } from './api-general-info.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Category } from '../../../entities/category/Category';
import { Api, DefinitionVersion, fakeApiV1, fakeApiV2, fakeApiV4, fakeProxyTcpApiV4 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';

describe('ApiGeneralInfoComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiGeneralInfoComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const initComponent = (installationType = 'standalone') => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiGeneralInfoModule, MatIconTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiId: API_ID }),
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-u', 'api-definition-d', 'api-definition-c', 'api-definition-r'],
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
        {
          provide: Constants,
          useValue: {
            ...CONSTANTS_TESTING,
            org: {
              ...CONSTANTS_TESTING.org,
              settings: {
                ...CONSTANTS_TESTING.org.settings,
                v4EmulationEngine: {
                  defaultValue: 'v4-emulation-engine',
                },
                management: {
                  ...CONSTANTS_TESTING.org.settings.management,
                  installationType,
                },
              },
            },
          },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });

    window.URL.createObjectURL = jest.fn();
    fixture = TestBed.createComponent(ApiGeneralInfoComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  };
  beforeAll(() => {
    GioFormFilePickerInputHarness.forceImageOnload();
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
    jest.resetAllMocks();
  });

  describe('API V1', () => {
    beforeEach(() => initComponent());

    it('should not be editable', async () => {
      const api = fakeApiV1({
        id: API_ID,
        name: '👴🏻 Old API',
        apiVersion: '1.0.0',
        labels: ['label1', 'label2'],
        categories: ['category1'],
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest([
        { id: 'category1', name: 'Category 1', key: 'category1' },
        { id: 'category2', name: 'Category 2', key: 'category2' },
      ]);

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.isDisabled()).toEqual(true);

      const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
      expect(await versionInput.isDisabled()).toEqual(true);

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      expect(await descriptionInput.isDisabled()).toEqual(true);

      const picturePicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
      expect(await picturePicker.isDisabled()).toEqual(true);

      const backgroundPicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }));
      expect(await backgroundPicker.isDisabled()).toEqual(true);

      const labelsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labels"]' }));
      expect(await labelsInput.isDisabled()).toEqual(true);

      const categoriesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="categories"]' }));
      expect(await categoriesInput.isDisabled()).toEqual(true);

      await Promise.all(
        [/Import/, /Duplicate/, /Promote/].map(async (btnText) => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );

      await Promise.all(
        [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async (btnText) => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );
    });
  });

  describe('API V2', () => {
    describe('with standalone installation', () => {
      beforeEach(() => initComponent());

      it('should edit api details', async () => {
        const api = fakeApiV2({
          id: API_ID,
          name: '🐶 API',
          apiVersion: '1.0.0',
          labels: ['label1', 'label2'],
          categories: ['category1'],
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest([
          { id: 'category1', name: 'Category 1', key: 'category1' },
          { id: 'category2', name: 'Category 2', key: 'category2' },
        ]);

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
        expect(await nameInput.getValue()).toEqual('🐶 API');
        await nameInput.setValue('🦊 API');

        const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
        expect(await versionInput.getValue()).toEqual('1.0.0');
        await versionInput.setValue('2.0.0');

        const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
        expect(await descriptionInput.getValue()).toEqual('The whole universe in your hand.');
        await descriptionInput.setValue('🦊 API description');

        const picturePicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
        expect((await picturePicker.getPreviews())[0]).toContain(api._links['pictureUrl']);
        await picturePicker.dropFiles([newImageFile('new-image.png', 'image/png')]);

        const backgroundPicker = await loader.getHarness(
          GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }),
        );
        expect((await backgroundPicker.getPreviews())[0]).toContain(api._links['backgroundUrl']);
        await backgroundPicker.dropFiles([newImageFile('new-image.png', 'image/png')]);

        const labelsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labels"]' }));
        expect(await labelsInput.getTags()).toEqual(['label1', 'label2']);
        await labelsInput.addTag('label3');

        const categoriesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="categories"]' }));
        expect(await categoriesInput.getValueText()).toEqual('Category 1');
        await categoriesInput.clickOptions({ text: 'Category 2' });

        const emulateV4EngineInput = await loader.getHarness(
          MatSlideToggleHarness.with({ selector: '[formControlName="emulateV4Engine"]' }),
        );
        expect(await emulateV4EngineInput.isChecked()).toBe(false);
        await emulateV4EngineInput.check();

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);

        // Wait image to be covert to base64
        await new Promise((resolve) => setTimeout(resolve, 10));

        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.name).toEqual('🦊 API');
        expect(req.request.body.apiVersion).toEqual('2.0.0');
        expect(req.request.body.description).toEqual('🦊 API description');
        expect(req.request.body.labels).toEqual(['label1', 'label2', 'label3']);
        expect(req.request.body.categories).toEqual(['category1', 'category2']);
        expect(req.request.body.executionMode).toEqual('V4_EMULATION_ENGINE');
        req.flush(api);

        const pictureReq = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/picture`,
        });
        expect(pictureReq.request.body).toEqual('data:image/png;base64,');
        pictureReq.flush(null);

        const backgroundReq = httpTestingController.expectOne({
          method: 'PUT',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/background`,
        });
        expect(backgroundReq.request.body).toEqual('data:image/png;base64,');
        backgroundReq.flush(null);

        // page reload after update
        expectApiGetRequest(api);
        expectCategoriesGetRequest([
          { id: 'category1', name: 'Category 1', key: 'category1' },
          { id: 'category2', name: 'Category 2', key: 'category2' },
        ]);
      });

      it('should disable field when origin is kubernetes', async () => {
        const api = fakeApiV2({
          id: API_ID,
          name: '🐶 API',
          apiVersion: '1.0.0',
          labels: ['label1', 'label2'],
          categories: ['category1'],
          definitionContext: {
            origin: 'KUBERNETES',
          },
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest([
          { id: 'category1', name: 'Category 1', key: 'category1' },
          { id: 'category2', name: 'Category 2', key: 'category2' },
        ]);

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
        expect(await nameInput.isDisabled()).toEqual(true);

        const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
        expect(await versionInput.isDisabled()).toEqual(true);

        const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
        expect(await descriptionInput.isDisabled()).toEqual(true);

        const picturePicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
        expect(await picturePicker.isDisabled()).toEqual(true);

        const backgroundPicker = await loader.getHarness(
          GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }),
        );
        expect(await backgroundPicker.isDisabled()).toEqual(true);

        const labelsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labels"]' }));
        expect(await labelsInput.isDisabled()).toEqual(true);

        const categoriesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="categories"]' }));
        expect(await categoriesInput.isDisabled()).toEqual(true);

        const emulateV4EngineInput = await loader.getHarness(
          MatSlideToggleHarness.with({ selector: '[formControlName="emulateV4Engine"]' }),
        );
        expect(await emulateV4EngineInput.isDisabled()).toEqual(true);

        await Promise.all(
          [/Import/, /Duplicate/, /Promote/].map(async (btnText) => {
            const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
            expect(await button.isDisabled()).toEqual(true);
          }),
        );

        await Promise.all(
          [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async (btnText) => {
            const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
            expect(await button.isDisabled()).toEqual(true);
          }),
        );
      });

      it('should export api', async () => {
        const api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();

        const button = await loader.getHarness(MatButtonHarness.with({ text: /Export/ }));
        await button.click();

        await waitImageCheck();
        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#exportApiDialog' }));

        const groupCheckbox = await confirmDialog.getHarness(MatCheckboxHarness.with({ selector: '[ng-reflect-name="groups"]' }));
        await groupCheckbox.uncheck();

        const confirmButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Export' }));
        await confirmButton.click();

        await expectExportGetRequest(API_ID);
      });

      it('should duplicate api', async () => {
        const api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();

        const button = await loader.getHarness(MatButtonHarness.with({ text: /Duplicate/ }));
        await button.click();

        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#duplicateApiDialog' }));

        const contextPathInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="contextPath"]' }));
        await contextPathInput.setValue('/duplicate');
        await expectVerifyContextPathGetRequest();

        const versionInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
        await versionInput.setValue('1.0.0');

        const confirmButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Duplicate' }));
        await confirmButton.click();

        await expectDuplicatePostRequest(API_ID);

        expect(routerNavigateSpy).toHaveBeenCalledWith(['../', 'newApiId'], expect.anything());
      });
    });

    describe('with multi-tenant installation', () => {
      beforeEach(() => initComponent('multi-tenant'));

      it('should not be able to promote api ', async () => {
        const api = fakeApiV2({
          id: API_ID,
          name: '🐶 API',
          apiVersion: '1.0.0',
          labels: ['label1', 'label2'],
          categories: ['category1'],
          definitionContext: {
            origin: 'KUBERNETES',
          },
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest([
          { id: 'category1', name: 'Category 1', key: 'category1' },
          { id: 'category2', name: 'Category 2', key: 'category2' },
        ]);

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();

        const button = await loader.getHarness(MatButtonHarness.with({ text: /Promote/ }));
        expect(await button.isDisabled()).toEqual(true);
      });
    });
  });

  describe('API V4', () => {
    beforeEach(() => initComponent());

    it('should edit api details', async () => {
      const api = fakeApiV4({
        id: API_ID,
        name: '🐶 API',
        apiVersion: '1.0.0',
        labels: ['label1', 'label2'],
        categories: ['category1'],
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest([
        { id: 'category1', name: 'Category 1', key: 'category1' },
        { id: 'category2', name: 'Category 2', key: 'category2' },
      ]);

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.getValue()).toEqual('🐶 API');
      await nameInput.setValue('🦊 API');

      const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
      expect(await versionInput.getValue()).toEqual('1.0.0');
      await versionInput.setValue('2.0.0');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      expect(await descriptionInput.getValue()).toEqual('The whole universe in your hand.');
      await descriptionInput.setValue('🦊 API description');

      const picturePicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
      expect((await picturePicker.getPreviews())[0]).toContain(api._links['pictureUrl']);
      await picturePicker.dropFiles([newImageFile('new-image.png', 'image/png')]);

      const backgroundPicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }));
      expect((await backgroundPicker.getPreviews())[0]).toContain(api._links['backgroundUrl']);
      await backgroundPicker.dropFiles([newImageFile('new-image.png', 'image/png')]);

      const labelsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labels"]' }));
      expect(await labelsInput.getTags()).toEqual(['label1', 'label2']);
      await labelsInput.addTag('label3');

      const categoriesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="categories"]' }));
      expect(await categoriesInput.getValueText()).toEqual('Category 1');
      await categoriesInput.clickOptions({ text: 'Category 2' });

      // Should not display emulateV4Engine toggle for v4 APIs
      const emulateV4EngineInput = await loader.getAllHarnesses(
        MatSlideToggleHarness.with({ selector: '[formControlName="emulateV4Engine"]' }),
      );
      expect(emulateV4EngineInput.length).toEqual(0);

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api and update
      expectApiGetRequest(api);
      expectApiVerifyDeployment(api, true);

      // Wait image to be covert to base64
      await new Promise((resolve) => setTimeout(resolve, 10));

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.name).toEqual('🦊 API');
      expect(req.request.body.apiVersion).toEqual('2.0.0');
      expect(req.request.body.description).toEqual('🦊 API description');
      expect(req.request.body.labels).toEqual(['label1', 'label2', 'label3']);
      expect(req.request.body.categories).toEqual(['category1', 'category2']);
      req.flush(api);

      const pictureReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/picture`,
      });
      expect(pictureReq.request.body).toEqual('data:image/png;base64,');
      pictureReq.flush(null);

      const backgroundReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/background`,
      });
      expect(backgroundReq.request.body).toEqual('data:image/png;base64,');
      backgroundReq.flush(null);

      // page reload after update
      expectApiGetRequest(api);
      expectCategoriesGetRequest([
        { id: 'category1', name: 'Category 1', key: 'category1' },
        { id: 'category2', name: 'Category 2', key: 'category2' },
      ]);
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApiV4({
        id: API_ID,
        name: '🐶 API',
        apiVersion: '1.0.0',
        labels: ['label1', 'label2'],
        categories: ['category1'],
        definitionContext: {
          origin: 'KUBERNETES',
        },
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest([
        { id: 'category1', name: 'Category 1', key: 'category1' },
        { id: 'category2', name: 'Category 2', key: 'category2' },
      ]);

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.isDisabled()).toEqual(true);

      const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
      expect(await versionInput.isDisabled()).toEqual(true);

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      expect(await descriptionInput.isDisabled()).toEqual(true);

      const picturePicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="picture"]' }));
      expect(await picturePicker.isDisabled()).toEqual(true);

      const backgroundPicker = await loader.getHarness(GioFormFilePickerInputHarness.with({ selector: '[formControlName="background"]' }));
      expect(await backgroundPicker.isDisabled()).toEqual(true);

      const labelsInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName="labels"]' }));
      expect(await labelsInput.isDisabled()).toEqual(true);

      const categoriesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="categories"]' }));
      expect(await categoriesInput.isDisabled()).toEqual(true);

      expectApiVerifyDeployment(api, true);

      await Promise.all(
        [/Import/, /Duplicate/, /Promote/].map(async (btnText) => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );

      await Promise.all(
        [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async (btnText) => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );
    });

    it('should export api', async () => {
      const api = fakeApiV4({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const button = await loader.getHarness(MatButtonHarness.with({ text: /Export/ }));
      await button.click();

      await waitImageCheck();
      expectApiVerifyDeployment(api, true);

      await expectExportV4GetRequest(API_ID);
    });

    it('should duplicate HTTP api', async () => {
      const api = fakeApiV4({
        id: API_ID,
      });

      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const button = await loader.getHarness(MatButtonHarness.with({ text: /Duplicate/ }));
      expect(await button.isDisabled()).toBeFalsy();
      await button.click();
      expectApiVerifyDeployment(api, true);

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#duplicateApiDialog' }));

      const contextPathInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="contextPath"]' }));
      await contextPathInput.setValue('/duplicate');
      expectVerifyContextPathGetRequest('V4');

      const versionInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
      await versionInput.setValue('1.0.0');

      const confirmButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Duplicate' }));
      await confirmButton.click();

      const req = await expectDuplicatePostRequest(API_ID);

      expect(req.request.body.contextPath).toEqual('/duplicate');
      expect(req.request.body.host).toBeUndefined();

      expect(routerNavigateSpy).toHaveBeenCalledWith(['../', 'newApiId'], expect.anything());
    });

    it('should duplicate TCP api', async () => {
      const api = fakeProxyTcpApiV4({
        id: API_ID,
      });

      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();

      const button = await loader.getHarness(MatButtonHarness.with({ text: /Duplicate/ }));
      expect(await button.isDisabled()).toBeFalsy();
      await button.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#duplicateApiDialog' }));

      const hostInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="host"]' }));
      await hostInput.setValue('duplicate');
      expectVerifyHostGetRequest();

      const versionInput = await confirmDialog.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
      await versionInput.setValue('1.0.0');

      const confirmButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Duplicate' }));
      await confirmButton.click();

      const req = await expectDuplicatePostRequest(API_ID);

      expect(req.request.body.contextPath).toBeUndefined();
      expect(req.request.body.host).toEqual('duplicate');

      expect(routerNavigateSpy).toHaveBeenCalledWith(['../', 'newApiId'], expect.anything());
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectCategoriesGetRequest(categories: Category[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories`, method: 'GET' }).flush(categories);
    fixture.detectChanges();
  }

  function expectDuplicatePostRequest(apiId: string): TestRequest {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_duplicate`, method: 'POST' });
    req.flush({
      id: 'newApiId',
    });
    fixture.detectChanges();
    return req;
  }

  function expectVerifyContextPathGetRequest(definitionVersion: DefinitionVersion = 'V2') {
    if (definitionVersion === 'V4') {
      httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`, method: 'POST' });
    } else {
      httpTestingController.match({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' });
    }
  }

  function expectVerifyHostGetRequest() {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
  }

  function expectExportGetRequest(apiId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/export?exclude=groups&version=default`, method: 'GET' })
      .flush(new Blob(['a'], { type: 'text/json' }));
    fixture.detectChanges();
  }

  function expectExportV4GetRequest(apiId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_export/definition`, method: 'GET' })
      .flush(new Blob(['a'], { type: 'text/json' }));
    fixture.detectChanges();
  }

  function expectApiVerifyDeployment(api: Api, ok: boolean) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/deployments/_verify`, method: 'GET' }).flush({
      ok,
    });
  }
});

export function newImageFile(fileName: string, type: string): File {
  return new File([''], fileName, { type });
}

const waitImageCheck = () => new Promise((resolve) => setTimeout(resolve, 1));
