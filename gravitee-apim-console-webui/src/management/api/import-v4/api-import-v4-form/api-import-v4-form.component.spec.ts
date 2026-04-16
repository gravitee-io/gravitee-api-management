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

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render form root', async () => {
    expect(await harness.hasRoot()).toBe(true);
  });

  it.each([
    ['gravitee', ['json']],
    ['openapi', ['yml', 'yaml']],
  ])('should expose allowedImportFileExtensions for format %s', async (format, expected) => {
    await harness.selectFormat(format as string);
    fixture.detectChanges();
    const cmp = fixture.componentInstance as unknown as { allowedImportFileExtensions: () => string[] };
    expect(cmp.allowedImportFileExtensions()).toEqual(expected);
  });

  it('should expose wsdl/xml extensions when format is wsdl', () => {
    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'wsdl' });
    fixture.detectChanges();
    const cmp = fixture.componentInstance as unknown as { allowedImportFileExtensions: () => string[] };
    expect(cmp.allowedImportFileExtensions()).toEqual(['wsdl', 'xml']);
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
    const showOptions = (fixture.componentInstance as unknown as { showImportOptionsStep: () => boolean }).showImportOptionsStep;
    expect(showOptions()).toBe(true);
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

    const cmp = fixture.componentInstance as unknown as { importApi(): void; importInProgress: () => boolean };
    cmp.importApi();
    expect(cmp.importInProgress()).toBe(true);
    cmp.importApi();
    expect(apiV2.importSwaggerApi).toHaveBeenCalledTimes(1);

    result$.next({ id: 'new-api' } as ApiV4);
    result$.complete();
    fixture.detectChanges();
    expect(cmp.importInProgress()).toBe(false);

    cmp.importApi();
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

  it('should GET remote JSON then import for Gravitee remote', async () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    const httpMock = TestBed.inject(HttpTestingController);
    jest.spyOn(apiV2, 'import').mockReturnValue(of({ id: 'imported' } as ApiV4));

    await harness.selectFormat('gravitee');
    fixture.detectChanges();
    await harness.clickNext();
    await harness.selectSource('remote');
    await harness.setRemoteUrl('https://cdn.example/def.json');
    fixture.detectChanges();
    await harness.clickNext();

    expect(await harness.isImportButtonDisabled()).toBe(false);

    (fixture.componentInstance as unknown as { importApi(): void }).importApi();
    const remoteReq = httpMock.expectOne(r => r.url === 'https://cdn.example/def.json');
    expect(remoteReq.request.method).toBe('GET');
    remoteReq.flush(JSON.stringify({ api: { definitionVersion: 'V4' } }));

    expect(apiV2.import).toHaveBeenCalledWith(JSON.stringify({ api: { definitionVersion: 'V4' } }));
    httpMock.verify();
  });

  it('should show actionable message when remote fetch fails with status 0', async () => {
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

    (fixture.componentInstance as unknown as { importApi(): void }).importApi();
    const remoteReq = httpMock.expectOne(r => r.url === 'https://cdn.example/def.json');
    remoteReq.error(new ProgressEvent('error'));

    expect(snackBarErrorSpy).toHaveBeenCalledWith(
      'Could not fetch the remote URL. Check that the URL is reachable and allows CORS requests from this Console.',
    );
    httpMock.verify();
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
