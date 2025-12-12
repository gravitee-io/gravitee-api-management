/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Params, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs/internal/observable/of';

import { DocumentationFolderComponent } from './documentation-folder.component';
import { DocumentationFolderComponentHarness } from './documentation-folder.component.harness';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import {
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
} from '../../../../entities/portal-navigation/portal-navigation-item.fixture';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';

describe('DocumentationFolderComponent', () => {
  let fixture: ComponentFixture<DocumentationFolderComponent>;
  let harness: DocumentationFolderComponentHarness;
  let navigationService: PortalNavigationItemsService;
  let activatedRoute: ActivatedRoute;
  let routerSpy: jest.SpyInstance;

  const makeItem = (
    id: string,
    type: 'PAGE' | 'FOLDER' | 'LINK',
    title: string,
    order?: number,
    parentId?: string | null,
  ): PortalNavigationItem => {
    switch (type) {
      case 'FOLDER':
        return fakePortalNavigationFolder({ id, title, order, parentId });
      case 'LINK':
        return fakePortalNavigationLink({ id, title, order, parentId });
      case 'PAGE':
      default:
        return fakePortalNavigationPage({ id, title, order, parentId });
    }
  };

  const MOCK_ITEMS = [
    makeItem('f1', 'FOLDER', 'Folder 1', 0),
    makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
    makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
    makeItem('p2', 'PAGE', 'Page 2', 1, 'f1'),
    makeItem('p3', 'PAGE', 'Page 3', 1),
  ];
  const MOCK_CONTENT = 'MOCK_CONTENT';

  const gmdViewerContent = (content: string) => `<p>${content}</p>\n`;

  const init = async (
    params: Partial<{ queryParams: { pageId?: string }; items: PortalNavigationItem[]; content: string }> = {
      queryParams: { pageId: 'p2' },
      items: MOCK_ITEMS,
      content: MOCK_CONTENT,
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [DocumentationFolderComponent, MatIconTestingModule, HttpClientTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    navigationService = TestBed.inject(PortalNavigationItemsService);
    activatedRoute = TestBed.inject(ActivatedRoute);
    activatedRoute.queryParams = of(params.queryParams as unknown as Params);
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

    jest.spyOn(navigationService, 'getNavigationItems').mockReturnValue(of(params.items as unknown as PortalNavigationItem[]));
    jest.spyOn(navigationService, 'getNavigationItemContent').mockReturnValueOnce(of(params.content!));

    fixture = TestBed.createComponent(DocumentationFolderComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationFolderComponentHarness);
    fixture.componentRef.setInput('navItem', {});

    fixture.detectChanges();
  };

  describe('initial load', () => {
    describe('with empty states', () => {
      it('should not display tree', async () => {
        await init({ items: [], content: '', queryParams: {} });

        const tree = await harness.getTreeHarness();
        expect(tree).toBeNull();

        const emptyState = await harness.getItemsEmptyState();
        expect(await emptyState?.getText()).toEqual('No items to show');
      });

      it('should not display content', async () => {
        await init({ content: '' });

        const viewer = await harness.getGmdViewer();
        expect(viewer).toBeNull();

        const emptyState = await harness.getContentEmptyState();
        expect(await emptyState?.getText()).toEqual('No content to show');
      });
    });

    describe('with content', () => {
      it('should display tree', async () => {
        await init();

        const tree = await harness.getTreeHarness();
        expect(tree).not.toBeNull();

        expect(await tree!.getAllItemTitles()).toEqual(['Folder 1', 'Folder 2', 'Page 1', 'Page 2', 'Page 3']);

        const emptyState = await harness.getItemsEmptyState();
        expect(await emptyState?.getText()).toBeUndefined();
      });

      it('should display content', async () => {
        await init();

        const viewer = await harness.getGmdViewer();
        expect(viewer).not.toBeNull();

        expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));

        const emptyState = await harness.getContentEmptyState();
        expect(await emptyState?.getText()).toBeUndefined();
      });

      it('should select first page when no pageId provided', async () => {
        await init({ items: MOCK_ITEMS, queryParams: {}, content: MOCK_CONTENT });

        expect(routerSpy).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: { pageId: 'p1' },
        });
      });

      it('should select page by pageId', async () => {
        await init({ items: MOCK_ITEMS, queryParams: { pageId: 'p1' }, content: MOCK_CONTENT });

        expect(routerSpy).toHaveBeenCalledWith([], {
          relativeTo: expect.anything(),
          queryParams: { pageId: 'p1' },
        });
      });
    });
  });

  describe('item selection', () => {
    it('should navigate on page click', async () => {
      await init();

      const tree = await harness.getTreeHarness();
      expect(tree).not.toBeNull();

      const viewer = await harness.getGmdViewer();
      expect(viewer).not.toBeNull();
      expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));

      await tree!.clickItemByTitle('Page 1');

      expect(routerSpy).toHaveBeenCalledWith([], {
        relativeTo: expect.anything(),
        queryParams: { pageId: 'p1' },
      });
    });
  });

  it('should collapse item on folder click', async () => {
    await init();

    const tree = await harness.getTreeHarness();
    expect(tree).not.toBeNull();

    const viewer = await harness.getGmdViewer();
    expect(viewer).not.toBeNull();
    expect(await viewer!.getRenderedHtml()).toEqual(gmdViewerContent(MOCK_CONTENT));

    await tree!.clickItemByTitle('Folder 1');

    expect(routerSpy).not.toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: { pageId: 'f1' },
    });

    const item = await tree!.getFolderByTitle('Folder 1');
    expect(item?.expanded).toEqual(false);
  });
});
