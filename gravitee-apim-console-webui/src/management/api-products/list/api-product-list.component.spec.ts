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
import { isObject } from 'angular';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiProductListComponent } from './api-product-list.component';
import { ApiProductListEmptyStateHarness } from './api-product-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductListComponent', () => {
  let fixture: ComponentFixture<ApiProductListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let _router: Router;
  let queryParams$: BehaviorSubject<Record<string, string>>;
  let navigateSpy: jest.SpyInstance;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    queryParams$ = new BehaviorSubject<Record<string, string>>({});

    TestBed.configureTestingModule({
      imports: [ApiProductListComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: GioTestingPermissionProvider, useValue: ['environment-api_product-c', 'environment-api_product-u', 'environment-api_product-r'] },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            get snapshot() {
              return { queryParams: queryParams$.value };
            },
            queryParams: queryParams$.asObservable(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    _router = TestBed.inject(Router);

    // When router.navigate is called with queryParams, sync them to our mock so the component receives the update
    const originalNavigate = _router.navigate.bind(_router);
    navigateSpy = jest.spyOn(_router, 'navigate').mockImplementation((commands, extras) => {
      if (extras?.queryParams && isObject(extras.queryParams)) {
        const params: Record<string, string> = {};
        for (const [k, v] of Object.entries(extras.queryParams)) {
          if (v != null && v !== '') params[k] = String(v);
        }
        queryParams$.next({ ...queryParams$.value, ...params });
      }
      return originalNavigate(commands, extras);
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

  it('should display an empty state when no API products', async () => {
    await initComponent([]);

    const emptyState = await loader.getHarness(ApiProductListEmptyStateHarness);
    const emptyStateText = await emptyState.getText();
    expect(emptyStateText).toContain('No API Product created yet');
    expect(emptyStateText).toContain('Create API Products to group together your APIs.');
  });

  it('should display a table with one row', async () => {
    const apiProduct: ApiProduct = {
      id: 'product-1',
      name: 'Payments API Product',
      version: '1.0',
      apiIds: ['api-1', 'api-2'],
      primaryOwner: { displayName: 'Jane Doe' } as any,
    };

    await initComponent([apiProduct]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductsTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(1);

    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[1]).toContain('Payments API Product');
    expect(rowCells[2]).toContain('2');
    expect(rowCells[3]).toContain('1.0');
    expect(rowCells[4]).toContain('Jane Doe');
  });

  it('should display multiple API products', async () => {
    const apiProducts: ApiProduct[] = [
      {
        id: 'product-1',
        name: 'Payments API Product',
        version: '1.0',
        apiIds: ['api-1'],
        primaryOwner: { displayName: 'Jane Doe' } as any,
      },
      {
        id: 'product-2',
        name: 'Orders API Product',
        version: '2.0',
        apiIds: ['api-2', 'api-3'],
        primaryOwner: { displayName: 'John Smith' } as any,
      },
    ];

    await initComponent(apiProducts);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductsTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it('should handle error when loading products', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    req.flush({ message: 'Error loading products' }, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should use default sort (sortBy=name) when URL has no order param', async () => {
    queryParams$.next({});
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    expect(req.request.params.get('sortBy')).toEqual('name');
    req.flush({ data: [], pagination: { totalCount: 0 } });
    await fixture.whenStable();
  });

  it('should add order=name to URL when missing after first load', async () => {
    queryParams$.next({});
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    req.flush({ data: [], pagination: { totalCount: 0 } });
    await fixture.whenStable();

    // Router spy syncs queryParams on navigate, which may trigger a second request after debounce (100ms)
    await new Promise(resolve => setTimeout(resolve, 150));

    const pending = httpTestingController.match(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    pending.forEach(r => r.flush({ data: [], pagination: { totalCount: 0 } }));
    await fixture.whenStable();

    const orderCall = navigateSpy.mock.calls.find(call => call[1]?.queryParams?.order === 'name');
    expect(orderCall).toBeDefined();
  });

  it('should propagate sortBy from component sort state to _search request', async () => {
    // order=-name → queryParamsToFilters → toSort → { active: 'name', direction: 'desc' } → toOrder → '-name'
    queryParams$.next({ order: '-name' });
    try {
      fixture.detectChanges();
    } catch (e: unknown) {
      if ((e as Error)?.message?.includes('NG0100') === false) throw e;
    }
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    expect(req.request.params.get('sortBy')).toEqual('-name');
    expect(req.request.params.get('page')).toEqual('1');
    expect(req.request.params.get('perPage')).toEqual('10');
    req.flush({ data: [], pagination: { totalCount: 0 } });
    await fixture.whenStable();
  });

  it('should update filters and reload data when query params change', async () => {
    const apiProduct: ApiProduct = {
      id: 'product-1',
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
      primaryOwner: { displayName: 'Jane Doe' } as any,
    };
    await initComponent([apiProduct]);

    // Flush any second request from component syncing order: 'name' to URL
    await new Promise(resolve => setTimeout(resolve, 150));
    const pendingAfterInit = httpTestingController.match(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    pendingAfterInit.forEach(r => r.flush({ data: [apiProduct], pagination: { totalCount: 1 } }));
    await fixture.whenStable();

    const comp = fixture.componentInstance;
    comp.onFiltersChanged({
      searchTerm: 'test',
      pagination: { index: 1, size: 10 },
      sort: { active: 'name', direction: 'asc' },
    });

    expect(navigateSpy).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: expect.objectContaining({ q: 'test' }) }));
    const navigateCallWithSearch = navigateSpy.mock.calls.find(c => c[1]?.queryParams?.q === 'test');
    expect(navigateCallWithSearch).toBeDefined();
    const queryParams = navigateCallWithSearch![1].queryParams;
    expect(['q', 'page', 'size', 'order'].every(k => Object.prototype.hasOwnProperty.call(queryParams, k))).toBe(true);

    // Router spy synced queryParams when navigate was called - wait for debounce (100ms) so the request is sent
    await new Promise(resolve => setTimeout(resolve, 150));

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    expect(req.request.body).toEqual({ query: 'test' });
    req.flush({ data: [], pagination: { totalCount: 0 } });
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should display owner as N/A when no primary owner', async () => {
    const apiProduct: ApiProduct = {
      id: 'product-1',
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    await initComponent([apiProduct]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductsTable' }));
    const rows = await table.getRows();
    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[4]).toContain('N/A');
  });

  it('should display zero APIs count when apiIds is empty', async () => {
    const apiProduct: ApiProduct = {
      id: 'product-1',
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
      primaryOwner: { displayName: 'Jane Doe' } as any,
    };

    await initComponent([apiProduct]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductsTable' }));
    const rows = await table.getRows();
    const rowCells = await rows[0].getCellTextByIndex();
    expect(rowCells[2]).toContain('0');
  });

  it('should display "No API Products found." when search returns no results', async () => {
    const apiProduct: ApiProduct = {
      id: 'product-1',
      name: 'Payments API Product',
      version: '1.0',
      apiIds: [],
      primaryOwner: { displayName: 'Jane Doe' } as any,
    };
    await initComponent([apiProduct]);

    // Flush any second request from component syncing order: 'name' to URL
    await new Promise(resolve => setTimeout(resolve, 150));
    const pendingAfterInit = httpTestingController.match(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    pendingAfterInit.forEach(r => r.flush({ data: [apiProduct], pagination: { totalCount: 1 } }));
    await fixture.whenStable();

    const comp = fixture.componentInstance;
    comp.onFiltersChanged({
      searchTerm: 'nonexistent-search-term',
      pagination: { index: 1, size: 10 },
      sort: { active: 'name', direction: 'asc' },
    });

    expect(navigateSpy).toHaveBeenCalledWith(
      [],
      expect.objectContaining({ queryParams: expect.objectContaining({ q: 'nonexistent-search-term' }) }),
    );

    // Router spy synced queryParams when navigate was called - wait for debounce (100ms) so the request is sent
    await new Promise(resolve => setTimeout(resolve, 150));

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    expect(req.request.body).toEqual({ query: 'nonexistent-search-term' });
    req.flush({ data: [], pagination: { totalCount: 0 } });
    fixture.detectChanges();
    await fixture.whenStable();

    const noDataRow = fixture.nativeElement.querySelector('[data-testid="api_product_list_no_data_row"]');
    expect(noDataRow).toBeTruthy();
    expect(noDataRow?.textContent?.trim()).toContain('No API Products found.');
  });

  async function initComponent(apiProducts: ApiProduct[]) {
    fixture.detectChanges();
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      r => r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`) && r.method === 'POST',
    );
    expect(req.request.body).toEqual({});
    expect(req.request.params.get('page')).toEqual('1');
    expect(req.request.params.get('perPage')).toEqual('10');
    expect(req.request.params.get('sortBy')).toEqual('name');
    req.flush({ data: apiProducts, pagination: { totalCount: apiProducts.length } });
    fixture.detectChanges();
    await fixture.whenStable();
  }
});
