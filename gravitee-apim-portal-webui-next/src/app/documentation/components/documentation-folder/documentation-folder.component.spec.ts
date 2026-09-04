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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { DocumentationFolderComponent } from './documentation-folder.component';
import { DocumentationFolderComponentHarness } from './documentation-folder.component.harness';
import { ApiType } from '../../../../entities/api/api';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { fakePortalNavigationApiProduct } from '../../../../entities/portal-navigation/portal-navigation-item.fixture';
import { makeItem, MOCK_ITEMS } from '../../../../mocks/portal-navigation-item.mocks';
import { AgentSubscriptionAccess, AgentSubscriptionService } from '../../../../services/agent-subscription.service';
import { ApiService } from '../../../../services/api.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('DocumentationFolderComponent', () => {
  let fixture: ComponentFixture<DocumentationFolderComponent>;
  let harness: DocumentationFolderComponentHarness;
  let navigationServiceSpy: PortalNavigationItemsService;
  let apiServiceSpy: { details: jest.Mock };
  let agentSubscriptionServiceSpy: { forAgent: jest.Mock };
  let routerSpy: jest.Mocked<Router>;
  let queryParamsSubject: BehaviorSubject<{ selectedId?: string }>;

  const MOCK_ITEM = { title: 'Test item' };
  const MOCK_CHILDREN = MOCK_ITEMS;
  const MOCK_CONTENT = 'MOCK_CONTENT';

  const gmdViewerContent = (content: string) => `<p>${content}</p>\n`;

  const baseApiDetails = {
    name: 'API',
    version: '1',
    description: '',
    definitionVersion: 'V4' as const,
    entrypoints: ['https://gw.test'],
  };

  const init = async (
    params: Partial<{
      queryParams: { selectedId?: string };
      items: PortalNavigationItem[];
      content: string;
      isAuthenticated: boolean;
      apiHasMcp: boolean;
      apiType: ApiType;
      agentAccess: AgentSubscriptionAccess | null;
      apiEntrypoints: string[];
    }> = {
      queryParams: { selectedId: 'p1' },
      items: MOCK_CHILDREN,
      content: MOCK_CONTENT,
      isAuthenticated: true,
    },
  ) => {
    queryParamsSubject = new BehaviorSubject(params.queryParams ?? {});
    routerSpy = {
      navigate: jest.fn().mockImplementation((_, options) => {
        if (options?.queryParams) queryParamsSubject.next(options.queryParams);
        return Promise.resolve(true);
      }),
    } as unknown as jest.Mocked<Router>;

    navigationServiceSpy = {
      getNavigationItems: jest.fn().mockReturnValue(of(params.items ?? ([] as unknown as PortalNavigationItem[]))),
      getNavigationItemContent: jest.fn().mockReturnValue(of({ content: params.content!, type: 'GRAVITEE_MARKDOWN' })),
    } as unknown as PortalNavigationItemsService;

    const apiHasMcp = params.apiHasMcp === true;
    apiServiceSpy = {
      details: jest.fn().mockImplementation((id: string) =>
        of({
          ...baseApiDetails,
          ...(params.apiEntrypoints ? { entrypoints: params.apiEntrypoints } : {}),
          id,
          ...(apiHasMcp ? { mcp: { mcpPath: '/mcp', tools: [] as { toolDefinition: Record<string, unknown> }[] } } : {}),
          ...(params.apiType ? { type: params.apiType } : {}),
        }),
      ),
    };
    agentSubscriptionServiceSpy = { forAgent: jest.fn().mockReturnValue(of(params.agentAccess ?? null)) };

    await TestBed.configureTestingModule({
      animationsEnabled: true,
      imports: [DocumentationFolderComponent, MatIconTestingModule, AppTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParams: queryParamsSubject.asObservable() } },
        { provide: Router, useValue: routerSpy },
        { provide: PortalNavigationItemsService, useValue: navigationServiceSpy },
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: AgentSubscriptionService, useValue: agentSubscriptionServiceSpy },
        { provide: CurrentUserService, useValue: { isUserAuthenticated: signal(params?.isAuthenticated ?? true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentationFolderComponent);
    fixture.componentRef.setInput('navItem', MOCK_ITEM);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationFolderComponentHarness);
  };

  const initDeferredNavigationItems = async () => {
    const itemsSubject = new Subject<PortalNavigationItem[]>();
    queryParamsSubject = new BehaviorSubject<{ selectedId?: string }>({ selectedId: 'p1' });
    routerSpy = {
      navigate: jest.fn().mockImplementation((_, options) => {
        if (options?.queryParams) queryParamsSubject.next(options.queryParams);
        return Promise.resolve(true);
      }),
    } as unknown as jest.Mocked<Router>;

    navigationServiceSpy = {
      getNavigationItems: jest.fn().mockReturnValue(itemsSubject.asObservable()),
      getNavigationItemContent: jest.fn().mockReturnValue(of({ content: MOCK_CONTENT, type: 'GRAVITEE_MARKDOWN' })),
    } as unknown as PortalNavigationItemsService;

    await TestBed.configureTestingModule({
      animationsEnabled: true,
      imports: [DocumentationFolderComponent, MatIconTestingModule, AppTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParams: queryParamsSubject.asObservable() } },
        { provide: Router, useValue: routerSpy },
        { provide: PortalNavigationItemsService, useValue: navigationServiceSpy },
        { provide: CurrentUserService, useValue: { isUserAuthenticated: signal(true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentationFolderComponent);
    fixture.componentRef.setInput('navItem', MOCK_ITEM);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationFolderComponentHarness);
    return itemsSubject;
  };

  describe('loading skeletons', () => {
    it('should show sidenav, main content and breadcrumb skeletons while folder navigation items load', async () => {
      const itemsSubject = await initDeferredNavigationItems();
      await new Promise<void>(resolve => setTimeout(resolve, 0));

      const sidenavSkeleton = await harness.getSidenavSkeleton();
      const documentationSkeleton = await harness.getDocumentationSkeleton();
      const breadcrumbSkeleton = await harness.getBreadcrumbSkeleton();

      expect(sidenavSkeleton).not.toBeNull();
      expect(documentationSkeleton).not.toBeNull();
      expect(breadcrumbSkeleton).not.toBeNull();

      expect(await harness.getTreeHarness()).toBeNull();
      expect(await harness.getBreadcrumbs()).toBeNull();

      itemsSubject.next(MOCK_CHILDREN);
      itemsSubject.complete();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(await harness.getTreeHarness()).not.toBeNull();
      expect(await harness.getBreadcrumbs()).not.toBeNull();
      const viewer = await harness.getGmdViewer();
      expect(viewer).not.toBeNull();
      expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));
    });

    it('should show documentation skeleton while page content loads after selecting another page', async () => {
      await init();

      const contentSubject = new Subject<{ content: string; type: string }>();
      navigationServiceSpy.getNavigationItemContent = jest.fn().mockReturnValue(contentSubject.asObservable());

      const tree = await harness.getTreeHarness();
      await tree!.clickItemByTitle('Page 2');
      await new Promise<void>(resolve => setTimeout(resolve, 0));

      expect(await harness.getDocumentationSkeleton()).not.toBeNull();
      expect(await harness.getSidenavSkeleton()).toBeNull();
      expect(await harness.getBreadcrumbSkeleton()).toBeNull();

      contentSubject.next({ content: 'Content of Page 2', type: 'GRAVITEE_MARKDOWN' });
      contentSubject.complete();
      fixture.detectChanges();
      await fixture.whenStable();

      const viewer = await harness.getGmdViewer();
      expect(viewer).not.toBeNull();
      expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent('Content of Page 2'));
    });
  });

  describe('initial load', () => {
    describe('with content', () => {
      it('should display tree, content and breadcrumbs', async () => {
        await init();

        const tree = await harness.getTreeHarness();
        expect(tree).not.toBeNull();
        expect(await tree!.getAllItemTitles()).toEqual([
          'Folder 1',
          'Folder 2',
          'Page 1',
          'API 1',
          'API 1 Documentation',
          'Page 2',
          'Page 3',
        ]);

        const treeEmptyState = await harness.getSidenavEmptyState();
        expect(await treeEmptyState?.getText()).toBeUndefined();

        const viewer = await harness.getGmdViewer();
        expect(viewer).not.toBeNull();
        expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));

        const contentEmptyState = await harness.getContentEmptyState();
        expect(await contentEmptyState?.getText()).toBeUndefined();

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Folder 2/Page 1');
      });

      it('should select first page when no pageId provided', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: {}, content: MOCK_CONTENT });

        expect(routerSpy.navigate).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: { selectedId: 'p1' },
        });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('Page 1');

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Folder 2/Page 1');
      });

      it('should select page when pageId is provided', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: { selectedId: 'p2' }, content: MOCK_CONTENT });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('Page 2');

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Page 2');
      });

      it('should not show subscribe button when selected page has no API ancestor', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: { selectedId: 'p2' }, content: MOCK_CONTENT });

        const subscribeButton = await harness.getSubscribeButton();
        expect(subscribeButton).toBeNull();
      });

      it('should not select if no pages exist', async () => {
        const items = [
          makeItem('f1', 'FOLDER', 'Folder 1', 0),
          makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
          makeItem('f3', 'FOLDER', 'Folder 3', 0, 'f2'),
          makeItem('f4', 'FOLDER', 'Folder 4', 0, 'f3'),
          makeItem('l1', 'LINK', 'Link 1', 0, 'f4'),
        ];

        await init({ items, queryParams: {}, content: MOCK_CONTENT });

        const viewer = await harness.getGmdViewer();
        expect(viewer).toBeNull();

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).not.toBeDefined();

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item');
      });

      it('should redirect to 404 when provided pageId is unknown', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: { selectedId: 'p999' }, content: MOCK_CONTENT });
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/404']);
      });

      it('should redirect to first page when selectedId is folder', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: { selectedId: 'f1' }, content: MOCK_CONTENT });

        expect(routerSpy.navigate).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: { selectedId: 'p1' },
        });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('Page 1');

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Folder 2/Page 1');
      });

      it('should redirect to first page when selectedId is API', async () => {
        const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
        const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
        await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'api1' }, content: MOCK_CONTENT });

        expect(routerSpy.navigate).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: { selectedId: 'p-api1' },
        });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('API 1 Documentation');

        const viewer = await harness.getGmdViewer();
        expect(viewer).not.toBeNull();
        expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));
      });
    });

    describe('with empty states', () => {
      it('should not display tree', async () => {
        await init({ items: [], content: '', queryParams: {} });

        const tree = await harness.getTreeHarness();
        expect(tree).toBeNull();

        const emptyState = await harness.getSidenavEmptyState();
        expect(await emptyState?.getText()).toEqual('No items to show');
      });

      it('should not display content', async () => {
        await init({ content: '', queryParams: {} });

        const viewer = await harness.getGmdViewer();
        expect(viewer).toBeNull();

        const emptyState = await harness.getContentEmptyState();
        expect(await emptyState?.getText()).toEqual('No content to show');
      });
    });
  });

  describe('item selection', () => {
    it('should navigate on page click', async () => {
      await init();

      navigationServiceSpy.getNavigationItemContent = jest
        .fn()
        .mockReturnValueOnce(of({ content: 'Content of Page 2', type: 'GRAVITEE_MARKDOWN' }));

      const tree = await harness.getTreeHarness();
      expect(tree).not.toBeNull();
      await tree!.clickItemByTitle('Page 2');

      expect(routerSpy.navigate).toHaveBeenCalledWith([], {
        relativeTo: expect.anything(),
        queryParams: { selectedId: 'p2' },
      });

      const selectedItem = await tree?.getSelectedItem();
      expect(selectedItem).toBeDefined();
      expect(await selectedItem!.getText()).toEqual('Page 2');

      const viewer = await harness.getGmdViewer();
      expect(viewer).not.toBeNull();
      expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent('Content of Page 2'));

      const breadcrumbs = await harness.getBreadcrumbs();
      expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Page 2');
    });
  });

  describe('api product', () => {
    it('should redirect an API Product selection to its first readable descendant', async () => {
      const apiProduct = makeItem('product1', 'API_PRODUCT', 'API Product 1', 0, undefined, 'root1');
      const productFolder = makeItem('product-folder1', 'FOLDER', 'Product Documentation', 0, 'product1', 'root1');
      const overviewPage = makeItem('product-overview1', 'PAGE', 'Product Overview', 0, 'product-folder1', 'root1');
      const laterPage = makeItem('product-page2', 'PAGE', 'Later Product Page', 1, 'product1', 'root1');

      await init({
        items: [apiProduct, productFolder, overviewPage, laterPage],
        queryParams: { selectedId: 'product1' },
        content: MOCK_CONTENT,
      });

      expect(routerSpy.navigate).toHaveBeenCalledWith([], {
        relativeTo: expect.anything(),
        queryParams: { selectedId: 'product-overview1' },
      });
      expect(navigationServiceSpy.getNavigationItemContent).not.toHaveBeenCalledWith('product1');
      expect(navigationServiceSpy.getNavigationItemContent).toHaveBeenCalledWith('product-overview1');

      const breadcrumbs = await harness.getBreadcrumbs();
      expect(await breadcrumbs?.getText()).toEqual('Test item/API Product 1/Product Documentation/Product Overview');
    });

    it('should keep the existing empty state when an API Product has no readable descendant', async () => {
      const apiProduct = makeItem('product1', 'API_PRODUCT', 'API Product 1', 0, undefined, 'root1');
      const productFolder = makeItem('product-folder1', 'FOLDER', 'Product Documentation', 0, 'product1', 'root1');
      const productLink = makeItem('product-link1', 'LINK', 'External Documentation', 0, 'product-folder1', 'root1');

      await init({
        items: [apiProduct, productFolder, productLink],
        queryParams: { selectedId: 'product1' },
        content: MOCK_CONTENT,
      });

      expect(routerSpy.navigate).not.toHaveBeenCalled();
      expect(navigationServiceSpy.getNavigationItemContent).not.toHaveBeenCalled();

      const contentEmptyState = await harness.getContentEmptyState();
      expect(await contentEmptyState?.getText()).toEqual('No content to show');
    });

    it('should show a subscription action for product documentation and navigate to the product flow', async () => {
      const apiProduct = fakePortalNavigationApiProduct({
        id: 'product1',
        apiProductId: 'api-product-1',
        title: 'API Product 1',
        rootId: 'root1',
      });
      const productPage = makeItem('product-overview1', 'PAGE', 'Product Overview', 0, 'product1', 'root1');

      await init({ items: [apiProduct, productPage], queryParams: { selectedId: 'product-overview1' }, content: MOCK_CONTENT });

      const subscribeButton = await harness.getSubscribeButton();
      expect(subscribeButton).not.toBeNull();
      expect(await subscribeButton!.getText()).toEqual('Subscribe');

      await subscribeButton!.click();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['api-product', 'api-product-1', 'subscribe'], {
        relativeTo: expect.anything(),
        queryParamsHandling: 'preserve',
      });
    });

    it('should keep the product subscription action disabled for an unauthenticated user', async () => {
      const apiProduct = fakePortalNavigationApiProduct({ id: 'product1', apiProductId: 'api-product-1', rootId: 'root1' });
      const productPage = makeItem('product-overview1', 'PAGE', 'Product Overview', 0, 'product1', 'root1');

      await init({
        items: [apiProduct, productPage],
        queryParams: { selectedId: 'product-overview1' },
        content: MOCK_CONTENT,
        isAuthenticated: false,
      });

      const subscribeButton = await harness.getSubscribeButton();
      expect(subscribeButton).not.toBeNull();
      expect(await subscribeButton!.getText()).toEqual('Sign in to subscribe');
      expect(await subscribeButton!.isDisabled()).toBe(true);
    });

    it('should not load API details or show MCP actions for product documentation', async () => {
      const apiProduct = fakePortalNavigationApiProduct({ id: 'product1', apiProductId: 'api-product-1', rootId: 'root1' });
      const productPage = makeItem('product-overview1', 'PAGE', 'Product Overview', 0, 'product1', 'root1');

      await init({
        items: [apiProduct, productPage],
        queryParams: { selectedId: 'product-overview1' },
        content: MOCK_CONTENT,
        apiHasMcp: true,
      });

      expect(apiServiceSpy.details).not.toHaveBeenCalled();
      expect(await harness.getMcpButton()).toBeNull();
    });

    it('should hide Subscribe and preserve API tooling for documentation nested under an API Product', async () => {
      const apiProduct = makeItem('product1', 'API_PRODUCT', 'API Product 1', 0, undefined, 'root1');
      const apiItem = makeItem('api1', 'API', 'API 1', 0, 'product1', 'root1');
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1', 'root1');

      await init({
        items: [apiProduct, apiItem, apiPage],
        queryParams: { selectedId: 'p-api1' },
        content: MOCK_CONTENT,
        apiHasMcp: true,
      });

      expect(routerSpy.navigate).not.toHaveBeenCalled();
      expect(await harness.getSubscribeButton()).toBeNull();
      expect(apiServiceSpy.details).toHaveBeenCalledWith('api-api1');
      expect(await harness.getMcpButton()).not.toBeNull();

      const breadcrumbs = await harness.getBreadcrumbs();
      expect(await breadcrumbs?.getText()).toEqual('Test item/API Product 1/API 1/API 1 Documentation');
    });

    it('should hide the previous API action while product documentation is loading', async () => {
      const apiProduct = fakePortalNavigationApiProduct({
        id: 'product1',
        apiProductId: 'api-product-1',
        title: 'API Product 1',
        rootId: 'root1',
      });
      const productPage = makeItem('product-overview1', 'PAGE', 'Product Overview', 0, 'product1', 'root1');
      const apiItem = makeItem('api1', 'API', 'API 1', 1, undefined, 'root1');
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1', 'root1');

      await init({ items: [apiProduct, productPage, apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT });
      expect(await harness.getSubscribeButton()).not.toBeNull();

      const contentSubject = new Subject<{ content: string; type: string }>();
      navigationServiceSpy.getNavigationItemContent = jest.fn().mockReturnValue(contentSubject.asObservable());

      const tree = await harness.getTreeHarness();
      await tree!.clickItemByTitle('Product Overview');
      await new Promise<void>(resolve => setTimeout(resolve, 0));

      expect(await harness.getSubscribeButton()).toBeNull();

      contentSubject.next({ content: MOCK_CONTENT, type: 'GRAVITEE_MARKDOWN' });
      contentSubject.complete();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(await harness.getSubscribeButton()).not.toBeNull();
    });
  });

  describe('agent', () => {
    it('should redirect an Agent selection to its first readable descendant', async () => {
      const agent = makeItem('agent1', 'AGENT', 'Helpdesk Agent', 0, undefined, 'root1');
      const agentFolder = makeItem('agent-folder1', 'FOLDER', 'Agent Documentation', 0, 'agent1', 'root1');
      const overviewPage = makeItem('agent-overview1', 'PAGE', 'Agent Overview', 0, 'agent-folder1', 'root1');
      const laterPage = makeItem('agent-page2', 'PAGE', 'Later Agent Page', 1, 'agent1', 'root1');

      await init({
        items: [agent, agentFolder, overviewPage, laterPage],
        queryParams: { selectedId: 'agent1' },
        content: MOCK_CONTENT,
      });

      expect(routerSpy.navigate).toHaveBeenCalledWith([], {
        relativeTo: expect.anything(),
        queryParams: { selectedId: 'agent-overview1' },
      });
      expect(navigationServiceSpy.getNavigationItemContent).not.toHaveBeenCalledWith('agent1');
      expect(navigationServiceSpy.getNavigationItemContent).toHaveBeenCalledWith('agent-overview1');

      const breadcrumbs = await harness.getBreadcrumbs();
      expect(await breadcrumbs?.getText()).toEqual('Test item/Helpdesk Agent/Agent Documentation/Agent Overview');
    });

    it('should show a subscription action for agent documentation and navigate to the API flow', async () => {
      const agent = makeItem('agent1', 'AGENT', 'Helpdesk Agent', 0, undefined, 'root1');
      const agentPage = makeItem('agent-overview1', 'PAGE', 'Agent Overview', 0, 'agent1', 'root1');

      await init({ items: [agent, agentPage], queryParams: { selectedId: 'agent-overview1' }, content: MOCK_CONTENT });

      const subscribeButton = await harness.getSubscribeButton();
      expect(subscribeButton).not.toBeNull();
      expect(await subscribeButton!.getText()).toEqual('Subscribe');

      await subscribeButton!.click();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['api', 'api-agent1', 'subscribe'], {
        relativeTo: expect.anything(),
        queryParamsHandling: 'preserve',
      });
    });
  });

  describe('api', () => {
    it('should show subscribe button when api documentation is clicked', async () => {
      const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
      await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT });

      expect(await harness.getSubscribeButton()).not.toBeNull();
    });

    it('should navigate to subscribe page when subscribe button is clicked and user is authenticated', async () => {
      const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
      await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT });

      const subscribeButton = await harness.getSubscribeButton();
      expect(subscribeButton).not.toBeNull();
      await subscribeButton!.click();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['api', 'api-api1', 'subscribe'], {
        relativeTo: expect.anything(),
        queryParamsHandling: 'preserve',
      });
    });

    it('should show disabled button when user is not authenticated', async () => {
      const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
      await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT, isAuthenticated: false });

      const subscribeButton = await harness.getSubscribeButton();
      expect(subscribeButton).not.toBeNull();
      expect(await subscribeButton!.isDisabled()).toBe(true);
    });

    it('should not show MCP button when API has no MCP', async () => {
      const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
      await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT });

      expect(await harness.getMcpButton()).toBeNull();
    });

    it('should open MCP drawer with tools when MCP button clicked', async () => {
      const apiItem = makeItem('api1', 'API', 'API 1', 0, undefined);
      const apiPage = makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1');
      await init({ items: [apiItem, apiPage], queryParams: { selectedId: 'p-api1' }, content: MOCK_CONTENT, apiHasMcp: true });

      fixture.detectChanges();
      await fixture.whenStable();

      const mcpButton = await harness.getMcpButton();
      expect(mcpButton).not.toBeNull();
      await mcpButton!.click();
      fixture.detectChanges();

      expect(await harness.getApiTabToolsHarness()).not.toBeNull();
    });
  });

  describe('agent chat', () => {
    const AGENT_ACCESS: AgentSubscriptionAccess = { subscriptionId: 'sub-1', apiKey: 'key-1', applicationName: 'My App' };

    // jsdom ships no streams API, so the response body is faked down to what the store reads.
    const answeringGateway = () => {
      const encoded = new TextEncoder().encode(
        'data: {"result":{"kind":"status-update","contextId":"ctx-1","status":{"state":"completed"}}}\n\n',
      );
      let sent = false;
      return jest.fn().mockResolvedValue({
        ok: true,
        status: 200,
        body: {
          getReader: () => ({
            read: () => Promise.resolve(sent ? { done: true, value: undefined } : ((sent = true), { done: false, value: encoded })),
          }),
        },
      } as unknown as Response);
    };

    beforeEach(() => {
      globalThis.fetch = answeringGateway() as unknown as typeof fetch;
    });

    const initAgentPage = async (params: {
      apiType?: ApiType;
      agentAccess?: AgentSubscriptionAccess | null;
      isAuthenticated?: boolean;
      apiEntrypoints?: string[];
    }) => {
      const agentItem = makeItem('agent1', 'AGENT', 'Agent 1', 0, undefined);
      const agentPage = makeItem('p-agent1', 'PAGE', 'Agent 1 Documentation', 0, 'agent1');
      await init({ items: [agentItem, agentPage], queryParams: { selectedId: 'p-agent1' }, content: MOCK_CONTENT, ...params });
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
    };

    const openChat = async () => {
      await (await harness.getChatButton())!.click();
      fixture.detectChanges();
    };

    const closeChat = async () => {
      await (await harness.getSidePanel())!.clickClose();
      fixture.detectChanges();
    };

    it('should not show the chat button for an api that is not an agent', async () => {
      await initAgentPage({ apiType: 'PROXY', agentAccess: AGENT_ACCESS });

      expect(await harness.getChatButton()).toBeNull();
    });

    it('should not show the chat button to a viewer with no usable subscription', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', agentAccess: null });

      expect(await harness.getChatButton()).toBeNull();
    });

    it('should show the chat button to a subscriber of an agent', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', agentAccess: AGENT_ACCESS });

      expect(await harness.getChatButton()).not.toBeNull();
    });

    it('should open the chat panel when the chat button is clicked', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', agentAccess: AGENT_ACCESS });

      expect(await harness.getAgentChat()).toBeNull();

      await openChat();

      expect(await harness.getAgentChat()).not.toBeNull();
    });

    it('should not offer the chat for an agent that publishes no gateway entrypoint', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', agentAccess: AGENT_ACCESS, apiEntrypoints: [] });

      expect(await harness.getChatButton()).toBeNull();
    });

    it('should keep the conversation when the panel is closed and reopened', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', agentAccess: AGENT_ACCESS });
      await openChat();

      const chat = (await harness.getAgentChat())!;
      await chat.type('what happened?');
      await chat.clickSend();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await chat.plainTurnTexts()).toContain('what happened?');

      await closeChat();
      expect(await harness.getAgentChat()).toBeNull();

      await openChat();

      expect(await (await harness.getAgentChat())!.plainTurnTexts()).toContain('what happened?');
    });

    it('should not look up a subscription for an anonymous viewer', async () => {
      await initAgentPage({ apiType: 'A2A_PROXY', isAuthenticated: false });

      expect(agentSubscriptionServiceSpy.forAgent).not.toHaveBeenCalled();
      expect(await harness.getChatButton()).toBeNull();
    });

    it('should not look up a subscription on a page that is not an agent', async () => {
      await initAgentPage({ apiType: 'PROXY' });

      expect(agentSubscriptionServiceSpy.forAgent).not.toHaveBeenCalled();
    });
  });
});
