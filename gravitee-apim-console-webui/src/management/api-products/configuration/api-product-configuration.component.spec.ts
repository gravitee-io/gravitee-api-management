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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { GioConfirmAndValidateDialogHarness, GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiProductConfigurationComponent } from './api-product-configuration.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

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
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductConfigurationComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        { provide: GioTestingPermissionProvider, useValue: ['environment-api_product-u'] },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiProductId: API_PRODUCT_ID }),
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductConfigurationComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    routerNavigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load and display API product configuration', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(req.request.method).toBe('GET');
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const form = fixture.componentInstance.form;
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

    const form = fixture.componentInstance.form;
    expect(form?.controls.name.hasError('required')).toBe(true);
    expect(form?.controls.version.hasError('required')).toBe(true);
  });

  it('should update API product on submit', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const form = fixture.componentInstance.form;
    expect(form).toBeTruthy();
    // Keep same name to avoid async validator's verify call (deterministic, CI-friendly)
    form!.patchValue({ name: 'Test API Product', version: '1.0', description: 'Updated desc' });
    form!.markAsDirty();
    await fixture.whenStable();

    fixture.componentInstance.onSubmit();
    await fixture.whenStable();

    const updateReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
    });
    expect(updateReq.request.body.name).toBe('Test API Product');
    expect(updateReq.request.body.description).toBe('Updated desc');
    updateReq.flush({ ...fakeApiProduct, description: 'Updated desc' });
    await fixture.whenStable();

    // Refetch triggered after save
    const refetchReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    refetchReq.flush({ ...fakeApiProduct, description: 'Updated desc' });
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('Configuration successfully saved!');
  });

  it('should show snackbar on load error', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should remove all APIs when confirmed', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();

    const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    await dialog.confirm();

    const putReq = httpTestingController.expectOne(req => req.url.includes(`/api-products/${API_PRODUCT_ID}`) && req.method === 'PUT');
    expect(putReq.request.body).toEqual({ apiIds: [] });
    putReq.flush({ ...fakeApiProduct, apiIds: [] });
    await fixture.whenStable();

    const reloadReq = httpTestingController.expectOne(req => req.url.includes(`/api-products/${API_PRODUCT_ID}`) && req.method === 'GET');
    reloadReq.flush({ ...fakeApiProduct, apiIds: [] });
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('All APIs have been removed from the API Product.');
  });

  it('should delete API product when confirmed', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_product_dangerzone_delete"]' }));
    await deleteButton.click();

    const dialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await dialog.confirm();

    const deleteReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({});
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('The API Product has been deleted.');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], expect.anything());
  });
});
