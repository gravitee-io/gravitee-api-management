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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiProductApisComponent } from './api-product-apis.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Api, fakeProxyApiV4 } from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Constants } from '../../../entities/Constants';

describe('ApiProductApisComponent', () => {
  let fixture: ComponentFixture<ApiProductApisComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let _router: Router;
  let queryParams$: BehaviorSubject<Record<string, string>>;

  const API_PRODUCT_ID = 'product-1';
  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const fakeApiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    apiIds: ['api-1', 'api-2'],
  };

  const fakeApi1 = fakeProxyApiV4({
    id: 'api-1',
    name: 'Payments API',
    apiVersion: '1.0',
    listeners: [
      {
        type: 'HTTP',
        paths: [{ path: '/payments' }],
        entrypoints: [{ type: 'http-proxy' }],
      },
    ] as any,
  }) as Api;

  const fakeApi2 = fakeProxyApiV4({
    id: 'api-2',
    name: 'Orders API',
    apiVersion: '2.0',
    listeners: [
      {
        type: 'HTTP',
        paths: [{ path: '/orders' }],
        entrypoints: [{ type: 'http-proxy' }],
      },
    ] as any,
  }) as Api;

  beforeEach(() => {
    queryParams$ = new BehaviorSubject<Record<string, string>>({});

    TestBed.configureTestingModule({
      imports: [ApiProductApisComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: {}, queryParams: {} },
            params: new BehaviorSubject({}),
            queryParams: queryParams$.asObservable(),
            parent: {
              snapshot: { params: { apiProductId: API_PRODUCT_ID }, queryParams: {} },
              params: new BehaviorSubject({ apiProductId: API_PRODUCT_ID }),
              queryParams: queryParams$.asObservable(),
            } as any,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductApisComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    _router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display empty state when no APIs in product', async () => {
    await initComponent({ ...fakeApiProduct, apiIds: [] });

    const emptyState = fixture.nativeElement.querySelector('.api-product-apis-empty-state');
    expect(emptyState).toBeTruthy();
    expect(emptyState?.textContent).toContain('There are no APIs in this API Product (yet).');
  });

  it('should display table with API rows when product has APIs', async () => {
    await initComponent(fakeApiProduct, [fakeApi1, fakeApi2]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);

    const rowCells0 = await rows[0].getCellTextByIndex();
    expect(rowCells0[1]).toContain('Payments API');
    expect(rowCells0[2]).toContain('/payments');

    const rowCells1 = await rows[1].getCellTextByIndex();
    expect(rowCells1[1]).toContain('Orders API');
    expect(rowCells1[2]).toContain('/orders');
  });

  it('should display Loading... then data', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const emptyState = fixture.nativeElement.querySelector('.api-product-apis-empty-state');
    expect(emptyState?.textContent).toContain('Loading...');

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush(fakeApiProduct);

    await fixture.whenStable();

    const api1Req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1`);
    api1Req.flush(fakeApi1);
    const api2Req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-2`);
    api2Req.flush(fakeApi2);

    await new Promise(r => setTimeout(r, 50));
    fixture.detectChanges();
    await fixture.whenStable();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it('should handle error when loading API product', async () => {
    fixture.detectChanges();
    await new Promise(r => setTimeout(r, 150));

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should have Add API button', async () => {
    await initComponent({ ...fakeApiProduct, apiIds: ['api-1'] }, [fakeApi1]);

    const addButton = fixture.nativeElement.querySelector('[data-testid="add-api-button"]');
    expect(addButton).toBeTruthy();
    expect(addButton?.textContent?.trim()).toContain('Add API');
  });

  it('should open Add API dialog when Add API clicked', async () => {
    const matDialog = TestBed.inject(MatDialog);
    const dialogOpenSpy = jest.spyOn(matDialog, 'open');
    await initComponent({ ...fakeApiProduct, apiIds: ['api-1'] }, [fakeApi1]);

    const addButton = fixture.nativeElement.querySelector('[data-testid="add-api-button"]');
    addButton?.dispatchEvent(new Event('click'));
    fixture.detectChanges();

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush({ ...fakeApiProduct, apiIds: ['api-1'] });
    await fixture.whenStable();

    expect(dialogOpenSpy).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({
          apiProductId: API_PRODUCT_ID,
          existingApiIds: ['api-1'],
        }),
      }),
    );
  });

  it('should filter APIs by search term', async () => {
    await initComponent(fakeApiProduct, [fakeApi1, fakeApi2]);

    fixture.componentInstance.onFiltersChanged({
      searchTerm: 'Orders',
      pagination: { index: 1, size: 10 },
    });
    await new Promise(r => setTimeout(r, 150));

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush(fakeApiProduct);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1`).flush(fakeApi1);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-2`).flush(fakeApi2);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.apisTableDS.length).toBe(1);
    expect(fixture.componentInstance.apisTableDS[0].name).toBe('Orders API');
  });

  async function initComponent(apiProduct: ApiProduct, apis: Api[] = []) {
    fixture.detectChanges();
    await new Promise(r => setTimeout(r, 150));

    const productReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    productReq.flush(apiProduct);

    for (const api of apis) {
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`);
      req.flush(api);
    }

    fixture.detectChanges();
    await fixture.whenStable();
  }
});
