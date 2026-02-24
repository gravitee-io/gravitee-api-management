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
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmAndValidateDialogHarness, GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

import SpyInstance = jest.SpyInstance;

import { findFirstAvailablePage, PortalNavigationItemsComponent } from './portal-navigation-items.component';
import { PortalNavigationItemsHarness } from './portal-navigation-items.harness';
import { SectionEditorDialogHarness } from './section-editor-dialog/section-editor-dialog.harness';
import { ApiSectionEditorDialogHarness } from './api-section-editor-dialog/api-section-editor-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';
import {
  fakeNewLinkPortalNavigationItem,
  fakeNewPagePortalNavigationItem,
  fakePortalNavigationApi,
  fakePortalNavigationFolder,
  fakePortalNavigationItemsResponse,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
  fakeUpdateApiPortalNavigationItem,
  fakeUpdatePagePortalNavigationItem,
  NewPortalNavigationItem,
  PortalNavigationItem,
  PortalNavigationItemsResponse,
  UpdateFolderPortalNavigationItem,
  UpdateLinkPortalNavigationItem,
  UpdatePortalNavigationItem,
} from '../../entities/management-api-v2';
import { SectionNode } from '../components/flat-tree/flat-tree.component';

describe('PortalNavigationItemsComponent', () => {
  let fixture: ComponentFixture<PortalNavigationItemsComponent>;
  let harness: PortalNavigationItemsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerSpy: SpyInstance;
  let component: PortalNavigationItemsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNavigationItemsComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d'],
        },
      ],
    }).compileComponents();

    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(PortalNavigationItemsComponent);
    component = fixture.componentInstance;
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
      it('should have the save button disabled', async () => {
        expect(await harness.isSaveButtonDisabled()).toBe(true);
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
      await expectGetNavigationItems(fakeResponse);
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
        await dialogHarness.clickSubmitButton();

        expectCreateNavigationItem(
          fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE', contentType: 'GRAVITEE_MARKDOWN' }),
          fakePortalNavigationPage({
            title,
            area: 'TOP_NAVBAR',
            type: 'PAGE',
            portalPageContentId: 'content-id',
          }),
        );
        await expectGetNavigationItems(fakeResponse);
      });
      it('should create the page with contentType OPENAPI when OpenAPI is selected in the dialog', async () => {
        const title = 'Open API Page';
        await dialogHarness.selectPageType('OPENAPI');
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickSubmitButton();

        expectCreateNavigationItem(
          fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE', contentType: 'OPENAPI' }),
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
        await dialogHarness.clickSubmitButton();

        const createdItem = fakePortalNavigationPage({
          id: 'newly-created-id',
          title,
          area: 'TOP_NAVBAR',
          type: 'PAGE',
          portalPageContentId: 'content-id',
        });
        expectCreateNavigationItem(
          fakeNewPagePortalNavigationItem({
            title,
            area: 'TOP_NAVBAR',
            type: 'PAGE',
            contentType: 'GRAVITEE_MARKDOWN',
          }),
          createdItem,
        );
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
        await dialogHarness.clickSubmitButton();

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
        await dialogHarness.clickSubmitButton();

        expectCreateNavigationItem(
          {
            title,
            area: 'TOP_NAVBAR',
            type: 'FOLDER',
            visibility: 'PUBLIC',
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

  describe('editing a section from tree node "More actions" menu', () => {
    it('opens edit dialog for page and prefills title', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationLink({ id: 'nav-item-2', title: 'Nav Item 2', url: 'https://old.com' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const pageData = fakeResponse.items[0] as PortalNavigationItem;
      const pageNode = { id: pageData.id, label: pageData.title, type: pageData.type, data: pageData } as any;
      component.onNodeMenuAction({ action: 'edit', itemType: 'PAGE', node: pageNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getDialogTitle()).toBe('Edit "Nav Item 1" page');

      const titleInput = await dialog.getTitleInput();
      expect(await titleInput.getValue()).toBe('Nav Item 1');
    });

    it('calls backend update when dialog is submitted (link)', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationLink({
            id: 'nav-item-2',
            title: 'Nav Item 2',
            url: 'https://old.com',
            area: 'TOP_NAVBAR',
          }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const linkData = fakeResponse.items[1] as PortalNavigationItem;
      const linkNode = { id: linkData.id, label: linkData.title, type: linkData.type, data: linkData } as any;
      component.onNodeMenuAction({ action: 'edit', itemType: 'LINK', node: linkNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      const titleInput = await dialog.getTitleInput();
      await titleInput.setValue('Updated Link');
      await dialog.setUrlInputValue('https://new.com');

      await dialog.clickSubmitButton();
      fixture.detectChanges();

      expectPutPortalNavigationItem(
        linkData.id,
        {
          title: 'Updated Link',
          type: linkData.type,
          parentId: linkData.parentId,
          order: linkData.order,
          url: 'https://new.com',
          published: linkData.published,
          visibility: linkData.visibility,
        } as UpdateLinkPortalNavigationItem,
        fakePortalNavigationLink({
          id: linkData.id,
          title: 'Updated Link',
          url: 'https://new.com',
          area: linkData.area,
          type: 'LINK',
        }),
      );

      // After update, component refreshes the list — satisfy the subsequent GET
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: fakeResponse.items }));
    });
  });

  describe('editing a section', () => {
    it('opens edit dialog for page and prefills title', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      // Select the item by title
      await harness.selectNavigationItemByTitle('Nav Item 1');
      fixture.detectChanges();

      const component = fixture.componentInstance;
      component.onEdit();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getDialogTitle()).toBe('Edit "Nav Item 1" page');

      const titleInput = await dialog.getTitleInput();
      expect(await titleInput.getValue()).toBe('Nav Item 1');
    });
  });

  describe('creating a page under a folder from tree node "More actions" menu', () => {
    it('opens create dialog and does not call backend when cancelled', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const folderData = fakeResponse.items[1] as PortalNavigationItem;
      const folderNode = { id: folderData.id, label: folderData.title, type: folderData.type, data: folderData } as any;

      component.onNodeMenuAction({ action: 'create', itemType: 'PAGE', node: folderNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      // cancel should not send any POST request
      await dialog.clickCancelButton();
    });

    it('calls backend create with parentId when dialog is submitted', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const folderData = fakeResponse.items[1] as PortalNavigationItem;
      const folderNode = { id: folderData.id, label: folderData.title, type: folderData.type, data: folderData } as any;

      component.onNodeMenuAction({ action: 'create', itemType: 'PAGE', node: folderNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      const title = 'New Child Page';
      await dialog.setTitleInputValue(title);
      await dialog.clickSubmitButton();

      const createdItem = fakePortalNavigationPage({
        id: 'child-page-1',
        title,
        area: 'TOP_NAVBAR',
        type: 'PAGE',
        parentId: folderData.id,
        portalPageContentId: 'content-id',
      });

      expectCreateNavigationItem(
        fakeNewPagePortalNavigationItem({
          title,
          area: 'TOP_NAVBAR',
          type: 'PAGE',
          parentId: folderData.id,
          contentType: 'GRAVITEE_MARKDOWN',
        }),
        createdItem,
      );
      await expectGetNavigationItems(fakeResponse);

      expect(routerSpy).toHaveBeenCalledWith(['.'], expect.objectContaining({ queryParams: { navId: createdItem.id } }));
    });
  });

  describe('creating a folder under a folder from tree node "More actions" menu', () => {
    it('calls backend create with parentId when dialog is submitted', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const folderData = fakeResponse.items[1] as PortalNavigationItem;
      const folderNode = { id: folderData.id, label: folderData.title, type: folderData.type, data: folderData } as any;

      component.onNodeMenuAction({ action: 'create', itemType: 'FOLDER', node: folderNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      const title = 'New Child Folder';
      await dialog.setTitleInputValue(title);
      await dialog.clickSubmitButton();

      const createdItem = fakePortalNavigationFolder({
        id: 'child-folder-1',
        title,
        area: 'TOP_NAVBAR',
        type: 'FOLDER',
        parentId: folderData.id,
      });

      expectCreateNavigationItem(
        {
          title,
          area: 'TOP_NAVBAR',
          type: 'FOLDER',
          parentId: folderData.id,
          visibility: 'PUBLIC',
        } as NewPortalNavigationItem,
        createdItem,
      );
      await expectGetNavigationItems(fakeResponse);
    });
  });

  describe('creating a link under a folder from tree node "More actions" menu', () => {
    it('calls backend create with parentId when dialog is submitted', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const folderData = fakeResponse.items[1] as PortalNavigationItem;
      const folderNode = { id: folderData.id, label: folderData.title, type: folderData.type, data: folderData } as any;

      component.onNodeMenuAction({ action: 'create', itemType: 'LINK', node: folderNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      const title = 'New Child Link';
      const url = 'https://example.com';
      await dialog.setTitleInputValue(title);
      await dialog.setUrlInputValue(url);
      await dialog.clickSubmitButton();

      const createdItem = fakePortalNavigationLink({
        id: 'child-link-1',
        title,
        url,
        area: 'TOP_NAVBAR',
        type: 'LINK',
        parentId: folderData.id,
      });

      expectCreateNavigationItem(
        fakeNewLinkPortalNavigationItem({ title, url, area: 'TOP_NAVBAR', type: 'LINK', parentId: folderData.id }),
        createdItem,
      );
      await expectGetNavigationItems(fakeResponse);
    });
  });

  describe('editing a folder from tree node "More actions" menu', () => {
    it('calls backend update when dialog is submitted (folder)', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', area: 'TOP_NAVBAR' }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      await expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      const component = fixture.componentInstance;
      const folderData = fakeResponse.items[1] as PortalNavigationItem;
      const folderNode = { id: folderData.id, label: folderData.title, type: folderData.type, data: folderData } as any;
      component.onNodeMenuAction({ action: 'edit', itemType: 'FOLDER', node: folderNode });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();

      await dialog.setTitleInputValue('Updated Folder');
      await dialog.clickSubmitButton();

      expectPutPortalNavigationItem(
        folderData.id,
        {
          title: 'Updated Folder',
          type: folderData.type,
          parentId: folderData.parentId,
          order: folderData.order,
          published: folderData.published,
          visibility: folderData.visibility,
        } as UpdateFolderPortalNavigationItem,
        fakePortalNavigationFolder({
          id: folderData.id,
          title: 'Updated Folder',
          area: folderData.area,
          type: 'FOLDER',
        }),
      );

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: fakeResponse.items }));
    });
  });

  describe('deleting a section from tree node "More actions" menu', () => {
    it('should call DELETE and refresh list when deleting a non-selected item', async () => {
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

      await harness.deleteNodeById('nav-item-2');
      fixture.detectChanges();
      await fixture.whenStable();

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.confirm();

      const deleteReq = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/nav-item-2`,
      });
      deleteReq.flush({});

      // After deletion, component refreshes the list — satisfy the subsequent GET
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [fakeResponse.items[0]] }));
      expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      expect(await harness.getNavigationItemTitles()).toEqual(['Nav Item 1']);
    });

    it('should clear selection when deleted item is selected', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');

      await harness.deleteNodeById('nav-item-1');
      fixture.detectChanges();
      await fixture.whenStable();

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.confirm();

      const deleteReq = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/nav-item-1`,
      });
      deleteReq.flush({});

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [] }));

      expect(routerSpy).toHaveBeenCalled();
    });

    it('should show error handling when delete API fails', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [fakePortalNavigationFolder({ id: 'nav-item-2', title: 'Nav Item 2' })],
      });
      await expectGetNavigationItems(fakeResponse);

      await harness.deleteNodeById('nav-item-2');
      fixture.detectChanges();
      await fixture.whenStable();

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.confirm();

      const deleteReq = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/nav-item-2`,
      });
      deleteReq.flush('error', { status: 500, statusText: 'Server Error' });

      // No further GET expected; afterEach will verify outstanding requests are handled
    });
  });

  describe('selecting a navigation item', () => {
    beforeEach(async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
          fakePortalNavigationFolder({ id: 'nav-item-2', title: 'Nav Item 2' }),
          fakePortalNavigationPage({
            id: 'nav-item-3',
            title: 'Nav Item 3',
            portalPageContentId: 'nav-item-3-content',
          }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
    });

    it('should load page content when a PAGE item is selected', async () => {
      expect(await harness.isSaveButtonDisabled()).toBe(true);

      await harness.selectNavigationItemByTitle('Nav Item 3');
      expectGetPageContent('nav-item-3-content', 'This is the content of Nav Item 3');
      expect(await harness.getEditorContentText()).toBe('This is the content of Nav Item 3');
      expect(await harness.isSaveButtonDisabled()).toBe(true);
    });

    it('should show empty message when non-PAGE is selected', async () => {
      await harness.selectNavigationItemByTitle('Nav Item 2');
      expect(await harness.isEditorEmptyStateDisplayed()).toBe(true);
    });

    it('should disable save button after selecting a PAGE after editing its content', async () => {
      await harness.setEditorContentText('Edited content');
      expect(await harness.isSaveButtonDisabled()).toBe(false);

      await harness.selectNavigationItemByTitle('Nav Item 3');

      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.confirm();

      expectGetPageContent('nav-item-3-content', 'This is the content of Nav Item 3');
      expect(await harness.isSaveButtonDisabled()).toBe(true);
    });
  });

  describe('saving content', () => {
    beforeEach(async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
        ],
      });

      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
    });

    it('should call PUT on save and disable save button after success', async () => {
      await harness.setEditorContentText('Edited content');
      expect(await harness.isSaveButtonDisabled()).toBe(false);

      const saveButton = await rootLoader.getHarness(MatButtonHarness.with({ text: /Save/i }));
      await saveButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/nav-item-1-content`,
      });
      expect(req.request.body).toEqual({ content: 'Edited content' });
      req.flush({ id: 'nav-item-1-content', content: 'Edited content' });

      fixture.detectChanges();

      expect(await harness.isSaveButtonDisabled()).toBe(true);
      expect(await harness.getEditorContentText()).toBe('Edited content');
    });
  });

  describe('publishing and unpublishing a navigation item', () => {
    describe('published navigation item', () => {
      const publishedNavItem = fakePortalNavigationPage({
        id: 'nav-item-1',
        title: 'Nav Item 1',
        portalPageContentId: 'nav-item-1-content',
        published: true,
      });
      beforeEach(async () => {
        const fakeResponse = fakePortalNavigationItemsResponse({
          items: [publishedNavItem],
        });

        await expectGetNavigationItems(fakeResponse);
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
      it('should show "Unpublish" button and "Publish" label', async () => {
        expect(await harness.isPublishButtonVisible()).toBe(false);
        expect(await harness.isUnpublishButtonVisible()).toBe(true);

        expect(await harness.isPublishedBadgeVisible()).toBe(true);
      });
      it('should unpublish the item when "Unpublish" button is clicked', async () => {
        await harness.clickUnpublishButton();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectPutPortalNavigationItem(
          'nav-item-1',
          {
            ...publishedNavItem,
            published: false,
          },
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [publishedNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });

      it('should unpublish the item from the "More Actions" menu', async () => {
        await harness.unpublishNodeById('nav-item-1');

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectPutPortalNavigationItem(
          'nav-item-1',
          {
            ...publishedNavItem,
            published: false,
          },
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [publishedNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
    });

    describe('unpublished navigation item', () => {
      const unpublishedNavItem = fakePortalNavigationPage({
        id: 'nav-item-1',
        title: 'Nav Item 1',
        portalPageContentId: 'nav-item-1-content',
        published: false,
      });
      beforeEach(async () => {
        const fakeResponse = fakePortalNavigationItemsResponse({
          items: [unpublishedNavItem],
        });

        await expectGetNavigationItems(fakeResponse);
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
      it('should show "Publish" button and "Unpublished" label', async () => {
        expect(await harness.isPublishButtonVisible()).toBe(true);
        expect(await harness.isUnpublishButtonVisible()).toBe(false);

        expect(await harness.isPublishedBadgeVisible()).toBe(false);
        expect(await harness.isUnpublishedBadgeVisible()).toBe(true);
      });
      it('should publish the item when "Publish" button is clicked', async () => {
        await harness.clickPublishButton();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectPutPortalNavigationItem(
          'nav-item-1',
          {
            ...unpublishedNavItem,
            published: true,
          },
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [unpublishedNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });

      it('should publish item when publish action is clicked in More Actions dropdown', async () => {
        await harness.publishNodeById('nav-item-1');

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        expectPutPortalNavigationItem(
          'nav-item-1',
          {
            ...unpublishedNavItem,
            published: true,
          },
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [unpublishedNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
    });
  });

  describe('changing navigation item visibility', () => {
    describe('public navigation item', () => {
      const publicNavItem = fakePortalNavigationPage({
        id: 'nav-item-1',
        title: 'Nav Item 1',
        portalPageContentId: 'nav-item-1-content',
        visibility: 'PUBLIC',
      });
      beforeEach(async () => {
        const fakeResponse = fakePortalNavigationItemsResponse({
          items: [publicNavItem],
        });

        await expectGetNavigationItems(fakeResponse);
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
      it('should show "Public" badge', async () => {
        expect(await harness.isPublicBadgeVisible()).toBe(true);
      });

      it('should change visibility in edit dialog', async () => {
        await harness.editNodeById('nav-item-1');

        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const authToggle = await dialog.getAuthenticationToggle();
        expect(await authToggle.isChecked()).toBe(false);

        await authToggle.toggle();

        await dialog.clickSubmitButton();
        expectPutPortalNavigationItem(
          publicNavItem.id,
          fakeUpdatePagePortalNavigationItem({
            title: publicNavItem.title,
            parentId: publicNavItem.parentId,
            order: publicNavItem.order,
            published: publicNavItem.published,
            visibility: 'PRIVATE',
          }),
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [publicNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
    });

    describe('private navigation item', () => {
      const privateNavItem = fakePortalNavigationPage({
        id: 'nav-item-1',
        title: 'Nav Item 1',
        portalPageContentId: 'nav-item-1-content',
        visibility: 'PRIVATE',
      });
      beforeEach(async () => {
        const fakeResponse = fakePortalNavigationItemsResponse({
          items: [privateNavItem],
        });

        await expectGetNavigationItems(fakeResponse);
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
      it('should show "Private" badge', async () => {
        expect(await harness.isPrivateBadgeVisible()).toBe(true);
      });

      it('should change visibility in edit dialog', async () => {
        await harness.editNodeById('nav-item-1');

        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const authToggle = await dialog.getAuthenticationToggle();
        expect(await authToggle.isChecked()).toBe(true);

        await authToggle.toggle();

        await dialog.clickSubmitButton();
        expectPutPortalNavigationItem(
          privateNavItem.id,
          fakeUpdatePagePortalNavigationItem({
            title: privateNavItem.title,
            parentId: privateNavItem.parentId,
            order: privateNavItem.order,
            published: privateNavItem.published,
            visibility: 'PUBLIC',
          }),
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [privateNavItem] }));
        expectGetPageContent('nav-item-1-content', 'This is the content of Nav Item 1');
      });
    });
  });

  describe('changing api navigation item visibility', () => {
    const apiNavItem = fakePortalNavigationApi({
      id: 'nav-api-1',
      parentId: 'nav-folder-1',
      apiId: 'api-v2',
      visibility: 'PUBLIC',
      area: 'TOP_NAVBAR',
    });

    beforeEach(async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [fakePortalNavigationFolder({ id: 'nav-folder-1', area: 'TOP_NAVBAR' }), apiNavItem],
      });

      await expectGetNavigationItems(fakeResponse);
    });

    it('should update visibility using edit dialog', async () => {
      await harness.editNodeById('nav-api-1');

      const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
      expect(await dialog.getDialogTitle()).toContain('Edit');

      const authToggle = await dialog.getAuthenticationToggle();
      expect(await authToggle.isChecked()).toBe(false);
      await authToggle.toggle();

      await dialog.clickSubmitButton();

      expectPutPortalNavigationItem(
        apiNavItem.id,
        fakeUpdateApiPortalNavigationItem({
          title: apiNavItem.title,
          parentId: apiNavItem.parentId,
          order: apiNavItem.order,
          published: apiNavItem.published,
          visibility: 'PRIVATE',
          apiId: apiNavItem.apiId,
        }),
        fakePortalNavigationApi({}),
      );

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [apiNavItem] }));
    });
  });

  describe('resizing the sections panel', () => {
    beforeEach(async () => {
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [] }));
    });

    it('should have default panel width of 350px', () => {
      const component = fixture.componentInstance;
      expect(component.panelWidth()).toBe(350);
    });

    it('should increase panel width when dragging right', () => {
      const component = fixture.componentInstance;
      const resizeHandle = fixture.nativeElement.querySelector('.resize-handle');

      const initialWidth = component.panelWidth();
      expect(initialWidth).toBe(350);

      // Simulate mousedown
      const mousedownEvent = new MouseEvent('mousedown', {
        clientX: 100,
        bubbles: true,
        cancelable: true,
      });
      resizeHandle.dispatchEvent(mousedownEvent);
      fixture.detectChanges();

      // Simulate mousemove (dragging right by 50px)
      const mousemoveEvent = new MouseEvent('mousemove', {
        clientX: 150,
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mousemoveEvent);
      fixture.detectChanges();

      expect(component.panelWidth()).toBe(400);

      // Simulate mouseup
      const mouseupEvent = new MouseEvent('mouseup', {
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mouseupEvent);
      fixture.detectChanges();
    });

    it('should decrease panel width when dragging left', () => {
      const component = fixture.componentInstance;
      const resizeHandle = fixture.nativeElement.querySelector('.resize-handle');

      const initialWidth = component.panelWidth();
      expect(initialWidth).toBe(350);

      // Simulate mousedown
      const mousedownEvent = new MouseEvent('mousedown', {
        clientX: 100,
        bubbles: true,
        cancelable: true,
      });
      resizeHandle.dispatchEvent(mousedownEvent);
      fixture.detectChanges();

      // Simulate mousemove (dragging left by 50px)
      const mousemoveEvent = new MouseEvent('mousemove', {
        clientX: 50,
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mousemoveEvent);
      fixture.detectChanges();

      expect(component.panelWidth()).toBe(300);

      // Simulate mouseup
      const mouseupEvent = new MouseEvent('mouseup', {
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mouseupEvent);
      fixture.detectChanges();
    });

    it('should constrain panel width to minimum of 280px', () => {
      const component = fixture.componentInstance;
      const resizeHandle = fixture.nativeElement.querySelector('.resize-handle');

      const initialWidth = component.panelWidth();
      expect(initialWidth).toBe(350);

      // Simulate mousedown
      const mousedownEvent = new MouseEvent('mousedown', {
        clientX: 100,
        bubbles: true,
        cancelable: true,
      });
      resizeHandle.dispatchEvent(mousedownEvent);
      fixture.detectChanges();

      // Simulate mousemove (dragging left by 200px, which would go below minimum)
      const mousemoveEvent = new MouseEvent('mousemove', {
        clientX: -100,
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mousemoveEvent);
      fixture.detectChanges();

      expect(component.panelWidth()).toBe(280);

      // Simulate mouseup
      const mouseupEvent = new MouseEvent('mouseup', {
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mouseupEvent);
      fixture.detectChanges();
    });

    it('should constrain panel width to maximum of 600px', () => {
      const component = fixture.componentInstance;
      const resizeHandle = fixture.nativeElement.querySelector('.resize-handle');

      const initialWidth = component.panelWidth();
      expect(initialWidth).toBe(350);

      // Simulate mousedown
      const mousedownEvent = new MouseEvent('mousedown', {
        clientX: 100,
        bubbles: true,
        cancelable: true,
      });
      resizeHandle.dispatchEvent(mousedownEvent);
      fixture.detectChanges();

      // Simulate mousemove (dragging right by 300px, which would exceed maximum)
      const mousemoveEvent = new MouseEvent('mousemove', {
        clientX: 400,
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mousemoveEvent);
      fixture.detectChanges();

      expect(component.panelWidth()).toBe(600);

      // Simulate mouseup
      const mouseupEvent = new MouseEvent('mouseup', {
        bubbles: true,
        cancelable: true,
      });
      document.dispatchEvent(mouseupEvent);
      fixture.detectChanges();
    });
  });

  describe('page content load failure', () => {
    it('should show the "Page Not Found" empty state when page content fails to load', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({
            id: 'nav-item-1',
            title: 'Nav Item 1',
            portalPageContentId: 'nav-item-1-content',
          }),
        ],
      });

      // First load page tree
      await expectGetNavigationItems(fakeResponse);

      // Simulate failure when loading page content
      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/nav-item-1-content`,
      });
      req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

      fixture.detectChanges();

      // Now the UI should show the empty state
      expect(await harness.isPageNotFoundDisplayed()).toBe(true);

      const message = await harness.getPageNotFoundMessage();
      expect(message).toContain('Failed to load page content.');
    });
  });

  describe('first available page selection (findFirstAvailablePage)', () => {
    it('should navigate to the first PAGE inside the first root FOLDER when no navId is provided', async () => {
      const folder = fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', order: 1 });
      const childPage = fakePortalNavigationPage({
        id: 'child-page-1',
        title: 'Child Page 1',
        parentId: 'folder-1',
        portalPageContentId: 'child-content-1',
        order: 1,
      });
      const rootPage = fakePortalNavigationPage({
        id: 'root-page-1',
        title: 'Root Page 1',
        portalPageContentId: 'root-content-1',
        order: 2,
      });

      const fakeResponse = fakePortalNavigationItemsResponse({ items: [folder, childPage, rootPage] });

      // Load tree, then expect the component to auto-select the first page within the first folder
      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent('child-content-1', 'This is the content of Child Page 1');

      // Verify selection and navigation
      expect(await harness.getSelectedNavigationItemTitle()).toBe('Child Page 1');
      expect(routerSpy).toHaveBeenCalledWith(['.'], expect.objectContaining({ queryParams: { navId: 'child-page-1' } }));

      // Verify content is displayed
      expect(await harness.getEditorContentText()).toBe('This is the content of Child Page 1');
    });
  });

  describe('findFirstAvailablePage', () => {
    beforeEach(async () => {
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [] }));
    });
    it('selects root page when it appears before a folder', () => {
      /**
       * page1
       * folder1
       *  └─ page11
       *
       * selected => page1
       */
      const folder1 = fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', order: 1 });
      const page11 = fakePortalNavigationPage({
        id: 'page-11',
        title: 'Page 11',
        parentId: 'folder-1',
        portalPageContentId: 'content-11',
        order: 1,
      });
      const page1 = fakePortalNavigationPage({
        id: 'page-1',
        title: 'Page 1',
        portalPageContentId: 'content-1',
        order: 0,
      });
      const items: PortalNavigationItem[] = [page1, folder1, page11];

      const result = findFirstAvailablePage(null, items);

      expect(result?.id).toBe('page-1');
    });

    it('selects page inside folder when folder appears before root page', () => {
      /**
       * folder1
       *  └─ page11
       * page1
       *
       * selected => page11
       */
      const folder1 = fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', order: 1 });
      const page1 = fakePortalNavigationPage({
        id: 'page-1',
        title: 'Page 1',
        portalPageContentId: 'content-1',
        order: 1,
      });
      const page11 = fakePortalNavigationPage({
        id: 'page-11',
        title: 'Page 11',
        portalPageContentId: 'content-11',
        order: 0,
        parentId: 'folder-1',
      });
      const items: PortalNavigationItem[] = [folder1, page1, page11];

      const result = findFirstAvailablePage(null, items);

      expect(result?.id).toBe('page-11');
    });

    it('selects deeply nested page when no page exists at higher levels', () => {
      /**
       * folder1
       *  └─ folder11
       *      ├─ page111
       *      └─ page112
       *
       * selected => page111
       */
      const folder1 = fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', order: 0 });
      const folder11 = fakePortalNavigationFolder({
        id: 'folder-11',
        title: 'Folder 11',
        parentId: 'folder-1',
        order: 0,
      });
      const page111 = fakePortalNavigationPage({
        id: 'page-111',
        title: 'Page 111',
        portalPageContentId: 'content-111',
        parentId: 'folder-11',
        order: 0,
      });
      const page112 = fakePortalNavigationPage({
        id: 'page-112',
        title: 'Page 112',
        portalPageContentId: 'content-112',
        order: 1,
        parentId: 'folder-111',
      });
      const items: PortalNavigationItem[] = [folder1, folder11, page111, page112];

      const result = findFirstAvailablePage(null, items);

      expect(result?.id).toBe('page-111');
    });

    it('returns null when no pages exist', () => {
      const folder1 = fakePortalNavigationFolder({ id: 'folder-1', title: 'Folder 1', order: 0 });
      const folder11 = fakePortalNavigationFolder({
        id: 'folder-11',
        parentId: 'folder-1',
        title: 'Folder 11',
        order: 0,
      });
      const items: PortalNavigationItem[] = [folder1, folder11];

      const result = findFirstAvailablePage(null, items);

      expect(result).toBeNull();
    });
  });

  describe('item reordering', () => {
    const page1 = fakePortalNavigationPage({
      id: 'page-1',
      title: 'Page 1',
      order: 0,
      portalPageContentId: 'content-1',
    });
    const page2 = fakePortalNavigationPage({
      id: 'page-2',
      title: 'Page 2',
      order: 1,
      portalPageContentId: 'content-2',
    });
    const fakeResponse = fakePortalNavigationItemsResponse({
      items: [page1, page2],
    });
    const page1Content = 'This is the content of Page 1';

    beforeEach(async () => {
      await expectGetNavigationItems(fakeResponse);
      expectGetPageContent(page1.portalPageContentId, page1Content);
    });

    it('should call backend update with new order when items are reordered', async () => {
      const component = fixture.componentInstance;

      // Simulate reordering: move "Page 2" to position 0
      const reorderedItems = [
        { ...fakeResponse.items[1], order: 0 },
        { ...fakeResponse.items[0], order: 1 },
      ];

      component.onNodeMoved({
        node: fakeSectionNode({ id: 'page-2', data: page2 }),
        newParentId: null,
        newOrder: 0,
      });
      fixture.detectChanges();

      expectPutPortalNavigationItem(
        'page-2',
        fakeUpdatePagePortalNavigationItem({
          title: page2.title,
          parentId: page2.parentId,
          order: 0,
          published: page2.published,
          visibility: page2.visibility,
        }),
        fakePortalNavigationPage({}),
      );

      // After update, component refreshes the list — satisfy the subsequent GET
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: reorderedItems }));
      expectGetPageContent('content-1', 'This is the content of Page 1');
    });

    describe('when user has edited the current page', () => {
      let component: PortalNavigationItemsComponent;
      beforeEach(async () => {
        await harness.setEditorContentText('Cats rule');
        component = fixture.componentInstance;
      });

      it('should show dialog when user has edited page content and then reordered', async () => {
        component.onNodeMoved({
          node: fakeSectionNode({ id: 'page-2', data: page2 }),
          newParentId: null,
          newOrder: 0,
        });
        fixture.detectChanges();

        expect(await rootLoader.getHarnessOrNull(GioConfirmDialogHarness)).toBeTruthy();
      });

      it('should not update order if user cancels dialog', async () => {
        component.onNodeMoved({
          node: fakeSectionNode({ id: 'page-2', data: page2 }),
          newParentId: null,
          newOrder: 0,
        });
        fixture.detectChanges();

        const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await dialog.cancel();
      });

      it('should update order of Page 2 and discard content changes on confirm', async () => {
        component.onNodeMoved({
          node: fakeSectionNode({ id: 'page-2', data: page2 }),
          newParentId: null,
          newOrder: 0,
        });
        fixture.detectChanges();

        const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await dialog.confirm();

        expectPutPortalNavigationItem(
          'page-2',
          fakeUpdatePagePortalNavigationItem({
            title: page2.title,
            parentId: page2.parentId,
            order: 0,
            published: page2.published,
            visibility: page2.visibility,
          }),
          fakePortalNavigationPage({}),
        );

        // After update, component refreshes the list — satisfy the subsequent GET
        await expectGetNavigationItems(
          fakePortalNavigationItemsResponse({
            items: [
              fakePortalNavigationPage({ id: 'page-1', portalPageContentId: page1.portalPageContentId, order: 1 }),
              fakePortalNavigationPage({ id: 'page-2', order: 0 }),
            ],
          }),
        );
        expectGetPageContent(page1.portalPageContentId, page1Content);

        expect(await harness.getEditorContentText()).toEqual(page1Content);
      });

      it('should update order of Page 1 and update Page 1 content if user confirms', async () => {
        component.onNodeMoved({
          node: fakeSectionNode({ id: 'page-1', data: page1 }),
          newParentId: null,
          newOrder: 1,
        });
        fixture.detectChanges();

        const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await dialog.confirm();

        expectPutPortalNavigationItem(
          'page-1',
          fakeUpdatePagePortalNavigationItem({
            title: page1.title,
            parentId: page1.parentId,
            order: 1,
            published: page1.published,
            visibility: page1.visibility,
          }),
          fakePortalNavigationPage({}),
        );

        await expectGetNavigationItems(
          fakePortalNavigationItemsResponse({
            items: [
              fakePortalNavigationPage({ id: 'page-1', portalPageContentId: page1.portalPageContentId, order: 1 }),
              fakePortalNavigationPage({ id: 'page-2', order: 0 }),
            ],
          }),
        );
        expectGetPageContent(page1.portalPageContentId, page1Content);

        expect(await harness.getEditorContentText()).toEqual(page1Content);
      });
    });
  });

  describe('creating API navigation items in bulk', () => {
    const folder = fakePortalNavigationFolder({ id: 'folder-1', title: 'API Folder' });

    beforeEach(async () => {
      await expectGetNavigationItems(
        fakePortalNavigationItemsResponse({
          items: [folder],
        }),
      );
    });

    it('should create multiple API navigation items using bulk endpoint', async () => {
      const apiIds = ['api-1', 'api-2', 'api-3'];
      const createdApis = [
        fakePortalNavigationApi({ id: 'nav-api-1', apiId: 'api-1', title: '', parentId: folder.id }),
        fakePortalNavigationApi({ id: 'nav-api-2', apiId: 'api-2', title: '', parentId: folder.id }),
        fakePortalNavigationApi({ id: 'nav-api-3', apiId: 'api-3', title: '', parentId: folder.id }),
      ];

      await harness.selectNavigationItemByTitle(folder.title);

      const component = fixture.componentInstance;
      const folderNode = { id: folder.id, label: folder.title, type: folder.type, data: folder } as any;
      component.onNodeMenuAction({ action: 'create', itemType: 'API', node: folderNode });
      fixture.detectChanges();
      await fixture.whenStable();

      await expectApiSearchResponse(apiIds);

      const checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
      await checkboxes[0].check();
      await checkboxes[1].check();
      await checkboxes[2].check();

      const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
      await dialog.clickSubmitButton();

      expectCreateNavigationItemsInBulk(
        apiIds.map(apiId => ({
          title: '',
          type: 'API',
          area: 'TOP_NAVBAR',
          parentId: folder.id,
          visibility: 'PUBLIC',
          apiId,
        })),
        fakePortalNavigationItemsResponse({ items: createdApis }),
      );

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [folder, ...createdApis] }));
    });

    it('should not call bulk endpoint when no folder is selected', async () => {
      const component = fixture.componentInstance as any;

      const obs = component.createApisInOrder(undefined, ['api-1']);
      let result: any;
      obs.subscribe(r => (result = r));

      expect(result).toBeNull();

      httpTestingController.expectNone({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/_bulk`,
      });
    });

    it('should not call bulk endpoint when apiIds array is empty', async () => {
      await harness.selectNavigationItemByTitle(folder.title);

      const component = fixture.componentInstance;
      const folderNode = { id: folder.id, label: folder.title, type: folder.type, data: folder } as any;
      component.onNodeMenuAction({ action: 'create', itemType: 'API', node: folderNode });
      fixture.detectChanges();
      await fixture.whenStable();

      await expectApiSearchResponse([]);

      const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
      expect(await dialog.isSubmitButtonDisabled()).toBeTruthy();

      httpTestingController.expectNone({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/_bulk`,
      });
    });

    it('should show error when bulk create fails', async () => {
      const apiIds = ['api-1', 'api-2'];

      await harness.selectNavigationItemByTitle(folder.title);

      const component = fixture.componentInstance;
      const folderNode = { id: folder.id, label: folder.title, type: folder.type, data: folder } as any;
      component.onNodeMenuAction({ action: 'create', itemType: 'API', node: folderNode });
      fixture.detectChanges();
      await fixture.whenStable();

      await expectApiSearchResponse(apiIds);

      const checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
      await checkboxes[0].check();
      await checkboxes[1].check();

      const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
      await dialog.clickSubmitButton();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/_bulk`,
      });
      expect(req.request.body).toEqual({
        items: apiIds.map(apiId => ({
          title: '',
          type: 'API',
          area: 'TOP_NAVBAR',
          parentId: folder.id,
          visibility: 'PUBLIC',
          apiId,
        })),
      });
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [folder] }));

      await fixture.whenStable();
      fixture.detectChanges();

      expect(document.body.textContent).toContain('Failed to create API navigation items');
    });
  });

  function fakeSectionNode(sectionNode: Partial<SectionNode>): SectionNode {
    return {
      id: 'node-1',
      label: 'Node 1',
      type: 'PAGE',
      data: fakePortalNavigationPage({ id: 'node-1', title: 'Node 1', portalPageContentId: 'node-1-content' }),
      children: [],
      ...sectionNode,
    };
  }

  async function expectGetNavigationItems(response: PortalNavigationItemsResponse = fakePortalNavigationItemsResponse()) {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR` })
      .flush(response);

    // Used for the navId query param handling for getting page content
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function expectGetPageContent(contentId: string, content: string) {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}` })
      .flush({ id: contentId, content });

    fixture.detectChanges();
  }

  function expectCreateNavigationItem(requestBody: NewPortalNavigationItem, result: PortalNavigationItem) {
    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items`,
    });
    expect(req.request.body).toEqual(requestBody);
    req.flush(result);
  }

  function expectCreateNavigationItemsInBulk(requestItems: NewPortalNavigationItem[], result: PortalNavigationItemsResponse) {
    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/_bulk`,
    });
    expect(req.request.body).toEqual({ items: requestItems });
    req.flush(result);
  }

  function expectPutPortalNavigationItem(id: string, expectedBody: UpdatePortalNavigationItem, response: PortalNavigationItem) {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${id}`,
    });
    expect(req.request.body).toEqual(expectedBody);
    req.flush(response);
  }

  function expectApiSearchResponse(apiIds: string[]) {
    const req = httpTestingController.expectOne(request => {
      return (
        request.method === 'POST' &&
        request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search` &&
        (request.params.get('page') ?? '1') === '1' &&
        (request.params.get('perPage') ?? '10') === '10' &&
        (request.params.get('manageOnly') ?? 'false') === 'false'
      );
    });

    expect(req.request.body).toEqual({ query: '' });

    req.flush({
      data: apiIds.map(id => ({ id, name: id })),
      pagination: { totalCount: apiIds.length },
    });

    fixture.detectChanges();
  }

  it('should have unsaved changes when content is modified', async () => {
    await expectGetNavigationItems({
      items: [fakePortalNavigationPage({ id: 'page-1', title: 'Page 1', portalPageContentId: 'content-1' })],
    });
    expectGetPageContent('content-1', 'Initial content');

    expect(component.hasUnsavedChanges()).toBeFalsy();

    component.contentControl.setValue('Modified content');
    component.contentControl.markAsDirty();
    expect(component.hasUnsavedChanges()).toBeTruthy();

    component.contentControl.markAsPristine();
    expect(component.hasUnsavedChanges()).toBeTruthy();
  });

  it('should not have unsaved changes when content is modified and then reverted to initial value', async () => {
    await expectGetNavigationItems({
      items: [fakePortalNavigationPage({ id: 'page-1', title: 'Page 1', portalPageContentId: 'content-1' })],
    });
    expectGetPageContent('content-1', 'Initial content');

    expect(component.hasUnsavedChanges()).toBeFalsy();

    component.contentControl.setValue('Modified content');
    component.contentControl.markAsDirty();
    expect(component.hasUnsavedChanges()).toBeTruthy();

    component.contentControl.setValue('Initial content');
    expect(component.hasUnsavedChanges()).toBeFalsy();
  });

  it('should not have unsaved changes after opening and closing the edit dialog without changes', async () => {
    await expectGetNavigationItems({
      items: [fakePortalNavigationPage({ id: 'page-1', title: 'Page 1', portalPageContentId: 'content-1' })],
    });
    expectGetPageContent('content-1', 'Initial content');

    expect(component.hasUnsavedChanges()).toBeFalsy();

    await harness.clickEditButton();
    const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
    await dialog.clickCancelButton();

    expect(component.hasUnsavedChanges()).toBeFalsy();
  });

  describe('calling onAddSection with API type', () => {
    it('should not open any dialog when onAddSection is called with API type', async () => {
      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [] }));

      component.onAddSection('API');
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ApiSectionEditorDialogHarness);
      expect(dialog).toBeNull();
    });
  });

  describe('moving an API node under another API node', () => {
    it('should show an error and refresh the list when an API is moved under another API', async () => {
      const apiItem1 = fakePortalNavigationApi({ id: 'api-nav-1', title: 'API 1', apiId: 'api-id-1' });
      const apiItem2 = fakePortalNavigationApi({ id: 'api-nav-2', title: 'API 2', apiId: 'api-id-2' });

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [apiItem1, apiItem2] }));

      component.onNodeMoved({
        node: { id: apiItem2.id, label: apiItem2.title, type: 'API', data: apiItem2 },
        newParentId: apiItem1.id,
        newOrder: 0,
      });
      fixture.detectChanges();

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [apiItem1, apiItem2] }));

      expect(document.body.textContent).toContain('API cannot be moved under an API navigation item');
    });
  });

  describe('editing an API item from the toolbar', () => {
    it('should open SectionEditorDialog when Edit is clicked on a selected API item', async () => {
      const apiItem = fakePortalNavigationApi({ id: 'api-nav-1', title: 'My API', apiId: 'api-id-1' });

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [apiItem] }));

      const apiNode = { id: apiItem.id, label: apiItem.title, type: 'API', data: apiItem } as any;
      component.onNodeMenuAction({ action: 'edit', itemType: 'API', node: apiNode });
      fixture.detectChanges();
      await fixture.whenStable();

      const dialog = await rootLoader.getHarnessOrNull(SectionEditorDialogHarness);
      expect(dialog).toBeTruthy();
    });
  });

  describe('navigation items load failure', () => {
    it('should show an empty tree when loading navigation items fails', async () => {
      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR` })
        .flush({ message: 'Server Error' }, { status: 500, statusText: 'Server Error' });

      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.isNavigationTreeEmpty()).toBe(true);
    });
  });

  describe('publish/unpublish action on API type node', () => {
    it('should not open a confirm dialog when publish is triggered on an API type node', async () => {
      const apiItem = fakePortalNavigationApi({ id: 'api-nav-1', title: 'My API', apiId: 'api-id-1' });

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: [apiItem] }));

      component.onNodeMenuAction({
        action: 'publish',
        itemType: 'API',
        node: { id: apiItem.id, label: apiItem.title, type: 'API', data: apiItem },
      });
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(GioConfirmDialogHarness);
      expect(dialog).toBeNull();
      httpTestingController.expectNone({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${apiItem.id}`,
      });
    });
  });
});
