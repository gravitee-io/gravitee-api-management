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
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { set } from 'lodash';
import { ActivatedRoute } from '@angular/router';
import { GioMonacoEditorHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';

import { DocumentationEditHomepageComponent } from './documentation-edit-homepage.component';

import {
  Breadcrumb,
  Page,
  fakeMarkdown,
  Group,
  fakeGroupsResponse,
  fakeGroup,
  Api,
  fakeApiV2,
} from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { DocumentationEditPageHarness } from '../components/documentation-edit-page/documentation-edit-page.harness';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
  parentId?: string;
  mode?: 'create' | 'edit';
}

describe('DocumentationEditHomepageComponent', () => {
  let fixture: ComponentFixture<DocumentationEditHomepageComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let api = fakeApiV2({ id: API_ID, lifecycleState: 'PUBLISHED' });
  api = {
    ...api,
    originContext: {
      origin: 'KUBERNETES',
    },
  };
  const PAGE = fakeMarkdown({
    id: 'page_id',
    name: 'page-name',
    type: 'MARKDOWN',
    content: 'my content',
    visibility: 'PUBLIC',
    published: true,
    homepage: true,
  });

  const init = async (
    parentId: string,
    pageId: string,
    portalUrl = 'portal.url',
    apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
  ) => {
    await TestBed.configureTestingModule({
      imports: [DocumentationEditHomepageComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
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
          isFocusable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DocumentationEditHomepageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    expectGetApiId(api);
    fixture.detectChanges();
  };

  const initPageServiceRequests = (input: InitInput, page: Page = {}) => {
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

  describe('Homepage', () => {
    beforeEach(async () => {
      await init(undefined, undefined);
      const editPage = await harnessLoader.getHarness(DocumentationEditPageHarness);
      await editPage.setName('New page');
      await editPage.checkVisibility('PUBLIC');

      await editPage.getNextButton().then(async (btn) => {
        expect(await btn.isDisabled()).toEqual(false);
        return btn.click();
      });

      await editPage.getNextButton().then(async (btn) => {
        expect(await btn.isDisabled()).toEqual(false);
        return btn.click();
      });
    });
    it('should save content', async () => {
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'create' }, PAGE);
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
        visibility: 'PUBLIC',
        content: '#TITLE  This is the file content',
        parentId: 'ROOT',
        accessControls: [],
        excludedAccessControls: false,
        homepage: true,
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

  const expectGetApiId = (api: Api) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    req.flush(api);
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
