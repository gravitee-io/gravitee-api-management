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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioFormFilePickerInputHarness } from '@gravitee/ui-particles-angular';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../testing';

import { GioApiImportDialogComponent } from './gio-api-import-dialog.component';
import { GioApiImportDialogModule } from './gio-api-import-dialog.module';

describe('GioApiImportDialogComponent', () => {
  let component: GioApiImportDialogComponent;
  let fixture: ComponentFixture<GioApiImportDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, GioApiImportDialogModule, MatIconTestingModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {},
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            policies: [
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
            ],
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
      await new Promise((resolve) => setTimeout(resolve, 50));

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
      await new Promise((resolve) => setTimeout(resolve, 50));

      const importDocumentationInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importDocumentation"]' }),
      );
      await importDocumentationInput.check();

      const importPathMappingInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importPathMapping"]' }),
      );
      await importPathMappingInput.check();

      const importPolicyPathsInput = await loader.getHarness(
        MatCheckboxHarness.with({ selector: '[formControlName="importPolicyPaths"]' }),
      );
      await importPolicyPathsInput.check();

      const mockInput = await loader.getHarness(MatCheckboxHarness.with({ selector: '[ng-reflect-name="mock"]' }));
      await mockInput.uncheck();

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
      await new Promise((resolve) => setTimeout(resolve, 50));

      // expect not config needed
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
});
