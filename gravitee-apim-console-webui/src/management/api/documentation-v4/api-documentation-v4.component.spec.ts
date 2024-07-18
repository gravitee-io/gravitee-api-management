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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { set } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiDocumentationV4Component } from './api-documentation-v4.component';
import { ApiDocumentationV4Module } from './api-documentation-v4.module';
import { ApiDocumentationV4EmptyStateHarness } from './components/documentation-empty-state/api-documentation-v4-empty-state.harness';
import { ApiDocumentationV4ListNavigationHeaderHarness } from './components/documentation-list-navigation-header/api-documentation-v4-list-navigation-header.harness';
import { ApiDocumentationV4EditFolderDialogHarness } from './dialog/documentation-edit-folder-dialog/api-documentation-v4-edit-folder-dialog.harness';
import { ApiDocumentationV4PagesListHarness } from './components/api-documentation-v4-pages-list/api-documentation-v4-pages-list.harness';
import { ApiDocumentationV4PageTitleHarness } from './components/api-documentation-v4-page-title/api-documentation-v4-page-title.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Breadcrumb, Page } from '../../../entities/management-api-v2/documentation/page';
import { fakeFolder, fakeMarkdown } from '../../../entities/management-api-v2/documentation/page.fixture';
import { ApiLifecycleState, fakeApiV4 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { PageType } from '../../../entities/page';
import { Constants } from '../../../entities/Constants';

describe('ApiDocumentationV4', () => {
  let fixture: ComponentFixture<ApiDocumentationV4Component>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    pages: Page[],
    breadcrumb: Breadcrumb[],
    parentId = 'ROOT',
    portalUrl = 'portal.url',
    apiLifecycleStatus: ApiLifecycleState = 'PUBLISHED',
  ) => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4Component],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiId: API_ID } },
            queryParams: new BehaviorSubject({ parentId }),
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.portal', {
              get url() {
                return portalUrl;
              },
            });
            return constants;
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to avoid the warning
          isTabbable: () => true, // This checks tab trap, set it to true to avoid the warning
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4Component);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      })
      .flush(fakeApiV4({ id: API_ID, lifecycleState: apiLifecycleStatus }));

    expectGetPages(pages, breadcrumb, parentId);
  };

  afterEach(() => {
    httpTestingController.verify();
    jest.resetAllMocks();
  });

  describe('API does not have pages', () => {
    beforeEach(async () => await init([], []));

    it('should show empty state when no documentation for API', async () => {
      const emptyState = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
      expect(emptyState).toBeDefined();
    });

    it('should navigate to create page', async () => {
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
      await headerHarness.clickAddNewPage(PageType.MARKDOWN);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['new'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'ROOT', pageType: 'MARKDOWN' },
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
      expect(await headerHarness.getBreadcrumb()).toEqual('Home > level 1 > level 2');

      await headerHarness.clickOnBreadcrumbItem('level 1');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['.'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'level-1' },
      });
    });

    it('should navigate to root if in a sub folder', async () => {
      await init([], [{ name: 'level 1', id: 'level-1', position: 1 }]);
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickOnBreadcrumbItem('Home');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['.'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'ROOT' },
      });
    });

    it('should not navigate to root if already in root', async () => {
      await init([], []);
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickOnBreadcrumbItem('Home');
      expect(routerNavigateSpy).toHaveBeenCalledTimes(0);
    });

    it('should show breadcrumb items and not be able to click on last item', async () => {
      await init(
        [],
        [
          { name: 'level 1', id: 'level-1', position: 1 },
          { name: 'level 2', id: 'level-2', position: 2 },
        ],
      );
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      expect(await headerHarness.getBreadcrumb()).toEqual('Home > level 1 > level 2');

      await headerHarness.clickOnBreadcrumbItem('level 2');
      expect(routerNavigateSpy).toHaveBeenCalledTimes(0);
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
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      await headerHarness.clickAddNewPage(PageType.MARKDOWN);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['new'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'ROOT', pageType: 'MARKDOWN' },
      });
    });

    it('should hide create page button when folder is empty', async () => {
      await init([], []);
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4ListNavigationHeaderHarness);
      expect(await headerHarness.getNewPageButton()).toBeNull();
    });

    it('should navigate to folder when click in the list', async () => {
      await init([fakeFolder({ name: 'my first folder', id: 'my-first-folder', visibility: 'PUBLIC' })], []);
      const pageListHarness = await harnessLoader.getHarness(ApiDocumentationV4PagesListHarness);
      const nameDiv = await pageListHarness.getNameDivByRowIndex(0);
      await nameDiv.host().then((host) => host.click());

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'my-first-folder' },
      });
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

      expectGetApi();
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
      expectGetApi();
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
      expectGetApi();
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
      expectGetApi();
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

      expectGetApi();
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

      expectGetApi();
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

      expectGetApi();
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

      expectGetApi();
      expectGetPages([], []);
    });
  });

  describe('Header', () => {
    describe('with Published API', () => {
      it('should display Open in Portal button', async () => {
        await init([], []);
        const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
        expect(header).toBeDefined();
        const openInPortalBtn = await header.getOpenInPortalBtn();
        expect(openInPortalBtn).toBeDefined();
        expect(await openInPortalBtn.isDisabled()).toEqual(false);
        expect(await header.getApiPortalUrl()).toEqual('portal.url/catalog/api/api-id');
      });

      it('should parse Portal url ending with "/"', async () => {
        await init([], [], 'ROOT', 'portal.url/');
        const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
        expect(await header.getApiPortalUrl()).toEqual('portal.url/catalog/api/api-id');
      });

      it('should not display Open in Portal button if Portal url not defined', async () => {
        await init([], [], 'ROOT', null);
        const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
        expect(await header.getOpenInPortalBtn()).toEqual(null);
      });
    });

    describe('with Created API', () => {
      it('should have disabled Open in Portal button', async () => {
        await init([], [], 'ROOT', 'portal.url', 'CREATED');
        const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
        expect(await header.getOpenInPortalBtn().then((btn) => btn.isDisabled())).toEqual(true);
      });
    });
  });

  const expectGetPages = (pages: Page[], breadcrumb: Breadcrumb[], parentId = 'ROOT') => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages?parentId=${parentId}`,
    });

    req.flush({ pages, breadcrumb });
  };

  const expectGetApi = () => {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      })
      .flush(fakeApiV4({ id: API_ID, lifecycleState: 'PUBLISHED' }));
  };
});
