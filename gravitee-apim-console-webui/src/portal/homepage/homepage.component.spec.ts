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
import { ConfigureTestingGraviteeMarkdownEditor, GraviteeMarkdownEditorHarness } from '@gravitee/gravitee-markdown';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';

import { HomepageComponent } from './homepage.component';

import { GioTestingModule, CONSTANTS_TESTING } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { fakePortalPageWithDetails } from '../../entities/portal/portal-page-with-details.fixture';
import { PatchPortalPage } from '../../entities/portal/patch-portal-page';
import { PortalPageWithDetails } from '../../entities/portal/portal-page-with-details';
import { SnackBarService } from '../../services-ngx/snack-bar.service';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let snackBarService: SnackBarService;

  const init = async (canUpdate: boolean, portalPage = fakePortalPageWithDetails()) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, HomepageComponent],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(canUpdate),
          },
        },
      ],
    }).compileComponents();

    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(HomepageComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture); // Used for dialogs

    // Spy on snackbar
    snackBarService = TestBed.inject(SnackBarService);
    jest.spyOn(snackBarService, 'success');
    jest.spyOn(snackBarService, 'error');

    fixture.detectChanges();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages?type=homepage&expands=content`,
      })
      .flush({ pages: [portalPage] });
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load homepage content from API', async () => {
    const fakePortalPage = fakePortalPageWithDetails({
      content: '# Welcome to Gravitee -- This is the homepage content from API.',
    });

    await init(true, fakePortalPage);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.getEditorValue()).toEqual(fakePortalPage.content);
  });

  it('should disable editor when user has no update permission', async () => {
    const fakePortalPage = fakePortalPageWithDetails();
    await init(false, fakePortalPage);
    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(true);
  });

  it('should disable editor when content has not changed or is empty', async () => {
    await init(true, fakePortalPageWithDetails({ content: '# Hello world' }));

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    await editorHarness.setEditorValue('Updated page content');

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();

    await editorHarness.setEditorValue('# Hello world');
    expect(await saveButton.isDisabled()).toBeTruthy();

    await editorHarness.setEditorValue('');
    expect(await saveButton.isDisabled()).toBeTruthy();

    await editorHarness.setEditorValue('     ');
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should enable editor when user has update permission', async () => {
    await init(true);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(false);

    await editorHarness.setEditorValue('Updated page content');
    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();
  });

  it('should update home page content', async () => {
    const page = fakePortalPageWithDetails();
    const updatedContent = 'Updated page content';
    await init(true, page);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    await editorHarness.setEditorValue(updatedContent);

    const saveButton = await getSaveButton();
    await saveButton.click();

    expectPortalPageUpdate({ content: updatedContent }, { ...page, content: updatedContent });
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  describe('togglePublish functionality', () => {
    it('should publish an unpublished page after confirmation', async () => {
      const unpublishedPage = fakePortalPageWithDetails({ published: false });
      await init(true, unpublishedPage);

      const toggleButton = await getToggleButton();
      expect(await toggleButton.getText()).toBe('Publish');
      await toggleButton.click();

      await confirmDialog();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPageWithDetails().id}/_publish`,
      });

      const publishedPage = { ...unpublishedPage, published: true };
      req.flush(publishedPage);
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Page has been published successfully.');
      expect(await toggleButton.getText()).toBe('Unpublish');
    });

    it('should unpublish a published page after confirmation', async () => {
      const publishedPage = fakePortalPageWithDetails({ published: true });
      await init(true, publishedPage);

      const toggleButton = await getToggleButton();
      expect(await toggleButton.getText()).toBe('Unpublish');

      await toggleButton.click();

      await confirmDialog();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPageWithDetails().id}/_unpublish`,
      });
      const unpublishedPage = { ...publishedPage, published: false };
      req.flush(unpublishedPage);
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Page has been unpublished successfully.');
      expect(await toggleButton.getText()).toBe('Publish');
    });

    it('should not perform any action if the confirmation dialog is cancelled', async () => {
      const unpublishedPage = fakePortalPageWithDetails({ published: false });
      await init(true, unpublishedPage);

      const toggleButton = await getToggleButton();
      await toggleButton.click();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog.close(); // Simulates clicking cancel or escape

      // No other HTTP requests should be made
      httpTestingController.verify();
    });

    it('should show an error message if publishing fails', async () => {
      await init(true, fakePortalPageWithDetails({ published: false }));

      const toggleButton = await getToggleButton();
      await toggleButton.click();
      await confirmDialog();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPageWithDetails().id}/_publish`,
      });
      req.flush({ message: 'API error on publish' }, { status: 500, statusText: 'Server Error' });

      expect(snackBarService.error).toHaveBeenCalledWith('API error on publish');
    });

    it('should show an error message if unpublishing fails', async () => {
      await init(true, fakePortalPageWithDetails({ published: true }));

      const toggleButton = await getToggleButton();
      await toggleButton.click();
      await confirmDialog();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPageWithDetails().id}/_unpublish`,
      });
      req.flush({ message: 'API error on unpublish' }, { status: 400, statusText: 'Bad Request' });

      expect(snackBarService.error).toHaveBeenCalledWith('API error on unpublish');
    });

    it('should disable publish/unpublish button if user lacks permission', async () => {
      await init(false, fakePortalPageWithDetails({ published: true }));

      const toggleButton = await getToggleButton();
      expect(await toggleButton.isDisabled()).toBe(true);
    });

    it('should disable publish/unpublish button homepage data is null', async () => {
      await init(true, null);

      const toggleButton = await getToggleButton();
      expect(await toggleButton.isDisabled()).toBe(true);
    });
  });

  async function getToggleButton() {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=toggle-publish-button]' }));
  }

  async function confirmDialog() {
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Publish|Unpublish/ }));
    await confirmButton.click();
  }

  async function getSaveButton() {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Update portal page"]' }));
  }

  function expectPortalPageUpdate(expected: PatchPortalPage, response: PortalPageWithDetails) {
    const req = httpTestingController.expectOne({
      method: 'PATCH',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${response.id}`,
    });
    expect(req.request.body).toStrictEqual(expected);
    req.flush(response);
  }
});
