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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
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

function expectApisSearchRequest(
  httpTestingController: HttpTestingController,
  apis: Api[],
  options?: { apiProductId?: string; query?: string; page?: number; perPage?: number; totalCount?: number },
) {
  const { apiProductId = 'product-1', query, page = 1, perPage = 25, totalCount = apis.length } = options ?? {};
  const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`;
  const req = httpTestingController.expectOne((r: { method: string; urlWithParams?: string; url?: string }) => {
    const url = r.urlWithParams ?? r.url ?? '';
    return r.method === 'POST' && url.startsWith(baseUrl) && url.includes(`page=${page}`) && url.includes(`perPage=${perPage}`);
  });
  expect(req.request.body).toEqual({
    apiProductId,
    ...(query ? { query } : {}),
  });
  req.flush({
    data: apis,
    pagination: {
      page,
      perPage,
      pageCount: Math.ceil((totalCount || 1) / perPage),
      pageItemsCount: apis.length,
      totalCount,
    },
  });
  return req;
}

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

    expectApisSearchRequest(httpTestingController, [fakeApi1, fakeApi2]);

    fixture.detectChanges();
    await fixture.whenStable();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it('should handle error when loading APIs', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne((r: { url: string }) => r.url.includes('/apis/_search'));
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
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

  it('should filter APIs by search term', fakeAsync(async () => {
    initComponentFakeAsync([fakeApi1, fakeApi2]);

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('Orders');
    tick(450);

    expectApisSearchRequest(httpTestingController, [fakeApi2], { query: 'Orders' });

    fixture.detectChanges();
    tick();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);
    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[1]).toContain('Orders API');
  }));

  it('should show pagination and load next page', fakeAsync(async () => {
    const api3 = fakeProxyApiV4({
      id: 'api-3',
      name: 'API 3',
      listeners: [{ type: 'HTTP', paths: [{ path: '/api3' }], entrypoints: [{ type: 'http-proxy' }] }],
    });
    const api4 = fakeProxyApiV4({
      id: 'api-4',
      name: 'API 4',
      listeners: [{ type: 'HTTP', paths: [{ path: '/api4' }], entrypoints: [{ type: 'http-proxy' }] }],
    });
    const api5 = fakeProxyApiV4({
      id: 'api-5',
      name: 'API 5',
      listeners: [{ type: 'HTTP', paths: [{ path: '/api5' }], entrypoints: [{ type: 'http-proxy' }] }],
    });
    const page1Apis = [fakeApi1, fakeApi2, api3, api4, api5];
    const page2Apis = [
      fakeProxyApiV4({
        id: 'api-6',
        name: 'Shipping API',
        apiVersion: '1.0',
        listeners: [{ type: 'HTTP', paths: [{ path: '/shipping' }], entrypoints: [{ type: 'http-proxy' }] }],
      }),
    ];
    const totalCount = 6;

    initComponentFakeAsync(page1Apis, { totalCount });

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    const paginator = await tableWrapper.getPaginator();
    expect(await paginator.getRangeLabel()).toContain('6');

    await paginator.goToNextPage();
    tick(450);

    expectApisSearchRequest(httpTestingController, page2Apis, { page: 2, totalCount });

    fixture.detectChanges();
    tick();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);
    const rowCells0 = await rows[0].getCellTextByIndex();
    expect(rowCells0[1]).toContain('Shipping API');
  }));

  it('should search with pagination and load page when changing page after search', fakeAsync(async () => {
    queryParams$.next({ page: '1', size: '1', q: '' });
    const page1SearchResult = [fakeApi2];
    const page2SearchResult = [
      fakeProxyApiV4({
        id: 'api-3',
        name: 'Orders Extended API',
        apiVersion: '1.0',
        listeners: [{ type: 'HTTP', paths: [{ path: '/orders-ext' }], entrypoints: [{ type: 'http-proxy' }] }],
      }),
    ];
    const totalCount = 2;

    initComponentFakeAsync([fakeApi1, fakeApi2], { perPage: 1 });

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('Order');
    tick(450);

    expectApisSearchRequest(httpTestingController, page1SearchResult, {
      query: 'Order',
      totalCount,
      perPage: 1,
    });

    fixture.detectChanges();
    tick();

    const paginator = await tableWrapper.getPaginator();
    await paginator.goToNextPage();
    tick(450);

    expectApisSearchRequest(httpTestingController, page2SearchResult, {
      query: 'Order',
      page: 2,
      totalCount,
      perPage: 1,
    });

    fixture.detectChanges();
    tick();

    const table = await loader.getHarness(MatTableHarness.with({ selector: 'table' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);
    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[1]).toContain('Orders Extended API');
  }));

  async function initComponent(apis: Api[] = []) {
    fixture.detectChanges();
    await fixture.whenStable();

    expectApisSearchRequest(httpTestingController, apis);

    fixture.detectChanges();
    await fixture.whenStable();
  }

  /** Sync init for fakeAsync tests - whenStable() hangs in fakeAsync zone */
  function initComponentFakeAsync(
    apis: Api[] = [],
    options?: { apiProductId?: string; query?: string; page?: number; perPage?: number; totalCount?: number },
  ) {
    fixture.detectChanges();
    tick(150); // debounceTime(100) on queryParams

    expectApisSearchRequest(httpTestingController, apis, options);

    fixture.detectChanges();
    tick();
  }
});
