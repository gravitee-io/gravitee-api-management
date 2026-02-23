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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { DocumentationFolderComponent } from './documentation-folder.component';
import { DocumentationFolderComponentHarness } from './documentation-folder.component.harness';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { makeItem, MOCK_ITEMS } from '../../../../mocks/portal-navigation-item.mocks';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('DocumentationFolderComponent', () => {
  let fixture: ComponentFixture<DocumentationFolderComponent>;
  let harness: DocumentationFolderComponentHarness;
  let navigationServiceSpy: PortalNavigationItemsService;
  let routerSpy: jest.Mocked<Router>;
  let queryParamsSubject: BehaviorSubject<{ pageId?: string }>;

  const MOCK_ITEM = { title: 'Test item' };
  const MOCK_CHILDREN = MOCK_ITEMS;
  const MOCK_CONTENT = 'MOCK_CONTENT';

  const gmdViewerContent = (content: string) => `<p>${content}</p>\n`;

  const init = async (
    params: Partial<{ queryParams: { pageId?: string }; items: PortalNavigationItem[]; content: string }> = {
      queryParams: { pageId: 'p1' },
      items: MOCK_CHILDREN,
      content: MOCK_CONTENT,
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

    await TestBed.configureTestingModule({
      imports: [DocumentationFolderComponent, MatIconTestingModule, AppTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParams: queryParamsSubject.asObservable() } },
        { provide: Router, useValue: routerSpy },
        { provide: PortalNavigationItemsService, useValue: navigationServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentationFolderComponent);
    fixture.componentRef.setInput('navItem', MOCK_ITEM);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationFolderComponentHarness);
  };

  describe('initial load', () => {
    describe('with content', () => {
      it('should display tree, content and breadcrumbs', async () => {
        await init();

        const tree = await harness.getTreeHarness();
        expect(tree).not.toBeNull();
        expect(await tree!.getAllItemTitles()).toEqual(['Folder 1', 'Folder 2', 'Page 1', 'Page 2', 'Page 3']);

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
          queryParams: { pageId: 'p1' },
        });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('Page 1');

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Folder 2/Page 1');
      });

      it('should select page when pageId is provided', async () => {
        await init({ items: MOCK_CHILDREN, queryParams: { pageId: 'p2' }, content: MOCK_CONTENT });

        const treeHarness = await harness.getTreeHarness();
        const selectedItem = await treeHarness?.getSelectedItem();
        expect(selectedItem).toBeDefined();
        expect(await selectedItem!.getText()).toEqual('Page 2');

        const breadcrumbs = await harness.getBreadcrumbs();
        expect(await breadcrumbs?.getText()).toEqual('Test item/Folder 1/Page 2');
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
        await init({ items: MOCK_CHILDREN, queryParams: { pageId: 'p999' }, content: MOCK_CONTENT });
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/404']);
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
        queryParams: { pageId: 'p2' },
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
});
