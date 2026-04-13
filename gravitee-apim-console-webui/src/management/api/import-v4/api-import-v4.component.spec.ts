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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiImportV4Component, ApiImportV4DialogData } from './api-import-v4.component';
import { ApiImportV4Harness } from './api-import-v4.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiV4, fakePolicyPlugin, PolicyPlugin } from '../../../entities/management-api-v2';

describe('ImportV4Component', () => {
  let fixture: ComponentFixture<ApiImportV4Component>;
  let componentHarness: ApiImportV4Harness;
  let httpTestingController: HttpTestingController;
  const apiJson = JSON.stringify(fakeApiV4({ definitionVersion: 'V4' }));
  const openApiJson = JSON.stringify({ openapi: '3.1.0' });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiImportV4Component, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportV4Component);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiImportV4Harness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    expectGetPolicies([fakePolicyPlugin({ id: 'oas-validation' })]);
  });

  afterEach(() => {
    httpTestingController.verify();
    fixture.destroy();
  });

  it('should not be able to save when form is invalid', async () => {
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
  });

  it('should import an API V4', async () => {
    const apiV4 = fakeApiV4({ definitionVersion: 'V4' });
    const importDefinition = JSON.stringify({ api: apiV4 });

    await componentHarness.selectFormat('gravitee');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();

    await componentHarness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition`,
      })
      .flush(fakeApiV4({ definitionVersion: 'V4' }));
  });

  it('should import an OpenAPI specification', async () => {
    const importDefinition = 'openapi: 3.1.0';

    await componentHarness.selectFormat('openapi');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();

    await componentHarness.pickFiles([new File([importDefinition], 'openapi.yml', { type: 'application/x-yaml' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/swagger`,
      })
      .flush(fakeApiV4({ definitionVersion: 'V4' }));
  });

  it('should allow documentation import only when OpenAPI specification type is selected', async () => {
    await componentHarness.selectFormat('openapi');
    expect(await componentHarness.isDocumentationImportSelected()).toBeTruthy();
    expect(await componentHarness.isDocumentationImportDisabled()).toBeFalsy();
    expect(await componentHarness.isOASValidationPolicyImportSelected()).toBeTruthy();
    expect(await componentHarness.isOASValidationPolicyImportDisabled()).toBeFalsy();

    await componentHarness.selectFormat('gravitee');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
    expect(await componentHarness.isDocumentationImportDisabled()).toBeTruthy();
    expect(await componentHarness.isDocumentationImportSelected()).toBeFalsy();
    expect(await componentHarness.isOASValidationPolicyImportDisabled()).toBeTruthy();
    expect(await componentHarness.isOASValidationPolicyImportSelected()).toBeFalsy();

    await componentHarness.selectFormat('openapi');
    expect(await componentHarness.isDocumentationImportDisabled()).toBeFalsy();
    expect(await componentHarness.isDocumentationImportSelected()).toBeTruthy();
    await componentHarness.toggleDocumentationImport();
    expect(await componentHarness.isDocumentationImportSelected()).toBeFalsy();
    await componentHarness.toggleOASValidationPolicyImport();
    expect(await componentHarness.isOASValidationPolicyImportSelected()).toBeFalsy();

    await componentHarness.toggleDocumentationImport();
  });

  it.each`
    fileName           | importDefinition    | type                    | selectedFormat
    ${'openapi.json'}  | ${openApiJson}      | ${'application/json'}   | ${'gravitee'}
    ${'openapi.yml'}   | ${'openapi: 3.1.0'} | ${'application/x-yaml'} | ${'gravitee'}
    ${'openapi.yaml'}  | ${'openapi: 3.1.0'} | ${'application/x-yaml'} | ${'gravitee'}
    ${'gravitee.json'} | ${apiJson}          | ${'application/json'}   | ${'openapi'}
  `(
    'should display an error when file is $fileName and selected format is $selectedFormat',
    async ({ fileName, importDefinition, type, selectedFormat }) => {
      await componentHarness.selectFormat(selectedFormat);
      expect(await componentHarness.isSaveDisabled()).toBeTruthy();

      await componentHarness.pickFiles([new File([importDefinition], fileName, { type })]);
      expect(await componentHarness.isSaveDisabled()).toBeTruthy();
      expect(await componentHarness.isFormatErrorBannerDisplayed()).toBeTruthy();
    },
  );
});

function expectGetPolicies(policies: PolicyPlugin[]) {
  TestBed.inject(HttpTestingController)
    .expectOne({
      url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies`,
      method: 'GET',
    })
    .flush(policies);
}

describe('ImportV4Component - Update mode (opened as dialog)', () => {
  const API_ID = 'test-api-id';
  let fixture: ComponentFixture<ApiImportV4Component>;
  let componentHarness: ApiImportV4Harness;
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<ApiImportV4Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiImportV4Component, GioTestingModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { apiId: API_ID } as ApiImportV4DialogData },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportV4Component);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiImportV4Harness);
    httpTestingController = TestBed.inject(HttpTestingController);
    dialogRef = TestBed.inject(MatDialogRef);
    fixture.detectChanges();
    expectGetPolicies([fakePolicyPlugin({ id: 'oas-validation' })]);
  });

  afterEach(() => {
    httpTestingController.verify();
    fixture.destroy();
  });

  it('should show Update API button text in update mode', async () => {
    const saveButton = await componentHarness.getSaveButtonText();
    expect(saveButton).toContain('Update API');
  });

  it('should call PUT endpoint when updating V4 API from Gravitee definition', async () => {
    const apiV4 = fakeApiV4({ definitionVersion: 'V4' });
    const importDefinition = JSON.stringify({ api: apiV4 });

    await componentHarness.selectFormat('gravitee');
    await componentHarness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_import/definition`,
    });
    req.flush(fakeApiV4({ id: API_ID, definitionVersion: 'V4' }));

    expect(dialogRef.close).toHaveBeenCalledWith(API_ID);
  });

  it('should PUT OpenAPI update with withDocumentation and withOASValidationPolicy flags', async () => {
    await componentHarness.selectFormat('openapi');
    await componentHarness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_import/swagger`,
    });
    expect(req.request.body).toMatchObject({
      payload: 'openapi: 3.1.0',
      withDocumentation: true,
      withOASValidationPolicy: true,
    });
    req.flush(fakeApiV4({ id: API_ID, definitionVersion: 'V4' }));
  });

  it('should send withDocumentation false on OpenAPI update when documentation toggle is off', async () => {
    await componentHarness.selectFormat('openapi');
    await componentHarness.toggleDocumentationImport();
    await componentHarness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);

    await componentHarness.save();
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_import/swagger`,
    });
    expect(req.request.body).toMatchObject({
      withDocumentation: false,
      withOASValidationPolicy: true,
    });
    req.flush(fakeApiV4({ id: API_ID, definitionVersion: 'V4' }));
  });

  it('should close dialog when cancel is clicked', async () => {
    await componentHarness.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
