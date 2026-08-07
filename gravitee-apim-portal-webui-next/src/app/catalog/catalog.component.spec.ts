/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { CatalogComponent } from './catalog.component';
import { CatalogHarness } from './catalog.component.harness';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { ApiProductCardHarness } from '../../components/api-product-card/api-product-card.harness';
import { PaginationHarness } from '../../components/pagination/pagination.harness';
import { fakeApi } from '../../entities/api/api.fixtures';
import { PortalNavigationItemsSearchResponse } from '../../entities/portal-navigation/portal-navigation-apis-search';
import { fakePortalNavigationApi, fakePortalNavigationApiProduct } from '../../entities/portal-navigation/portal-navigation-item.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let catalogHarness: CatalogHarness;
  let httpTestingController: HttpTestingController;

  const api = fakeApi({
    id: 'api-1',
    name: 'Weather API',
    version: '1.0',
    description: 'Weather forecasts and historical data.',
    labels: ['weather'],
  });
  const mcpApi = fakeApi({
    id: 'api-2',
    name: 'MCP Server API',
    version: '2.0',
    description: 'An MCP-enabled API.',
    mcp: {
      mcpPath: '/mcp',
      tools: [{ toolDefinition: { name: 'MCP Tool', description: 'MCP Tool Description', inputSchema: {} } }],
    },
  });
  const apiProduct = {
    id: '4f6597ca-74b8-4e68-a597-ca74b83e6824',
    name: 'AI Workspace',
    description: 'APIs for AI applications.',
    version: '3.0',
    navigationItemId: 'product-nav-1',
    apis: [
      { id: api.id, name: api.name, version: api.version },
      { id: mcpApi.id, name: mcpApi.name, version: mcpApi.version },
    ],
  };

  const createCatalogResponse = (overrides: Partial<PortalNavigationItemsSearchResponse> = {}): PortalNavigationItemsSearchResponse => ({
    data: [
      fakePortalNavigationApi({ id: 'api-nav-1', rootId: 'api-root-1', apiId: api.id }),
      fakePortalNavigationApiProduct({
        id: apiProduct.navigationItemId,
        rootId: 'product-root-1',
        apiProductId: apiProduct.id,
      }),
      fakePortalNavigationApi({ id: 'api-nav-2', rootId: 'api-root-2', apiId: mcpApi.id }),
    ],
    apis: [api, mcpApi],
    apiProducts: [apiProduct],
    links: {},
    metadata: {
      pagination: {
        current_page: 1,
        size: 20,
        total: 3,
        total_pages: 1,
      },
    },
    ...overrides,
  });

  const initBase = async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    catalogHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CatalogHarness);
  };

  const init = async (response: PortalNavigationItemsSearchResponse = createCatalogResponse()) => {
    await initBase();
    expectCatalogRequest().flush(response);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should render APIs and API Products in one grid', async () => {
    await init();

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
    const productCards = await harnessLoader.getAllHarnesses(ApiProductCardHarness);

    expect(apiCards).toHaveLength(2);
    expect(await apiCards[0].getTitle()).toBe('Weather API');
    expect(await apiCards[0].getType()).toBe('API');
    expect(await apiCards[1].isMcpServer()).toBe(true);
    expect(productCards).toHaveLength(1);
    expect(await productCards[0].getTitle()).toBe('AI Workspace');
    expect(await productCards[0].getType()).toBe('API PRODUCT');
    expect(await productCards[0].getApiCount()).toContain('2 APIS INCLUDED');
  });

  it('should render APIs and API Products in one list', async () => {
    await init();

    fixture.componentInstance.toggleViewMode();
    fixture.detectChanges();

    const rows = await catalogHarness.getRowCellText();
    expect(rows).toHaveLength(3);
    expect(rows[0]).toMatchObject({
      name: expect.stringContaining('Weather API'),
      labels: expect.stringContaining('# weather'),
      version: '1.0',
    });
    expect(rows[1]).toMatchObject({
      name: expect.stringContaining('AI Workspace'),
      labels: expect.stringContaining('Included APIs: 2'),
      version: '3.0',
    });
  });

  it('should navigate an API Product card to its documentation context', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await (await harnessLoader.getHarness(ApiProductCardHarness)).select();

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'product-root-1'], {
      queryParams: { selectedId: 'product-nav-1' },
    });
  });

  it('should preserve API card documentation navigation', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await (await harnessLoader.getAllHarnesses(ApiCardHarness))[0].select();

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'api-root-1'], {
      queryParams: { selectedId: 'api-nav-1' },
    });
  });

  it('should navigate an API Product row to its documentation context', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.toggleViewMode();
    fixture.detectChanges();

    await catalogHarness.selectRow(1);

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'product-root-1'], {
      queryParams: { selectedId: 'product-nav-1' },
    });
  });

  it('should request the next mixed catalog page', async () => {
    await init(
      createCatalogResponse({
        metadata: {
          pagination: {
            current_page: 1,
            size: 20,
            total: 40,
            total_pages: 2,
          },
        },
      }),
    );

    const nextButton = await (await harnessLoader.getHarness(PaginationHarness)).getNextPageButton();
    await nextButton.click();
    expectCatalogRequest(2).flush(
      createCatalogResponse({
        data: [fakePortalNavigationApiProduct({ id: apiProduct.navigationItemId, rootId: 'product-root-1' })],
        apis: [],
        metadata: {
          pagination: {
            current_page: 2,
            size: 20,
            total: 40,
            total_pages: 2,
          },
        },
      }),
    );
    fixture.detectChanges();

    expect(await harnessLoader.getAllHarnesses(ApiProductCardHarness)).toHaveLength(1);
    expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(0);
  });

  it('should request a new page size for the mixed catalog', async () => {
    await init();

    await (await harnessLoader.getHarness(PaginationHarness)).changePageSize(40);
    expectCatalogRequest(1, 40).flush(createCatalogResponse());
  });

  it('should preserve URL-backed search navigation', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance.onSearchResults('workspace');

    expect(navigate).toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: { query: 'workspace' },
    });
  });

  it('should show the generalized empty state when no catalog items are returned', async () => {
    await init(createCatalogResponse({ data: [], apis: [], apiProducts: [] }));

    expect(await catalogHarness.getEmptyStateText()).toContain('No catalog items available yet');
  });

  it('should show the generalized empty state when the catalog request fails', async () => {
    await initBase();
    expect(await catalogHarness.isLoading()).toBe(true);
    expectCatalogRequest().flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
    fixture.detectChanges();

    expect(await catalogHarness.isLoading()).toBe(false);
    expect(await catalogHarness.getEmptyStateText()).toContain('No catalog items available yet');
  });

  function expectCatalogRequest(page = 1, size = 20) {
    return httpTestingController.expectOne(
      request =>
        request.method === 'GET' &&
        request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search` &&
        request.params.get('type') === 'catalog' &&
        request.params.getAll('include')?.join(',') === 'api,api_product' &&
        request.params.get('page') === `${page}` &&
        request.params.get('size') === `${size}`,
    );
  }
});
