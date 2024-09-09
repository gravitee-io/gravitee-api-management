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

import { ApiImportV4Component } from './api-import-v4.component';
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
  });

  it('should not be able to save when form is invalid', async () => {
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
  });

  it('should import an API V4', async () => {
    const apiV4 = fakeApiV4({ definitionVersion: 'V4' });
    const importDefinition = JSON.stringify({ api: apiV4 });

    await componentHarness.selectFormat('gravitee');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
    await componentHarness.selectSource('local');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();

    await componentHarness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition`,
    });
  });

  it('should import an OpenAPI specification', async () => {
    const importDefinition = 'openapi: 3.1.0';

    await componentHarness.selectFormat('openapi');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
    await componentHarness.selectSource('local');
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();

    await componentHarness.pickFiles([new File([importDefinition], 'openapi.yml', { type: 'application/x-yaml' })]);
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();
    httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/swagger`,
    });
  });

  it('should allow documentation import only when OpenAPI specification type is selected', async () => {
    expectGetPolicies([fakePolicyPlugin({ id: 'oas-validation' })]);

    await componentHarness.selectFormat('openapi');
    await componentHarness.selectSource('local');
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
      await componentHarness.selectSource('local');
      expect(await componentHarness.isSaveDisabled()).toBeTruthy();

      await componentHarness.pickFiles([new File([importDefinition], fileName, { type })]);
      expect(await componentHarness.isSaveDisabled()).toBeTruthy();
      expect(await componentHarness.isFormatErrorBannerDisplayed()).toBeTruthy();
    },
  );

  function expectGetPolicies(policies: PolicyPlugin[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies`,
        method: 'GET',
      })
      .flush(policies);
  }
});
