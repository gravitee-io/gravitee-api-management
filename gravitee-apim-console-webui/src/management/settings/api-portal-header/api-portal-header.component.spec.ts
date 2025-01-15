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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HttpTestingController } from '@angular/common/http/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiPortalHeaderModule } from './api-portal-header.module';
import { ApiPortalHeaderComponent } from './api-portal-header.component';
import { ApiPortalHeaderHarness } from './api-portal-header.harness.';
import { ApiPortalHeaderEditDialogHarness } from './api-portal-header-edit-dialog/api-portal-header-edit-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { ApiPortalHeader } from '../../../entities/apiPortalHeader';
import { Constants } from '../../../entities/Constants';

describe('ApiPortalHeaderComponent', () => {
  let fixture: ComponentFixture<ApiPortalHeaderComponent>;
  let componentHarness: ApiPortalHeaderHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiPortalHeaderComponent],
      imports: [GioTestingModule, ApiPortalHeaderModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-settings-c', 'environment-settings-u', 'environment-settings-d'],
        },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: [] },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(ApiPortalHeaderComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiPortalHeaderHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('toggles', () => {
    it('should do not be present when no setting are loaded', async () => {
      expectSettingsGetRequest({});
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);
      expect(await componentHarness.getShowTagsToggle()).toBeFalsy();
      expect(await componentHarness.getShowCategoriesToggle()).toBeFalsy();
      expect(await componentHarness.getPromotedApiModeToggle()).toBeFalsy();
    });

    it('should reflect settings state', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      const isShowTagsToggleChecked = await componentHarness.isToggleChecked(componentHarness.getShowTagsToggle());
      const isShowCategoriesToggle = await componentHarness.isToggleChecked(componentHarness.getShowCategoriesToggle());
      const isPromotedApiModeToggle = await componentHarness.isToggleChecked(componentHarness.getPromotedApiModeToggle());
      expect(isShowTagsToggleChecked).toEqual(true);
      expect(isShowCategoriesToggle).toEqual(false);
      expect(isPromotedApiModeToggle).toEqual(true);
    });
  });

  describe('table', () => {
    it('should have proper number of rows', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      const rows = await componentHarness.rowsNumber();
      expect(rows).toEqual(3);
    });

    it('should be able delete header', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      await componentHarness.deleteHeader(0);
      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      expectHeaderDELETERequest({
        id: '8ea',
        name: 'api.version',
        value: '${api.version}',
        order: 1,
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);
    });

    it('should move columns up', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      await componentHarness.moveHeaderUp(2);

      expectHeaderPUTRequest({
        id: 'a37',
        name: 'api.publishedAt',
        value: '${(api.deployedAt?date)!}',
        order: 3,
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);
    });

    it('should move columns down', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      await componentHarness.moveHeaderDown(1);

      expectHeaderPUTRequest({
        id: '084',
        name: 'api.owner',
        value: '${api.owner}',
        order: 2,
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);
    });

    it('should be able to edit header', async () => {
      expectSettingsGetRequest({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: true,
            },
            apiHeaderShowCategories: {
              enabled: false,
            },
            promotedApiMode: {
              enabled: true,
            },
            tilesMode: {
              enabled: true,
            },
            documentationOnlyMode: {
              enabled: true,
            },
            categoryMode: {
              enabled: true,
            },
          },
        },
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);

      const editButton = await componentHarness.getEditButton(1);
      await editButton.click();

      const editHeaderPopUp = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(ApiPortalHeaderEditDialogHarness);
      expect(editHeaderPopUp).toBeTruthy();

      await editHeaderPopUp.setName('test-name');
      await editHeaderPopUp.setValue('test-value');
      await editHeaderPopUp.clickOnSave();
      fixture.detectChanges();

      expectHeaderPUTRequest({
        id: '084',
        name: 'test-name',
        value: 'test-value',
        order: 2,
      });
      expectHeadersGetRequest([
        {
          id: '8ea',
          name: 'api.version',
          value: '${api.version}',
          order: 1,
        },
        {
          id: '084',
          name: 'api.owner',
          value: '${api.owner}',
          order: 2,
        },
        {
          id: 'a37',
          name: 'api.publishedAt',
          value: '${(api.deployedAt?date)!}',
          order: 3,
        },
      ]);
    });
  });

  function expectSettingsGetRequest(settings: PortalSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/settings`);
    req.flush(settings);
    expect(req.request.method).toEqual('GET');
  }

  function expectHeadersGetRequest(headers: ApiPortalHeader[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/apiheaders/`);
    req.flush(headers);
    expect(req.request.method).toEqual('GET');
  }

  function expectHeaderDELETERequest(apiPortalHeader: ApiPortalHeader) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/apiheaders/${apiPortalHeader.id}`,
    );
    req.flush([]);
    expect(req.request.method).toEqual('DELETE');
  }

  function expectHeaderPUTRequest(apiPortalHeader: ApiPortalHeader) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/apiheaders/${apiPortalHeader.id}`,
    );
    req.flush([]);
    expect(req.request.method).toEqual('PUT');
  }
});
