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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import SpyInstance = jest.SpyInstance;

import { PortalNavigationItemsComponent } from './portal-navigation-items.component';
import { PortalNavigationItemsHarness } from './portal-navigation-items.harness';
import { SectionEditorDialogHarness } from './section-editor-dialog/section-editor-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';
import {
  fakeNewPagePortalNavigationItem,
  fakePortalNavigationItemsResponse,
  fakePortalNavigationPage,
  NewPortalNavigationItem,
  PortalNavigationItem,
  PortalNavigationItemsResponse,
} from '../../entities/management-api-v2';

describe('PortalNavigationItemsComponent', () => {
  let fixture: ComponentFixture<PortalNavigationItemsComponent>;
  let harness: PortalNavigationItemsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerSpy: SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNavigationItemsComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-documentation-c'],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PortalNavigationItemsComponent);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalNavigationItemsHarness);
    httpTestingController = TestBed.inject(HttpTestingController);

    const router = TestBed.inject(Router);
    routerSpy = jest.spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe('adding a section', () => {
    describe('adding a page', () => {
      let dialogHarness: SectionEditorDialogHarness;
      beforeEach(async () => {
        expectGetMenuLinks();
        await harness.clickAddButton();
        await harness.clickPageMenuItem();
        dialogHarness = await rootLoader.getHarness(SectionEditorDialogHarness);
      });
      it('opens the dialog when the Add button is clicked and Page is selected', async () => {
        expect(dialogHarness).toBeTruthy();
      });

      it('should not create the page when the dialog is cancelled', async () => {
        await dialogHarness.clickCancelButton();
      });

      it('should create the page when the dialog is submitted', async () => {
        const title = 'New Page Title';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickAddButton();

        expectCreateNavigationItem(
          fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE' }),
          fakePortalNavigationPage({
            title,
            area: 'TOP_NAVBAR',
            type: 'PAGE',
            configuration: {
              portalPageContentId: 'content-id',
            },
          }),
        );
        expectGetNavigationItems();
      });
      it('should navigate to the created page after creation', async () => {
        const title = 'New Page Title';
        await dialogHarness.setTitleInputValue(title);
        await dialogHarness.clickAddButton();

        const createdItem = fakePortalNavigationPage({
          title,
          area: 'TOP_NAVBAR',
          type: 'PAGE',
          configuration: {
            portalPageContentId: 'content-id',
          },
        });
        expectCreateNavigationItem(fakeNewPagePortalNavigationItem({ title, area: 'TOP_NAVBAR', type: 'PAGE' }), createdItem);
        expectGetNavigationItems();

        expect(routerSpy).toHaveBeenCalledWith(['.'], expect.objectContaining({ queryParams: { navId: createdItem.id } }));
      });
    });
  });

  function expectGetMenuLinks() {
    httpTestingController.expectOne('assets/mocks/portal-menu-links.json').flush({ data: [] });
  }
  function expectGetNavigationItems(response: PortalNavigationItemsResponse = fakePortalNavigationItemsResponse()) {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/portal-navigation-items` }).flush(response);
  }
  function expectCreateNavigationItem(requestBody: NewPortalNavigationItem, result: PortalNavigationItem) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/portal-navigation-items` });
    expect(req.request.body).toEqual(requestBody);
    req.flush(result);
  }
});
