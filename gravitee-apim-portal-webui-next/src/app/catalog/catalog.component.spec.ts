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
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { CategorySelectHarness } from '../../components/category-select/category-select.component.harness';
import { PaginationHarness } from '../../components/pagination/pagination.harness';
import { fakeApi, fakeApisResponse } from '../../entities/api/api.fixtures';
import { ApisResponse } from '../../entities/api/apis-response';
import { PortalCategory } from '../../entities/categories/portal-category';
import { fakePortalCategory } from '../../entities/categories/portal-category.fixture';
import { PortalNavigationItemsSearchResponse } from '../../entities/portal-navigation/portal-navigation-apis-search';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const initBase = async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    flushCategories();

    fixture.detectChanges();
  };

  const init = async (
    params: Partial<{
      apisResponse: ApisResponse;
      page: number;
      size: number;
      hasNextPage: boolean;
    }> = {
      apisResponse: fakeApisResponse(),
      page: 1,
      size: 20,
      hasNextPage: false,
    },
  ) => {
    await initBase();
    expectApiList(params.apisResponse, params.page ?? 1, params.size ?? 20, params.hasNextPage ?? false);
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
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('populated api list', () => {
    beforeEach(async () => {
      await init({
        apisResponse: fakeApisResponse({
          data: [
            fakeApi({
              id: '1',
              name: 'Test title',
              version: 'v.1.2',
              description:
                'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
            }),
            fakeApi({
              id: '2',
              name: 'MCP Server API',
              version: 'v.2.0',
              description:
                'Access enterprise-level financial data, reports, and analytics to empower your applications with financial insights.',
              mcp: {
                mcpPath: '/mcp',
                tools: [{ toolDefinition: { name: 'MCP Tool', description: 'MCP Tool Description', inputSchema: {} } }],
              },
            }),
          ],
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 2,
              total: 40,
            },
          },
        }),
        hasNextPage: true,
      });
    });

    it('should show API list', async () => {
      const apiCard = await harnessLoader.getHarness(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(await apiCard.getTitle()).toEqual('Test title');
      expect(await apiCard.getDescription()).toEqual(
        'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
      );
    });

    it('should call second page when pagination changes', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCards.length).toEqual(2);
      expect(await apiCards[0].getTitle()).toEqual('Test title');

      const pagination = await harnessLoader.getHarness(PaginationHarness);
      const nextButton = await pagination.getNextPageButton();
      await nextButton.click();
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api', name: 'second page api', version: '24' })],
          metadata: {
            pagination: {
              current_page: 2,
              total_pages: 2,
            },
          },
        }),
        2,
        20,
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(allHarnesses.length).toEqual(1);
      expect(await allHarnesses[0].getTitle()).toEqual('second page api');
    });

    it('should show MCP server chip', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(await apiCards[1].isMcpServer()).toBeTruthy();
    });
  });

  describe('empty component', () => {
    describe('when no results', () => {
      beforeEach(async () => {
        await init({ apisResponse: fakeApisResponse({ data: [] }) });
      });

      it('should show empty API list', () => {
        const emptyState = fixture.nativeElement.querySelector('.api-list__empty-state');
        expect(emptyState).toBeTruthy();
        expect(emptyState.textContent).toContain('No APIs available yet');
      });
    });

    describe('when error occurs', () => {
      it('should show a generic error state', async () => {
        await initBase();
        httpTestingController
          .expectOne(portalSearchUrl(1, 20))
          .flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
        fixture.detectChanges();

        const emptyState = fixture.nativeElement.querySelector('.api-list__empty-state');
        expect(emptyState).toBeTruthy();
        expect(emptyState.textContent).toContain('Something went wrong');
      });
    });
  });

  describe('category filtering', () => {
    const categories = [
      fakePortalCategory({ id: 'cat-1', title: 'Category One' }),
      fakePortalCategory({ id: 'cat-2', title: 'Category Two' }),
    ];

    it('should send categoryId to the search endpoint and hydrate the selector from the URL', async () => {
      await initWithQueryParams({ category: 'cat-1' });

      const req = httpTestingController.expectOne(
        r => r.url === `${TESTING_BASE_URL}/portal-navigation-items/_search` && r.params.get('categoryId') === 'cat-1',
      );
      req.flush(toPortalSearchResponse(fakeApisResponse({ data: [] }), 1, 20));

      flushCategories(categories);
      fixture.detectChanges();

      const categorySelect = await harnessLoader.getHarness(CategorySelectHarness);
      expect(await categorySelect.getSelectedText()).toEqual('Category One');
    });

    it('should show a generic error state for an unknown or hidden category id', async () => {
      await initWithQueryParams({ category: 'unknown-id' });

      // the initial search fires before the visible categories have loaded, since validity isn't known yet
      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/portal-navigation-items/_search`)
        .flush(toPortalSearchResponse(fakeApisResponse({ data: [] }), 1, 20));

      flushCategories(categories);
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.api-list__empty-state');
      expect(emptyState).toBeTruthy();
      expect(emptyState.textContent).toContain('Something went wrong');
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

  function portalSearchUrl(page: number, size: number, q = '') {
    const base = `${TESTING_BASE_URL}/portal-navigation-items/_search?type=api&include=api&page=${page}&size=${size}`;
    return q ? `${base}&query=${encodeURIComponent(q)}` : base;
  }

  function toPortalSearchResponse(
    apisResponse: ApisResponse,
    page: number,
    size: number,
    hasNextPage = false,
  ): PortalNavigationItemsSearchResponse {
    const apis = apisResponse.data ?? [];
    const links = { ...apisResponse.links };
    if (hasNextPage) links.next = `${TESTING_BASE_URL}/portal-navigation-items/_search?page=${page + 1}&size=${size}`;
    const pagination = apisResponse.metadata?.pagination;
    return {
      data: apis.map(api => ({
        type: 'API' as const,
        apiId: api.id,
        id: `nav-${api.id}`,
        rootId: `root-${page}`,
      })),
      apis,
      links,
      metadata: pagination
        ? {
            pagination: {
              current_page: pagination.current_page ?? page,
              size: pagination.size ?? size,
              total: pagination.total ?? apis.length,
              total_pages: pagination.total_pages ?? 1,
            },
          }
        : undefined,
    } as PortalNavigationItemsSearchResponse;
  }

  function expectApiList(apisResponse: ApisResponse = fakeApisResponse(), page: number = 1, size: number = 20, hasNextPage = false) {
    const url = `${TESTING_BASE_URL}/portal-navigation-items/_search?type=api&include=api&page=${page}&size=${size}`;
    const req = httpTestingController.expectOne(url);
    req.flush(toPortalSearchResponse(apisResponse, page, size, hasNextPage));
  }

  function flushCategories(categories: PortalCategory[] = []) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-categories`).flush({ data: categories });
  }
});
