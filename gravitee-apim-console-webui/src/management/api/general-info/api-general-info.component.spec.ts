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
import { ApiGeneralInfoExportV4DialogHarness } from './api-general-info-export-v4-dialog/api-general-info-export-v4-dialog.harness';
import { ApiGeneralInfoQualityHarness } from './api-general-info-quality/api-general-info-quality.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Category } from '../../../entities/category/Category';
import {
  Api,
  DefinitionVersion,
  fakeApiV1,
  fakeApiV2,
  fakeApiV4,
  fakeProxyTcpApiV4,
  fakeApiFederated,
} from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { Promotion, PromotionTarget } from '../../../entities/promotion';

describe('ApiGeneralInfoComponent', () => {
  const API_ID = 'apiId';
  const promotionTarget: PromotionTarget = {
    id: '42',
    hrids: ['dev'],
    name: 'dev',
    description: 'a cockpit environment',
    organizationId: 'DEFAULT',
    installationId: 'DEFAULT',
  };

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
            snapshot: { params: { apiId: 'apiId' } },
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
        [/Import/, /Duplicate/, /Promote/].map(async btnText => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );

      await Promise.all(
        [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async btnText => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );
    });

    it('should display quality info when enabled', async () => {
      fixture.componentInstance.isQualityEnabled = true;
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
      fixture.detectChanges();

      expectQualityRulesRequest();
      expectQualityRequest(api.id);

      const apiQualityInfo = await loader.getHarness(ApiGeneralInfoQualityHarness);
      expect(apiQualityInfo).toBeTruthy();
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
        await new Promise(resolve => setTimeout(resolve, 10));

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
          originContext: {
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
          [/Import/, /Duplicate/, /Promote/].map(async btnText => {
            const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
            expect(await button.isDisabled()).toEqual(true);
          }),
        );

        await Promise.all(
          [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async btnText => {
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

        const checkboxes = await confirmDialog.getAllHarnesses(MatCheckboxHarness);
        const groupCheckbox = checkboxes[0];
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

      it('should display quality info when enabled', async () => {
        fixture.componentInstance.isQualityEnabled = true;
        const api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();

        // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
        await waitImageCheck();
        fixture.detectChanges();

        await expectQualityRulesRequest();
        await expectQualityRequest(api.id);

        const apiQualityInfo = await loader.getHarness(ApiGeneralInfoQualityHarness);
        expect(apiQualityInfo).toBeTruthy();
      });

      it('should display migrate to v4 button for V2 APIs only', async () => {
        const api = fakeApiV2({ id: API_ID });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();

        // Wait image to be loaded
        await waitImageCheck();

        const migrateBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_info_migrate_menu"]' }));
        expect(migrateBtn).toBeTruthy();
      });

      it('should open migration dialog and perform normal migration on MIGRATABLE', async () => {
        const api = fakeApiV2({ id: API_ID });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();
        await waitImageCheck();

        // Click migrate
        const migrateBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_info_migrate_menu"]' }));
        await migrateBtn.click();

        // Expect a DRY_RUN from the dialog initialization
        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_migrate?mode=DRY_RUN`,
            method: 'POST',
          })
          .flush({ state: 'MIGRATABLE', issues: [] });

        // Expect initial check content shows migratable info
        const dialog = await rootLoader.getHarness(MatDialogHarness);
        const contentText = await dialog.getContentText();
        expect(contentText).toContain('Migration ready');

        // Move to confirmation step
        const continueBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Continue' }));
        await continueBtn.click();

        // Confirm and start migration
        const confirmCheckbox = await dialog.getHarness(MatCheckboxHarness);
        await confirmCheckbox.check();
        const startBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Start Migration' }));
        await startBtn.click();

        // Component then calls migrate without mode
        const postReq = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_migrate`,
          method: 'POST',
        });
        postReq.flush({ state: 'MIGRATED', issues: [] });

        // After migration success, component reloads API and categories
        expectApiGetRequest(api);
        expectCategoriesGetRequest();
      });

      it('should open migration dialog and perform forced migration on CAN_BE_FORCED', async () => {
        const api = fakeApiV2({ id: API_ID });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();
        await waitImageCheck();

        const migrateBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_info_migrate_menu"]' }));
        await migrateBtn.click();

        // Expect a DRY_RUN from the dialog initialization
        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_migrate?mode=DRY_RUN`,
            method: 'POST',
          })
          .flush({ state: 'CAN_BE_FORCED', issues: [{ message: 'Requires manual cleanup', state: 'CAN_BE_FORCED' }] });

        // Expect initial check content shows forcible info
        const dialog = await rootLoader.getHarness(MatDialogHarness);
        const contentText = await dialog.getContentText();
        expect(contentText).toContain('Migration allowed');
        expect(contentText).toContain('Requires manual cleanup');

        // Move to confirmation step
        const continueBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Continue' }));
        await continueBtn.click();

        // Confirm and start migration
        const confirmCheckbox = await dialog.getHarness(MatCheckboxHarness);
        await confirmCheckbox.check();
        const startBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Start Migration' }));
        await startBtn.click();

        // Simulate forced migration
        const postReq = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_migrate?mode=FORCE`,
          method: 'POST',
        });
        postReq.flush({ state: 'MIGRATED', issues: [] });

        // After migration success, component reloads API and categories
        expectApiGetRequest(api);
        expectCategoriesGetRequest();
      });

      it('should show IMPOSSIBLE state and not allow migration', async () => {
        const api = fakeApiV2({ id: API_ID });
        expectApiGetRequest(api);
        expectCategoriesGetRequest();
        await waitImageCheck();

        // Open dialog
        const btn = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_info_migrate_menu"]' }));
        await btn.click();

        // DRY_RUN result: IMPOSSIBLE with issues
        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_migrate?mode=DRY_RUN`,
            method: 'POST',
          })
          .flush({
            state: 'IMPOSSIBLE',
            issues: [
              { message: 'Unsupported policy XYZ', state: 'IMPOSSIBLE' },
              { message: 'Requires manual cleanup', state: 'CAN_BE_FORCED' },
            ],
          });

        const dialog = await rootLoader.getHarness(MatDialogHarness);

        const content = await dialog.getContentText();
        expect(content).toContain('Migration blocked');
        expect(content).toContain('Unsupported policy XYZ');
        expect(content).toContain('Requires manual cleanup');

        // No actions to proceed
        expect(await dialog.getHarnessOrNull(MatButtonHarness.with({ text: 'Continue' }))).toBeNull();
        expect(await dialog.getHarnessOrNull(MatButtonHarness.with({ text: 'Start Migration' }))).toBeNull();

        // Close the dialog
        const cancelBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Cancel' }));
        await cancelBtn.click();

        // No further HTTP calls should be made (no migrate, no refresh)
        httpTestingController.verify({ ignoreCancelled: true });
      });
    });
  });

  describe('API V4', () => {
    beforeEach(() => initComponent());

    it('should display Allow in API Products toggle only for V4 PROXY', async () => {
      const apiProxy = fakeApiV4({ id: API_ID, type: 'PROXY' });
      expectApiGetRequest(apiProxy);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      const productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [] });
      fixture.detectChanges();

      let allowInProductToggle = await loader.getHarnessOrNull(
        MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }),
      );
      expect(allowInProductToggle).not.toBeNull();

      const apiNative = fakeApiV4({ id: API_ID, type: 'NATIVE' });
      fixture.componentInstance['refresh$']?.next?.();
      expectApiGetRequest(apiNative);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      allowInProductToggle = await loader.getHarnessOrNull(
        MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }),
      );
      expect(allowInProductToggle).toBeNull();
    });

    it('should set allowedInApiProducts default value from API or false when undefined', async () => {
      // Case true
      let api = fakeApiV4({ id: API_ID, type: 'PROXY', allowedInApiProducts: true });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();
      await waitImageCheck();
      fixture.detectChanges();
      let productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [] });
      fixture.detectChanges();
      let toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isChecked()).toBe(true);

      // Case false
      api = fakeApiV4({ id: API_ID, type: 'PROXY', allowedInApiProducts: false });
      fixture.componentInstance['refresh$']?.next?.();
      expectApiGetRequest(api);
      expectCategoriesGetRequest();
      await waitImageCheck();
      fixture.detectChanges();
      productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [] });
      fixture.detectChanges();
      toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isChecked()).toBe(false);

      // Case undefined -> default false
      api = fakeApiV4({ id: API_ID, type: 'PROXY' });
      delete (api as any).allowedInApiProducts;
      fixture.componentInstance['refresh$']?.next?.();
      expectApiGetRequest(api);
      expectCategoriesGetRequest();
      await waitImageCheck();
      fixture.detectChanges();
      productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [] });
      fixture.detectChanges();
      toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isChecked()).toBe(false);
    });

    it('should disable allowedInApiProducts toggle when API is used in products', async () => {
      const api = fakeApiV4({ id: API_ID, type: 'PROXY' });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      // Simulate API is used in products (non-empty data)
      const productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [{ id: 'apip1' }] });
      fixture.detectChanges();

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isDisabled()).toBe(true);
    });

    it('should keep allowedInApiProducts enabled when api-products call fails', async () => {
      const api = fakeApiV4({ id: API_ID, type: 'PROXY', allowedInApiProducts: false });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      const productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush(
        { message: 'error' },
        {
          status: 500,
          statusText: 'Server Error',
        },
      );
      fixture.detectChanges();

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isDisabled()).toBe(false);
    });

    it('should disable allowedInApiProducts toggle when isReadOnly (e.g. kubernetes origin)', async () => {
      const api = fakeApiV4({
        id: API_ID,
        type: 'PROXY',
        originContext: { origin: 'KUBERNETES' },
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      const productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: [] });
      fixture.detectChanges();

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isDisabled()).toBe(true);
    });

    it('should enable allowedInApiProducts when api-products returns null or empty data', async () => {
      const api = fakeApiV4({ id: API_ID, type: 'PROXY', allowedInApiProducts: false });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      await waitImageCheck();
      fixture.detectChanges();

      const productsReq = httpTestingController.expectOne(
        (req) => req.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/api-products`) && req.method === 'GET',
      );
      productsReq.flush({ data: null });
      fixture.detectChanges();

      const toggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="allowedInApiProducts"]' }));
      expect(await toggle.isDisabled()).toBe(false);
      expect(await toggle.isChecked()).toBe(false);
    });

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
      await new Promise(resolve => setTimeout(resolve, 10));

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
        originContext: {
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
        [/Import/, /Duplicate/, /Promote/].map(async btnText => {
          const button = await loader.getHarness(MatButtonHarness.with({ text: btnText }));
          expect(await button.isDisabled()).toEqual(true);
        }),
      );

      await Promise.all(
        [/Stop the API/, /Unpublish/, /Make Private/, /Deprecate/, /Delete/].map(async btnText => {
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
      fixture.detectChanges();
      expectApiVerifyDeployment(api, true);

      const button = await loader.getHarness(MatButtonHarness.with({ text: /Export/ }));
      await button.click();

      const apiGeneralInfoExportV4Dialog = await rootLoader.getHarness(ApiGeneralInfoExportV4DialogHarness);

      expect(await apiGeneralInfoExportV4Dialog.getExportOptions()).toEqual(['Groups', 'Members', 'Pages', 'Plans', 'Metadata']);

      await apiGeneralInfoExportV4Dialog.setExportOptions(['Members', 'Pages', 'Plans', 'Metadata']);

      await apiGeneralInfoExportV4Dialog.export();

      expectExportV4GetRequest(API_ID, ['groups']);
    });

    it('should export api CRD', async () => {
      const api = fakeApiV4({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();
      fixture.detectChanges();
      expectApiVerifyDeployment(api, true);

      const button = await loader.getHarness(MatButtonHarness.with({ text: /Export/ }));
      await button.click();

      const apiGeneralInfoExportV4Dialog = await rootLoader.getHarness(ApiGeneralInfoExportV4DialogHarness);

      await apiGeneralInfoExportV4Dialog.selectCRDTab();

      await apiGeneralInfoExportV4Dialog.export();

      expectExportV4CRDGetRequest(API_ID);
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
      expectApiProductsRequest();

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

    it('should not display quality info when enabled', async () => {
      fixture.componentInstance.isQualityEnabled = true;
      const api = fakeApiV4({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();
      fixture.detectChanges();

      expectApiVerifyDeployment(api, true);

      const apiQualityInfo = await loader.getHarnessOrNull(ApiGeneralInfoQualityHarness);
      expect(apiQualityInfo).toBeNull();
    });

    it('should promote V4 API', async () => {
      const api = fakeApiV4({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
      await waitImageCheck();
      fixture.detectChanges();
      expectApiVerifyDeployment(api, true);

      const generalInfoPromote = await loader.getHarness(MatButtonHarness.with({ text: /Promote/ }));
      expect(await generalInfoPromote.isDisabled()).toBeFalsy();
      await generalInfoPromote.click();

      expectPromotionTargets();
      expectExistingPromotion();

      const promotionTarget = await rootLoader.getHarness(
        MatSelectHarness.with({ selector: '[data-testid="promotion-dialog-target-environment"]' }),
      );
      expect(await promotionTarget.getValueText()).toEqual('dev');

      const promoteDialogButton = await rootLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid="promotion-dialog-confirm-button"]' }),
      );
      expect(await promoteDialogButton.isDisabled()).toBeFalsy();
      await promoteDialogButton.click();

      expectPromoteRequest();
    });
  });

  describe('API FEDERATED', () => {
    beforeEach(() => initComponent());

    it('should not display quality info when enabled', async () => {
      fixture.componentInstance.isQualityEnabled = true;
      const api = fakeApiFederated({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      const apiQualityInfo = await loader.getHarnessOrNull(ApiGeneralInfoQualityHarness);
      expect(apiQualityInfo).toBeNull();
    });

    it('should not display api action buttons', async () => {
      fixture.componentInstance.isQualityEnabled = true;
      const api = fakeApiFederated({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectCategoriesGetRequest();

      await Promise.all(
        [/Import/, /Export/, /Duplicate/, /Promote/].map(async btnText => {
          const button = await loader.getHarnessOrNull(MatButtonHarness.with({ text: btnText }));
          expect(button).toBeNull();
        }),
      );
    });
  });

  function expectApiProductsRequest(apiId: string = API_ID, data: { data: unknown[] } = { data: [] }) {
    const req = httpTestingController.expectOne(
      (r) => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/api-products`) && r.method === 'GET',
    );
    req.flush(data);
    fixture.detectChanges();
  }

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

  function expectExportV4GetRequest(apiId: string, excludeAdditionalData: string[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_export/definition?excludeAdditionalData=${excludeAdditionalData.join(',')}`,
        method: 'GET',
      })
      .flush(new Blob(['a'], { type: 'text/json' }));
    fixture.detectChanges();
  }

  function expectExportV4CRDGetRequest(apiId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_export/crd`,
        method: 'GET',
      })
      .flush(new Blob(['a'], { type: 'text/json' }));
    fixture.detectChanges();
  }

  function expectApiVerifyDeployment(api: Api, ok: boolean) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/deployments/_verify`, method: 'GET' }).flush({
      ok,
    });
  }

  function expectQualityRequest(apiId: string) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality`, method: 'GET' });
    fixture.detectChanges();
  }

  function expectQualityRulesRequest() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules`, method: 'GET' });
    fixture.detectChanges();
  }

  function expectPromotionTargets(targets: PromotionTarget[] = [promotionTarget]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/promotion-targets`, method: 'GET' }).flush(targets);
    fixture.detectChanges();
  }

  function expectExistingPromotion(promotions: Promotion[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/promotions/_search?apiId=apiId&statuses=CREATED&statuses=TO_BE_VALIDATED`,
        method: 'POST',
      })
      .flush(promotions);
    fixture.detectChanges();
  }

  function expectPromoteRequest(apiId: string = API_ID, target = promotionTarget) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_promote`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({ targetEnvCockpitId: target.id, targetEnvName: target.name });
  }
});

export function newImageFile(fileName: string, type: string): File {
  return new File([''], fileName, { type });
}

const waitImageCheck = () => new Promise(resolve => setTimeout(resolve, 1));
