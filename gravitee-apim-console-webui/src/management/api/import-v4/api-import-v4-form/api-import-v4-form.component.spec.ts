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
import { of } from 'rxjs';

import { ApiImportV4FormComponent } from './api-import-v4-form.component';
import { ApiImportV4FormHarness } from './api-import-v4-form.harness';

import { ApiV4 } from '../../../../entities/management-api-v2';
import { GioTestingModule } from '../../../../shared/testing';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { PolicyV2Service } from '../../../../services-ngx/policy-v2.service';

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

  it('should hide the options step for Gravitee definition', async () => {
    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'gravitee' });
    fixture.detectChanges();
    expect(await harness.hasOptionsStep()).toBe(false);
  });

  it('should show the options step for OpenAPI specification', async () => {
    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'openapi' });
    fixture.detectChanges();
    expect(await harness.hasOptionsStep()).toBe(true);
  });


  it('should call importSwaggerApi with remote URL for OpenAPI', () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    spyOn(apiV2, 'importSwaggerApi').and.returnValue(of({ id: 'new-api' } as ApiV4));

    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'openapi' });
    fixture.componentInstance.configureFileSourceForm.patchValue({
      source: 'remote',
      remoteUrl: 'https://example.com/openapi.yaml',
    });
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { importApi(): void }).importApi();

    expect(apiV2.importSwaggerApi).toHaveBeenCalledWith(
      jasmine.objectContaining({
        payload: 'https://example.com/openapi.yaml',
        withDocumentation: true,
        withOASValidationPolicy: false,
      }),
    );
  });

  it('should call updateApiFromSwagger when updateTargetApiId is set (remote OpenAPI)', () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    spyOn(apiV2, 'updateApiFromSwagger').and.returnValue(of({ id: 'same-api' } as ApiV4));
    fixture.componentRef.setInput('updateTargetApiId', 'same-api');

    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'openapi' });
    fixture.componentInstance.configureFileSourceForm.patchValue({
      source: 'remote',
      remoteUrl: 'https://example.com/openapi.yaml',
    });
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { importApi(): void }).importApi();

    expect(apiV2.updateApiFromSwagger).toHaveBeenCalledWith(
      'same-api',
      jasmine.objectContaining({
        payload: 'https://example.com/openapi.yaml',
        withDocumentation: true,
        withOASValidationPolicy: false,
      }),
    );
    expect(apiV2.importSwaggerApi).not.toHaveBeenCalled();
  });

  it('should GET remote JSON then import for Gravitee remote', () => {
    const apiV2 = TestBed.inject(ApiV2Service);
    const httpMock = TestBed.inject(HttpTestingController);
    spyOn(apiV2, 'import').and.returnValue(of({ id: 'imported' } as ApiV4));

    fixture.componentInstance.selectApiFormatForm.patchValue({ format: 'gravitee' });
    fixture.componentInstance.configureFileSourceForm.patchValue({
      source: 'remote',
      remoteUrl: 'https://cdn.example/def.json',
    });
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { importApi(): void }).importApi();

    const remoteReq = httpMock.expectOne(r => r.url === 'https://cdn.example/def.json');
    expect(remoteReq.request.method).toBe('GET');
    remoteReq.flush(JSON.stringify({ api: { definitionVersion: 'V4' } }));

    expect(apiV2.import).toHaveBeenCalledWith(JSON.stringify({ api: { definitionVersion: 'V4' } }));
    httpMock.verify();
  });
});
