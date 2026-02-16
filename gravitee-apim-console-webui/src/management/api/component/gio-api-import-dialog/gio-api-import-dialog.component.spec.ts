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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioFormFilePickerInputHarness } from '@gravitee/ui-particles-angular';

import { GioApiImportDialogComponent } from './gio-api-import-dialog.component';
import { GioApiImportDialogModule } from './gio-api-import-dialog.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

describe('GioApiImportDialogComponent', () => {
  let component: GioApiImportDialogComponent;
  let fixture: ComponentFixture<GioApiImportDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const policies = [
    {
      id: 'json-validation',
      name: 'JSON Validation',
      onRequest: false,
      onResponse: false,
    },
    {
      id: 'mock',
      name: 'Mock',
      onRequest: false,
      onResponse: false,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, GioApiImportDialogModule, MatIconTestingModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {},
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            policies: [...policies],
          },
        },
      ],
    });
    fixture = TestBed.createComponent(GioApiImportDialogComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('With file', () => {
    it('should import Gravitee ApiDefinition file', async () => {
      const fileInput = await loader.getHarness(GioFormFilePickerInputHarness);

      await fileInput.dropFiles([new File(['{}'], 'gravitee-api-definition.json', { type: 'application/json' })]);
      // Wait for the file to be read
      await new Promise(resolve => setTimeout(resolve, 50));

      // expect not config needed
      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(0);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual('{}');
    });

    it('should import swagger file', async () => {
      const fileInput = await loader.getHarness(GioFormFilePickerInputHarness);

      await fileInput.dropFiles([new File(['{"swagger": true}'], 'swagger.json', { type: 'application/json' })]);
      // Wait for the file to be read
      await new Promise(resolve => setTimeout(resolve, 50));

      const importDocumentationInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importDocumentation"]' }),
      );
      await importDocumentationInput.check();

      const importPathMappingInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importPathMapping"]' }),
      );
      await importPathMappingInput.check();

      const policyCheckboxes = await loader.getAllHarnesses(MatCheckboxHarness.with({ ancestor: '[formgroupname="importPolicies"]' }));

      await parallel(() =>
        policyCheckboxes.map(async policy => {
          expect(await policy.isDisabled()).toBeTruthy();
        }),
      );

      const importPolicyPathsInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importPolicyPaths"]' }),
      );
      await importPolicyPathsInput.check();

      // Can select policies if "Create flows on path is not selected"
      await parallel(() =>
        policyCheckboxes.map(async policy => {
          expect(await policy.isDisabled()).toBeFalsy();
        }),
      );

      const jsonValidationInput = policyCheckboxes[0];
      await jsonValidationInput.check();

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import/swagger?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual({
        format: 'API',
        payload: '{"swagger": true}',
        type: 'INLINE',
        with_documentation: true,
        with_path_mapping: true,
        with_policies: ['json-validation'],
        with_policy_paths: true,
      });
    });

    it('should import wsdl file', async () => {
      const fileInput = await loader.getHarness(GioFormFilePickerInputHarness);

      await fileInput.dropFiles([new File(['<wsdl></wsdl>'], 'wsdl.wsdl', { type: 'application/xml' })]);
      // Wait for the file to be read
      await new Promise(resolve => setTimeout(resolve, 50));

      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(5);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import/swagger?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual({
        format: 'WSDL',
        payload: '<wsdl></wsdl>',
        type: 'INLINE',
        with_documentation: false,
        with_path_mapping: false,
        with_policies: [],
        with_policy_paths: false,
      });
    });
  });

  describe('With URL', () => {
    it('should import with swagger URL', async () => {
      const swaggerTab = await loader.getHarness(MatTabHarness.with({ label: component.tabLabels.SwaggerOpenAPI }));
      await swaggerTab.select();

      const descriptorUrlInput = (await (
        await loader.getHarness(MatFormFieldHarness.with({ selector: '.content__url-tab__field' }))
      ).getControl()) as MatInputHarness;
      await descriptorUrlInput.setValue('https://gravitee.io');

      const importDocumentationInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importDocumentation"]' }),
      );
      await importDocumentationInput.check();

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import/swagger?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual({
        format: 'API',
        payload: 'https://gravitee.io',
        type: 'URL',
        with_documentation: true,
        with_path_mapping: false,
        with_policies: [],
        with_policy_paths: false,
      });
    });

    it('should import with Gravitee ApiDefinition URL', async () => {
      const swaggerTab = await loader.getHarness(MatTabHarness.with({ label: component.tabLabels.ApiDefinition }));
      await swaggerTab.select();

      const descriptorUrlInput = (await (
        await loader.getHarness(MatFormFieldHarness.with({ selector: '.content__url-tab__field' }))
      ).getControl()) as MatInputHarness;
      await descriptorUrlInput.setValue('https://gravitee.io');

      // expect not config needed
      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(0);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import-url?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual('https://gravitee.io');
    });

    it('should import with WSDL URL', async () => {
      const swaggerTab = await loader.getHarness(MatTabHarness.with({ label: component.tabLabels.WSDL }));
      await swaggerTab.select();

      const descriptorUrlInput = (await (
        await loader.getHarness(MatFormFieldHarness.with({ selector: '.content__url-tab__field' }))
      ).getControl()) as MatInputHarness;
      await descriptorUrlInput.setValue('https://gravitee.io');

      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(5);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/import/swagger?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual({
        format: 'WSDL',
        payload: 'https://gravitee.io',
        type: 'URL',
        with_documentation: false,
        with_path_mapping: false,
        with_policies: [],
        with_policy_paths: false,
      });
    });
  });

  describe('With apiId', () => {
    beforeEach(() => {
      component.isUpdateMode = true;
      component.updateModeApiId = 'apiId';
      fixture.detectChanges();
    });

    it('should import with Gravitee ApiDefinition URL', async () => {
      const swaggerTab = await loader.getHarness(MatTabHarness.with({ label: component.tabLabels.ApiDefinition }));
      await swaggerTab.select();

      const descriptorUrlInput = (await (
        await loader.getHarness(MatFormFieldHarness.with({ selector: '.content__url-tab__field' }))
      ).getControl()) as MatInputHarness;
      await descriptorUrlInput.setValue('https://gravitee.io');

      // expect not config needed
      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(0);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/apiId/import-url?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual('https://gravitee.io');
    });

    it('should import wsdl file', async () => {
      const fileInput = await loader.getHarness(GioFormFilePickerInputHarness);

      await fileInput.dropFiles([new File(['<wsdl></wsdl>'], 'wsdl.wsdl', { type: 'application/xml' })]);
      // Wait for the file to be read
      await new Promise(resolve => setTimeout(resolve, 50));

      const checkboxInput = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxInput.length).toEqual(5);

      const importButton = await loader.getHarness(MatButtonHarness.with({ text: 'Import' }));
      expect(await importButton.isDisabled()).toBe(false);
      await importButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/apiId/import/swagger?definitionVersion=2.0.0`,
      });

      expect(req.request.body).toEqual({
        format: 'WSDL',
        payload: '<wsdl></wsdl>',
        type: 'INLINE',
        with_documentation: false,
        with_path_mapping: false,
        with_policies: [],
        with_policy_paths: false,
      });
    });
  });
});
