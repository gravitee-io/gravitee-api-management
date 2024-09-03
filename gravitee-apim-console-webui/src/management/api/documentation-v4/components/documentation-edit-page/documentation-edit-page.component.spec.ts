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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormsModule } from '@angular/forms';
import { GioConfirmDialogHarness, GioMonacoEditorHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatTabHarness } from '@angular/material/tabs/testing';

import { DocumentationEditPageHarness } from './documentation-edit-page.harness';
import { DocumentationEditPageComponent } from './documentation-edit-page.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import {
  Breadcrumb,
  Page,
  fakeFolder,
  fakeMarkdown,
  fakeApiV4,
  Group,
  fakeGroupsResponse,
  fakeGroup,
  fakeSwagger,
} from '../../../../../entities/management-api-v2';
import { ApiDocumentationV4ContentEditorHarness } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.harness';
import { ApiDocumentationV4BreadcrumbHarness } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDocumentationV4PageConfigurationHarness } from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.harness';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
  parentId?: string;
}

describe('DocumentationEditPageComponent', () => {
  let fixture: ComponentFixture<DocumentationEditPageComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    page: Page,
    apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
  ) => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        ApiDocumentationV4Module,
        MatIconTestingModule,
        FormsModule,
        GioTestingModule,
        CommonModule,
        DocumentationEditPageComponent,
      ],
      providers: [{ provide: GioTestingPermissionProvider, useValue: apiPermissions }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DocumentationEditPageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.componentInstance.api = fakeApiV4({ id: API_ID, lifecycleState: 'PUBLISHED' });
    fixture.componentInstance.goBackRouterLink = [];
    fixture.componentInstance.page = page;
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
          await init(PAGE, undefined);
          initPageServiceRequests({ pages: [PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined });
        });

        it('should have 2 taps', async () => {
          const tabs = await harnessLoader.getAllHarnesses(MatTabHarness);
          const tabLabels = await Promise.all(tabs.map(async (t) => await t.getLabel()));
          expect(tabLabels.length).toEqual(2);
          expect(await tabLabels[0]).toEqual('Configure Page');
          expect(await tabLabels[1]).toEqual('Content');
        });

        it('should load content tab by default', async () => {
          const activeTab = await harnessLoader.getHarness(MatTabHarness.with({ selected: true }));
          expect(await activeTab.getLabel()).toEqual('Content');
        });

        it('should not save button disabled if duplicate name', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();
          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);

          await pageConfiguration.setName(' Other-page-Name  ');
          expect(await harness.getPublishChangesButton().then((btn) => btn.isDisabled())).toEqual(true);
        });

        it('should show markdown editor with existing content', async () => {
          const editor = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor());
          expect(editor).toBeDefined();
          expect(await editor.getValue()).toEqual(PAGE.content);
        });

        it('should not save content if no changes', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          const saveBtn = await harness.getPublishChangesButton();
          expect(await saveBtn.isDisabled()).toEqual(true);
        });

        it('should save content', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.setName('New name');
          await pageConfiguration.checkVisibility('PRIVATE');

          await harness.openContentTab();
          await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor())
            .then((editor) => editor.setValue('New content'));

          const saveBtn = await harness.getPublishChangesButton();
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

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: 'ROOT' },
          });
        });

        it('should save new access control settings', async () => {
          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          await pageConfiguration.checkVisibility('PRIVATE');

          const selectAccessControlGroups = await pageConfiguration.getAccessControlGroups();
          await selectAccessControlGroups.open();
          await selectAccessControlGroups.clickOptions({ text: 'group 2' });
          await selectAccessControlGroups.clickOptions({ text: 'group 1' });

          const toggleExcludeGroups = await pageConfiguration.getExcludeGroups();
          expect(toggleExcludeGroups).toBeTruthy();
          await toggleExcludeGroups.toggle();

          const saveBtn = await harness.getPublishChangesButton();
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

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: 'ROOT' },
          });
        });
      });
      describe('with unpublished page', () => {
        const PAGE = fakeMarkdown({ id: 'page-id', content: 'my content', visibility: 'PUBLIC', published: false });

        beforeEach(async () => {
          await init(PAGE, undefined);
          initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
        });

        describe('and no changes', () => {
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
            const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
            await harness.openConfigurePageTab();

            const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
            await pageConfiguration.setName('New name');
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
      describe('with fetched page', () => {
        const PAGE = fakeMarkdown({
          id: 'page-id',
          name: 'page-name',
          content: 'my content',
          source: { type: 'http-fetcher', configuration: { some: 'config' } },
        });

        beforeEach(async () => {
          await init(PAGE, undefined);
          initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
        });

        it('should not allow editing content', async () => {
          const editor = await harnessLoader
            .getHarness(ApiDocumentationV4ContentEditorHarness)
            .then((harness) => harness.getContentEditor());
          expect(editor).toBeDefined();
          expect(await editor.isDisabled()).toEqual(true);
        });
      });
      describe('with OpenAPI page', () => {
        describe('with no configuration', () => {
          const PAGE = fakeSwagger({
            id: 'page-id',
            name: 'page-name',
            content: 'my content',
            configuration: {},
            published: true,
          });
          let harness: DocumentationEditPageHarness;

          beforeEach(async () => {
            await init(PAGE, undefined);
            initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
            harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
            await harness.openOpenApiConfigurationTab();
          });
          it('should show OpenAPI Viewer Configuration with default values + save is disabled', async () => {
            const entrypointsAsServersToggle = await harness.getEntrypointsAsServersToggle();
            expect(await entrypointsAsServersToggle.isChecked()).toEqual(false);

            const contextPathAsServerToggle = await harness.getContextPathAsServerToggle();
            expect(await contextPathAsServerToggle.isChecked()).toEqual(false);

            const baseUrlInput = await harness.getBaseUrlInput();
            expect(await baseUrlInput.getValue()).toEqual('');

            const openApiViewerSelect = await harness.getOpenApiViewerSelect();
            expect(await openApiViewerSelect.getValueText()).toEqual('SwaggerUI');

            const tryItToggle = await harness.getTryItToggle();
            expect(await tryItToggle.isChecked()).toEqual(false);

            const tryItAnonymousToggle = await harness.getTryItAnonymousToggle();
            expect(await tryItAnonymousToggle.isChecked()).toEqual(false);

            const showUrlToggle = await harness.getShowUrlToggle();
            expect(await showUrlToggle.isChecked()).toEqual(false);

            const displayOperationIdToggle = await harness.getDisplayOperationIdToggle();
            expect(await displayOperationIdToggle.isChecked()).toEqual(false);

            const usePkceToggle = await harness.getUsePkceToggle();
            expect(await usePkceToggle.isChecked()).toEqual(false);

            const enableFilteringToggle = await harness.getEnableFilteringToggle();
            expect(await enableFilteringToggle.isChecked()).toEqual(false);

            const showExtensionsToggle = await harness.getShowExtensionsToggle();
            expect(await showExtensionsToggle.isChecked()).toEqual(false);

            const showCommonExtensionsToggle = await harness.getShowCommonExtensionsToggle();
            expect(await showCommonExtensionsToggle.isChecked()).toEqual(false);

            const docExpansionSelect = await harness.getDocExpansionSelect();
            expect(await docExpansionSelect.getValueText()).toEqual('Default');

            const maxOperationsDisplayedInput = await harness.getMaxOperationsDisplayedInput();
            expect(await maxOperationsDisplayedInput.getValue()).toEqual('-1');

            const saveBtn = await harness.getPublishChangesButton();
            expect(await saveBtn.isDisabled()).toEqual(true);
          });

          it('should disable Base URL input when user enables entrypoints as server URLs', async () => {
            const baseUrlInput = await harness.getBaseUrlInput();
            expect(await baseUrlInput.isDisabled()).toEqual(false);

            const entrypointsAsServersToggle = await harness.getEntrypointsAsServersToggle();
            expect(await entrypointsAsServersToggle.isChecked()).toEqual(false);
            await entrypointsAsServersToggle.toggle();

            expect(await baseUrlInput.isDisabled()).toEqual(true);
          });

          it('should save new configuration', async () => {
            const baseUrlInput = await harness.getBaseUrlInput();
            await baseUrlInput.setValue('cats-rule');

            const publishBtn = await harness.getPublishChangesButton();
            expect(await publishBtn.isDisabled()).toEqual(false);

            await publishBtn.click();

            expectGetPage(PAGE);

            const req = httpTestingController.expectOne({
              method: 'PUT',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            });
            expect(req.request.body).toEqual({
              ...PAGE,
              accessControls: [],
              excludedAccessControls: false,
              configuration: {
                viewer: 'Swagger',
                entrypointAsBasePath: false,
                entrypointsAsServers: false,
                tryItURL: 'cats-rule',
                tryIt: false,
                tryItAnonymous: false,
                showURL: false,
                displayOperationId: false,
                usePkce: false,
                docExpansion: 'none',
                enableFiltering: false,
                showExtensions: false,
                showCommonExtensions: false,
                maxDisplayedTags: -1,
              },
            });
            req.flush(PAGE);
          });
        });
        describe('with existing Swagger viewer configuration', () => {
          const PAGE = fakeSwagger({
            id: 'page-id',
            name: 'page-name',
            content: 'my content',
            published: true,
            configuration: {
              viewer: 'Swagger',
              entrypointAsBasePath: 'true',
              entrypointsAsServers: 'true',
              tryItURL: 'cats-rule',
              tryIt: 'true',
              tryItAnonymous: 'true',
              showURL: 'true',
              displayOperationId: 'true',
              usePkce: 'true',
              docExpansion: 'full',
              enableFiltering: 'true',
              showExtensions: 'true',
              showCommonExtensions: 'true',
              maxDisplayedTags: '0',
            },
          });
          let harness: DocumentationEditPageHarness;

          beforeEach(async () => {
            await init(PAGE, undefined);
            initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
            harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
            await harness.openOpenApiConfigurationTab();
          });
          it('should show OpenAPI Viewer Configuration with default values + save is disabled', async () => {
            const entrypointsAsServersToggle = await harness.getEntrypointsAsServersToggle();
            expect(await entrypointsAsServersToggle.isChecked()).toEqual(true);

            const contextPathAsServerToggle = await harness.getContextPathAsServerToggle();
            expect(await contextPathAsServerToggle.isChecked()).toEqual(true);

            const baseUrlInput = await harness.getBaseUrlInput();
            expect(await baseUrlInput.getValue()).toEqual('cats-rule');

            const openApiViewerSelect = await harness.getOpenApiViewerSelect();
            expect(await openApiViewerSelect.getValueText()).toEqual('SwaggerUI');

            const tryItToggle = await harness.getTryItToggle();
            expect(await tryItToggle.isChecked()).toEqual(true);

            const tryItAnonymousToggle = await harness.getTryItAnonymousToggle();
            expect(await tryItAnonymousToggle.isChecked()).toEqual(true);

            const showUrlToggle = await harness.getShowUrlToggle();
            expect(await showUrlToggle.isChecked()).toEqual(true);

            const displayOperationIdToggle = await harness.getDisplayOperationIdToggle();
            expect(await displayOperationIdToggle.isChecked()).toEqual(true);

            const usePkceToggle = await harness.getUsePkceToggle();
            expect(await usePkceToggle.isChecked()).toEqual(true);

            const enableFilteringToggle = await harness.getEnableFilteringToggle();
            expect(await enableFilteringToggle.isChecked()).toEqual(true);

            const showExtensionsToggle = await harness.getShowExtensionsToggle();
            expect(await showExtensionsToggle.isChecked()).toEqual(true);

            const showCommonExtensionsToggle = await harness.getShowCommonExtensionsToggle();
            expect(await showCommonExtensionsToggle.isChecked()).toEqual(true);

            const docExpansionSelect = await harness.getDocExpansionSelect();
            expect(await docExpansionSelect.getValueText()).toEqual('Tags and operations');

            const maxOperationsDisplayedInput = await harness.getMaxOperationsDisplayedInput();
            expect(await maxOperationsDisplayedInput.getValue()).toEqual('0');

            const saveBtn = await harness.getPublishChangesButton();
            expect(await saveBtn.isDisabled()).toEqual(true);
          });

          it('should save configuration changes', async () => {
            const baseUrlInput = await harness.getBaseUrlInput();
            await baseUrlInput.setValue('dogs-drool');

            const publishBtn = await harness.getPublishChangesButton();
            expect(await publishBtn.isDisabled()).toEqual(false);

            await publishBtn.click();

            expectGetPage(PAGE);

            const req = httpTestingController.expectOne({
              method: 'PUT',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            });
            expect(req.request.body).toEqual({
              ...PAGE,
              accessControls: [],
              excludedAccessControls: false,
              configuration: {
                viewer: 'Swagger',
                entrypointAsBasePath: true,
                entrypointsAsServers: true,
                tryIt: true,
                tryItAnonymous: true,
                showURL: true,
                displayOperationId: true,
                usePkce: true,
                docExpansion: 'full',
                enableFiltering: true,
                showExtensions: true,
                showCommonExtensions: true,
                maxDisplayedTags: 0,
                tryItURL: 'dogs-drool',
              },
            });
            req.flush(PAGE);
          });

          it('should change viewer to Redoc', async () => {
            const viewerSelect = await harness.getOpenApiViewerSelect();
            await viewerSelect.open();
            await viewerSelect.clickOptions({ text: 'Redoc' });

            const publishBtn = await harness.getPublishChangesButton();
            expect(await publishBtn.isDisabled()).toEqual(false);

            await publishBtn.click();

            expectGetPage(PAGE);

            const req = httpTestingController.expectOne({
              method: 'PUT',
              url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE.id}`,
            });
            expect(req.request.body).toEqual({
              ...PAGE,
              accessControls: [],
              excludedAccessControls: false,
              configuration: {
                viewer: 'Redoc',
                entrypointAsBasePath: true,
                entrypointsAsServers: true,
                tryIt: true,
                tryItAnonymous: true,
                showURL: true,
                displayOperationId: true,
                usePkce: true,
                docExpansion: 'full',
                enableFiltering: true,
                showExtensions: true,
                showCommonExtensions: true,
                maxDisplayedTags: 0,
                tryItURL: 'cats-rule',
              },
            });
            req.flush(PAGE);
          });
        });
        describe('with existing Redoc viewer configuration', () => {
          const PAGE = fakeSwagger({
            id: 'page-id',
            name: 'page-name',
            content: 'my content',
            published: true,
            configuration: {
              viewer: 'Redoc',
              entrypointAsBasePath: 'true',
              entrypointsAsServers: 'true',
              tryItURL: 'cats-rule',
            },
          });
          let harness: DocumentationEditPageHarness;

          beforeEach(async () => {
            await init(PAGE, undefined);
            initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
            harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          });

          it('should not display SwaggerUI fields', async () => {
            await harness.openOpenApiConfigurationTab();
            await harness
              .getEnableFilteringToggle()
              .then((_) => fail('Enable filtering toggle should not be displayed'))
              .catch((_) => {});
          });

          it('should show page content by default', async () => {
            const activeTab = await harnessLoader.getHarness(MatTabHarness.with({ selected: true }));
            expect(await activeTab.getLabel()).toEqual('Content');
          });
        });
      });
    });
    describe('In parent folder', () => {
      const PAGE = fakeMarkdown({
        id: 'page-id',
        content: 'my content',
        visibility: 'PUBLIC',
        parentId: 'parent-folder-id',
      });

      beforeEach(async () => {
        await init(PAGE);
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

      it('should exit without saving', async () => {
        const exitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Exit without saving' }));
        await exitBtn.click();

        expect(routerNavigateSpy).toHaveBeenCalledWith([], expect.anything());
      });
    });
    describe('In read-only mode', () => {
      const PAGE = fakeMarkdown({
        id: 'page-id',
        name: 'page-name',
        content: 'my content',
        visibility: 'PUBLIC',
        published: true,
      });

      beforeEach(async () => {
        await init(PAGE, ['api-documentation-r']);
        initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined });
      });

      it('should not be able to delete page and form is disabled', async () => {
        // No delete button
        const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
        expect(await harness.getDeleteButton()).toBeNull();

        await harness.openConfigurePageTab();

        const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
        // All fields disabled
        expect(await pageConfiguration.nameIsDisabled()).toEqual(true);
        expect(await pageConfiguration.visibilityIsDisabled()).toEqual(true);

        await harness.openContentTab();
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
        const PAGE = fakeMarkdown({
          id: 'page-id',
          name: 'page-name',
          content: 'my content',
          visibility: 'PUBLIC',
          published: true,
        });
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

          await init(GENERAL_CONDITION_PAGE);
          initPageServiceRequests({ pages: [GENERAL_CONDITION_PAGE, PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined });

          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          expect(await pageConfiguration.getName()).toEqual(GENERAL_CONDITION_PAGE.name);
          expect(await pageConfiguration.getVisibility()).toEqual(GENERAL_CONDITION_PAGE.visibility);

          const deleteButton = await harness.getDeleteButton();
          expect(await deleteButton.isDisabled()).toBeTruthy();
        });

        it('should delete page and navigate to root list', async () => {
          await init(PAGE, undefined);
          initPageServiceRequests({ pages: [PAGE, OTHER_PAGE], breadcrumb: [], parentId: undefined });

          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          expect(await pageConfiguration.getName()).toEqual(PAGE.name);
          expect(await pageConfiguration.getVisibility()).toEqual(PAGE.visibility);

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

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], {
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
          await init(SUB_FOLDER_PAGE);
          initPageServiceRequests({
            pages: [FOLDER, SUB_FOLDER_PAGE],
            parentId: FOLDER.id,
          });

          const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationEditPageHarness);
          await harness.openConfigurePageTab();

          const pageConfiguration = await harnessLoader.getHarness(ApiDocumentationV4PageConfigurationHarness);
          expect(await pageConfiguration.getName()).toEqual(SUB_FOLDER_PAGE.name);
          expect(await pageConfiguration.getVisibility()).toEqual(SUB_FOLDER_PAGE.visibility);

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

          expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], {
            relativeTo: expect.anything(),
            queryParams: { parentId: FOLDER.id },
          });
        });
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
});
