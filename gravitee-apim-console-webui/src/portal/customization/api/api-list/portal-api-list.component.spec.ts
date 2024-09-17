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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { HarnessLoader } from '@angular/cdk/testing';

import { PortalApiListComponent } from './portal-api-list.component';
import { PortalApiListHarness } from './portal-api-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPortalHeader } from '../../../../entities/apiPortalHeader';
import { PortalSettings } from '../../../../entities/portal/portalSettings';
import { PortalBannerComponent } from '../../banner/portal-banner.component';

describe('PortalApiListComponent', () => {
  let fixture: ComponentFixture<PortalApiListComponent>;
  let componentHarness: PortalApiListHarness;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const fakeEmptyApiPortalHeaders: ApiPortalHeader[] = [];
  const fakeApiPortalHeaders: ApiPortalHeader[] = [
    {
      id: '0258b2a8-d5d9-479d-98b2-a8d5d9c79d19',
      name: 'team',
      value: "${api.metadata['team']}",
      order: 1,
    },
    {
      id: 'cdb6d74d-5c63-4f85-b6d7-4d5c631f85fc',
      name: 'api.publishedAt',
      value: '${(api.deployedAt?date)!}',
      order: 2,
    },
    {
      id: '8e8bfdf8-65d3-4f8c-8bfd-f865d38f8cf2',
      name: 'api.owner',
      value: '${api.owner}',
      order: 3,
    },
    {
      id: '962ae750-12df-40e9-aae7-5012dff0e9d4',
      name: 'country',
      value: "${api.metadata['country']}",
      order: 4,
    },
    {
      id: '76309b7c-e301-4743-b09b-7ce301d743e8',
      name: 'api.version',
      value: '${api.version}',
      order: 5,
    },
  ];

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
      imports: [GioTestingModule, NoopAnimationsModule, PortalBannerComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-settings-u', 'environment-settings-d'],
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(PortalApiListComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalApiListHarness);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

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
    await init(fakeApiPortalHeaders);
    expect(await componentHarness.getApiKeyHeader()).toEqual(portalSettingsOld.portal.apikeyHeader);
    expect(await componentHarness.getAddButton()).toBeTruthy();
    expect(await componentHarness.getBothPortalsForApiSubscription()).toBeTruthy();
    expect(await componentHarness.getBothPortalsForApiDetails()).toBeTruthy();
    expect(await componentHarness.getTableRows()).toBeTruthy();
  });

  it('should verify that there is edit button', async () => {
    await init(fakeApiPortalHeaders);
    const hasRows = await componentHarness.hasRows();
    expect(hasRows).toBe(true);
    expect(await componentHarness.getEditButton()).toBeTruthy();
  });

  it('should verify that there is delete button', async () => {
    await init(fakeApiPortalHeaders);
    const hasRows = await componentHarness.hasRows();
    expect(hasRows).toBe(true);
    expect(await componentHarness.getDeleteButton()).toBeTruthy();
  });

  it('should verify the no data row text', async () => {
    await init(fakeEmptyApiPortalHeaders);
    const noDataRowText = await componentHarness.getNoDataRowText();
    expect(noDataRowText).toBe('No headers to display.');
  });

  it('test http calls after updating api header', async () => {
    await init(fakeApiPortalHeaders);
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

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/portal`,
        method: 'GET',
      })
      .flush({});

    configureApiHeaders(fakeApiPortalHeaders);

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(portalSettingsNew);
  });

  it('test api call after clicking discard on save bar', async () => {
    await init(fakeApiPortalHeaders);
    fixture.detectChanges();

    await componentHarness.setApiKeyHeader(apiKeyHeaderNew);
    fixture.detectChanges();

    const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickReset();
  });
});
