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
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { CatalogComponent } from './catalog.component';
import { CatalogHarness } from './catalog.component.harness';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { ApiProductCardHarness } from '../../components/api-product-card/api-product-card.harness';
import { DropdownSearchComponentHarness } from '../../components/dropdown-search/dropdown-search.component.harness';
import { PaginationHarness } from '../../components/pagination/pagination.harness';
import { fakeApi } from '../../entities/api/api.fixtures';
import { PortalCategory } from '../../entities/categories/portal-category';
import { fakePortalCategory } from '../../entities/categories/portal-category.fixture';
import { PortalNavigationItemsSearchResponse } from '../../entities/portal-navigation/portal-navigation-apis-search';
import { fakePortalNavigationApi, fakePortalNavigationApiProduct, fakePortalNavigationAgent } from '../../entities/portal-navigation/portal-navigation-item.fixture';
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
  const agent = fakeApi({
    id: 'agent-api-1',
    name: 'Helpdesk Agent',
    version: '1.2',
    description: 'Triage and route IT tickets.',
    labels: ['ticketing'],
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
      fakePortalNavigationApi({ id: 'api-nav-1', rootId: 'api-root-1', apiId: api.id, categoryIds: ['cat-1'] }),
      fakePortalNavigationApiProduct({
        id: apiProduct.navigationItemId,
        rootId: 'product-root-1',
        apiProductId: apiProduct.id,
        categoryIds: ['cat-1'],
      }),
      fakePortalNavigationApi({ id: 'api-nav-2', rootId: 'api-root-2', apiId: mcpApi.id }),
      fakePortalNavigationAgent({ id: 'agent-nav-1', rootId: 'agent-root-1', agentId: agent.id, categoryIds: ['cat-1'] }),
    ],
    apis: [api, mcpApi, agent],
    apiProducts: [apiProduct],
    links: {},
    metadata: {
      pagination: {
        current_page: 1,
        size: 20,
        total: 4,
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
    flushCategories();
    fixture.detectChanges();
    catalogHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CatalogHarness);
  };

  const init = async (response: PortalNavigationItemsSearchResponse = createCatalogResponse()) => {
    await initBase();
    expectCatalogRequest().flush(response);
    fixture.detectChanges();
  };

  const initWithQueryParams = async (queryParams: Record<string, string>) => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
    })
      .overrideProvider(ActivatedRoute, {
        useValue: { snapshot: { queryParams }, queryParams: of(queryParams) },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    catalogHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CatalogHarness);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should render APIs, API Products, and Agents in one grid', async () => {
    await init();

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
    const productCards = await harnessLoader.getAllHarnesses(ApiProductCardHarness);

    expect(apiCards).toHaveLength(3);
    expect(await apiCards[0].getTitle()).toBe('Weather API');
    expect(await apiCards[0].getType()).toBe('API');
    expect(await apiCards[1].isMcpServer()).toBe(true);
    expect(await apiCards[2].getTitle()).toBe('Helpdesk Agent');
    expect(await apiCards[2].getType()).toBe('AGENT');
    expect(productCards).toHaveLength(1);
    expect(await productCards[0].getTitle()).toBe('AI Workspace');
    expect(await productCards[0].getType()).toBe('API PRODUCT');
    expect(await productCards[0].getApiCount()).toContain('2 APIS INCLUDED');
  });

  it('should render APIs, API Products, and Agents in one list', async () => {
    await init();

    fixture.componentInstance.toggleViewMode();
    fixture.detectChanges();

    const rows = await catalogHarness.getAllRowsCellText();
    expect(rows).toHaveLength(4);
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
    expect(rows[3]).toMatchObject({
      name: expect.stringContaining('Helpdesk Agent'),
      labels: expect.stringContaining('# ticketing'),
      version: '1.2',
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

  it('should navigate an Agent card to its documentation context', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await (await harnessLoader.getAllHarnesses(ApiCardHarness))[2].select();

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'agent-root-1'], {
      queryParams: { selectedId: 'agent-nav-1' },
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
      queryParamsHandling: 'merge',
    });
  });

  it('should show the generalized empty state when no catalog items are returned', async () => {
    await init(createCatalogResponse({ data: [], apis: [], apiProducts: [] }));

    expect(await catalogHarness.getEmptyStateText()).toContain('No catalog items available yet');
  });

  it('should show a generic error state when the catalog request fails', async () => {
    await initBase();
    expect(await catalogHarness.isLoading()).toBe(true);
    expectCatalogRequest().flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
    fixture.detectChanges();

    expect(await catalogHarness.isLoading()).toBe(false);
    expect(await catalogHarness.getEmptyStateText()).toContain('Something went wrong');
  });

  describe('category filtering', () => {
    const categories = [
      fakePortalCategory({ id: 'cat-1', title: 'Category One' }),
      fakePortalCategory({ id: 'cat-2', title: 'Category Two' }),
    ];

    it('should send categoryId to the catalog search and hydrate the selector from the URL', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      flushCategories(categories);
      fixture.detectChanges();

      expectCatalogRequest(1, 20, 'cat-1').flush(createCatalogResponse({ data: [], apis: [], apiProducts: [] }));
      fixture.detectChanges();

      const categorySelect = await harnessLoader.getHarness(DropdownSearchComponentHarness);
      expect(await categorySelect.getTriggerText()).toContain('Category One');
    });

    it('should render the selected category for an API Product in list view', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      flushCategories(categories);
      fixture.detectChanges();

      expectCatalogRequest(1, 20, 'cat-1').flush(
        createCatalogResponse({
          data: [
            fakePortalNavigationApiProduct({
              id: apiProduct.navigationItemId,
              rootId: 'product-root-1',
              apiProductId: apiProduct.id,
              categoryIds: ['cat-1'],
            }),
          ],
          apis: [],
          apiProducts: [apiProduct],
          metadata: {
            pagination: {
              current_page: 1,
              size: 20,
              total: 1,
              total_pages: 1,
            },
          },
        }),
      );
      fixture.detectChanges();

      fixture.componentInstance.toggleViewMode();
      fixture.detectChanges();

      const rows = await catalogHarness.getAllRowsCellText();
      expect(rows).toHaveLength(1);
      expect(rows[0]).toMatchObject({
        name: expect.stringContaining('AI Workspace'),
        category: expect.stringContaining('Category One'),
      });
    });

    it('should show a generic error state for an unknown or hidden category id, without calling search', async () => {
      await initWithQueryParams({ category: 'unknown-id' });

      flushCategories(categories);
      fixture.detectChanges();

      httpTestingController.expectNone(request => request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search`);
      expect(fixture.nativeElement.querySelector('.api-list__empty-state').textContent).toContain('Something went wrong');
    });

    it('should still run the catalog search when the categories list fails to load', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      httpTestingController
        .expectOne(`${TESTING_BASE_URL}/portal-categories`)
        .flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
      fixture.detectChanges();

      expectCatalogRequest(1, 20, 'cat-1').flush(createCatalogResponse());
      fixture.detectChanges();

      expect(await harnessLoader.getHarness(ApiProductCardHarness)).toBeDefined();
    });

    it('should navigate with the selected category and clear the search query', async () => {
      await init();
      const router = TestBed.inject(Router);
      const route = TestBed.inject(ActivatedRoute);
      const navigateSpy = jest.spyOn(router, 'navigate');

      fixture.componentInstance.onCategorySelect('cat-1');

      expect(navigateSpy).toHaveBeenCalledWith([], {
        relativeTo: route,
        queryParams: { category: 'cat-1', query: null },
        queryParamsHandling: 'merge',
      });
    });

    it('should preserve the selected category when the search query changes', async () => {
      await init();
      const router = TestBed.inject(Router);
      const route = TestBed.inject(ActivatedRoute);
      const navigateSpy = jest.spyOn(router, 'navigate');

      fixture.componentInstance.onSearchResults('weather');

      expect(navigateSpy).toHaveBeenCalledWith([], {
        relativeTo: route,
        queryParams: { query: 'weather' },
        queryParamsHandling: 'merge',
      });
    });
  });

  function expectCatalogRequest(page = 1, size = 20, categoryId?: string) {
    return httpTestingController.expectOne(
      request =>
        request.method === 'GET' &&
        request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search` &&
        request.params.get('type') === 'catalog' &&
        request.params.getAll('include')?.join(',') === 'api,api_product,agent' &&
        request.params.get('page') === `${page}` &&
        request.params.get('size') === `${size}` &&
        request.params.get('categoryId') === (categoryId ?? null),
    );
  }

  function flushCategories(categories: PortalCategory[] = []) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-categories`).flush({ data: categories });
  }
});
