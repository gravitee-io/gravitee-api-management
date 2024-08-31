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
import { of } from 'rxjs';

import { DocumentationEditCustomPageComponent } from './documentation-edit-custom-page.component';

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
import { DocumentationNewPageHarness } from '../components/documentation-new-page/documentation-new-page.harness';
import { DocumentationEditPageHarness } from '../components/documentation-edit-page/documentation-edit-page.harness';

interface InitInput {
  pages?: Page[];
  breadcrumb?: Breadcrumb[];
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
          useValue: { params: of({ apiId: API_ID, pageId }), queryParams: of({ parentId, pageType: 'MARKDOWN' }) },
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

  const initPageServiceRequests = (input: InitInput, page: Page) => {
    if (page) {
      expectGetPage(page);
      fixture.detectChanges();
    }
    expectGetPages(input.pages, input.breadcrumb, page?.parentId ?? 'ROOT');
    expectGetGroups([fakeGroup({ id: 'group-1', name: 'group 1' }), fakeGroup({ id: 'group-2', name: 'group 2' })]);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create', () => {
    beforeEach(async () => {
      await init(undefined, undefined);
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [] }, undefined);
    });
    it('should show create component if no page id found', async () => {
      const newPageHarness = await harnessLoader.getHarnessOrNull(DocumentationNewPageHarness);
      expect(newPageHarness).toBeTruthy();
    });
  });

  describe('Edit', () => {
    beforeEach(async () => {
      await init(undefined, PAGE.id);
      initPageServiceRequests({ pages: [PAGE], breadcrumb: [] }, PAGE);
    });
    it('should show edit component if page id found', async () => {
      const editPageHarness = await harnessLoader.getHarnessOrNull(DocumentationEditPageHarness);
      expect(editPageHarness).toBeTruthy();
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
