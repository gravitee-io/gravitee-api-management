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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormsModule } from '@angular/forms';
import { GioConfirmDialogHarness, GioMonacoEditorHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { set } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiDocumentationV4EditPageHarness } from './api-documentation-v4-edit-page.harness';
import { ApiDocumentationV4EditPageComponent } from './api-documentation-v4-edit-page.component';

import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import {
  Breadcrumb,
  Page,
  fakeFolder,
  fakeMarkdown,
  fakeApiV4,
  Group,
  fakeGroupsResponse,
  fakeGroup,
} from '../../../../entities/management-api-v2';
import { ApiDocumentationV4ContentEditorHarness } from '../components/api-documentation-v4-content-editor/api-documentation-v4-content-editor.harness';
import { ApiDocumentationV4BreadcrumbHarness } from '../documentation-custom-page/api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';
import { ApiDocumentationV4PageTitleHarness } from '../components/api-documentation-v4-page-title/api-documentation-v4-page-title.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDocumentationV4FileUploadHarness } from '../components/api-documentation-v4-file-upload/api-documentation-v4-file-upload.harness';
import { Constants } from '../../../../entities/Constants';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
  parentId?: string;
  mode?: 'create' | 'edit';
}

describe('ApiDocumentationV4EditPageComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4EditPageComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    parentId: string,
    pageId: string,
    portalUrl = 'portal.url',
    apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
  ) => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4EditPageComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, FormsModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID, pageId }, queryParams: { parentId, pageType: 'MARKDOWN' } } },
        },
        { provide: GioTestingPermissionProvider, useValue: apiPermissions },
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
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4EditPageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  };

  const initPageServiceRequests = (input: InitInput, page: Page = {}) => {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      })
      .flush(fakeApiV4({ id: API_ID, lifecycleState: 'PUBLISHED' }));

    if (input.mode === 'edit') {
      expectGetPage(page);
    }
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

        expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
          relativeTo: expect.anything(),
          queryParams: { parentId: 'ROOT' },
        });
      });

      describe('step 1 - Configure page', () => {
        it('should set name and visibility', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);

          expect(getPageTitle().includes('Add new page')).toBeTruthy();

          const nextBtn = await harness.getNextButton();
          expect(await nextBtn.isDisabled()).toEqual(true);

          // In creation mode, delete button should not be present
          expect(await harness.getDeleteButton()).toBeUndefined();

          await harness.setName('New page');
          await harness.checkVisibility('PRIVATE');

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

        it('should not allow duplicate name', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName(EXISTING_PAGE.name);
          expect(await harness.getNextButton().then((btn) => btn.isDisabled())).toEqual(true);
        });

        it('should not show select groups + exclude groups if public', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);

          await harness.setName('New page');

          expect(await harness.getAccessControlGroups()).toBeFalsy();
          expect(await harness.getExcludeGroups()).toBeFalsy();

          const nextBtn = await harness.getNextButton();

          expect(await nextBtn.isDisabled()).toEqual(false);
          expect(await nextBtn.click());
          expect(fixture.componentInstance.form.getRawValue()).toEqual({
            stepOne: {
              name: 'New page',
              visibility: 'PUBLIC',
              accessControlGroups: [],
              excludeGroups: false,
            },
            content: '',
            source: 'FILL',
            sourceConfiguration: {},
          });
        });

        it('should select groups and set exclude groups if private', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);

          await harness.setName('New page');
          await harness.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await harness.getAccessControlGroups();
          expect(selectAccessControlGroups).toBeTruthy();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 1' });

          const toggleExcludeGroups = await harness.getExcludeGroups();
          expect(toggleExcludeGroups).toBeTruthy();
          await toggleExcludeGroups.toggle();

          const nextBtn = await harness.getNextButton();

          expect(await nextBtn.isDisabled()).toEqual(false);
          expect(await nextBtn.click());
          expect(fixture.componentInstance.form.getRawValue()).toEqual({
            stepOne: {
              name: 'New page',
              visibility: 'PRIVATE',
              accessControlGroups: ['group-1'],
              excludeGroups: true,
            },
            content: '',
            source: 'FILL',
            sourceConfiguration: {},
          });
        });
      });

      describe('step 2 - Determine source', () => {
        beforeEach(async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('New page');

          await harness.getNextButton().then(async (btn) => {
            expect(await btn.isDisabled()).toEqual(false);
            return btn.click();
          });
          fixture.detectChanges();
        });

        it('should select source', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
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
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('New page');
          await harness.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await harness.getAccessControlGroups();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 1' });

          const toggleExcludeGroups = await harness.getExcludeGroups();
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
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('New page');

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
        let harness: ApiDocumentationV4EditPageHarness;
        const openApiUrl = 'https://openapi.yml';
        const http = 'HTTP';
        const pageName = 'New page';
        const emptyUrlSaveErrorMessage = 'Cannot save without a url';
        const emptyUrlPublishErrorMessage = 'Cannot publish with empty URL';
        beforeEach(async () => {
          harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName(pageName);

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
          });

          const publishReq = httpTestingController.expectOne({
            method: 'POST',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}/_publish`,
          });
          publishReq.flush({ message: emptyUrlPublishErrorMessage }, { status: 400, statusText: emptyUrlPublishErrorMessage });

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
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
        await harness.setName('New page');

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
        });
      });
    });
  });

  describe('Edit page', () => {
    describe('In the root folder', () => {
      describe('with published page', () => {
        const PAGE = fakeMarkdown({
          id: 'page-id',
          name: 'page-name',
          content: 'my content',
          visibility: 'PUBLIC',
          published: true,
          accessControls: [
            { referenceId: 'group-1', referenceType: 'GROUP' },
            { referenceId: 'role-1', referenceType: 'ROLE' },
          ],
          excludedAccessControls: true,
        });
        const OTHER_PAGE = fakeMarkdown({
          id: 'other-page',
          name: 'other-page-name',
          content: 'my other content',
          visibility: 'PUBLIC',
          published: true,
        });

        beforeEach(async () => {
          await init(undefined, PAGE.id);
          initPageServiceRequests({ pages: [PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);
        });

        it('should have 2 steps', async () => {
          const stepper = await harnessLoader.getHarness(MatStepperHarness);
          const steps = await Promise.all(await stepper.getSteps());
          expect(steps.length).toEqual(2);
          expect(await steps[0].getLabel()).toEqual('Configure page');
          expect(await steps[1].getLabel()).toEqual('Edit content');
        });

        it('should load step one with existing name and visibility', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          expect(await harness.getName()).toEqual(PAGE.name);
          expect(await harness.getVisibility()).toEqual(PAGE.visibility);
        });

        it('should load step one and have Next button clickable without any changes', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          expect(await harness.getNextButton().then((btn) => btn.isDisabled())).toEqual(false);
        });

        it('should not have Next button clickable with name blank', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('');
          expect(await harness.getNextButton().then((btn) => btn.isDisabled())).toEqual(true);
        });

        it('should not have Next button clickable with duplicate name', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName(' Other-page-Name  ');
          expect(await harness.getNextButton().then((btn) => btn.isDisabled())).toEqual(true);
        });

        it('should not show select groups + exclude groups if public', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('another name');

          expect(await harness.getAccessControlGroups()).toBeFalsy();
          expect(await harness.getExcludeGroups()).toBeFalsy();

          const nextBtn = await harness.getNextButton();

          expect(await nextBtn.isDisabled()).toEqual(false);
          expect(await nextBtn.click());
          expect(fixture.componentInstance.form.getRawValue()).toEqual({
            stepOne: {
              name: 'another name',
              visibility: 'PUBLIC',
              accessControlGroups: ['group-1'],
              excludeGroups: true,
            },
            content: 'my content',
            source: 'FILL',
            sourceConfiguration: {},
          });
        });

        it('should select groups and set exclude groups if private', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await harness.getAccessControlGroups();
          expect(selectAccessControlGroups).toBeTruthy();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 2' });

          const toggleExcludeGroups = await harness.getExcludeGroups();
          expect(toggleExcludeGroups).toBeTruthy();
          await toggleExcludeGroups.toggle();

          const nextBtn = await harness.getNextButton();

          expect(await nextBtn.isDisabled()).toEqual(false);
          expect(await nextBtn.click());
          expect(fixture.componentInstance.form.getRawValue()).toEqual({
            stepOne: {
              name: 'page-name',
              visibility: 'PRIVATE',
              accessControlGroups: ['group-1', 'group-2'],
              excludeGroups: false,
            },
            content: 'my content',
            source: 'FILL',
            sourceConfiguration: {},
          });
        });

        it('should show markdown editor with existing content', async () => {
          const editor = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor());
          expect(editor).toBeDefined();
          expect(await editor.getValue()).toEqual(PAGE.content);
        });

        it('should not save content if no changes', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);

          await harness.getNextButton().then(async (btn) => btn.click());
          await harness.getNextButton().then(async (btn) => btn.click());

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Publish changes' }));
          expect(await saveBtn.isDisabled()).toEqual(true);
        });

        it('should save content', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.setName('New name');
          await harness.checkVisibility('PRIVATE');

          await harness.getNextButton().then(async (btn) => btn.click());
          await harness.getNextButton().then(async (btn) => btn.click());

          await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor())
            .then((editor) => editor.setValue('New content'));

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Publish changes' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          expectGetPage(PAGE);

          const req = httpTestingController.expectOne({
            method: 'PUT',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
          });
          expect(req.request.body).toEqual({
            ...PAGE,
            name: 'New name',
            visibility: 'PRIVATE',
            content: 'New content',
            accessControls: [
              { referenceId: 'role-1', referenceType: 'ROLE' },
              { referenceId: 'group-1', referenceType: 'GROUP' },
            ],
            excludedAccessControls: true,
          });
          req.flush(PAGE);

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: 'ROOT' },
          });
        });

        it('should save new access control settings', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          await harness.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await harness.getAccessControlGroups();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 2' });
          await selectAccessControlGroups.clickOptions({ text: 'group 1' });

          const toggleExcludeGroups = await harness.getExcludeGroups();
          expect(toggleExcludeGroups).toBeTruthy();
          await toggleExcludeGroups.toggle();

          await harness.getNextButton().then(async (btn) => btn.click());
          await harness.getNextButton().then(async (btn) => btn.click());

          const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Publish changes' }));
          expect(await saveBtn.isDisabled()).toEqual(false);
          await saveBtn.click();

          expectGetPage(PAGE);

          const req = httpTestingController.expectOne({
            method: 'PUT',
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
          });
          expect(req.request.body).toEqual({
            ...PAGE,
            name: 'page-name',
            visibility: 'PRIVATE',
            content: 'my content',
            accessControls: [
              { referenceId: 'role-1', referenceType: 'ROLE' },
              { referenceId: 'group-2', referenceType: 'GROUP' },
            ],
            excludedAccessControls: false,
          });
          req.flush(PAGE);

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: 'ROOT' },
          });
        });
      });
      describe('with unpublished page', () => {
        const PAGE = fakeMarkdown({ id: 'page-id', content: 'my content', visibility: 'PUBLIC', published: false });

        beforeEach(async () => {
          await init(undefined, PAGE.id);
          initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);
        });

        describe('and no changes', () => {
          beforeEach(async () => {
            const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
            await harness.getNextButton().then(async (btn) => btn.click());
            await harness.getNextButton().then(async (btn) => btn.click());
          });

          it('should not update page', async () => {
            const publishBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
            expect(await publishBtn.isDisabled()).toEqual(true);
          });

          it('should not update and publish page', async () => {
            const publishBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
            expect(await publishBtn.isDisabled()).toEqual(true);
          });
        });
        describe('and with changes', () => {
          beforeEach(async () => {
            const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
            await harness.setName('New name');
            await harness.getNextButton().then(async (btn) => btn.click());
            await harness.getNextButton().then(async (btn) => btn.click());
          });

          it('should update page', async () => {
            const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
            expect(await saveBtn.isDisabled()).toEqual(false);
            await saveBtn.click();

            expectGetPage(PAGE);

            const req = httpTestingController.expectOne({
              method: 'PUT',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            });

            expect(req.request.body).toEqual({
              ...PAGE,
              name: 'New name',
              visibility: 'PUBLIC',
              content: PAGE.content,
              accessControls: [],
              excludedAccessControls: false,
            });
            req.flush({ ...PAGE, name: 'New name' });
          });

          it('should update and publish page', async () => {
            const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save and publish' }));
            expect(await saveBtn.isDisabled()).toEqual(false);
            await saveBtn.click();

            expectGetPage(PAGE);

            const updatedPage = { ...PAGE, name: 'New name' };
            const req = httpTestingController.expectOne({
              method: 'PUT',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            });

            expect(req.request.body).toEqual({
              ...PAGE,
              name: 'New name',
              visibility: 'PUBLIC',
              content: PAGE.content,
              accessControls: [],
              excludedAccessControls: false,
            });
            req.flush(updatedPage);

            httpTestingController
              .expectOne({
                method: 'POST',
                url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}/_publish`,
              })
              .flush({ updatedPage, published: true });
          });
        });
      });
    });
    describe('In parent folder', () => {
      const PAGE = fakeMarkdown({ id: 'page-id', content: 'my content', visibility: 'PUBLIC', parentId: 'parent-folder-id' });

      beforeEach(async () => {
        await init('parent-folder-id', PAGE.id);
        initPageServiceRequests(
          {
            pages: [],
            breadcrumb: [{ name: 'Parent Folder', id: 'parent-folder-id', position: 1 }],
            parentId: 'parent-folder-id',
            mode: 'edit',
          },
          PAGE,
        );
      });

      it('should show breadcrumb', async () => {
        const harness = await harnessLoader.getHarness(ApiDocumentationV4BreadcrumbHarness);
        expect(await harness.getContent()).toEqual('Home > Parent Folder');
      });

      it('should exit without saving', async () => {
        const exitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Exit without saving' }));
        await exitBtn.click();
        expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
          relativeTo: expect.anything(),
          queryParams: { parentId: PAGE.parentId },
        });
      });
    });
    describe('In read-only mode', () => {
      const PAGE = fakeMarkdown({ id: 'page-id', name: 'page-name', content: 'my content', visibility: 'PUBLIC', published: true });

      beforeEach(async () => {
        await init(undefined, PAGE.id, 'portal.url', ['api-documentation-r']);
        initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);
      });

      it('should not be able to delete page and form is disabled', async () => {
        // No delete button
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
        expect(await harness.getDeleteButton()).toBeUndefined();

        // All fields disabled
        expect(await harness.nameIsDisabled()).toEqual(true);
        expect(await harness.visibilityIsDisabled()).toEqual(true);

        const editor = await harnessLoader.getHarness(GioMonacoEditorHarness);
        expect(await editor.isDisabled()).toEqual(true);

        // At end, no Publish Changes button
        const publishChangesButtons = await harnessLoader.getAllHarnesses(MatButtonHarness.with({ text: 'Publish changes' }));
        expect(publishChangesButtons.length).toEqual(0);
      });
    });
  });

  describe('Delete page', () => {
    describe('In the root folder', () => {
      describe('with published page', () => {
        const PAGE = fakeMarkdown({ id: 'page-id', name: 'page-name', content: 'my content', visibility: 'PUBLIC', published: true });
        const OTHER_PAGE = fakeMarkdown({
          id: 'other-page',
          name: 'other-page-name',
          content: 'my other content',
          visibility: 'PUBLIC',
          published: true,
        });

        it('should not delete page used as general condition', async () => {
          const GENERAL_CONDITION_PAGE = fakeMarkdown({
            id: 'general-condition-page',
            name: 'general-condition-page-name',
            content: 'my other content',
            visibility: 'PUBLIC',
            published: true,
            generalConditions: true,
          });

          await init(undefined, GENERAL_CONDITION_PAGE.id);
          initPageServiceRequests(
            { pages: [GENERAL_CONDITION_PAGE, PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' },
            GENERAL_CONDITION_PAGE,
          );

          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          expect(await harness.getName()).toEqual(GENERAL_CONDITION_PAGE.name);
          expect(await harness.getVisibility()).toEqual(GENERAL_CONDITION_PAGE.visibility);
          const deleteButton = await harness.getDeleteButton();
          expect(await deleteButton.isDisabled()).toBeTruthy();
        });

        it('should delete page and navigate to root list', async () => {
          await init(undefined, PAGE.id);
          initPageServiceRequests({ pages: [PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          expect(await harness.getName()).toEqual(PAGE.name);
          expect(await harness.getVisibility()).toEqual(PAGE.visibility);
          const deleteButton = await harness.getDeleteButton();
          await deleteButton.click();

          const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
          await dialogHarness.confirm();

          httpTestingController
            .expectOne({
              method: 'DELETE',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            })
            .flush(null);

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: 'ROOT' },
          });
        });
      });
    });
    describe('Under another folder', () => {
      describe('with published page', () => {
        const FOLDER = fakeFolder({
          id: 'folder',
          name: 'folder-name',
          visibility: 'PUBLIC',
          published: true,
        });
        const SUB_FOLDER_PAGE = fakeMarkdown({
          id: 'folder-page',
          name: 'folder-page-name',
          content: 'my other content',
          visibility: 'PUBLIC',
          published: true,
          parentId: FOLDER.id,
        });

        it('should delete page and navigate to parent folder', async () => {
          await init(undefined, SUB_FOLDER_PAGE.id);
          initPageServiceRequests(
            {
              pages: [FOLDER, SUB_FOLDER_PAGE],
              parentId: FOLDER.id,
              mode: 'edit',
            },
            SUB_FOLDER_PAGE,
          );
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4EditPageHarness);
          expect(await harness.getName()).toEqual(SUB_FOLDER_PAGE.name);
          expect(await harness.getVisibility()).toEqual(SUB_FOLDER_PAGE.visibility);
          const deleteButton = await harness.getDeleteButton();
          await deleteButton.click();

          const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
          await dialogHarness.confirm();

          httpTestingController
            .expectOne({
              method: 'DELETE',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${SUB_FOLDER_PAGE.id}`,
            })
            .flush(null);

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: FOLDER.id },
          });
        });
      });
    });
  });

  describe('Header', () => {
    const PAGE = fakeMarkdown({ id: 'page-id', name: 'page-name', content: 'my content', visibility: 'PUBLIC', published: true });

    it('should display Open in Portal button', async () => {
      await init(undefined, PAGE.id);
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);

      const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
      expect(header).toBeDefined();
      const openInPortalBtn = await header.getOpenInPortalBtn();
      expect(openInPortalBtn).toBeDefined();
      expect(await openInPortalBtn.isDisabled()).toEqual(false);
    });
    it('should not display Open in Portal button if Portal url not defined', async () => {
      await init(undefined, PAGE.id, null);
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);

      const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
      expect(await header.getOpenInPortalBtn()).toEqual(null);
    });
  });

  const expectGetPages = (pages: Page[], breadcrumb: Breadcrumb[], parentId = 'ROOT') => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages?parentId=${parentId}`,
    });

    req.flush({ pages, breadcrumb });
  };

  const expectGetPage = (page: Page) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${page.id}`,
    });

    req.flush(page);
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
