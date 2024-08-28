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
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatStepperHarness } from '@angular/material/stepper/testing';
import { FormsModule } from '@angular/forms';
import { GioMonacoEditorHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

import { DocumentationNewPageHarness } from './documentation-new-page.harness';
import { DocumentationNewPageComponent } from './documentation-new-page.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { Breadcrumb, Page, fakeMarkdown, fakeApiV4, Group, fakeGroupsResponse, fakeGroup } from '../../../../../entities/management-api-v2';
import { ApiDocumentationV4ContentEditorHarness } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.harness';
import { ApiDocumentationV4BreadcrumbHarness } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';
import { ApiDocumentationV4FileUploadHarness } from '../api-documentation-v4-file-upload/api-documentation-v4-file-upload.harness';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDocumentationV4PageConfigurationHarness } from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.harness';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
  parentId?: string;
  mode?: 'create' | 'edit';
}

describe('DocumentationNewPageComponent', () => {
  let fixture: ComponentFixture<DocumentationNewPageComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    parentId: string,
    pageId: string,
    apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
    homepage: boolean = false,
  ) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, FormsModule, GioTestingModule, CommonModule, DocumentationNewPageComponent],
      providers: [
        // {
        //   provide: ActivatedRoute,
        //   useValue: { snapshot: { params: { apiId: API_ID, pageId }, queryParams: { parentId, pageType: 'MARKDOWN' } } },
        // },
        { provide: GioTestingPermissionProvider, useValue: apiPermissions },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DocumentationNewPageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.componentInstance.api = fakeApiV4({ id: API_ID, lifecycleState: 'PUBLISHED' });
    fixture.componentInstance.goBackRouterLink = [];
    fixture.componentInstance.createHomepage = homepage;
    fixture.componentInstance.pageType = 'MARKDOWN';
    fixture.componentInstance.parentId = parentId;
    fixture.detectChanges();
  };

  const initPageServiceRequests = (input: InitInput) => {
    expectGetPages(input.pages, input.breadcrumb, input.parentId);
    expectGetGroups([fakeGroup({ id: 'group-1', name: 'group 1' }), fakeGroup({ id: 'group-2', name: 'group 2' })]);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create page', () => {
    describe('In the root folder of the API', () => {
      const EXISTING_PAGE = fakeMarkdown({ id: 'page-id', name: 'page-name' });

      beforeEach(async () => {
        await init('ROOT', undefined);
        initPageServiceRequests({ pages: [EXISTING_PAGE], breadcrumb: [], parentId: 'ROOT' });
      });

      it('should have 3 steps', async () => {
        const stepper = await harnessLoader.getHarness(MatStepperHarness);
        const steps = await Promise.all(await stepper.getSteps());
        expect(steps.length).toEqual(3);
        expect(await steps[0].getLabel()).toEqual('Configure page');
        expect(await steps[1].getLabel()).toEqual('Determine source');
        expect(await steps[2].getLabel()).toEqual('Add content');
      });

      it('should exit without saving', async () => {
        const exitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Exit without saving' }));
        await exitBtn.click();

        expect(routerNavigateSpy).toHaveBeenCalledWith([], expect.anything());
      });

      describe('step 1 - Configure page', () => {
        it('should set name and visibility', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);

          expect(getPageTitle().includes('Add new page')).toBeTruthy();

          const nextBtn = await harness.getNextButton();
          expect(await nextBtn.isDisabled()).toEqual(true);

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName('New page');
          await pageConfiguration.checkVisibility('PRIVATE');

          expect(await nextBtn.isDisabled()).toEqual(false);
          expect(await nextBtn.click());
          expect(fixture.componentInstance.form.getRawValue()).toEqual({
            stepOne: {
              name: 'New page',
              visibility: 'PRIVATE',
              accessControlGroups: [],
              excludeGroups: false,
            },
            content: '',
            source: 'FILL',
            sourceConfiguration: {},
          });

          expect(getPageTitle().includes('New page')).toBeTruthy();
        });
      });

      describe('step 2 - Determine source', () => {
        beforeEach(async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName('New page');

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });
          fixture.detectChanges();
        });

        it('should select source', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
          const options = await harness.getSourceOptions();

          expect(options.length).toEqual(3);
          const sourceOptions = options.map((option) => option.text);
          expect(sourceOptions).toEqual(['Fill in the content myself', 'Import from file', 'Import from URL']);
          expect(await options[0].disabled).toEqual(false);
          expect(await options[1].disabled).toEqual(false);
          expect(await options[2].disabled).toEqual(false);

          await harness.selectSource('IMPORT');
        });
      });

      describe('step 3 - Fill content ', () => {
        beforeEach(async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName('New page');
          await pageConfiguration.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await pageConfiguration.getAccessControlGroups();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 1' });

          const toggleExcludeGroups = await pageConfiguration.getExcludeGroups();
          await toggleExcludeGroups.toggle();

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });
        });

        it('should show markdown editor', async () => {
          const editor = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor());
          expect(editor).toBeDefined();

          await editor.setValue('#TITLE \n This is the file content');
          expect(fixture.componentInstance.form.getRawValue().content).toEqual('#TITLE  This is the file content');
        });

        it('should show markdown preview', async () => {
          const preview = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((editor) => editor.getMarkdownPreview());
          expect(preview).toBeTruthy();

          const togglePreviewButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Toggle preview' }));
          await togglePreviewButton.click();

          const previewAfter = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((editor) => editor.getMarkdownPreview());

          expect(previewAfter).toBeFalsy();
        });

        it('should save content', async () => {
          const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
          await editor.setValue('#TITLE \n This is the file content');

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          req.flush({});
          expect(req.request.body).toEqual({
            type: 'MARKDOWN',
            name: 'New page',
            visibility: 'PRIVATE',
            content: '#TITLE  This is the file content', // TODO: check why \n is removed
            parentId: 'ROOT',
            accessControls: [{ referenceId: 'group-1', referenceType: 'GROUP' }],
            excludedAccessControls: true,
            homepage: false,
          });
        });

        it('should save and publish content', async () => {
          const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
          await editor.setValue('#TITLE \n This is the file content');

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          const page = fakeMarkdown({ id: 'page-id' });
          req.flush(page);
          expect(req.request.body).toEqual({
            type: 'MARKDOWN',
            name: 'New page',
            visibility: 'PRIVATE',
            content: '#TITLE  This is the file content', // TODO: check why \n is removed
            parentId: 'ROOT',
            accessControls: [{ referenceId: 'group-1', referenceType: 'GROUP' }],
            excludedAccessControls: true,
            homepage: false,
          });

          const publishReq = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
          });
          publishReq.flush({ ...page, published: true });
        });

        it('should show error if raised on save', async () => {
          const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
          await editor.setValue('unsafe content');

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          req.flush({ message: 'Cannot save unsafe content' }, { status: 400, statusText: 'Cannot save unsafe content' });
          fixture.detectChanges();

          const snackBar = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
          expect(await snackBar.getMessage()).toEqual('Cannot save unsafe content');
        });

        it('should show error if raised on publish', async () => {
          const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
          await editor.setValue('unsafe content');

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const page = fakeMarkdown({ id: 'page-id' });
          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          req.flush(page);

          const publishReq = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
          });
          publishReq.flush({ message: 'Error on publish' }, { status: 400, statusText: 'Error on publish' });

          fixture.detectChanges();

          const snackBar = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
          expect(await snackBar.getMessage()).toEqual('Error on publish');
        });
      });

      describe('step 3 - Import content ', () => {
        beforeEach(async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName('New page');

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });

          await harness.selectSource('IMPORT');

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });
        });

        it('should show import', async () => {
          const fileSelector = await harnessLoader
            .getHarness(ApiDocumentationV4FileUploadHarness)
            .then((harness) => harness.getFileSelector());
          expect(fileSelector).toBeDefined();

          const file = new File(['# Markdown content'], 'readme.md', { type: 'text/markdown' });
          await fileSelector.dropFiles([file]);
          expect(fixture.componentInstance.form.getRawValue().content).toEqual('# Markdown content');
        });
      });

      describe('step 3 - Import from URL ', () => {
        let harness: DocumentationNewPageHarness;
        const openApiUrl = 'https://openapi.yml';
        const http = 'HTTP';
        const pageName = 'New page';
        const emptyUrlSaveErrorMessage = 'Cannot save without a url';
        const emptyUrlPublishErrorMessage = 'Cannot publish with empty URL';
        beforeEach(async () => {
          harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName(pageName);

          let nextBtn = await harness.getNextButton();
          expect(await nextBtn.isDisabled()).toEqual(false);
          await nextBtn.click();

          await harness.selectSource(http);

          const req = httpTestingController.expectOne({
            method: 'GET',
            url: `${CONSTANTS_TESTING.env.baseURL}/fetchers?expand=schema`,
          });

          req.flush([
            {
              id: 'http-fetcher',
              name: http,
              description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
              version: '2.0.1',
              schema:
                '{"type": "object","title": "http","properties": {"url": {"title": "URL","description": "Url to the file you want to fetch","type": "string"}}}',
            },
          ]);

          fixture.detectChanges();

          nextBtn = await harness.getNextButton();
          expect(await nextBtn.isDisabled()).toEqual(false);
          await nextBtn.click();
        });

        it('should show http url', async () => {
          const httpUrlInput = await harness.getHttpUrlHarness();
          expect(httpUrlInput).toBeDefined();
          expect(await httpUrlInput.isDisabled()).toEqual(false);
          await httpUrlInput.setValue(openApiUrl);
          expect(await httpUrlInput.getValue()).toEqual(openApiUrl);
        });

        it('should save', async () => {
          const httpUrlInput = await harness.getHttpUrlHarness();
          await httpUrlInput.setValue(http);

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          req.flush({});
          expect(req.request.body).toEqual({
            type: 'MARKDOWN',
            name: pageName,
            visibility: 'PUBLIC',
            content: '',
            parentId: 'ROOT',
            source: {
              configuration: {
                url: http,
              },
              type: 'http-fetcher',
            },
            accessControls: [],
            excludedAccessControls: false,
            homepage: false,
          });
        });

        it('should save and publish', async () => {
          const httpUrlInput = await harness.getHttpUrlHarness();
          await httpUrlInput.setValue(http);

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          const page = fakeMarkdown({ id: 'page-id' });
          req.flush(page);
          expect(req.request.body).toEqual({
            type: 'MARKDOWN',
            name: pageName,
            visibility: 'PUBLIC',
            content: '',
            parentId: 'ROOT',
            source: {
              configuration: {
                url: http,
              },
              type: 'http-fetcher',
            },
            accessControls: [],
            excludedAccessControls: false,
            homepage: false,
          });

          const publishReq = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
          });
          publishReq.flush({ ...page, published: true });
        });

        it('should show error if raised on save', async () => {
          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          req.flush({ message: emptyUrlSaveErrorMessage }, { status: 400, statusText: emptyUrlSaveErrorMessage });
          fixture.detectChanges();

          const snackBar = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
          expect(await snackBar.getMessage()).toEqual(emptyUrlSaveErrorMessage);
        });

        it('should show error if raised on publish', async () => {
          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          const req = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
          });

          const page = fakeMarkdown({ id: 'page-id' });
          req.flush(page);
          expect(req.request.body).toEqual({
            type: 'MARKDOWN',
            name: pageName,
            visibility: 'PUBLIC',
            content: '',
            parentId: 'ROOT',
            source: {
              configuration: {},
              type: 'http-fetcher',
            },
            accessControls: [],
            excludedAccessControls: false,
            homepage: false,
          });

          const publishReq = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
          });
          publishReq.flush(
            { message: emptyUrlPublishErrorMessage },
            {
              status: 400,
              statusText: emptyUrlPublishErrorMessage,
            },
          );

          fixture.detectChanges();

          const snackBar = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
          expect(await snackBar.getMessage()).toEqual(emptyUrlPublishErrorMessage);
        });
      });
    });
    describe('Under another folder', () => {
      beforeEach(async () => {
        await init('parent-folder-id', undefined);
        initPageServiceRequests({
          pages: [],
          breadcrumb: [{ name: 'Parent Folder', id: 'parent-folder-id', position: 1 }],
          parentId: 'parent-folder-id',
        });
      });

      it('should show breadcrumb', async () => {
        const harness = await harnessLoader.getHarness(ApiDocumentationV4BreadcrumbHarness);
        expect(await harness.getContent()).toEqual('Home > Parent Folder');
      });

      it('should save page in the correct folder', async () => {
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
        const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
        await pageConfiguration.setName('New page');

        await harness.getNextButton().then(async (btn) => btn.click());
        await harness.getNextButton().then(async (btn) => btn.click());

        const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
        await editor.setValue('File content');

        const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
        expect(await saveBtn.isDisabled()).toEqual(false);
        await saveBtn.click();

        const req = httpTestingController.expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        });

        req.flush({});
        expect(req.request.body).toEqual({
          type: 'MARKDOWN',
          name: 'New page',
          visibility: 'PUBLIC',
          content: 'File content',
          parentId: 'parent-folder-id',
          accessControls: [],
          excludedAccessControls: false,
          homepage: false,
        });
      });
    });
  });
  describe('Create homepage', () => {
    const EXISTING_PAGE = fakeMarkdown({ id: 'page-id', name: 'page-name' });

    beforeEach(async () => {
      await init('ROOT', undefined, undefined, true);
      initPageServiceRequests({ pages: [EXISTING_PAGE], breadcrumb: [], parentId: 'ROOT' });
    });

    describe('step 1 - Configure page', () => {
      it('should not show name', async () => {
        expect(getPageTitle().includes('Homepage')).toBeTruthy();
        const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
        expect(await pageConfiguration.nameFieldDisplayed()).toEqual(false);
      });
      it('should set visibility', async () => {
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
        const nextBtn = await harness.getNextButton();
        expect(await nextBtn.isDisabled()).toEqual(false);

        const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
        await pageConfiguration.checkVisibility('PRIVATE');

        expect(await nextBtn.isDisabled()).toEqual(false);
        expect(await nextBtn.click());
        expect(fixture.componentInstance.form.getRawValue()).toEqual({
          stepOne: {
            name: 'Homepage',
            visibility: 'PRIVATE',
            accessControlGroups: [],
            excludeGroups: false,
          },
          content: '',
          source: 'FILL',
          sourceConfiguration: {},
        });
      });
    });

    describe('step 2 - Determine source', () => {
      beforeEach(async () => {
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
        await harness.getNextButton().then(async (btn) => {
          expect(await btn.isDisabled()).toEqual(false);
          return btn.click();
        });
        fixture.detectChanges();
      });

      it('should select source', async () => {
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
        const options = await harness.getSourceOptions();

        expect(options.length).toEqual(3);
        const sourceOptions = options.map((option) => option.text);
        expect(sourceOptions).toEqual(['Fill in the content myself', 'Import from file', 'Import from URL']);
        expect(await options[0].disabled).toEqual(false);
        expect(await options[1].disabled).toEqual(false);
        expect(await options[2].disabled).toEqual(false);

        await harness.selectSource('IMPORT');
      });
    });

    describe('step 3 - Fill content ', () => {
      beforeEach(async () => {
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationNewPageHarness);
        const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
        await pageConfiguration.checkVisibility('PRIVATE');

        const selectAccessControlGroups = await pageConfiguration.getAccessControlGroups();
        await selectAccessControlGroups.open();
        await selectAccessControlGroups.clickOptions({ text: 'group 1' });

        const toggleExcludeGroups = await pageConfiguration.getExcludeGroups();
        await toggleExcludeGroups.toggle();

        await harness.getNextButton().then(async (btn) => {
          expect(await btn.isDisabled()).toEqual(false);
          return btn.click();
        });

        await harness.getNextButton().then(async (btn) => {
          expect(await btn.isDisabled()).toEqual(false);
          return btn.click();
        });
      });

      it('should save content', async () => {
        const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
        await editor.setValue('#TITLE \n This is the file content');

        const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
        expect(await saveBtn.isDisabled()).toEqual(false);
        await saveBtn.click();

        const req = httpTestingController.expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        });

        req.flush({});
        expect(req.request.body).toEqual({
          type: 'MARKDOWN',
          name: 'Homepage',
          visibility: 'PRIVATE',
          content: '#TITLE  This is the file content', // TODO: check why \n is removed
          parentId: 'ROOT',
          accessControls: [{ referenceId: 'group-1', referenceType: 'GROUP' }],
          excludedAccessControls: true,
          homepage: true,
        });
      });

      it('should save and publish content', async () => {
        const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
        await editor.setValue('#TITLE \n This is the file content');

        const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
        expect(await saveBtn.isDisabled()).toEqual(false);
        await saveBtn.click();

        const req = httpTestingController.expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        });

        const page = fakeMarkdown({ id: 'page-id' });
        req.flush(page);
        expect(req.request.body).toEqual({
          type: 'MARKDOWN',
          name: 'Homepage',
          visibility: 'PRIVATE',
          content: '#TITLE  This is the file content', // TODO: check why \n is removed
          parentId: 'ROOT',
          accessControls: [{ referenceId: 'group-1', referenceType: 'GROUP' }],
          excludedAccessControls: true,
          homepage: true,
        });

        const publishReq = httpTestingController.expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
        });
        publishReq.flush({ ...page, published: true });
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

  const expectGetGroups = (groups: Group[]) => {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=999`,
      })
      .flush(fakeGroupsResponse({ data: groups }));
  };

  const getPageTitle = (): string => {
    return fixture.nativeElement.querySelector('h3').innerHTML;
  };
});
