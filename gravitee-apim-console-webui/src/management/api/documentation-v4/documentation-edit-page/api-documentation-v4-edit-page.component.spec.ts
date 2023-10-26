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
import { UIRouterModule } from '@uirouter/angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormsModule } from '@angular/forms';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiDocumentationV4EditPageComponent } from './api-documentation-v4-edit-page.component';

import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { Breadcrumb, Page } from '../../../../entities/management-api-v2/documentation/page';
import { ApiDocumentationV4ContentEditorHarness } from '../components/api-documentation-v4-content-editor/api-documentation-v4-content-editor.harness';
import { fakeMarkdown } from '../../../../entities/management-api-v2/documentation/page.fixture';
import { ApiDocumentationV4BreadcrumbHarness } from '../components/api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';

describe('ApiDocumentationV4EditPageComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4EditPageComponent>;
  let harnessLoader: HarnessLoader;
  const fakeUiRouter = { go: jest.fn() };
  const API_ID = 'api-id';
  const PAGE_ID = 'page-id';
  const page = fakeMarkdown({
    id: PAGE_ID,
    parentId: 'parent-id',
    content: 'Initial content',
    name: 'Page to edit',
    visibility: 'PUBLIC',
  });
  const breadcrumb: Breadcrumb[] = [{ name: 'Parent folder', position: 1, id: 'parent-id' }];
  let httpTestingController: HttpTestingController;

  const init = async (page: Page = fakeMarkdown(), pages: Page[] = [], breadcrumb: Breadcrumb[] = []) => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4EditPageComponent],
      imports: [
        NoopAnimationsModule,
        ApiDocumentationV4Module,
        GioUiRouterTestingModule,
        UIRouterModule.forRoot({ useHash: true }),
        MatIconTestingModule,
        FormsModule,
        GioHttpTestingModule,
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, pageId: page.id } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4EditPageComponent);
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    expectGetPage(page, page.id);
    expectGetPages(pages, breadcrumb, page.parentId);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  beforeEach(async () => await init(page, [page], breadcrumb));

  it('should edit page content', async () => {
    expect(await getPageTitle()).toEqual('Page to edit');

    const editor = await harnessLoader.getHarness(ApiDocumentationV4ContentEditorHarness).then((harness) => harness.getContentEditor());
    expect(editor).toBeDefined();
    expect(await editor.getValue()).toEqual('Initial content');

    await editor.setValue('## New content');
    expect(fixture.componentInstance.content).toEqual('## New content');

    const saveBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Save' }));
    expect(await saveBtn.isDisabled()).toEqual(false);
    await saveBtn.click();

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${PAGE_ID}`,
    });
    req.flush(page);
    expect(req.request.body).toEqual({
      ...page,
      type: 'MARKDOWN',
      content: '## New content',
    });
  });

  it('should request confirmation before exit without saving', async () => {
    const editor = await harnessLoader.getHarness(ApiDocumentationV4ContentEditorHarness).then((harness) => harness.getContentEditor());
    expect(editor).toBeDefined();
    expect(await editor.getValue()).toEqual('Initial content');

    await editor.setValue('## New content');

    const exitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Exit without saving' }));
    await exitBtn.click();

    const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    expect(confirmDialog).toBeDefined();

    // should stay on page if cancel
    await confirmDialog.cancel();

    await exitBtn.click();
    const newConfirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    expect(newConfirmDialog).toBeDefined();
    // should leave page on confirm
    await newConfirmDialog.confirm();
    expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4', {
      apiId: API_ID,
      pageId: 'page-id',
      parentId: 'parent-id',
    });
  });

  it('should exit without confirmation when no changes', async () => {
    const exitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Exit without saving' }));
    await exitBtn.click();
    expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.documentationV4', {
      apiId: API_ID,
      pageId: 'page-id',
      parentId: 'parent-id',
    });
  });

  it('should show breadcrumb', async () => {
    const harness = await harnessLoader.getHarness(ApiDocumentationV4BreadcrumbHarness);
    expect(await harness.getContent()).toEqual('Home>Parent folder');
  });

  const expectGetPage = (page: Page, pageId: string) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages/${pageId}`,
    });

    req.flush(page);
  };

  const expectGetPages = (pages: Page[], breadcrumb: Breadcrumb[], parentId = 'ROOT') => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages?parentId=${parentId}`,
    });

    req.flush({ pages, breadcrumb });
  };

  const getPageTitle = () => {
    return fixture.nativeElement.querySelector('h3')?.innerHTML;
  };
});
