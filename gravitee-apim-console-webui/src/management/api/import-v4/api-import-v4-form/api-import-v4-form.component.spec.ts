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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of, Subject } from 'rxjs';

import { ApiImportV4FormComponent } from './api-import-v4-form.component';
import { ApiImportV4FormHarness } from './api-import-v4-form.harness';

import { ApiV4, fakeApiV4, fakePolicyPlugin } from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { PolicyV2Service } from '../../../../services-ngx/policy-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('ApiImportV4FormComponent', () => {
  let fixture: ComponentFixture<ApiImportV4FormComponent>;
  let harness: ApiImportV4FormHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiImportV4FormComponent, GioTestingModule],
      providers: [{ provide: PolicyV2Service, useValue: { list: () => of([]) } }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportV4FormComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiImportV4FormHarness);
    fixture.detectChanges();
  });

  it('should create', async () => {
    expect(await harness.hasRoot()).toBe(true);
  });

  it('should render form root', async () => {
    expect(await harness.hasRoot()).toBe(true);
  });

  it.each([
    ['gravitee', 'Supported file formats: yml, yaml, json'],
    ['openapi', 'Supported file formats: yml, yaml, json'],
  ])('should surface supported file formats in the UI for format %s', async (format, expectedBanner) => {
    await harness.selectFormat(format as string);
    fixture.detectChanges();
    await harness.clickNext();
    fixture.detectChanges();
    expect(await harness.getSupportedFileFormatsBannerText()).toBe(expectedBanner);
  });

  it('should align native file input accept with OpenAPI yaml extensions on the configure step', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    fixture.detectChanges();
    const accept = await harness.getFilePickerInputAccept();
    expect(accept).toContain('.yml');
    expect(accept).toContain('.yaml');
    expect(accept).toContain('.json');
  });

  it('should keep picked file when going back from configure step to format step without changing API format', async () => {
    const importDefinition = JSON.stringify({ api: fakeApiV4({ definitionVersion: 'V4' }) });

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(await harness.isConfigureSourceNextDisabled()).toBe(false);
    expect(await harness.getPickedFilesCount()).toBe(1);

    await harness.clickBack();
    fixture.detectChanges();

    await harness.clickNext();
    fixture.detectChanges();
    expect(await harness.isConfigureSourceNextDisabled()).toBe(false);
    expect(await harness.getPickedFilesCount()).toBe(1);
  });

  it('should clear picked file only when API format changes on step 1 (e.g. OpenAPI → Gravitee → OpenAPI)', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(await harness.isConfigureSourceNextDisabled()).toBe(false);

    await harness.clickBack();
    fixture.detectChanges();

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.selectFormat('openapi');
    fixture.detectChanges();

    await harness.clickNext();
    fixture.detectChanges();
    expect(await harness.isConfigureSourceNextDisabled()).toBe(true);
  });

  it('should surface wsdl/xml in the supported-formats banner when format is wsdl', async () => {
    // WSDL is not yet selectable from the inline format cards (disabled in the template).
    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'wsdl' });
    fixture.detectChanges();
    await harness.clickNext();
    fixture.detectChanges();
    expect(await harness.getSupportedFileFormatsBannerText()).toBe('Supported file formats: wsdl, xml');
  });

  it('should POST Gravitee definition from a local JSON file', async () => {
    const httpMock = TestBed.inject(HttpTestingController);
    const apiV4 = fakeApiV4({ definitionVersion: 'V4' });
    const importDefinition = JSON.stringify({ api: apiV4 });

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickImport();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition`);
    expect(req.request.body).toEqual(importDefinition);
    req.flush(fakeApiV4({ id: 'imported-local' }));
    httpMock.verify();
  });

  it('should POST OpenAPI from a local YAML file', async () => {
    const httpMock = TestBed.inject(HttpTestingController);
    const yaml = 'openapi: 3.1.0';

    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File([yaml], 'openapi.yml', { type: 'application/x-yaml' })]);
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickNext();
    await harness.clickImport();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/swagger`);
    expect(req.request.body).toEqual(
      expect.objectContaining({
        payload: yaml,
        withDocumentation: true,
        withOASValidationPolicy: false,
      }),
    );
    req.flush(fakeApiV4({ id: 'imported-oas' }));
    httpMock.verify();
  });

  const openApiJson = JSON.stringify({ openapi: '3.1.0' });

  it('should block Next and show mismatch when Gravitee is selected but the JSON file is OpenAPI content', async () => {
    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    expect(await harness.isConfigureSourceNextDisabled()).toBe(true);

    await harness.pickFiles([new File([openApiJson], 'openapi.json', { type: 'application/json' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isConfigureSourceNextDisabled()).toBe(true);
    expect(fixture.nativeElement.querySelector('gio-banner-error')).not.toBeNull();
  });

  it('should hide the options step for Gravitee definition', async () => {
    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    expect(await harness.hasOptionsStep()).toBe(false);
  });

  it('should show the options step for OpenAPI specification', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    expect(await harness.hasOptionsStep()).toBe(true);
  });

  it('should keep OpenAPI options when navigating back from options step to configure step', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    await harness.clickNext();

    await harness.toggleDocumentationImport();
    expect(await harness.isDocumentationImportSelected()).toBe(false);

    await harness.clickPrevious();
    fixture.detectChanges();
    await harness.clickNext();
    fixture.detectChanges();

    expect(await harness.isDocumentationImportSelected()).toBe(false);
  });

  it('should not start a second import while the first is in progress', async () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    const result$ = new Subject<ApiV4>();
    jest.spyOn(apiV2, 'importSwaggerApi').mockReturnValue(result$.asObservable());

    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://example.com/openapi.yaml');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickNext();

    await harness.clickImport();
    expect(await harness.isImportButtonDisabled()).toBe(true);
    expect(apiV2.importSwaggerApi).toHaveBeenCalledTimes(1);

    await harness.clickImport();
    expect(apiV2.importSwaggerApi).toHaveBeenCalledTimes(1);

    result$.next({ id: 'new-api' } as ApiV4);
    result$.complete();
    fixture.detectChanges();
    expect(await harness.isImportButtonDisabled()).toBe(false);

    await harness.clickImport();
    expect(apiV2.importSwaggerApi).toHaveBeenCalledTimes(2);
  });

  it('should call importSwaggerApi with remote URL for OpenAPI', async () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    jest.spyOn(apiV2, 'importSwaggerApi').mockReturnValue(of({ id: 'new-api' } as ApiV4));

    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://example.com/openapi.yaml');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickNext();
    await harness.clickImport();

    expect(apiV2.importSwaggerApi).toHaveBeenCalledWith(
      expect.objectContaining({
        payload: 'https://example.com/openapi.yaml',
        withDocumentation: true,
        withOASValidationPolicy: false,
      }),
    );
  });

  it('should call updateApiFromSwagger when updateTargetApiId is set (remote OpenAPI)', async () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    jest.spyOn(apiV2, 'importSwaggerApi');
    jest.spyOn(apiV2, 'updateApiFromSwagger').mockReturnValue(of({ id: 'same-api' } as ApiV4));
    fixture.componentRef.setInput('updateTargetApiId', 'same-api');
    fixture.detectChanges();

    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://example.com/openapi.yaml');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickNext();
    await harness.clickImport();

    expect(apiV2.updateApiFromSwagger).toHaveBeenCalledWith(
      'same-api',
      expect.objectContaining({
        payload: 'https://example.com/openapi.yaml',
        withDocumentation: true,
        withOASValidationPolicy: false,
      }),
    );
    expect(apiV2.importSwaggerApi).not.toHaveBeenCalled();
  });

  it('should POST the remote URL as text/plain to the backend for Gravitee remote', async () => {
    const httpMock = TestBed.inject(HttpTestingController);

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://cdn.example/def.json');
    fixture.detectChanges();
    await harness.clickNext();

    expect(await harness.isImportButtonDisabled()).toBe(false);

    await harness.clickImport();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition-url`);
    expect(req.request.body).toBe('https://cdn.example/def.json');
    expect(req.request.headers.get('Content-Type')).toBe('text/plain');
    req.flush(fakeApiV4({ id: 'imported-remote' }));
    httpMock.verify();
  });

  it('should not GET the remote URL from the browser when Gravitee remote is selected', async () => {
    const httpMock = TestBed.inject(HttpTestingController);

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://cdn.example/def.json');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.clickImport();

    httpMock.expectNone(r => r.method === 'GET' && r.url === 'https://cdn.example/def.json');
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition-url`);
    req.flush(fakeApiV4({ id: 'imported-remote' }));
    httpMock.verify();
  });

  it.each([
    [400, { message: 'Unable to reach the remote URL' }, 'Unable to reach the remote URL'],
    [403, { message: 'You are not allowed to create APIs in this environment' }, 'You are not allowed to create APIs in this environment'],
    [500, { message: 'Internal server error' }, 'Internal server error'],
  ])('should surface backend message in snackbar when import-url endpoint returns %s', async (status, body, expectedMessage) => {
    const snackBarService = TestBed.inject(SnackBarService);
    const snackBarErrorSpy = jest.spyOn(snackBarService, 'error');
    const httpMock = TestBed.inject(HttpTestingController);

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://cdn.example/def.json');
    fixture.detectChanges();
    await harness.clickNext();

    await harness.clickImport();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition-url`);
    req.flush(body, { status, statusText: 'error' });

    expect(snackBarErrorSpy).toHaveBeenCalledWith(expectedMessage);
    httpMock.verify();
  });

  describe('update mode', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('updateTargetApiId', 'existing-api');
      fixture.detectChanges();
    });

    it('should disable the Remote source card with a tooltip explaining the limitation', () => {
      // `sources` is a protected signal exposed only to the template; bracket-access keeps the component API clean.
      const sources = (
        fixture.componentInstance as unknown as { sources: () => { value: string; disabledReason: string | null }[] }
      ).sources();
      const remote = sources.find(s => s.value === 'remote');
      expect(remote?.disabledReason).toBe('Updating an API from a remote URL is not yet supported');
      expect(sources.find(s => s.value === 'local')?.disabledReason).toBeNull();
    });

    it('should not POST to the import-url endpoint when the form is in update mode', async () => {
      const apiV2 = TestBed.inject(ApiV2Service);
      const importFromUrlSpy = jest.spyOn(apiV2, 'importFromUrl');

      // The Remote card is disabled in the UI in update mode; assert the service is never called
      // even if the form's `source` control is forced to 'remote' programmatically.
      fixture.componentInstance.configureFileSourceForm.controls.source.setValue('remote');
      fixture.componentInstance.configureFileSourceForm.controls.remoteUrl.setValue('https://cdn.example/def.json');
      fixture.detectChanges();

      expect(importFromUrlSpy).not.toHaveBeenCalled();
    });
  });
});

describe('ApiImportV4FormComponent (oas-validation policy installed)', () => {
  let fixture: ComponentFixture<ApiImportV4FormComponent>;
  let harness: ApiImportV4FormHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiImportV4FormComponent, GioTestingModule],
      providers: [
        {
          provide: PolicyV2Service,
          useValue: {
            list: () => of([fakePolicyPlugin({ id: 'oas-validation' })]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportV4FormComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiImportV4FormHarness);
    fixture.detectChanges();
  });

  it('should expose documentation and OpenAPI validation toggles when oas-validation policy is installed', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('local');
    await harness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    await harness.clickNext();

    expect(await harness.isDocumentationImportSelected()).toBe(true);
    expect(await harness.isDocumentationImportDisabled()).toBe(false);
    expect(await harness.isOasValidationPolicyImportPresent()).toBe(true);
    expect(await harness.isOasValidationPolicyImportSelected()).toBe(true);
    expect(await harness.isOasValidationPolicyImportDisabled()).toBe(false);
  });

  it('should allow turning documentation and OpenAPI validation off and documentation back on', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('local');
    await harness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    await harness.clickNext();

    await harness.toggleDocumentationImport();
    expect(await harness.isDocumentationImportSelected()).toBe(false);

    await harness.toggleOasValidationPolicyImport();
    expect(await harness.isOasValidationPolicyImportSelected()).toBe(false);

    await harness.toggleDocumentationImport();
    expect(await harness.isDocumentationImportSelected()).toBe(true);
  });

  it('should keep picked file after toggling OpenAPI validation on options and returning to configure', async () => {
    await harness.selectFormat('openapi');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('local');
    await harness.pickFiles([new File(['openapi: 3.1.0'], 'openapi.yml', { type: 'application/x-yaml' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(await harness.getPickedFilesCount()).toBe(1);
    await harness.clickNext();

    await harness.toggleOasValidationPolicyImport();
    expect(await harness.isOasValidationPolicyImportSelected()).toBe(false);

    await harness.clickPrevious();
    fixture.detectChanges();

    expect(await harness.isConfigureSourceNextDisabled()).toBe(false);
    expect(await harness.getPickedFilesCount()).toBe(1);
  });

  it('should still skip the options step for Gravitee definition when policy is installed', async () => {
    const importDefinition = JSON.stringify({ api: fakeApiV4({ definitionVersion: 'V4' }) });

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.pickFiles([new File([importDefinition], 'gravitee-api-definition.json', { type: 'application/json' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.hasOptionsStep()).toBe(false);
  });
});
