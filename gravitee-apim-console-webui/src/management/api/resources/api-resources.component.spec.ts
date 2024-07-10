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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiResourcesHarness } from './api-resources.harness';
import { ApiResourcesComponent } from './api-resources.component';
import { ApiResourcesAddDialogHarness } from './api-resources-add-dialog/api-resources-add-dialog.harness';
import { ApiResourcesEditDialogHarness } from './api-resources-edit-dialog/api-resources-edit-dialog.harness';

import { ApiV2, ApiV4, fakeApiV4, fakeResourcePlugin, ResourcePlugin } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiResourcesComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiResourcesComponent>;
  let componentHarness: ApiResourcesHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconTestingModule, GioTestingModule, NoopAnimationsModule, ApiResourcesComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                apiId: API_ID,
              },
            },
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-c', 'api-definition-r', 'api-definition-u', 'api-definition-d'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiResourcesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiResourcesHarness);
    fixture.detectChanges();
  });

  it('should display resources table', async () => {
    const table = await componentHarness.getTable();
    expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);

    expectApiGetRequest(
      fakeApiV4({
        id: API_ID,
        resources: [
          { name: 'MyCache', type: 'cache', configuration: '', enabled: true },
          {
            name: 'MyOAuth2',
            type: 'oauth2',
            configuration: '',
            enabled: false,
          },
        ],
      }),
    );
    expectListResourcesRequest([fakeResourcePlugin({ id: 'cache', name: 'Cache' }), fakeResourcePlugin({ id: 'oauth2', name: 'OAuth2' })]);

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['MyCache', 'Cache', 'toggle_on'],
      ['MyOAuth2 Disabled', 'OAuth2', 'toggle_off'],
    ]);
  });

  it('should add a resource', async () => {
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [] }));
    expectListResourcesRequest([fakeResourcePlugin({ id: 'cache', name: 'Cache' })]);

    // Open Add dialog
    await componentHarness.clickAddResource();
    const resourceAddDialog = await rootLoader.getHarness(ApiResourcesAddDialogHarness);
    expect(await resourceAddDialog.getTitleText()).toEqual('Add API Resource');

    // Select Cache resource
    await resourceAddDialog.select('Cache');

    // Configure Cache resource
    const resourceEditDialog = await rootLoader.getHarness(ApiResourcesEditDialogHarness);
    expectGetResourceSchemaRequest('cache');
    expect(await resourceEditDialog.getTitleText()).toEqual('Configure Cache resource');

    await resourceEditDialog.setResourceName('My Cache');

    // Save
    await resourceEditDialog.save();

    // Expect the resource to be added
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [] }));
    const updateApiReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'PUT',
    });
    expect(updateApiReq.request.body.resources).toStrictEqual([{ name: 'My Cache', type: 'cache', configuration: undefined }]);
  });

  it('should edit a resource', async () => {
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [{ name: 'MyCache', type: 'cache', configuration: '' }] }));
    expectListResourcesRequest([fakeResourcePlugin({ id: 'cache', name: 'Cache' })]);

    // Open Edit dialog
    await componentHarness.clickEditResource(0);
    const resourceEditDialog = await rootLoader.getHarness(ApiResourcesEditDialogHarness);
    expectGetResourceSchemaRequest('cache');
    expect(await resourceEditDialog.getTitleText()).toEqual('Configure Cache resource');

    await resourceEditDialog.setResourceName('Renamed Cache');

    // Save
    await resourceEditDialog.save();

    // Expect the resource to be saved
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [{ name: 'Renamed Cache', type: 'cache', configuration: '' }] }));
    const updateApiReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'PUT',
    });
    expect(updateApiReq.request.body.resources).toStrictEqual([{ name: 'Renamed Cache', type: 'cache', configuration: '' }]);
  });

  it('should remove a resource', async () => {
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [{ name: 'MyCache', type: 'cache', configuration: '' }] }));
    expectListResourcesRequest([fakeResourcePlugin({ id: 'cache', name: 'Cache' })]);

    // Remove resource
    await componentHarness.clickRemoveButton(0);
    const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    await confirmDialog.confirm();

    // Expect the resource to be removed
    expectApiGetRequest(fakeApiV4({ id: API_ID, resources: [{ name: 'MyCache', type: 'cache', configuration: '' }] }));
    const updateApiReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'PUT',
    });
    expect(updateApiReq.request.body.resources).toStrictEqual([]);
  });

  function expectApiGetRequest(api: ApiV2 | ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectListResourcesRequest(resourceList: ResourcePlugin[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources`,
        method: 'GET',
      })
      .flush(resourceList);
  }

  function expectGetResourceSchemaRequest(resourceId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources/${resourceId}/schema`,
        method: 'GET',
      })
      .flush(null);
  }
});
