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
import { BehaviorSubject, of } from 'rxjs';

import { CatalogComponent } from './catalog.component';
import { CatalogHarness } from './catalog.component.harness';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { ApiProductCardHarness } from '../../components/api-product-card/api-product-card.harness';
import { PaginationHarness } from '../../components/pagination/pagination.harness';
import { fakeApi } from '../../entities/api/api.fixtures';
import { PortalCategory } from '../../entities/categories/portal-category';
import { fakePortalCategory } from '../../entities/categories/portal-category.fixture';
import { Plan } from '../../entities/plan/plan';
import { PortalNavigationItemsSearchResponse } from '../../entities/portal-navigation/portal-navigation-apis-search';
import {
  fakePortalNavigationAgent,
  fakePortalNavigationApi,
  fakePortalNavigationApiProduct,
} from '../../entities/portal-navigation/portal-navigation-item.fixture';
import { CurrentUserService } from '../../services/current-user.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { stubOverflowLabelsLayout } from '../../testing/overflow-labels-layout';

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
    version: '1.5',
    description: 'An agent the backend already knows is an agent.',
    labels: ['helpdesk'],
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

  const createCatalogResponse = (overrides: Partial<PortalNavigationItemsSearchResponse> = {}): PortalNavigationItemsSearchResponse => {
    const response: PortalNavigationItemsSearchResponse = {
      data: [
        fakePortalNavigationApi({ id: 'api-nav-1', rootId: 'api-root-1', apiId: api.id, categoryIds: ['cat-1'] }),
        fakePortalNavigationApiProduct({
          id: apiProduct.navigationItemId,
          rootId: 'product-root-1',
          apiProductId: apiProduct.id,
          categoryIds: ['cat-1'],
        }),
        fakePortalNavigationApi({ id: 'api-nav-2', rootId: 'api-root-2', apiId: mcpApi.id }),
        fakePortalNavigationAgent({ id: 'agent-nav-1', rootId: 'agent-root-1', agentId: agent.id }),
      ],
      apis: [api, mcpApi, agent],
      apiProducts: [apiProduct],
      links: {},
      ...overrides,
    };
    return {
      ...response,
      metadata: overrides.metadata ?? {
        pagination: { current_page: 1, size: -1, total: response.data?.length ?? 0, total_pages: 1 },
      },
    };
  };

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

  const init = async (
    response: PortalNavigationItemsSearchResponse = createCatalogResponse(),
    plansByApiId: Record<string, Plan[]> = {},
  ) => {
    await initBase();
    expectCatalogRequest().flush(response);
    fixture.detectChanges();
    flushPlans(plansByApiId);
    fixture.detectChanges();
  };

  const flushSubscriptions = (apiIds: string[]) => {
    httpTestingController
      .match(request => request.url === `${TESTING_BASE_URL}/subscriptions`)
      .forEach(request =>
        request.flush({
          data: apiIds.map((api, index) => ({ id: `sub-${index}`, api, application: 'app', plan: 'plan', status: 'ACCEPTED' })),
        }),
      );
  };

  const flushPlans = (plansByApiId: Record<string, Plan[]> = {}) => {
    for (;;) {
      const pending = httpTestingController.match(
        request => request.url.startsWith(`${TESTING_BASE_URL}/apis/`) && request.url.includes('/plans'),
      );
      if (pending.length === 0) {
        return;
      }
      pending.forEach(request => {
        const apiId = request.request.url.replace(`${TESTING_BASE_URL}/apis/`, '').replace(/\/plans.*$/, '');
        request.flush({ data: plansByApiId[apiId] ?? [] });
      });
    }
  };

  const fakePlan = (overrides: Partial<Plan>): Plan =>
    ({ id: 'plan-1', name: 'Plan', security: 'API_KEY', validation: 'AUTO', order: 1, mode: 'STANDARD', ...overrides }) as Plan;

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

  it('should open on the agents, not on a mixed list', async () => {
    await init();

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);

    expect(await Promise.all(apiCards.map(card => card.getTitle()))).toEqual(['Helpdesk Agent', 'MCP Server API']);
    expect(await apiCards[0].getType()).toBe('AGENT');
    expect(await harnessLoader.getAllHarnesses(ApiProductCardHarness)).toHaveLength(0);
  });

  it('should take an item the backend calls an agent at its word', async () => {
    await init();

    expect(await catalogHarness.getKindCounts()).toEqual(['2', '2']);
    expect(await catalogHarness.getTally()).toBe('2 Agents');
  });

  it('should render APIs and API Products together on the APIs side', async () => {
    await init();

    await catalogHarness.selectKind('APIs');
    fixture.detectChanges();

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
    const productCards = await harnessLoader.getAllHarnesses(ApiProductCardHarness);

    expect(apiCards).toHaveLength(1);
    expect(await apiCards[0].getTitle()).toBe('Weather API');
    expect(await apiCards[0].getType()).toBe('API');
    expect(productCards).toHaveLength(1);
    expect(await productCards[0].getTitle()).toBe('AI Workspace');
    expect(await productCards[0].getApiCount()).toContain('2 APIS INCLUDED');
  });

  it('should render the APIs side as a list', async () => {
    await init();

    await catalogHarness.selectKind('APIs');
    fixture.componentInstance.toggleViewMode();
    fixture.detectChanges();

    const rows = await catalogHarness.getAllRowsCellText();
    expect(rows).toHaveLength(2);
    expect(rows[0]).toMatchObject({
      name: expect.stringContaining('AI Workspace'),
      labels: expect.stringContaining('Included APIs: 2'),
      version: '3.0',
    });
    expect(rows[1]).toMatchObject({
      name: expect.stringContaining('Weather API'),
      labels: expect.stringContaining('# weather'),
      version: '1.0',
    });
  });

  it('should navigate an API Product card to its documentation context', async () => {
    await init();
    await catalogHarness.selectKind('APIs');
    fixture.detectChanges();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await (await harnessLoader.getHarness(ApiProductCardHarness)).select();

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'product-root-1'], {
      queryParams: { selectedId: 'product-nav-1' },
    });
  });

  it('should preserve API card documentation navigation', async () => {
    await init();
    await catalogHarness.selectKind('APIs');
    fixture.detectChanges();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await (await harnessLoader.getAllHarnesses(ApiCardHarness))[0].select();

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'api-root-1'], {
      queryParams: { selectedId: 'api-nav-1' },
    });
  });

  it('should navigate an API Product row to its documentation context', async () => {
    await init();
    await catalogHarness.selectKind('APIs');
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.toggleViewMode();
    fixture.detectChanges();

    await catalogHarness.selectRow(0);

    expect(navigate).toHaveBeenCalledWith(['/documentation', 'product-root-1'], {
      queryParams: { selectedId: 'product-nav-1' },
    });
  });

  it('should page through the loaded catalog without asking the server again', async () => {
    const apis = Array.from({ length: 12 }, (_, index) => fakeApi({ id: `api-${index}`, name: `API ${index}`, labels: [] }));
    await init(
      createCatalogResponse({
        data: apis.map((catalogApi, index) =>
          fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: catalogApi.id }),
        ),
        apis,
        apiProducts: [],
      }),
    );

    await catalogHarness.selectKind('APIs');
    fixture.detectChanges();

    await (await harnessLoader.getHarness(PaginationHarness)).changePageSize(8);
    fixture.detectChanges();
    expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(8);

    await (await (await harnessLoader.getHarness(PaginationHarness)).getNextPageButton()).click();
    fixture.detectChanges();

    expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(4);
    httpTestingController.expectNone(request => request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search`);
  });

  it('should count what the filters leave, not what one page holds', async () => {
    const apis = Array.from({ length: 12 }, (_, index) =>
      fakeApi({ id: `api-${index}`, name: `API ${index}`, labels: [], type: index < 5 ? 'A2A_PROXY' : 'PROXY' }),
    );
    await init(
      createCatalogResponse({
        data: apis.map((catalogApi, index) =>
          fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: catalogApi.id }),
        ),
        apis,
        apiProducts: [],
      }),
    );

    await catalogHarness.selectKind('APIs');
    fixture.detectChanges();

    await (await harnessLoader.getHarness(PaginationHarness)).changePageSize(8);
    fixture.detectChanges();

    expect(await catalogHarness.getKindCounts()).toEqual(['5', '7']);
    expect(await catalogHarness.getTally()).toBe('7 APIs');
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
    await init(
      createCatalogResponse({
        data: [],
        apis: [],
        apiProducts: [],
        metadata: { pagination: { current_page: 1, size: -1, total: 0, total_pages: 1 } },
      }),
    );

    expect(await catalogHarness.getEmptyStateText()).toContain('No catalog items available yet');
  });

  it('should render the items it could resolve when one entry has no backing api', async () => {
    await init(
      createCatalogResponse({
        data: [
          fakePortalNavigationAgent({ id: 'agent-nav-1', rootId: 'agent-root-1', agentId: agent.id }),
          fakePortalNavigationAgent({ id: 'agent-nav-orphan', rootId: 'agent-root-orphan', agentId: 'deleted-api-id' }),
        ],
        apis: [agent],
        apiProducts: [],
        metadata: { pagination: { current_page: 1, size: -1, total: 2, total_pages: 1 } },
      }),
      { [agent.id]: [] },
    );

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
    expect(await Promise.all(apiCards.map(card => card.getTitle()))).toEqual(['Helpdesk Agent']);
    expect(await catalogHarness.getEmptyStateText()).toBeNull();
  });

  it('should sort agents by their own dates, not push them behind every api', async () => {
    const olderAgent = fakeApi({
      id: 'agent-api-old',
      name: 'Older Agent',
      created_at: new Date('2024-01-01T00:00:00Z'),
      updated_at: new Date('2024-01-01T00:00:00Z'),
    });
    const newerAgent = fakeApi({
      id: 'agent-api-new',
      name: 'Newer Agent',
      created_at: new Date('2025-06-01T00:00:00Z'),
      updated_at: new Date('2025-06-01T00:00:00Z'),
    });
    await init(
      createCatalogResponse({
        data: [
          fakePortalNavigationAgent({ id: 'agent-nav-old', rootId: 'agent-root-old', agentId: olderAgent.id }),
          fakePortalNavigationAgent({ id: 'agent-nav-new', rootId: 'agent-root-new', agentId: newerAgent.id }),
        ],
        apis: [olderAgent, newerAgent],
        apiProducts: [],
      }),
      { [olderAgent.id]: [], [newerAgent.id]: [] },
    );

    await catalogHarness.selectSort('newest');
    fixture.detectChanges();

    const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
    expect(await Promise.all(apiCards.map(card => card.getTitle()))).toEqual(['Newer Agent', 'Older Agent']);
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

    it('should seed the category filter from the URL', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      flushCategories(categories);
      fixture.detectChanges();

      expectCatalogRequest().flush(
        createCatalogResponse({
          data: [
            fakePortalNavigationApi({ id: 'api-nav-1', rootId: 'api-root-1', apiId: api.id, categoryIds: ['cat-1'] }),
            fakePortalNavigationApiProduct({
              id: apiProduct.navigationItemId,
              rootId: 'product-root-1',
              apiProductId: apiProduct.id,
              categoryIds: ['cat-2'],
            }),
          ],
          apis: [api],
        }),
      );
      fixture.detectChanges();
      flushPlans();
      fixture.detectChanges();

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await catalogHarness.isFilterPicked('category', 'Category One')).toBe(true);
      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(1);
      expect(await harnessLoader.getAllHarnesses(ApiProductCardHarness)).toHaveLength(0);
    });

    it('should render the selected category for an API Product in list view', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      flushCategories(categories);
      fixture.detectChanges();

      expectCatalogRequest().flush(
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

      await catalogHarness.selectKind('APIs');
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

      expectCatalogRequest().flush(createCatalogResponse());
      fixture.detectChanges();
      flushPlans();
      fixture.detectChanges();

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await harnessLoader.getHarness(ApiProductCardHarness)).toBeDefined();
    });

    it('should navigate with the selected category and keep the search query', async () => {
      await init();
      const router = TestBed.inject(Router);
      const route = TestBed.inject(ActivatedRoute);
      const navigateSpy = jest.spyOn(router, 'navigate');

      fixture.componentInstance.onCategorySelect('cat-1');

      expect(navigateSpy).toHaveBeenCalledWith([], {
        relativeTo: route,
        queryParams: { category: 'cat-1' },
        queryParamsHandling: 'merge',
      });
    });

    it('should drop the search and the category in a single navigation when the filters are cleared', async () => {
      await initWithQueryParams({ category: 'cat-1', query: 'weather' });
      flushCategories(categories);
      fixture.detectChanges();
      expectCatalogRequest().flush(createCatalogResponse());
      fixture.detectChanges();
      flushPlans();
      fixture.detectChanges();
      const navigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

      fixture.componentInstance.clearFilters();

      expect(navigateSpy).toHaveBeenCalledTimes(1);
      expect(navigateSpy).toHaveBeenCalledWith([], {
        relativeTo: expect.anything(),
        queryParams: { category: null, query: null },
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

  describe('agent-first catalog', () => {
    it('should tell agents and APIs apart, and count both sides', async () => {
      await init();

      expect(await catalogHarness.getKinds()).toEqual(['Agents', 'APIs']);
      expect(await catalogHarness.getKindCounts()).toEqual(['2', '2']);
      expect(await catalogHarness.getTally()).toBe('2 Agents');
    });

    it('should put the chosen side in the URL', async () => {
      await init();
      const router = TestBed.inject(Router);
      const route = TestBed.inject(ActivatedRoute);
      const navigateSpy = jest.spyOn(router, 'navigate');

      await catalogHarness.selectKind('APIs');

      expect(navigateSpy).toHaveBeenCalledWith([], {
        relativeTo: route,
        queryParams: { kind: 'apis' },
        queryParamsHandling: 'merge',
      });
    });

    it('should open on the side the URL asks for', async () => {
      await initWithQueryParams({ kind: 'apis' });
      flushCategories();
      fixture.detectChanges();
      expectCatalogRequest().flush(createCatalogResponse());
      fixture.detectChanges();
      flushPlans();
      fixture.detectChanges();

      expect(await catalogHarness.getTally()).toBe('2 APIs');
      expect(await (await harnessLoader.getAllHarnesses(ApiCardHarness))[0].getTitle()).toBe('Weather API');
    });

    it('should switch sides without asking the server again', async () => {
      await init();

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(1);
      expect(await catalogHarness.getTally()).toBe('2 APIs');
      httpTestingController.expectNone(request => request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search`);

      await catalogHarness.selectKind('Agents');
      fixture.detectChanges();

      expect(await catalogHarness.getTally()).toBe('2 Agents');
    });

    it('should read the access state off the plans and filter by it', async () => {
      await init(createCatalogResponse(), {
        'api-1': [fakePlan({ security: 'KEY_LESS' })],
        'api-2': [fakePlan({ security: 'API_KEY', validation: 'MANUAL' })],
      });

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      const cards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(await cards[0].getAccess()).toBe('No key needed');
      expect(await catalogHarness.getFilterValues('access')).toEqual(['No key needed 1']);

      await catalogHarness.pickFilterValue('access', 'No key needed');
      fixture.detectChanges();

      const filtered = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(filtered).toHaveLength(1);
      expect(await filtered[0].getTitle()).toBe('Weather API');
      expect(await harnessLoader.getAllHarnesses(ApiProductCardHarness)).toHaveLength(0);
    });

    it('should offer a field only while its values can narrow the list', async () => {
      const apis = [
        fakeApi({ id: 'api-a', name: 'A', type: 'PROXY', labels: ['same'] }),
        fakeApi({ id: 'api-b', name: 'B', type: 'PROXY', labels: ['same'] }),
      ];
      await init(
        createCatalogResponse({
          data: apis.map((api, index) => fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: api.id })),
          apis,
          apiProducts: [],
        }),
      );

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await catalogHarness.getFilters()).toEqual([]);
    });

    it('should offer to clear the filters it hides, not the ones already on screen', async () => {
      await init(createCatalogResponse(), {
        'api-1': [fakePlan({ security: 'KEY_LESS' })],
        'api-2': [fakePlan({ security: 'API_KEY', validation: 'MANUAL' })],
      });

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();
      expect(await catalogHarness.hasClearFilters()).toBe(false);

      await catalogHarness.pickFilterValue('access', 'No key needed');
      fixture.detectChanges();
      expect(await catalogHarness.hasClearFilters()).toBe(true);

      await catalogHarness.clearFilters();
      fixture.detectChanges();

      expect(await catalogHarness.hasClearFilters()).toBe(false);
      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(1);
    });

    it('should show the tools of an MCP server as its capabilities, and labels otherwise', async () => {
      stubOverflowLabelsLayout({ containerWidth: 500 });
      await init();

      expect(await (await harnessLoader.getAllHarnesses(ApiCardHarness))[1].getCapabilities()).toEqual(['MCP Tool']);

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await (await harnessLoader.getAllHarnesses(ApiCardHarness))[0].getCapabilities()).toEqual(['weather']);
    });

    it('should say when the filters hide everything, instead of claiming the catalog is empty', async () => {
      const apis = Array.from({ length: 4 }, (_, index) =>
        fakeApi({ id: `api-${index}`, name: `API ${index}`, type: 'PROXY', labels: [index < 2 ? 'a' : 'b'] }),
      );
      await init(
        createCatalogResponse({
          data: apis.map((api, index) => fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: api.id })),
          apis,
          apiProducts: [],
        }),
        {
          'api-0': [fakePlan({ security: 'KEY_LESS' })],
          'api-1': [fakePlan({ security: 'KEY_LESS' })],
          'api-2': [fakePlan({ security: 'API_KEY' })],
          'api-3': [fakePlan({ security: 'API_KEY' })],
        },
      );

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();
      await catalogHarness.pickFilterValue('access', 'No key needed');
      fixture.detectChanges();
      await catalogHarness.pickFilterValue('tag', 'b');
      fixture.detectChanges();

      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(0);
      expect(await catalogHarness.getEmptyStateText()).toContain('Nothing matches these filters');
    });

    it('should keep a filter the other side cannot match, and offer it back to be undone', async () => {
      const apis = [
        fakeApi({ id: 'api-agent', name: 'Agent', type: 'A2A_PROXY', labels: ['shared'] }),
        fakeApi({ id: 'api-rest', name: 'Rest', type: 'PROXY', labels: ['rest-only'] }),
        fakeApi({ id: 'api-other', name: 'Other', type: 'PROXY', labels: ['other'] }),
      ];
      await init(
        createCatalogResponse({
          data: apis.map((api, index) => fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: api.id })),
          apis,
          apiProducts: [],
        }),
      );

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();
      await catalogHarness.pickFilterValue('tag', 'rest-only');
      fixture.detectChanges();
      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(1);

      await catalogHarness.selectKind('Agents');
      fixture.detectChanges();

      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(0);
      expect(await catalogHarness.getEmptyStateText()).toContain('Nothing matches these filters');
      expect(await catalogHarness.isFilterPicked('tag', 'rest-only')).toBe(true);

      await catalogHarness.pickFilterValue('tag', 'rest-only');
      fixture.detectChanges();

      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(1);
      expect(await catalogHarness.getTally()).toBe('1 Agent');
    });

    it('should say how long ago an API was published', async () => {
      const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
      await init(
        createCatalogResponse({
          data: [fakePortalNavigationApi({ id: 'api-nav-1', rootId: 'api-root-1', apiId: api.id })],
          apis: [fakeApi({ ...api, created_at: threeDaysAgo })],
          apiProducts: [],
        }),
      );

      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      const card = (await harnessLoader.getAllHarnesses(ApiCardHarness))[0];
      await card.toggleDetails();

      expect(await card.getPublished()).toBe('3 days ago');
    });
  });

  describe('sorting', () => {
    const sortableApis = [
      fakeApi({
        id: 'api-old',
        name: 'Zulu',
        type: 'PROXY',
        labels: [],
        created_at: new Date('2020-01-01'),
        updated_at: new Date('2026-01-01'),
      }),
      fakeApi({
        id: 'api-new',
        name: 'Alpha',
        type: 'PROXY',
        labels: [],
        created_at: new Date('2026-08-01'),
        updated_at: new Date('2020-01-01'),
      }),
    ];
    const sortableResponse = () =>
      createCatalogResponse({
        data: sortableApis.map((api, index) => fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: api.id })),
        apis: sortableApis,
        apiProducts: [],
      });

    const titlesAfterSorting = async (sort: 'name' | 'newest' | 'updated') => {
      fixture.componentInstance.selectSort({ target: { value: sort } } as unknown as Event);
      fixture.detectChanges();
      const cards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      return Promise.all(cards.map(card => card.getTitle()));
    };

    it.each([
      ['name', ['Alpha', 'Zulu']],
      ['newest', ['Alpha', 'Zulu']],
      ['updated', ['Zulu', 'Alpha']],
    ])('should order by %s', async (sort, expected) => {
      await init(sortableResponse());
      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();

      expect(await titlesAfterSorting(sort as 'name' | 'newest' | 'updated')).toEqual(expected);
    });
  });

  describe('paging and the filters that change it', () => {
    const manyApis = Array.from({ length: 12 }, (_, index) =>
      fakeApi({ id: `api-${index}`, name: `API ${index}`, type: 'PROXY', labels: [index < 4 ? 'few' : 'many'] }),
    );
    const manyResponse = () =>
      createCatalogResponse({
        data: manyApis.map((api, index) => fakePortalNavigationApi({ id: `nav-${index}`, rootId: `root-${index}`, apiId: api.id })),
        apis: manyApis,
        apiProducts: [],
      });

    it('should go back to the first page when a filter narrows the list', async () => {
      await init(manyResponse());
      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();
      await (await harnessLoader.getHarness(PaginationHarness)).changePageSize(8);
      fixture.detectChanges();
      await (await (await harnessLoader.getHarness(PaginationHarness)).getNextPageButton()).click();
      fixture.detectChanges();
      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(4);

      await catalogHarness.pickFilterValue('tag', 'few');
      fixture.detectChanges();

      const cards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(cards).toHaveLength(4);
      expect(await cards[0].getTitle()).toBe('API 0');
    });

    it('should never leave the reader on a page the list no longer has', async () => {
      await init(manyResponse());
      await catalogHarness.selectKind('APIs');
      fixture.detectChanges();
      await (await harnessLoader.getHarness(PaginationHarness)).changePageSize(8);
      fixture.detectChanges();
      await (await (await harnessLoader.getHarness(PaginationHarness)).getNextPageButton()).click();
      fixture.detectChanges();

      fixture.componentInstance.onPageSizeChange(20);
      fixture.detectChanges();

      expect(await harnessLoader.getAllHarnesses(ApiCardHarness)).toHaveLength(12);
    });
  });

  describe('a signed-in reader', () => {
    it('should mark what they are already subscribed to', async () => {
      await initBase();
      TestBed.inject(CurrentUserService).user.set({ id: 'me', display_name: 'Me' });
      expectCatalogRequest().flush(createCatalogResponse());
      fixture.detectChanges();
      flushSubscriptions(['api-2']);
      fixture.detectChanges();
      flushPlans({ 'api-2': [fakePlan({ security: 'API_KEY' })] });
      fixture.detectChanges();

      const cards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(await cards[1].getAccess()).toBe('Subscribed');
    });

    it('should ask for its own subscriptions without listing every api in the url', async () => {
      await initBase();
      TestBed.inject(CurrentUserService).user.set({ id: 'me', display_name: 'Me' });
      expectCatalogRequest().flush(createCatalogResponse());
      fixture.detectChanges();

      const request = httpTestingController.expectOne(req => req.url === `${TESTING_BASE_URL}/subscriptions`);
      expect(request.request.params.getAll('apiIds')).toBeNull();
      request.flush({ data: [] });
      fixture.detectChanges();
      flushPlans();
    });
  });

  it('should not ask again for the plans of apis it already loaded', async () => {
    const firstAgent = fakeApi({ id: 'agent-api-first', name: 'First Agent' });
    const secondAgent = fakeApi({ id: 'agent-api-second', name: 'Second Agent' });
    const queryParams$ = new BehaviorSubject<Record<string, string>>({});

    await TestBed.configureTestingModule({ imports: [CatalogComponent, AppTestingModule] })
      .overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParams: {} }, queryParams: queryParams$ } })
      .compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    catalogHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CatalogHarness);
    flushCategories();
    fixture.detectChanges();

    expectCatalogRequest().flush(
      createCatalogResponse({
        data: [fakePortalNavigationAgent({ id: 'agent-nav-first', rootId: 'agent-root-first', agentId: firstAgent.id })],
        apis: [firstAgent],
        apiProducts: [],
      }),
    );
    fixture.detectChanges();
    expect(flushPlanRequestsAndReturnApiIds()).toEqual([firstAgent.id]);
    fixture.detectChanges();

    queryParams$.next({ query: 'agent' });
    fixture.detectChanges();
    expectCatalogRequest().flush(
      createCatalogResponse({
        data: [
          fakePortalNavigationAgent({ id: 'agent-nav-first', rootId: 'agent-root-first', agentId: firstAgent.id }),
          fakePortalNavigationAgent({ id: 'agent-nav-second', rootId: 'agent-root-second', agentId: secondAgent.id }),
        ],
        apis: [firstAgent, secondAgent],
        apiProducts: [],
      }),
    );
    fixture.detectChanges();

    expect(flushPlanRequestsAndReturnApiIds()).toEqual([secondAgent.id]);
    fixture.detectChanges();
  });

  it('should send the reader to the subscribe page of the catalog, not the root', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance.navigateToSubscribe('api-2');

    expect(navigate).toHaveBeenCalledWith(['api', 'api-2', 'subscribe'], { relativeTo: expect.anything() });
  });

  function flushPlanRequestsAndReturnApiIds(): string[] {
    const pending = httpTestingController.match(
      request => request.url.startsWith(`${TESTING_BASE_URL}/apis/`) && request.url.includes('/plans'),
    );
    pending.forEach(request => request.flush({ data: [] }));
    return pending.map(request => request.request.url.replace(`${TESTING_BASE_URL}/apis/`, '').replace(/\/plans.*$/, ''));
  }

  function expectCatalogRequest(categoryId?: string) {
    return httpTestingController.expectOne(
      request =>
        request.method === 'GET' &&
        request.url === `${TESTING_BASE_URL}/portal-navigation-items/_search` &&
        request.params.get('type') === 'catalog' &&
        request.params.getAll('include')?.join(',') === 'api,api_product,agent' &&
        request.params.get('page') === '1' &&
        request.params.get('size') === '-1' &&
        request.params.get('categoryId') === (categoryId ?? null),
    );
  }

  function flushCategories(categories: PortalCategory[] = []) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-categories`).flush({ data: categories });
  }
});
