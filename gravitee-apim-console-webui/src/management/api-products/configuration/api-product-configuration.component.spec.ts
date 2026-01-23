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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiProductConfigurationComponent } from './api-product-configuration.component';
import { ApiProductConfigurationModule } from './api-product-configuration.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductConfigurationComponent', () => {
  let fixture: ComponentFixture<ApiProductConfigurationComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const API_PRODUCT_ID = 'api-product-id';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductConfigurationModule, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            parent: {
              snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductConfigurationComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      description: '',
      apiIds: [],
    });
    tick();

    expect(fixture.componentInstance).toBeTruthy();
  }));

  it('should load API product and initialize form', fakeAsync(async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      description: 'Test description',
      apiIds: ['api-1'],
    };

    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(apiProduct);
    tick();
    fixture.detectChanges();

    expect(fixture.componentInstance.apiProduct).toEqual(apiProduct);
    expect(fixture.componentInstance.form.get('name')?.value).toBe('Test Product');
    expect(fixture.componentInstance.form.get('version')?.value).toBe('1.0');
    expect(fixture.componentInstance.form.get('description')?.value).toBe('Test description');
  }));

  it('should update API product successfully', fakeAsync(async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      description: 'Test description',
      apiIds: ['api-1'],
    };

    fixture.detectChanges();
    tick();

    const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    getReq.flush(apiProduct);
    tick();
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Updated Product');
    await nameInput.blur();
    tick(300);

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    tick();
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const updateReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(updateReq.request.body).toEqual({
      name: 'Updated Product',
      version: '1.0',
      description: 'Test description',
      apiIds: ['api-1'],
    });

    const updatedProduct: ApiProduct = {
      ...apiProduct,
      name: 'Updated Product',
    };
    updateReq.flush(updatedProduct);
    tick();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('Configuration successfully saved!');
  }));

  it('should handle error when loading API product', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    tick();
    fixture.detectChanges();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
    expect(fixture.componentInstance.form).toBeTruthy();
  }));

  it('should handle error when updating API product', fakeAsync(async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    fixture.detectChanges();
    tick();

    const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    getReq.flush(apiProduct);
    tick();
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const updateReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    updateReq.flush({ message: 'Update failed' }, { status: 400, statusText: 'Bad Request' });
    tick();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  }));

  it('should reset form to initial values', fakeAsync(async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      description: 'Test description',
      apiIds: [],
    };

    fixture.detectChanges();
    tick();

    const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    getReq.flush(apiProduct);
    tick();
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Modified Product');
    fixture.detectChanges();

    fixture.componentInstance.onReset();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.get('name')?.value).toBe('Test Product');
    expect(fixture.componentInstance.form.pristine).toBe(true);
  }));

  it('should validate name uniqueness excluding current product', fakeAsync(async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    fixture.detectChanges();
    tick();

    const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    getReq.flush(apiProduct);
    tick();
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Test Product');
    await nameInput.blur();
    tick(300);

    // Should not call verify if name hasn't changed
    httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
  }));
});
