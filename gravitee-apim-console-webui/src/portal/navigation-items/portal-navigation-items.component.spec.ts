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
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

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
  fakeUpdatePagePortalNavigationItem,
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
          useValue: ['environment-documentation-c', 'environment-documentation-u'],
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
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
          fakePortalNavigationLink({ id: 'nav-item-2', title: 'Nav Item 2', url: 'https://old.com', area: 'TOP_NAVBAR' }),
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
        },
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

  describe('creating a page under a folder from tree node "More actions" menu', () => {
    it('opens create dialog and does not call API when cancelled', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
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
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
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
        fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE', parentId: folderData.id }),
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
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
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
        { title, area: 'TOP_NAVBAR', type: 'FOLDER', parentId: folderData.id } as NewPortalNavigationItem,
        createdItem,
      );
      await expectGetNavigationItems(fakeResponse);
    });
  });

  describe('creating a link under a folder from tree node "More actions" menu', () => {
    it('calls backend create with parentId when dialog is submitted', async () => {
      const fakeResponse = fakePortalNavigationItemsResponse({
        items: [
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
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
          fakePortalNavigationPage({ id: 'nav-item-1', title: 'Nav Item 1', portalPageContentId: 'nav-item-1-content' }),
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
        },
        fakePortalNavigationFolder({ id: folderData.id, title: 'Updated Folder', area: folderData.area, type: 'FOLDER' }),
      );

      await expectGetNavigationItems(fakePortalNavigationItemsResponse({ items: fakeResponse.items }));
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

  function expectPutPortalNavigationItem(id: string, expectedBody: any, response: PortalNavigationItem) {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${id}`,
    });
    expect(req.request.body).toEqual(expectedBody);
    req.flush(response);
  }
});
