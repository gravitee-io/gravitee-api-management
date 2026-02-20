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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiProductAddApiDialogComponent, ApiProductAddApiDialogData } from './api-product-add-api-dialog.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeProxyApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('ApiProductAddApiDialogComponent', () => {
  let fixture: ComponentFixture<ApiProductAddApiDialogComponent>;
  let _loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<ApiProductAddApiDialogComponent>;

  const API_PRODUCT_ID = 'product-1';
  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const dialogData: ApiProductAddApiDialogData = {
    apiProductId: API_PRODUCT_ID,
    existingApiIds: ['api-1'],
  };

  const fakeApi = fakeProxyApiV4({
    id: 'api-2',
    name: 'New API',
    apiVersion: '1.0',
    listeners: [
      {
        type: 'HTTP',
        paths: [{ path: '/new-api' }],
        entrypoints: [{ type: 'http-proxy' }],
      },
    ] as any,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductAddApiDialogComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: SnackBarService, useValue: fakeSnackBarService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductAddApiDialogComponent);
    _loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    dialogRef = TestBed.inject(MatDialogRef);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should close dialog on cancel', () => {
    fixture.detectChanges();
    const cancelButton = fixture.nativeElement.querySelector('[data-testid="cancel-button"]');
    cancelButton?.dispatchEvent(new Event('click'));
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('should disable submit when no APIs selected', () => {
    fixture.detectChanges();
    const submitButton = fixture.nativeElement.querySelector('[data-testid="submit-button"]');
    expect(submitButton?.hasAttribute('disabled')).toBe(true);
  });

  it('should call close with true on successful submit', async () => {
    fixture.componentInstance.selectedApis = [fakeApi];
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('[data-testid="submit-button"]');
    (submitButton as HTMLButtonElement)?.click();
    fixture.detectChanges();

    const productReq = httpTestingController.expectOne(req => req.url.includes('/api-products/') && req.method === 'GET');
    productReq.flush({ id: API_PRODUCT_ID, apiIds: ['api-1'] });

    const updateReq = httpTestingController.expectOne(req => req.url.includes('/api-products/') && req.method === 'PUT');
    updateReq.flush({});

    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show error when API already in product', async () => {
    fixture.componentInstance.selectedApis = [fakeProxyApiV4({ id: 'api-1', name: 'Existing API' }) as any];
    fixture.detectChanges();
    await fixture.whenStable();

    const submitButton = fixture.nativeElement.querySelector('[data-testid="submit-button"]');
    (submitButton as HTMLButtonElement)?.click();
    fixture.detectChanges();

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush({ id: API_PRODUCT_ID, apiIds: ['api-1'] });

    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalledWith(expect.stringContaining('already in this API Product'));
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
