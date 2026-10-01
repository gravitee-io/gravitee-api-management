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
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Observable, of } from 'rxjs';

import { TreeRowHarness } from './tree/tree-node.component.harness';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { fakePortalNavigationFolder } from '../../../../entities/portal-navigation/portal-navigation-item.fixture';
import { makeItem } from '../../../../mocks/portal-navigation-item.mocks';
import { ApiService } from '../../../../services/api.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';
import { documentationResolver } from '../../resolvers/documentation.resolver';
import { DocumentationComponent } from '../documentation.component';

describe('Documentation folder tree expansion', () => {
  // folder > product > api > page (the depth-first first page), product > other-api > other-page,
  // plus a standalone API and a product holding no page at all.
  const items = [
    makeItem('folder', 'FOLDER', 'Folder', 0),
    makeItem('product', 'API_PRODUCT', 'Product', 0, 'folder'),
    makeItem('api', 'API', 'API', 0, 'product'),
    makeItem('page', 'PAGE', 'First page', 0, 'api'),
    makeItem('other-api', 'API', 'Other API', 1, 'product'),
    makeItem('other-page', 'PAGE', 'Other page', 0, 'other-api'),
    makeItem('standalone-api', 'API', 'Standalone API', 1),
    makeItem('standalone-page', 'PAGE', 'Standalone page', 0, 'standalone-api'),
    makeItem('empty-product', 'API_PRODUCT', 'Empty product', 2),
  ];

  let routeHarness: RouterTestingHarness;
  let router: Router;
  let navigationService: { getNavigationItems: jest.Mock; getNavigationItemContent: jest.Mock };

  const init = async (params: { url?: string; children?: PortalNavigationItem[] } = {}) => {
    navigationService = {
      getNavigationItems: jest.fn().mockReturnValue(of(params.children ?? items) as Observable<PortalNavigationItem[]>),
      getNavigationItemContent: jest.fn().mockReturnValue(of({ type: 'GRAVITEE_MARKDOWN', content: 'Documentation content' })),
    };

    await TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [
        provideRouter(
          [
            {
              path: 'documentation/:navId',
              resolve: { navItem: documentationResolver },
              children: [{ path: '', component: DocumentationComponent }],
            },
          ],
          withComponentInputBinding(),
        ),
        {
          provide: PortalNavigationItemsService,
          useValue: { ...navigationService, topNavbarItems: signal([fakePortalNavigationFolder({ id: 'root' })]) },
        },
        { provide: ApiService, useValue: { details: () => of({ id: 'api', name: 'API' }) } },
        { provide: CurrentUserService, useValue: { isUserAuthenticated: signal(true) } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    routeHarness = await RouterTestingHarness.create(params.url ?? '/documentation/root');
    await settle();
  };

  // The folder resolves children, redirects to a page, then loads that page: let every hop land.
  const settle = async () => {
    for (let i = 0; i < 3; i++) {
      routeHarness.detectChanges();
      await jest.runOnlyPendingTimersAsync();
      await routeHarness.fixture.whenStable();
    }
    routeHarness.detectChanges();
  };

  const row = (text: string) => TestbedHarnessEnvironment.loader(routeHarness.fixture).getHarness(TreeRowHarness.with({ text }));
  const expanded = (text: string) => row(text).then(found => found.getAriaExpanded());
  const click = async (text: string) => {
    await (await row(text)).click();
    await settle();
  };
  const navigate = async (url: string) => {
    await routeHarness.navigateByUrl(url);
    await settle();
  };

  it('should expand only the path to the depth-first first page on top bar entry', async () => {
    await init();

    expect(router.url).toBe('/documentation/root?selectedId=page');
    expect(navigationService.getNavigationItemContent).toHaveBeenCalledWith('page');
    for (const label of ['Folder', 'Product', 'API']) {
      expect(await expanded(label)).toBe('true');
    }
    for (const label of ['Other API', 'Standalone API', 'Empty product']) {
      expect(await expanded(label)).toBe('false');
    }
  });

  it('should expand only the path to the first page of an API Product opened from the catalog', async () => {
    await init({ url: '/documentation/root?selectedId=product' });

    expect(router.url).toBe('/documentation/root?selectedId=page');
    for (const label of ['Folder', 'Product', 'API']) {
      expect(await expanded(label)).toBe('true');
    }
    expect(await expanded('Other API')).toBe('false');
    expect(await expanded('Standalone API')).toBe('false');
  });

  it('should expand only the path to the first page of an API opened from the catalog', async () => {
    await init({ url: '/documentation/root?selectedId=standalone-api' });

    expect(router.url).toBe('/documentation/root?selectedId=standalone-page');
    expect(await expanded('Standalone API')).toBe('true');
    expect(await expanded('Folder')).toBe('false');
    expect(await expanded('Empty product')).toBe('false');
  });

  it('should expand only the path to a page reached by direct link', async () => {
    await init({ url: '/documentation/root?selectedId=other-page' });

    expect(router.url).toBe('/documentation/root?selectedId=other-page');
    for (const label of ['Folder', 'Product', 'Other API']) {
      expect(await expanded(label)).toBe('true');
    }
    expect(await expanded('API')).toBe('false');
    expect(await expanded('Standalone API')).toBe('false');
  });

  it('should keep branches the user opened by hand while browsing the tree', async () => {
    await init();

    await click('Standalone API');
    expect(await expanded('Standalone API')).toBe('true');

    await click('Other API');
    await click('Other page');

    expect(router.url).toBe('/documentation/root?selectedId=other-page');
    expect(await expanded('Standalone API')).toBe('true');
    expect(await expanded('Other API')).toBe('true');
  });

  it('should keep a branch the user closed by hand while browsing the tree', async () => {
    await init();

    await click('Other API');
    await click('Other page');
    await click('API');

    expect(await expanded('API')).toBe('false');
    expect(router.url).toBe('/documentation/root?selectedId=other-page');
  });

  it('should collapse branches the user opened by hand when re-entering from the top bar', async () => {
    await init();

    await click('Standalone API');
    expect(await expanded('Standalone API')).toBe('true');

    await navigate('/documentation/root');

    expect(router.url).toBe('/documentation/root?selectedId=page');
    expect(await expanded('Standalone API')).toBe('false');
    for (const label of ['Folder', 'Product', 'API']) {
      expect(await expanded(label)).toBe('true');
    }
  });

  it('should expand an API Product holding no page without falling back to an unrelated page', async () => {
    await init({ url: '/documentation/root?selectedId=empty-product' });

    expect(router.url).toBe('/documentation/root?selectedId=empty-product');
    expect(await expanded('Empty product')).toBe('true');
    expect(await expanded('Folder')).toBe('false');
    expect(navigationService.getNavigationItemContent).not.toHaveBeenCalled();
  });

  it('should collapse every branch when the folder holds no page at all', async () => {
    await init({ children: items.filter(item => item.type !== 'PAGE') });

    expect(router.url).toBe('/documentation/root');
    for (const label of ['Folder', 'Product', 'API', 'Other API', 'Standalone API', 'Empty product']) {
      expect(await expanded(label)).toBe('false');
    }
    expect(navigationService.getNavigationItemContent).not.toHaveBeenCalled();
  });
});
