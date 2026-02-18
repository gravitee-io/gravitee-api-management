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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmAndValidateDialogComponent } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { ApiProductDangerZoneComponent } from './api-product-danger-zone.component';
import { ApiProductConfigurationModule } from '../api-product-configuration.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('ApiProductDangerZoneComponent', () => {
  let fixture: ComponentFixture<ApiProductDangerZoneComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  let matDialog: MatDialog;
  const API_PRODUCT_ID = 'api-product-id';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const apiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test Product',
    version: '1.0',
    apiIds: ['api-1', 'api-2'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductConfigurationModule, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
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

    fixture = TestBed.createComponent(ApiProductDangerZoneComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    matDialog = TestBed.inject(MatDialog);
    fixture.componentRef.setInput('apiProduct', apiProduct);
    jest.spyOn(fixture.componentInstance.reloadDetails, 'emit');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should remove all APIs from API product', async () => {
    const dialogRef = { afterClosed: () => of(true) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis`,
    );
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('All APIs have been removed from the API Product.');
    expect(fixture.componentInstance.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should not remove APIs if dialog is cancelled', async () => {
    const dialogRef = { afterClosed: () => of(false) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();
    await fixture.whenStable();

    httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis`);
  });

  it('should handle error when removing APIs', async () => {
    const dialogRef = { afterClosed: () => of(true) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis`,
    );
    req.flush({ message: 'Error removing APIs' }, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should delete API product', async () => {
    const dialogRef = { afterClosed: () => of(true) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /Delete/i }));
    await deleteButton.click();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('The API Product has been deleted.');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], expect.anything());
  });

  it('should not delete API product if dialog is cancelled', async () => {
    const dialogRef = { afterClosed: () => of(false) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /Delete/i }));
    await deleteButton.click();
    await fixture.whenStable();

    httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
  });

  it('should handle error when deleting API product', async () => {
    const dialogRef = { afterClosed: () => of(true) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /Delete/i }));
    await deleteButton.click();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Error deleting product' }, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });
});
