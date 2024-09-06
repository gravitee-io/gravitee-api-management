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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { set } from 'lodash';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { By } from '@angular/platform-browser';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';

import { ApiDocumentationV4MainPagesTabComponent } from './api-documentation-v4-main-pages-tab.component';

import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { ApiDocumentationV4EmptyStateHarness } from '../components/documentation-empty-state/api-documentation-v4-empty-state.harness';
import { PageType } from '../../../../entities/page';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { ApiLifecycleState, Breadcrumb, fakeApiV4, fakeMarkdown, Page } from '../../../../entities/management-api-v2';

describe('ApiDocumentationV4MainPagesTab', () => {
  let fixture: ComponentFixture<ApiDocumentationV4MainPagesTabComponent>;
  let harnessLoader: HarnessLoader;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (
    pages: Page[],
    breadcrumb: Breadcrumb[],
    parentId = 'ROOT',
    portalUrl = 'portal.url',
    apiLifecycleStatus: ApiLifecycleState = 'PUBLISHED',
  ) => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4MainPagesTabComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiId: API_ID }),
            queryParams: new BehaviorSubject({ parentId }),
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-documentation-u', 'api-documentation-c', 'api-documentation-r', 'api-documentation-d'],
        },
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
          isFocusable: () => true, // This checks focus trap, set it to true to avoid the warning
          isTabbable: () => true, // This checks tab trap, set it to true to avoid the warning
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4MainPagesTabComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      })
      .flush(fakeApiV4({ id: API_ID, lifecycleState: apiLifecycleStatus }));

    expectGetPages(pages, breadcrumb);
  };

  afterEach(() => {
    httpTestingController.verify();
    jest.resetAllMocks();
  });

  describe('API does not have pages', () => {
    beforeEach(async () => await init([], []));

    it('should show empty state when no documentation for API', async () => {
      const emptyState = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
      expect(emptyState).toBeDefined();
    });

    it('should navigate to create page', async () => {
      const createNewPageBtn = await getCreateNewPageButton();
      expect(createNewPageBtn).toBeDefined();
      expect(await createNewPageBtn.isDisabled()).toEqual(false);
      await createNewPage(PageType.MARKDOWN);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'homepage', 'new'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: undefined, pageType: 'MARKDOWN', homepage: true },
      });
    });
  });

  describe('Homepage section', () => {
    it('should only show current homepage', async () => {
      const pages = [
        fakeMarkdown({ id: 'page-1', name: 'Page 1', homepage: true }),
        fakeMarkdown({ id: 'page-2', name: 'Page 2', homepage: false }),
      ];
      await init(pages, []);

      fixture.detectChanges();

      const pageElements = fixture.debugElement.queryAll(By.css('api-documentation-v4-pages-list'));
      expect(pageElements.length).toBe(1);

      const pageListComponent = pageElements[0].componentInstance;
      expect(pageListComponent.pages.length).toBe(1);
      expect(pageListComponent.pages[0].id).toBe('page-1');
    });
  });

  async function getCreateNewPageButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Create New Page' }));
  }

  async function createNewPage(pageType: PageType) {
    await getCreateNewPageButton().then((btn) => btn.click());
    const pageTypeMenu = await harnessLoader.getHarness(MatMenuHarness);
    return await pageTypeMenu.clickItem({ text: new RegExp(pageType, 'i') });
  }

  const expectGetPages = (pages: Page[], breadcrumb: Breadcrumb[]) => {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
    });

    req.flush({ pages, breadcrumb });
  };
});
