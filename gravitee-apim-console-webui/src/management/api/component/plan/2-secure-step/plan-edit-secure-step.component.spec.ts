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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { Subject, throwError } from 'rxjs';

import { PlanEditSecureStepComponent } from './plan-edit-secure-step.component';

import { ApiPlanFormModule } from '../api-plan-form.module';
import { DEFAULT_API_KEY_HEADER } from '../sanitize-api-key-security-configuration';
import { GioTestingModule } from '../../../../../shared/testing';
import { fakeApiV4 } from '../../../../../entities/management-api-v2';
import { PolicyV2Service } from '../../../../../services-ngx/policy-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { PlanMenuItemVM } from '../../../../../services-ngx/constants.service';

const API_KEY_SECURITY_TYPE: PlanMenuItemVM = {
  planFormType: 'API_KEY',
  name: 'API Key',
  policy: 'api-key',
};

const apiKeySchemaWithDefault = {
  type: 'object',
  properties: {
    source: { type: 'string', default: 'HEADER' },
    apiKeyHeader: {
      type: 'string',
      default: DEFAULT_API_KEY_HEADER,
    },
  },
};

describe('PlanEditSecureStepComponent — API Key header schema', () => {
  let fixture: ComponentFixture<PlanEditSecureStepComponent>;
  let component: PlanEditSecureStepComponent;
  let schema$: Subject<unknown>;
  let snackBarService: SnackBarService;

  beforeEach(async () => {
    schema$ = new Subject();

    await TestBed.configureTestingModule({
      declarations: [PlanEditSecureStepComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiPlanFormModule, MatIconTestingModule],
    })
      .overrideProvider(PolicyV2Service, {
        useValue: {
          getSchema: jest.fn().mockReturnValue(schema$.asObservable()),
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PlanEditSecureStepComponent);
    component = fixture.componentInstance;
    snackBarService = TestBed.inject(SnackBarService);
    component.api = fakeApiV4();
    component.securityType = API_KEY_SECURITY_TYPE;
  });

  it('strips apiKeyHeader default from schema on edit when stored plan has no header', () => {
    component.mode = 'edit';
    fixture.detectChanges();

    schema$.next(apiKeySchemaWithDefault);
    fixture.detectChanges();

    expect(component.securityConfigSchema).toEqual({
      type: 'object',
      properties: {
        source: { type: 'string', default: 'HEADER' },
        apiKeyHeader: { type: 'string' },
      },
    });
  });

  it('keeps apiKeyHeader default in schema on create', () => {
    component.mode = 'create';
    fixture.detectChanges();

    schema$.next(apiKeySchemaWithDefault);
    fixture.detectChanges();

    expect(component.securityConfigSchema).toBe(apiKeySchemaWithDefault);
  });

  it('keeps apiKeyHeader default in schema on edit when stored plan already has a header', () => {
    component.mode = 'edit';
    fixture.detectChanges();

    component.secureForm.get('securityConfig')?.setValue({
      source: 'HEADER',
      apiKeyHeader: DEFAULT_API_KEY_HEADER,
    });

    schema$.next(apiKeySchemaWithDefault);
    fixture.detectChanges();

    expect(component.securityConfigSchema).toBe(apiKeySchemaWithDefault);
  });

  it('does not load a policy schema for keyless plans', () => {
    const policyService = TestBed.inject(PolicyV2Service);
    component.securityType = { planFormType: 'KEY_LESS', name: 'Keyless (public)' };
    fixture.detectChanges();

    expect(policyService.getSchema).not.toHaveBeenCalled();
    expect(component.securityConfigSchema).toBeUndefined();
  });

  it('surfaces schema load errors to the user', () => {
    jest.spyOn(snackBarService, 'error');
    const policyService = TestBed.inject(PolicyV2Service);
    (policyService.getSchema as jest.Mock).mockReturnValue(throwError(() => ({ error: { message: 'Schema unavailable' } })));

    component.mode = 'edit';
    fixture.detectChanges();

    expect(snackBarService.error).toHaveBeenCalledWith('Schema unavailable');
    expect(component.securityConfigSchema).toBeUndefined();
  });
});
