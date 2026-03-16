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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiProductApisComponent } from './api-product-apis.component';

import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Api, fakeProxyApiV4 } from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Constants } from '../../../entities/Constants';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

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
    ],
  });

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
    ],
  });

  beforeEach(() => {
    queryParams$ = new BehaviorSubject<Record<string, string>>({});

    TestBed.configureTestingModule({
      imports: [ApiProductApisComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: GioTestingPermissionProvider, useValue: ['environment-api_product-u'] },
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
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductApisComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    _router = TestBed.inject(Router);

    // Simulate router.navigate updating query params (Router is source of truth)
    jest.spyOn(_router, 'navigate').mockImplementation((_commands, extras) => {
      if (extras?.['queryParams']) {
        const merged = { ...queryParams$.value, ...extras['queryParams'] };
        const filtered: Record<string, string> = {};
        for (const [k, v] of Object.entries(merged)) {
          if (v != null) filtered[k] = String(v);
        }
        queryParams$.next(filtered);
      }
      return Promise.resolve(true);
    });
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
    await initComponent([]);

    const emptyState = await loader.getHarness(DivHarness.with({ selector: '.api-product-apis-empty-state' }));
    expect(await emptyState.getText()).toContain('There are no APIs in this API Product (yet).');
  });

  it('should display table with API rows when product has APIs', async () => {
    await initComponent([fakeApi1, fakeApi2]);

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

    const emptyState = await loader.getHarness(DivHarness.with({ selector: '.api-product-apis-empty-state' }));
    expect(await emptyState.getText()).toContain('Loading...');

    const apisReq = expectApisRequest();
    apisReq.flush({ data: [fakeApi1, fakeApi2], pagination: { totalCount: 2 } });

    fixture.detectChanges();
    await fixture.whenStable();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it('should handle error when loading API product', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = expectApisRequest();
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should show error and navigate to list when API Product not found (404)', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = expectApisRequest();
    req.flush({ message: 'API Product not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalledWith('API Product not found');
    expect(_router.navigate).toHaveBeenCalledWith(['../..'], { relativeTo: expect.anything() });
  });

  it('should have Add API button', async () => {
    await initComponent([fakeApi1]);

    const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-api-button"]' }));
    expect(await addButton.getText()).toContain('Add API');
  });

  it('should open Add API dialog when Add API clicked', async () => {
    const matDialog = TestBed.inject(MatDialog);
    const dialogOpenSpy = jest.spyOn(matDialog, 'open');
    await initComponent([fakeApi1]);

    const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-api-button"]' }));
    await addButton.click();
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
    await initComponent([fakeApi1, fakeApi2]);

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('Orders');
    await fixture.whenStable();

    const filterReq = expectApisRequest('Orders');
    filterReq.flush({ data: [fakeApi2], pagination: { totalCount: 1 } });

    fixture.detectChanges();
    await fixture.whenStable();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);
    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[1]).toContain('Orders API');
  });

  function expectApisRequest(query?: string) {
    return httpTestingController.expectOne(req => {
      if (req.url !== `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis`) return false;
      if (req.params.get('page') !== '1' || req.params.get('perPage') !== '10') return false;
      if (query !== undefined && req.params.get('query') !== query) return false;
      if (query === undefined && req.params.has('query')) return false;
      return true;
    });
  }

  async function initComponent(apis: Api[] = []) {
    fixture.detectChanges();
    await fixture.whenStable();

    const apisReq = expectApisRequest();
    apisReq.flush({ data: apis, pagination: { totalCount: apis.length } });

    fixture.detectChanges();
    await fixture.whenStable();
  }
});
