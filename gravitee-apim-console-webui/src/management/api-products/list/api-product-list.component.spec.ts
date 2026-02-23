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
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

describe('ApiProductListComponent', () => {
  let fixture: ComponentFixture<ApiProductListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let _router: Router;
  let queryParams$: BehaviorSubject<Record<string, string>>;

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
        { provide: GioTestingPermissionProvider, useValue: ['environment-api_product-c'] },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: {} },
            queryParams: queryParams$.asObservable(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductListComponent);
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

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products?page=1&perPage=10`);
    req.flush({ message: 'Error loading products' }, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
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

    const navigateSpy = jest.spyOn(_router, 'navigate');
    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('test');

    expect(navigateSpy).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: expect.objectContaining({ q: 'test' }) }));

    // Simulate router having updated query params (router is the source of truth)
    queryParams$.next({ q: 'test' });
    await new Promise(resolve => setTimeout(resolve, 150));
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products?page=1&perPage=10`);
    req.flush({ data: [], pagination: { totalCount: 0 } });
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

    const navigateSpy = jest.spyOn(_router, 'navigate');
    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('nonexistent-search-term');

    expect(navigateSpy).toHaveBeenCalledWith(
      [],
      expect.objectContaining({ queryParams: expect.objectContaining({ q: 'nonexistent-search-term' }) }),
    );

    queryParams$.next({ q: 'nonexistent-search-term' });
    await new Promise(resolve => setTimeout(resolve, 150));

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products?page=1&perPage=10`);
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

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products?page=1&perPage=10`);
    expect(req.request.method).toEqual('GET');
    req.flush({ data: apiProducts, pagination: { totalCount: apiProducts.length } });
    fixture.detectChanges();
    await fixture.whenStable();
  }
});
