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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiDocumentationV4Component } from './api-documentation-v4.component';
import { ApiDocumentationV4Module } from './api-documentation-v4.module';
import { ApiDocumentationV4EmptyStateHarness } from './components/documentation-empty-state/api-documentation-v4-empty-state.harness';
import { ApiDocumentationV4ListNavigationHeaderHarness } from './components/documentation-list-navigation-header/api-documentation-v4-list-navigation-header.harness';
import { ApiDocumentationV4EditFolderDialogHarness } from './dialog/documentation-edit-folder-dialog/api-documentation-v4-edit-folder-dialog.harness';
import { ApiDocumentationV4PagesListHarness } from './documentation-pages-list/api-documentation-v4-pages-list.harness';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { Breadcrumb, Page } from '../../../entities/management-api-v2/documentation/page';
import { fakeFolder, fakeMarkdown } from '../../../entities/management-api-v2/documentation/page.fixture';

describe('ApiDocumentationV4', () => {
  let fixture: ComponentFixture<ApiDocumentationV4Component>;
  let harnessLoader: HarnessLoader;
  const fakeUiRouter = { go: jest.fn() };
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;

  const init = async (pages: Page[], breadcrumb: Breadcrumb[], parentId = 'ROOT') => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4Component],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, parentId } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4Component);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    expectGetPages(pages, breadcrumb, parentId);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('API does not have pages', () => {
    beforeEach(async () => await init([], []));

    it('should show empty state when no documentation for API', async () => {
      const emptyState = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
      expect(emptyState).toBeDefined();
    });

    it('should navigate to create page', async () => {
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
      await headerHarness.clickAddNewPage();

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4-create', {
        apiId: API_ID,
        parentId: 'ROOT',
      });
    });
  });

  describe('Breadcrumb', () => {
    it('should show breadcrumb items and navigate to folder', async () => {
      await init(
        [],
        [
          { name: 'level 1', id: 'level-1', position: 1 },
          { name: 'level 2', id: 'level-2', position: 2 },
        ],
      );
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      expect(await headerHarness.getBreadcrumb()).toEqual('Home>level 1>level 2');

      await headerHarness.clickOnBreadcrumbItem('level 1');
      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4', { parentId: 'level-1' }, { reload: true });
    });

    it('should navigate to root', async () => {
      await init([], [{ name: 'level 1', id: 'level-1', position: 1 }]);
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickOnBreadcrumbItem('Home');
      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4', { parentId: 'ROOT' }, { reload: true });
    });
  });

  describe('API has pages', () => {
    it('should show list of folders', async () => {
      await init(
        [
          fakeFolder({ name: 'my first folder', visibility: 'PUBLIC', hidden: false }),
          fakeFolder({ name: 'my private folder', visibility: 'PRIVATE', hidden: true }),
        ],
        [],
      );

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      expect(await pageListHarness.getNameByRowIndex(0)).toEqual('my first folder');
      expect(await pageListHarness.getNameByRowIndex(1)).toEqual('my private folder');
      expect(await pageListHarness.getVisibilityByRowIndex(0)).toEqual('Public');
      expect(await pageListHarness.getVisibilityByRowIndex(1)).toEqual('Private');
      expect(await pageListHarness.getStatusByRowIndex(0)).toEqual('');
      expect(await pageListHarness.getStatusByRowIndex(1)).toEqual('Hidden');
    });

    it('should navigate to create page', async () => {
      await init([fakeFolder({ name: 'my first folder', visibility: 'PUBLIC' })], []);
      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      await pageListHarness.clickAddNewPage();

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4-create', {
        apiId: API_ID,
        parentId: 'ROOT',
      });
    });

    it('should navigate to folder when click in the list', async () => {
      await init([fakeFolder({ name: 'my first folder', id: 'my-first-folder', visibility: 'PUBLIC' })], []);
      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const nameDiv = await pageListHarness.getNameDivByRowIndex(0);
      await nameDiv.host().then((host) => host.click());

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4', { parentId: 'my-first-folder' }, { reload: true });
    });
  });

  describe('Actions', () => {
    it('should show dialog to create folder', async () => {
      await init([], []);
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickAddNewFolder();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiDocumentationV4EditFolderDialogHarness,
      );
      await dialogHarness.setName('folder');
      await dialogHarness.selectVisibility('PRIVATE');
      await dialogHarness.clickOnSave();

      const page: Page = { type: 'FOLDER', name: 'folder', visibility: 'PRIVATE', published: false };
      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
      });
      req.flush(page);
      expect(req.request.body).toEqual({
        type: 'FOLDER',
        name: 'folder',
        visibility: 'PRIVATE',
        parentId: 'ROOT',
      });

      page.published = true;

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
        })
        .flush(page);

      expectGetPages([page], []);
    });
    it('should create new folder under the current folder', async () => {
      await init([], [], 'parent-folder-id');
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickAddNewFolder();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiDocumentationV4EditFolderDialogHarness,
      );
      await dialogHarness.setName('subfolder');
      await dialogHarness.clickOnSave();

      const page: Page = { type: 'FOLDER', name: 'subfolder', visibility: 'PUBLIC', parentId: 'parent-folder-id', published: false };
      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
      });
      req.flush(page);
      expect(req.request.body).toEqual({
        type: 'FOLDER',
        name: 'subfolder',
        visibility: 'PUBLIC',
        parentId: 'parent-folder-id',
      });

      page.published = true;

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
        })
        .flush(page);

      expectGetPages([page], [], 'parent-folder-id');
    });

    it('should update folder', async () => {
      const ID = 'page-id';
      const FOLDER = fakeFolder({ id: ID, name: 'my first folder', visibility: 'PUBLIC', order: 1 });
      const OTHER_FOLDER = fakeFolder({ id: 'top-folder', name: 'my top folder', visibility: 'PUBLIC', order: 0 });
      await init([FOLDER, OTHER_FOLDER], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const editFolderButton = await pageListHarness.getEditFolderButtonByRowIndex(0);
      await editFolderButton.click();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiDocumentationV4EditFolderDialogHarness,
      );
      await dialogHarness.setName('folder');
      await dialogHarness.selectVisibility('PRIVATE');
      await dialogHarness.clickOnSave();

      const page: Page = { type: 'FOLDER', name: 'folder', visibility: 'PRIVATE' };
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${ID}`,
      });
      req.flush(page);
      expect(req.request.body).toEqual({
        ...FOLDER,
        name: 'folder',
        visibility: 'PRIVATE',
      });

      expectGetPages([page], []);
    });

    it('should publish page', async () => {
      const ID = 'page-id';
      const PAGE = fakeMarkdown({ id: ID, name: 'my first folder', visibility: 'PUBLIC', published: false });
      await init([PAGE], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const publishPageBtn = await pageListHarness.getPublishPageButtonByRowIndex(0);
      await publishPageBtn.click();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${ID}/_publish`,
        })
        .flush({ ...PAGE, published: true });

      expectGetPages([{ ...PAGE, published: true }], []);
    });

    it('should not publish page that is already published', async () => {
      const ID = 'page-id';
      const PAGE = fakeMarkdown({ id: ID, name: 'my first folder', visibility: 'PUBLIC', published: true });
      await init([PAGE], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      await pageListHarness.getPublishPageButtonByRowIndex(0).then((found) => {
        expect(found).toBeFalsy();
      });
    });

    it('should move folder down', async () => {
      const firstPage = fakeFolder({ id: 'first-id', name: 'my first folder', visibility: 'PUBLIC', order: 0 });
      const secondPage = fakeFolder({ id: 'second-id', name: 'my private folder', visibility: 'PRIVATE', order: 1 });
      await init([firstPage, secondPage], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const firstRowButton = await pageListHarness.getMovePageDownButtonByRowIndex(0);
      const secondRowButton = await pageListHarness.getMovePageDownButtonByRowIndex(1);

      expect(await firstRowButton.isDisabled()).toEqual(false);
      expect(await secondRowButton.isDisabled()).toEqual(true);

      await firstRowButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${firstPage.id}`,
      });
      expect(req.request.body).toEqual({
        ...firstPage,
        order: 1,
      });
      req.flush(firstPage);
      expectGetPages([firstPage, secondPage], []);
    });

    it('should unpublish page', async () => {
      const ID = 'page-id';
      const PAGE = fakeMarkdown({ id: ID, name: 'my first page', type: 'MARKDOWN', published: true, generalConditions: false });
      await init([PAGE], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const unpublishPageBtn = await pageListHarness.getUnpublishPageButtonByRowIndex(0);
      await unpublishPageBtn.click();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${ID}/_unpublish`,
        })
        .flush({ ...PAGE, published: false });

      expectGetPages([{ ...PAGE, published: false }], []);
    });

    it('should not unpublish page if used as general conditions', async () => {
      const ID = 'page-id';
      const PAGE = fakeMarkdown({ id: ID, name: 'my first page', type: 'MARKDOWN', published: true, generalConditions: true });
      await init([PAGE], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      expect(await pageListHarness.getUnpublishPageButtonByRowIndex(0).then((btn) => btn.isDisabled())).toEqual(true);
    });

    it('should not unpublish folder', async () => {
      const ID = 'page-id';
      const PAGE = fakeMarkdown({ id: ID, name: 'my first page', type: 'FOLDER', published: true });
      await init([PAGE], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      await pageListHarness.getUnpublishPageButtonByRowIndex(0).then((btn) => expect(btn).toBeFalsy());
    });

    it('should move page up', async () => {
      const firstPage = fakeMarkdown({ id: 'first-id', name: 'my first folder', visibility: 'PUBLIC', order: 0, content: '1' });
      const secondPage = fakeMarkdown({ id: 'second-id', name: 'my private folder', visibility: 'PRIVATE', order: 1, content: '2' });
      await init([firstPage, secondPage], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const firstRowButton = await pageListHarness.getMovePageUpButtonByRowIndex(0);
      const secondRowButton = await pageListHarness.getMovePageUpButtonByRowIndex(1);

      expect(await firstRowButton.isDisabled()).toEqual(true);
      expect(await secondRowButton.isDisabled()).toEqual(false);

      await secondRowButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${secondPage.id}`,
      });
      expect(req.request.body).toEqual({
        ...secondPage,
        order: 0,
      });
      req.flush(secondPage);
      expectGetPages([firstPage, secondPage], []);
    });

    it.each([
      ['page', fakeMarkdown({ id: 'id', name: 'my first page', visibility: 'PUBLIC', published: false })],
      ['folder', fakeFolder({ id: 'id', name: 'my first folder', visibility: 'PUBLIC', published: false })],
    ])('should delete %s', async (name: string, page: Page) => {
      await init([page], []);

      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const publishPageBtn = await pageListHarness.getDeletePageButtonByRowIndex(0);
      await publishPageBtn.click();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}`,
        })
        .flush(null);

      expectGetPages([], []);
    });
  });

  const expectGetPages = (pages: Page[], breadcrumb: Breadcrumb[], parentId = 'ROOT') => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages?parentId=${parentId}`,
    });

    req.flush({ pages, breadcrumb });
  };
});
