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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { ApiProductCreateComponent } from './api-product-create.component';
import { ApiProductCreateModule } from './api-product-create.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductCreateComponent', () => {
  let fixture: ComponentFixture<ApiProductCreateComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  let matDialog: MatDialog;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductCreateModule, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
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
    matDialog = TestBed.inject(MatDialog);
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

    expect(fixture.componentInstance.form.get('name')?.hasError('required')).toBe(true);
    expect(fixture.componentInstance.form.get('version')?.hasError('required')).toBe(true);
  });

  it('should validate name uniqueness', fakeAsync(async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Existing Product');
    await nameInput.blur();
    tick(300);

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    expect(verifyReq.request.body).toEqual({ name: 'Existing Product' });
    verifyReq.flush({ ok: false });
    tick();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.get('name')?.hasError('unique')).toBe(true);
  }));

  it('should create API product successfully', fakeAsync(async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));

    await nameInput.setValue('New Product');
    await nameInput.blur();
    tick(300);

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    tick();

    await versionInput.setValue('1.0');
    await descriptionInput.setValue('Test description');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    expect(createReq.request.body).toEqual({
      name: 'New Product',
      version: '1.0',
      description: 'Test description',
      apiIds: [],
    });

    const createdProduct: ApiProduct = {
      id: 'product-1',
      name: 'New Product',
      version: '1.0',
      description: 'Test description',
      apiIds: [],
    };
    createReq.flush(createdProduct);
    tick();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('API Product product-1 successfully created');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['..', 'product-1', 'configuration'], expect.anything());
  }));

  it('should trim whitespace from form values', fakeAsync(async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));

    await nameInput.setValue('  Trimmed Product  ');
    await nameInput.blur();
    tick(300);

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    tick();

    await versionInput.setValue('  1.0  ');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    expect(createReq.request.body.name).toBe('Trimmed Product');
    expect(createReq.request.body.version).toBe('1.0');
  }));

  it('should handle creation error', fakeAsync(async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));

    await nameInput.setValue('New Product');
    await nameInput.blur();
    tick(300);

    const verifyReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_verify`);
    verifyReq.flush({ ok: true });
    tick();

    await versionInput.setValue('1.0');
    fixture.detectChanges();

    const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create/i }));
    await createButton.click();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products`);
    createReq.flush({ message: 'Creation failed' }, { status: 400, statusText: 'Bad Request' });
    tick();

    expect(fakeSnackBarService.error).toHaveBeenCalledWith('Creation failed');
  }));

  it('should show exit confirmation dialog when form is dirty', fakeAsync(async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('Test Product');
    fixture.detectChanges();

    const dialogOpenSpy = jest.spyOn(matDialog, 'open').mockReturnValue({
      afterClosed: () => ({ pipe: () => ({ subscribe: jest.fn() }) }),
    } as any);

    fixture.componentInstance.onExit();
    fixture.detectChanges();

    expect(dialogOpenSpy).toHaveBeenCalled();
  }));

  it('should navigate back without dialog when form is not dirty', () => {
    fixture.detectChanges();
    fixture.componentInstance.onExit();

    expect(routerNavigateSpy).toHaveBeenCalledWith(['..'], expect.anything());
  });

  it('should validate max length for name', async () => {
    fixture.detectChanges();

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    await nameInput.setValue('a'.repeat(256));
    fixture.detectChanges();

    expect(fixture.componentInstance.form.get('name')?.hasError('maxlength')).toBe(true);
  });

  it('should validate max length for version', async () => {
    fixture.detectChanges();

    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    await versionInput.setValue('a'.repeat(256));
    fixture.detectChanges();

    expect(fixture.componentInstance.form.get('version')?.hasError('maxlength')).toBe(true);
  });

  it('should validate max length for description', async () => {
    fixture.detectChanges();

    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
    await descriptionInput.setValue('a'.repeat(251));
    fixture.detectChanges();

    expect(fixture.componentInstance.form.get('description')?.hasError('maxlength')).toBe(true);
  });
});
