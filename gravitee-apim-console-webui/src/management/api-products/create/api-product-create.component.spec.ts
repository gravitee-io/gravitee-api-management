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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { ApiProductCreateComponent } from './api-product-create.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductCreateComponent', () => {
  let fixture: ComponentFixture<ApiProductCreateComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductCreateComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductCreateComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should validate required fields', async () => {
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.name.hasError('required')).toBe(true);
  });

  it('should validate name uniqueness', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Existing Product');
    await nameInput.blur();
    await fixture.whenStable();

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    expect(verifyReq.request.body).toEqual({ name: 'Existing Product' });
    verifyReq.flush({ ok: false });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.name.hasError('unique')).toBe(true);
  });

  it('should create API product successfully', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));

    await nameInput.setValue('New API Product');
    await nameInput.blur();
    await fixture.whenStable();

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    await fixture.whenStable();

    await versionInput.setValue('1.0');
    await descriptionInput.setValue('Test description');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    expect(createReq.request.body).toEqual({
      name: 'New API Product',
      version: '1.0',
      description: 'Test description',
      apiIds: [],
    });

    const createdProduct: ApiProduct = {
      id: 'product-1',
      name: 'New API Product',
      version: '1.0',
      description: 'Test description',
      apiIds: [],
    };
    createReq.flush(createdProduct);
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('New API Product - Successfully created');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['..'], expect.anything());
  });

  it('should trim whitespace from form values', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));

    await nameInput.setValue('  Trimmed Product  ');
    await nameInput.blur();
    await fixture.whenStable();

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    await fixture.whenStable();

    await versionInput.setValue('  1.0  ');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    expect(createReq.request.body.name).toBe('Trimmed Product');
    expect(createReq.request.body.version).toBe('1.0');
  });

  it('should handle creation error', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));

    await nameInput.setValue('New API Product');
    await nameInput.blur();
    await fixture.whenStable();

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    await fixture.whenStable();

    await versionInput.setValue('1.0');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    createReq.flush({ message: 'Creation failed' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalledWith('Creation failed');
  });

  it('should show exit confirmation dialog when form is dirty', () => {
    fixture.detectChanges();

    const dialogOpenSpy = jest.spyOn(MatDialog.prototype, 'open').mockReturnValue({
      afterClosed: () => ({ pipe: () => ({ subscribe: jest.fn() }) }),
    } as any);

    // Simulate dirty form (harness setValue doesn't reliably mark form dirty with updateOn configs)
    Object.defineProperty(fixture.componentInstance.form, 'dirty', {
      get: () => true,
      configurable: true,
    });

    fixture.componentInstance.onExit();
    fixture.detectChanges();

    expect(dialogOpenSpy).toHaveBeenCalled();
  });

  it('should navigate back without dialog when form is not dirty', () => {
    fixture.detectChanges();
    fixture.componentInstance.onExit();

    expect(routerNavigateSpy).toHaveBeenCalledWith(['..'], expect.anything());
  });

  it('should validate max length for name', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('a'.repeat(513));
    await nameInput.blur();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.name.hasError('maxlength')).toBe(true);
  });

  it('should validate max length for version', async () => {
    fixture.detectChanges();

    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    await versionInput.setValue('a'.repeat(65));
    await versionInput.blur();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.version.hasError('maxlength')).toBe(true);
  });
});
