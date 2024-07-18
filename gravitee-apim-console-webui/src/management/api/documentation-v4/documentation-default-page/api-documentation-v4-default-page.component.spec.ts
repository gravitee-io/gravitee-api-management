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
import { BehaviorSubject } from 'rxjs';
import { set } from 'lodash';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiDocumentationV4DefaultPageComponent } from './api-documentation-v4-default-page.component';

import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { ApiDocumentationV4EmptyStateHarness } from '../components/documentation-empty-state/api-documentation-v4-empty-state.harness';
import { PageType } from '../../../../entities/page';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { ApiLifecycleState, Breadcrumb, fakeApiV4, Page } from '../../../../entities/management-api-v2';
import { ApiDocumentationV4DefaultPageHarness } from '../components/documentation-home-page-header/api-documentation-v4-home-page-header.harness';

describe('ApiDocumentationV4DefaultPageComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4DefaultPageComponent>;
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
      declarations: [ApiDocumentationV4DefaultPageComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiId: API_ID } },
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

    fixture = TestBed.createComponent(ApiDocumentationV4DefaultPageComponent);
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

    expectGetPages(pages, breadcrumb, parentId);
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
      const headerHarness = await harnessLoader.getHarness(ApiDocumentationV4DefaultPageHarness);
      const createNewPageBtn = await headerHarness.getCreateNewPageButton();
      expect(createNewPageBtn).toBeDefined();
      expect(await createNewPageBtn.isDisabled()).toEqual(false);
      await headerHarness.clickCreateNewPage(PageType.MARKDOWN);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'homepage', 'new'], {
        relativeTo: expect.anything(),
        queryParams: { parentId: 'ROOT', pageType: 'MARKDOWN' },
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
});
