/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiProductConfigurationComponent } from './api-product-configuration.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductConfigurationComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  const fakeApiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    description: 'Test description',
    apiIds: ['api-1'],
  };

  let fixture: ComponentFixture<ApiProductConfigurationComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductConfigurationComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiProductId: API_PRODUCT_ID }),
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            parent: null,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductConfigurationComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load and display API product configuration', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(req.request.method).toBe('GET');
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const form = fixture.componentInstance.form();
    expect(form).toBeTruthy();
    expect(form?.getRawValue().name).toBe('Test API Product');
    expect(form?.getRawValue().version).toBe('1.0');
  });

  it('should validate required fields', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    await nameInput.setValue('');
    await versionInput.setValue('');
    await nameInput.blur();
    await versionInput.blur();
    fixture.detectChanges();

    const form = fixture.componentInstance.form();
    expect(form?.controls.name.hasError('required')).toBe(true);
    expect(form?.controls.version.hasError('required')).toBe(true);
  });

  it('should update API product on submit', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const form = fixture.componentInstance.form();
    expect(form).toBeTruthy();
    form!.patchValue({ name: 'Updated Name', version: '1.0', description: 'Updated desc' });
    form!.markAsDirty();

    fixture.componentInstance.onSubmit();
    await fixture.whenStable();

    const updateReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
    });
    expect(updateReq.request.body.name).toBe('Updated Name');
    updateReq.flush({ ...fakeApiProduct, name: 'Updated Name' });
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('Configuration successfully saved!');
  });

  it('should show snackbar on load error', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });
});
