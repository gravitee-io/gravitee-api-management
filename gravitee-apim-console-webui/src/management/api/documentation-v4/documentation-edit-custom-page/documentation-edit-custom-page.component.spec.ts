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

import { DocumentationEditCustomPageComponent } from './documentation-edit-custom-page.component';

import { Breadcrumb, Page, fakeMarkdown, Api, fakeApiV2 } from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDocumentationV4PageTitleHarness } from '../components/api-documentation-v4-page-title/api-documentation-v4-page-title.harness';
import { Constants } from '../../../../entities/Constants';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
  parentId?: string;
  mode?: 'create' | 'edit';
}

describe('DocumentationEditCustomPageComponent', () => {
  let fixture: ComponentFixture<DocumentationEditCustomPageComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  const PAGE = fakeMarkdown({
    id: 'page-id',
    name: 'page-name',
    content: 'my content',
    visibility: 'PUBLIC',
    published: true,
  });

  const init = async (
    parentId: string,
    pageId: string,
    portalUrl = 'portal.url',
    apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
  ) => {
    await TestBed.configureTestingModule({
      imports: [DocumentationEditCustomPageComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
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

    fixture = TestBed.createComponent(DocumentationEditCustomPageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectGetApiId(fakeApiV2({ id: API_ID, lifecycleState: 'PUBLISHED' }));
    fixture.detectChanges();
  };

  const initPageServiceRequests = (input: InitInput, page: Page = {}) => {
    if (input.mode === 'edit') {
      expectGetPage(page);
    }
    expectGetPages(input.pages, input.breadcrumb, input.parentId);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Header', () => {
    it('should display Open in Portal button', async () => {
      await init(undefined, PAGE.id);
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [], parentId: undefined, mode: 'edit' }, PAGE);
      const header = await harnessLoader.getHarness(ApiDocumentationV4PageTitleHarness);
      expect(header).toBeDefined();
      const openInPortalBtn = await header.getOpenInPortalBtn();
      expect(openInPortalBtn).toBeTruthy();
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

  const expectGetApiId = (api: Api) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    req.flush(api);
  };
});
