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
import { ConfigureTestingGraviteeMarkdownEditor } from '@gravitee/gravitee-markdown';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import SpyInstance = jest.SpyInstance;

import { PortalNavigationItemsComponent } from './portal-navigation-items.component';
import { PortalNavigationItemsHarness } from './portal-navigation-items.harness';
import { SectionEditorDialogHarness } from './section-editor-dialog/section-editor-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';
import {
  fakeNewLinkPortalNavigationItem,
  fakeNewPagePortalNavigationItem,
  fakePortalNavigationFolder,
  fakePortalNavigationItemsResponse,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
  NewPortalNavigationItem,
  PortalNavigationItem,
  PortalNavigationItemsResponse,
} from '../../entities/management-api-v2';

describe('PortalNavigationItemsComponent', () => {
  let fixture: ComponentFixture<PortalNavigationItemsComponent>;
  let harness: PortalNavigationItemsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerSpy: SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNavigationItemsComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-documentation-c'],
        },
      ],
    }).compileComponents();

    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(PortalNavigationItemsComponent);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalNavigationItemsHarness);

    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);

    routerSpy = jest.spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe('initial load', () => {
    describe('with tree items', () => {
      beforeEach(async () => {
        const fakeResponse = fakePortalNavigationItemsResponse({
          items: [
            fakePortalNavigationPage({
              id: 'nav-item-1',
              title: 'Nav Item 1',
              portalPageContentId: 'nav-item-1-content',
            }),
            fakePortalNavigationFolder({ id: 'nav-item-2', title: 'Nav Item 2' }),
          ],
        });

        await expectGetNavigationItems(fakeResponse);
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });

      it('should load and display navigation items', async () => {
        expect(await harness.getNavigationItemTitles()).toEqual(['Nav Item 1', 'Nav Item 2']);
      });
      it('should have the first item selected by default', async () => {
        expect(await harness.getSelectedNavigationItemTitle()).toBe('Nav Item 1');
      });
      it('should show default content of first page on load', async () => {
        expect(await harness.getEditorContentText()).toBe('This is the content of Nav Item 1');
      });
    });
    describe('with no tree items', () => {
      beforeEach(async () => {
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [] }));
      });

      it('should show no item message', async () => {
        expect(await harness.isNavigationTreeEmpty()).toBe(true);
      });
    });
  });

  describe('adding a section', () => {
    let dialogHarness: SectionEditorDialogHarness;
    const fakeResponse = fakePortalNavigationItemsResponse({
      items: [fakePortalNavigationPage({ portalPageContentId: 'nav-item-1-content' })],
    });
    const fakeContentText = 'This is the content.';
    beforeEach(async () => {
      expectGetMenuLinks(fakeResponse);
      await harness.clickAddButton();
      fixture.detectChanges();
      expectGetPageContent('nav-item-1-content', fakeContentText);
    });
    describe('adding a page', () => {
      beforeEach(async () => {
        await harness.clickPageMenuItem();
        dialogHarness = await rootLoader.getHarness(SectionEditorDialogHarness);
      });
      it('opens the dialog when the Add button is clicked and Page is selected', async () => {
        expect(dialogHarness).toBeTruthy();
      });

      it('should not create the page when the dialog is cancelled', async () => {
        await dialogHarness.clickCancelButton();
      });

      it('should create the page when the dialog is submitted', async () => {
        const title = 'New Page Title';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickAddButton();

        expectCreateNavigationItem(
          fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE' }),
          fakePortalNavigationPage({
            title,
            area: 'TOP_NAVBAR',
            type: 'PAGE',
            portalPageContentId: 'content-id',
          }),
        );
        await expectGetNavigationItems(fakeResponse);
      });
      it('should navigate to the created page after creation', async () => {
        const title = 'New Page Title';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickAddButton();

        const createdItem = fakePortalNavigationPage({
          id: 'newly-created-id',
          title,
          area: 'TOP_NAVBAR',
          type: 'PAGE',
          portalPageContentId: 'content-id',
        });
        expectCreateNavigationItem(fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE' }), createdItem);
        await expectGetNavigationItems(fakeResponse);

        expect(routerSpy).toHaveBeenCalledWith(['.'], expect.objectContaining({ queryParams: { navId: createdItem.id } }));
      });
    });
    describe('adding a link', () => {
      beforeEach(async () => {
        await harness.clickLinkMenuItem();
        dialogHarness = await rootLoader.getHarness(SectionEditorDialogHarness);
      });
      it('opens the dialog when the Add button is clicked and Link is selected', async () => {
        expect(dialogHarness).toBeTruthy();
      });
      it('should not create the link when the dialog is cancelled', async () => {
        await dialogHarness.clickCancelButton();
      });
      it('should create the link when the dialog is submitted', async () => {
        const title = 'New Link Title';
        const url = 'https://gravitee.io';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.setUrlInputValue(url);
        await dialogHarness.clickAddButton();

        expectCreateNavigationItem(
          fakeNewLinkPortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'LINK', url }),
          fakePortalNavigationLink({
            title,
            area: 'TOP_NAVBAR',
            type: 'LINK',
            url,
          }),
        );
        await expectGetNavigationItems(fakeResponse);
      });
    });
    describe('adding a folder', () => {
      beforeEach(async () => {
        await harness.clickFolderMenuItem();
        dialogHarness = await rootLoader.getHarness(SectionEditorDialogHarness);
      });
      it('opens the dialog when the Add button is clicked and Folder is selected', async () => {
        expect(dialogHarness).toBeTruthy();
      });
      it('should not create the folder when the dialog is cancelled', async () => {
        await dialogHarness.clickCancelButton();
      });
      it('should create the folder when the dialog is submitted', async () => {
        const title = 'New Folder Title';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickAddButton();

        expectCreateNavigationItem(
          {
            title,
            area: 'TOP_NAVBAR',
            type: 'FOLDER',
          },
          fakePortalNavigationFolder({
            title,
            area: 'TOP_NAVBAR',
            type: 'FOLDER',
          }),
        );
        await expectGetNavigationItems(fakeResponse);
      });
    });
  });

  function expectGetMenuLinks(response: PortalNavigationItemsResponse = fakePortalNavigationItemsResponse()) {
    httpTestingController.expectOne('assets/mocks/portal-menu-links.json').flush(response);
  }
  async function expectGetNavigationItems(response: PortalNavigationItemsResponse = fakePortalNavigationItemsResponse()) {
    expectGetMenuLinks(response);
    // TODO: Restore when calling the backend API
    // httpTestingController
    //   .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR` })
    //   .flush(response);
    // fixture.detectChanges();

    // Used for the navId query param handling for getting page content
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function expectGetPageContent(contentId: string, content: string) {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}` })
      .flush({ id: contentId, content });
  }

  function expectCreateNavigationItem(requestBody: NewPortalNavigationItem, result: PortalNavigationItem) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items` });
    expect(req.request.body).toEqual(requestBody);
    req.flush(result);
  }
});
