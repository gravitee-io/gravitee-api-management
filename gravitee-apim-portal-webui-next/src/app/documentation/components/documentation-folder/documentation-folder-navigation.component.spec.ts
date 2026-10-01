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
import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, NavigationSkipped, provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom, Observable, of, Subject, take } from 'rxjs';

import { DocumentationFolderComponentHarness } from './documentation-folder.component.harness';
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

describe('Documentation folder navigation', () => {
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

  const init = async (params: { url?: string; children?: (navId: string) => Observable<PortalNavigationItem[]> } = {}) => {
    navigationService = {
      getNavigationItems: jest.fn().mockImplementation((_, __, navId: string) => params.children?.(navId) ?? of(items)),
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
            { path: 'blocked', component: DocumentationComponent, canActivate: [() => false] },
          ],
          withComponentInputBinding(),
        ),
        provideLocationMocks(),
        {
          provide: PortalNavigationItemsService,
          useValue: {
            ...navigationService,
            topNavbarItems: signal([fakePortalNavigationFolder({ id: 'root' }), fakePortalNavigationFolder({ id: 'second-root' })]),
          },
        },
        { provide: ApiService, useValue: { details: () => of({ id: 'api', name: 'API' }) } },
        { provide: CurrentUserService, useValue: { isUserAuthenticated: signal(true) } },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
    router.setUpLocationChangeListener();
    routeHarness = await RouterTestingHarness.create(params.url ?? '/documentation/root');
    await settle();
  };

  const settle = async () => {
    routeHarness.detectChanges();
    await jest.runOnlyPendingTimersAsync();
    await routeHarness.fixture.whenStable();
    routeHarness.detectChanges();
    await jest.runOnlyPendingTimersAsync();
    await routeHarness.fixture.whenStable();
    routeHarness.detectChanges();
  };

  const row = (text: string) => TestbedHarnessEnvironment.loader(routeHarness.fixture).getHarness(TreeRowHarness.with({ text }));
  const expanded = async (text: string) => (await row(text)).getAriaExpanded();
  const navigate = async (url: string) => {
    await routeHarness.navigateByUrl(url);
    await settle();
  };
  const travelHistory = async (direction: 'back' | 'forward') => {
    const completed = firstValueFrom(
      router.events.pipe(
        filter(event => event instanceof NavigationEnd || event instanceof NavigationSkipped),
        take(1),
      ),
    );
    TestBed.inject(Location)[direction]();
    await completed;
    await settle();
  };

  it('should reveal only the automatic first-page path after menu entry', async () => {
    await init();
    expect(router.url).toBe('/documentation/root?selectedId=page');
    expect(navigationService.getNavigationItemContent).toHaveBeenCalledWith('page');
    for (const label of ['Folder', 'Product', 'API']) {
      expect(await expanded(label)).toBe('true');
    }
    for (const label of ['Other API', 'Standalone API', 'Empty product']) {
      expect(await expanded(label)).toBe('false');
    }
    const folder = await TestbedHarnessEnvironment.loader(routeHarness.fixture).getHarness(DocumentationFolderComponentHarness);
    const selectedItem = await (await folder.getTreeHarness())!.getSelectedItem();
    expect(await selectedItem!.getText()).toBe('First page');
    expect(await (await folder.getGmdViewer())!.getRenderedHtml()).toContain('Documentation content');
  });

  it('should collapse unrelated branches when the automatic first page is at the root', async () => {
    await init({
      url: '/documentation/root?selectedId=folder',
      children: () =>
        of([
          makeItem('root-page', 'PAGE', 'Root page', 0),
          makeItem('folder', 'FOLDER', 'Folder', 1),
          makeItem('nested-page', 'PAGE', 'Nested page', 0, 'folder'),
        ]),
    });
    expect(await expanded('Folder')).toBe('true');
    await navigate('/documentation/root');
    expect(router.url).toBe('/documentation/root?selectedId=root-page');
    expect(navigationService.getNavigationItemContent).toHaveBeenLastCalledWith('root-page');
    expect(await expanded('Folder')).toBe('false');
  });

  it('should collapse all branches on menu re-entry when no page is available', async () => {
    await init({ children: () => of(items.filter(item => item.type !== 'PAGE')) });
    expect(await expanded('Folder')).toBe('false');
    await (await row('Folder')).click();
    expect(await expanded('Folder')).toBe('true');
    await navigate('/documentation/root');
    for (const label of ['Folder', 'Product', 'API', 'Other API', 'Standalone API', 'Empty product']) {
      expect(await expanded(label)).toBe('false');
    }
    expect(router.url).toBe('/documentation/root');
    expect(navigationService.getNavigationItemContent).not.toHaveBeenCalled();
  });

  it('should allow the automatic first-page branch to stay manually collapsed after menu entry', async () => {
    await init();
    expect(await expanded('Folder')).toBe('true');
    await (await row('Folder')).click();
    await settle();
    expect(await expanded('Folder')).toBe('false');
    expect(router.url).toBe('/documentation/root?selectedId=page');
    const folder = await TestbedHarnessEnvironment.loader(routeHarness.fixture).getHarness(DocumentationFolderComponentHarness);
    expect(await (await folder.getGmdViewer())!.getRenderedHtml()).toContain('Documentation content');
  });

  it('should focus only the product first-page path on catalog entry', async () => {
    await init({ url: '/documentation/root?selectedId=product' });
    for (const label of ['Folder', 'Product', 'API']) expect(await expanded(label)).toBe('true');
    expect(await expanded('Other API')).toBe('false');
    expect(await expanded('Standalone API')).toBe('false');
    expect(router.url).toBe('/documentation/root?selectedId=page');
  });

  it('should refocus the first-page path on menu re-entry and focus a new catalog selection without reloading children', async () => {
    await init({ url: '/documentation/root?selectedId=product' });
    await (await row('Standalone API')).click();
    await navigate('/documentation/root?selectedId=other-api');
    expect(await expanded('API')).toBe('false');
    expect(await expanded('Other API')).toBe('true');
    expect(await expanded('Standalone API')).toBe('false');
    await (await row('Standalone API')).click();
    await navigate('/documentation/root');
    for (const label of ['Folder', 'Product', 'API']) {
      expect(await expanded(label)).toBe('true');
    }
    expect(await expanded('Other API')).toBe('false');
    expect(await expanded('Standalone API')).toBe('false');
    expect(router.url).toBe('/documentation/root?selectedId=page');
    expect(navigationService.getNavigationItems).toHaveBeenCalledTimes(1);
  });

  it('should preserve manual choices on tree page clicks including a click on the already selected page', async () => {
    await init({ url: '/documentation/root?selectedId=product' });
    await (await row('Other API')).click();
    await (await row('Other page')).click();
    await settle();
    expect(await expanded('API')).toBe('true');
    expect(await expanded('Other API')).toBe('true');
    await (await row('API')).click();
    await (await row('Other page')).click();
    await settle();
    expect(await expanded('API')).toBe('false');
    expect(await expanded('Other API')).toBe('true');
  });

  it('should reveal a directly linked page and leave unrelated branches collapsed', async () => {
    await init({ url: '/documentation/root?selectedId=other-page' });
    expect(await expanded('Folder')).toBe('true');
    expect(await expanded('Product')).toBe('true');
    expect(await expanded('Other API')).toBe('true');
    expect(await expanded('API')).toBe('false');
  });

  it('should reveal the page path on Back and Forward without replaying the tree-click marker', async () => {
    await init({ url: '/documentation/root?selectedId=page' });
    await (await row('Other API')).click();
    await (await row('Other page')).click();
    await settle();
    await (await row('API')).click();
    await travelHistory('back');
    expect(router.url).toBe('/documentation/root?selectedId=page');
    expect(await expanded('API')).toBe('true');
    await (await row('Other API')).click();
    await travelHistory('forward');
    expect(router.url).toBe('/documentation/root?selectedId=other-page');
    expect(await expanded('Other API')).toBe('true');
  });

  it('should reveal a same-URL external entry even if the selected branch was manually closed', async () => {
    await init({ url: '/documentation/root?selectedId=page' });
    await (await row('Folder')).click();
    await navigate('/documentation/root?selectedId=page');
    expect(await expanded('Folder')).toBe('true');
  });

  it('should focus an empty product without falling back to an unrelated page', async () => {
    await init({ url: '/documentation/root?selectedId=empty-product' });
    expect(await expanded('Empty product')).toBe('true');
    expect(await expanded('Folder')).toBe('false');
    expect(navigationService.getNavigationItemContent).not.toHaveBeenCalled();
    expect(router.url).toBe('/documentation/root?selectedId=empty-product');
  });

  it('should ignore a canceled navigation', async () => {
    await init({ url: '/documentation/root?selectedId=page' });
    await (await row('Folder')).click();
    await navigate('/blocked');
    expect(await expanded('Folder')).toBe('false');
  });

  it('should discard a previous root response when navigating while children are loading', async () => {
    const pending = new Subject<PortalNavigationItem[]>();
    await init({ url: '/documentation/root?selectedId=product', children: navId => (navId === 'root' ? pending : of(items)) });
    await navigate('/documentation/second-root?selectedId=other-api');
    pending.next([makeItem('stale-api', 'API', 'Stale API', 0)]);
    pending.complete();
    await settle();
    expect(router.url).toBe('/documentation/second-root?selectedId=other-page');
    expect(await expanded('Other API')).toBe('true');
    expect(await expanded('API')).toBe('false');
    expect(navigationService.getNavigationItems).toHaveBeenCalledTimes(2);
  });
});
