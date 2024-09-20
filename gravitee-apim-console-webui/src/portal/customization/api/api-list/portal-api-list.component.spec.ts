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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { HarnessLoader } from '@angular/cdk/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { PortalApiListComponent } from './portal-api-list.component';
import { PortalApiListHarness } from './portal-api-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPortalHeader } from '../../../../entities/apiPortalHeader';
import { PortalSettings } from '../../../../entities/portal/portalSettings';
import { PortalBannerComponent } from '../../banner/portal-banner.component';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('PortalApiListComponent', () => {
  const fakeSnackBarService = {
    success: jest.fn(),
  };

  let fixture: ComponentFixture<PortalApiListComponent>;
  let componentHarness: PortalApiListHarness;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;

  const appliesToBothPortals = 'Applies to both portals';
  const fakeEmptyApiPortalHeaders: ApiPortalHeader[] = [];
  const initialFakeApiPortalHeaders: ApiPortalHeader[] = [
    {
      id: 'id1',
      name: 'team',
      value: "${api.metadata['team']}",
      order: 1,
    },
    {
      id: 'id2',
      name: 'api.publishedAt',
      value: '${(api.deployedAt?date)!}',
      order: 2,
    },
    {
      id: 'id3',
      name: 'api.owner',
      value: '${api.owner}',
      order: 3,
    },
    {
      id: 'id4',
      name: 'country',
      value: "${api.metadata['country']}",
      order: 4,
    },
    {
      id: 'id5',
      name: 'api.version',
      value: '${api.version}',
      order: 5,
    },
  ];
  const fakeApiPortalHeadersAfterDelete = initialFakeApiPortalHeaders.slice(1);
  const apiKeyHeaderOld = 'X-Gravitee-Api-Key-Old';
  const apiKeyHeaderNew = 'X-Gravitee-Api-Key-New';
  const portalSettingsOld: PortalSettings = { portal: { apikeyHeader: apiKeyHeaderOld } };
  const portalSettingsNew: PortalSettings = { portal: { apikeyHeader: apiKeyHeaderNew } };

  function configureApiHeaders(apiPortalHeaders: ApiPortalHeader[]) {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/` })
      .flush(apiPortalHeaders);
  }
  const init = async (apiPortalHeaders: ApiPortalHeader[]) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalBannerComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-settings-u', 'environment-settings-d'],
        },
        { provide: SnackBarService, useValue: fakeSnackBarService },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PortalApiListComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalApiListHarness);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    configureApiHeaders(apiPortalHeaders);

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(portalSettingsOld);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('page should have elements', async () => {
    await init(initialFakeApiPortalHeaders);
    expect(await componentHarness.getApiKeyHeader()).toEqual(portalSettingsOld.portal.apikeyHeader);
    expect(await componentHarness.getAddButton()).toBeTruthy();
    expect(await componentHarness.getBadgeIconName1()).toBeTruthy();
    expect(await componentHarness.getBadgeIconName2()).toBeTruthy();
    expect(await componentHarness.getBadgeWarningText1()).toEqual(appliesToBothPortals);
    expect(await componentHarness.getBadgeWarningText2()).toEqual(appliesToBothPortals);
    expect(await componentHarness.getTableRows()).toBeTruthy();
  });

  it('should verify that there is edit button', async () => {
    await init(initialFakeApiPortalHeaders);
    const hasRows = await componentHarness.hasRows();
    expect(hasRows).toBe(true);
    expect(await componentHarness.getEditButton()).toBeTruthy();
  });

  it('should verify that there is delete button', async () => {
    await init(initialFakeApiPortalHeaders);

    const hasRows = await componentHarness.hasRows();
    expect(hasRows).toBe(true);
    expect(await componentHarness.getDeleteButton()).toBeTruthy();

    const row1 = await componentHarness.getRowByIndex(0);
    const deleteBtn = row1.deleteButton;
    expect(deleteBtn).toBeTruthy();

    await deleteBtn.click();

    const dia = await rootLoader.getHarness(GioConfirmDialogHarness);
    expect(dia).toBeTruthy();
    await dia.confirm();

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/apiheaders/id1`,
        method: 'DELETE',
      })
      .flush(null);

    configureApiHeaders(fakeApiPortalHeadersAfterDelete);
    expect(fakeSnackBarService.success).toHaveBeenCalledWith("API Information 'team' deleted successfully");
  });

  it('should verify the no data row text', async () => {
    await init(fakeEmptyApiPortalHeaders);
    const noDataRowText = await componentHarness.getNoDataRowText();
    expect(noDataRowText).toBe('No headers to display.');
  });

  it('test http calls after updating api header', async () => {
    await init(initialFakeApiPortalHeaders);
    fixture.detectChanges();

    await componentHarness.setApiKeyHeader(apiKeyHeaderNew);
    fixture.detectChanges();

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(portalSettingsOld);

    const postRequest = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
    });

    postRequest.flush(portalSettingsNew);
    expect(postRequest.request.body).toEqual(portalSettingsNew);

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('API Key Header updated successfully');

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/portal`,
        method: 'GET',
      })
      .flush({});

    configureApiHeaders(initialFakeApiPortalHeaders);

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(portalSettingsNew);
  });

  it('test api call after clicking discard on save bar', async () => {
    await init(initialFakeApiPortalHeaders);
    fixture.detectChanges();

    await componentHarness.setApiKeyHeader(apiKeyHeaderNew);
    fixture.detectChanges();

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickReset();
  });
});
