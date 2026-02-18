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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';

import { ApiProductApisComponent } from './api-product-apis.component';
import { ApiProductApisModule } from './api-product-apis.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Api, fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductApisComponent', () => {
  let fixture: ComponentFixture<ApiProductApisComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let router: Router;
  let matDialog: MatDialog;
  const API_PRODUCT_ID = 'api-product-id';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductApisModule, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiProductId: API_PRODUCT_ID }, queryParams: {} },
            parent: {
              snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductApisComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
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

  it('should display empty state when no APIs', async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    await initComponent(apiProduct, []);

    expect(fixture.nativeElement.textContent).toContain('There are no APIs in this API Product (yet).');
  });

  it('should display APIs in table', async () => {
    const api1 = fakeApiV2({ id: 'api-1', name: 'API 1', apiVersion: '1.0', contextPath: '/api1' });
    const api2 = fakeApiV2({ id: 'api-2', name: 'API 2', apiVersion: '2.0', contextPath: '/api2' });

    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: ['api-1', 'api-2'],
    };

    await initComponent(apiProduct, [api1, api2]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductApisTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);

    const row1Cells = await rows[0].getCellTextByIndex();
    expect(row1Cells[1]).toContain('API 1');
    expect(row1Cells[2]).toContain('/api1');
    expect(row1Cells[3]).toContain('HTTP Proxy Gravitee');
    expect(row1Cells[4]).toContain('1.0');
  });

  it('should handle error when loading API product', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should handle error when loading individual APIs', async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: ['api-1', 'api-2'],
    };

    fixture.detectChanges();
    await fixture.whenStable();

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush(apiProduct);
    await fixture.whenStable();

    const api1Req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1`);
    api1Req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    const api2Req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-2`);
    api2Req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    await fixture.whenStable();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductApisTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(0);
  });

  it('should do nothing when Add API button is clicked (placeholder for future work)', async () => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    await initComponent(apiProduct, []);

    const dialogOpenSpy = jest.spyOn(matDialog, 'open');

    const addButton = await loader.getHarness(MatButtonHarness.with({ text: /Add API/i }));
    await addButton.click();
    await fixture.whenStable();

    // No dialog should open; Add API is a placeholder for future work
    expect(dialogOpenSpy).not.toHaveBeenCalled();
  });

  it('should delete API from product', async () => {
    const api = fakeApiV2({ id: 'api-1', name: 'API 1' });
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: ['api-1'],
    };

    await initComponent(apiProduct, [api]);

    const dialogRef = { afterClosed: () => of(true) } as MatDialogRef<unknown, boolean>;
    jest.spyOn(matDialog, 'open').mockReturnValue(dialogRef);

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /delete/i }));
    await deleteButton.click();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis/api-1`,
    );
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('API "API 1" has been removed from the API Product');
  });

  it('should display V4 API correctly', async () => {
    const api = fakeApiV4({ id: 'api-1', name: 'V4 API', apiVersion: '1.0' });
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: ['api-1'],
    };

    await initComponent(apiProduct, [api]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductApisTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);

    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[1]).toContain('V4 API');
  });

  async function initComponent(apiProduct: ApiProduct, apis: Api[]) {
    fixture.detectChanges();
    await fixture.whenStable();

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush(apiProduct);
    await fixture.whenStable();

    if (apiProduct.apiIds && apiProduct.apiIds.length > 0) {
      apiProduct.apiIds.forEach((apiId) => {
        const api = apis.find((a) => a.id === apiId);
        if (api) {
          const apiReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`);
          apiReq.flush(api);
        }
      });
    }

    await fixture.whenStable();
    fixture.detectChanges();
  }
});
