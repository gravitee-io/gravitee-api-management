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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { ApiProductResourcesHarness } from './api-product-resources.harness';
import { ApiProductResourcesComponent } from './api-product-resources.component';

import { ApiResourcesAddDialogHarness } from '../../api/resources/api-resources-add-dialog/api-resources-add-dialog.harness';
import { ApiResourcesEditDialogHarness } from '../../api/resources/api-resources-edit-dialog/api-resources-edit-dialog.harness';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Resource, fakeResourcePlugin, ResourcePlugin } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductResourcesComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  const cacheResource: Resource = {
    name: 'MyCache',
    type: 'cache',
    configuration: { timeToLiveSeconds: 60 },
    enabled: true,
  };
  const oauthResource: Resource = {
    name: 'MyOAuth',
    type: 'oauth2',
    configuration: {},
    enabled: false,
  };
  const product: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    description: 'A product',
    apiIds: ['api-1'],
    groups: ['group-1'],
    tags: ['eu'],
    disableMembershipNotifications: false,
    deploymentState: 'DEPLOYED',
    resources: [cacheResource, oauthResource],
  };
  const plugins: ResourcePlugin[] = [
    fakeResourcePlugin({ id: 'cache', name: 'Cache' }),
    fakeResourcePlugin({ id: 'oauth2', name: 'OAuth2' }),
    fakeResourcePlugin({ id: 'cache-cloud-redis', name: 'Cloud Redis' }),
  ];

  let fixture: ComponentFixture<ApiProductResourcesComponent>;
  let harness: ApiProductResourcesHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const snackBar = { error: jest.fn(), success: jest.fn() };

  async function setup(permissions: string[]): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ApiProductResourcesComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: SnackBarService, useValue: snackBar },
        { provide: GioTestingPermissionProvider, useValue: permissions },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiProductId: API_PRODUCT_ID }),
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiProductResourcesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  async function show(apiProduct: ApiProduct = product, resourcePlugins: ResourcePlugin[] = plugins): Promise<void> {
    flushLoad(apiProduct, resourcePlugins);
    fixture.detectChanges();
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductResourcesHarness);
  }

  function flushLoad(apiProduct: ApiProduct = product, resourcePlugins: ResourcePlugin[] = plugins): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`, method: 'GET' })
      .flush(apiProduct);
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources`, method: 'GET' }).flush(resourcePlugins);
  }

  function flushSchema(resourceId: string): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources/${resourceId}/schema`, method: 'GET' })
      .flush(null);
  }

  async function saveAndExpectResources(expected: Resource[]): Promise<void> {
    const versionBefore = TestBed.inject(ApiProductV2Service).planStateVersion();
    const refetch = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
      method: 'GET',
    });
    refetch.flush(product);

    const update = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
      method: 'PUT',
    });
    expect(update.request.body.resources).toEqual(expected);
    expect(update.request.body.name).toBe(product.name);
    expect(update.request.body.version).toBe(product.version);
    expect(update.request.body.apiIds).toEqual(product.apiIds);
    expect(update.request.body.groups).toEqual(product.groups);
    expect(update.request.body.tags).toEqual(product.tags);
    update.flush({ ...product, resources: expected, deploymentState: 'NEED_REDEPLOY' });

    expect(TestBed.inject(ApiProductV2Service).planStateVersion()).toBe(versionBefore + 1);
    flushLoad({ ...product, resources: expected, deploymentState: 'NEED_REDEPLOY' });
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  async function openForUpdate(): Promise<void> {
    await setup(['api_product-definition-r', 'api_product-definition-u']);
  }

  it('lists the product resources and their enabled state', async () => {
    await openForUpdate();
    await show();

    const table = await harness.getTable();
    const rows = await table.getCellTextByIndex();
    expect(rows[0][0]).toContain('MyCache');
    expect(rows[0][0]).toContain('Enabled');
    expect(rows[0][1]).toContain('Cache');
    expect(rows[1][0]).toContain('MyOAuth');
    expect(rows[1][0]).toContain('Disabled');
    expect(rows[1][1]).toContain('OAuth2');
  });

  it('adds only an allow-listed plugin and saves the full resources list', async () => {
    await openForUpdate();
    await show();

    await harness.clickAddResource();
    const addDialog = await rootLoader.getHarness(ApiResourcesAddDialogHarness);
    expect(await addDialog.getDisplayedResources()).toEqual(['Cache', 'OAuth2']);

    await addDialog.select('Cache');
    const editDialog = await rootLoader.getHarness(ApiResourcesEditDialogHarness);
    flushSchema('cache');
    await editDialog.setResourceName('Second Cache');
    await editDialog.save();

    await saveAndExpectResources([cacheResource, oauthResource, { name: 'Second Cache', type: 'cache', configuration: undefined }]);
    expect(snackBar.success).toHaveBeenCalledWith('Resource added!');
    expect(fixture.nativeElement.textContent).toContain('Second Cache');
  });

  it('edits a resource and saves the full resources list', async () => {
    await openForUpdate();
    await show();

    await harness.clickEdit(0);
    const editDialog = await rootLoader.getHarness(ApiResourcesEditDialogHarness);
    flushSchema('cache');
    await editDialog.setResourceName('Renamed Cache');
    await editDialog.save();

    await saveAndExpectResources([{ ...cacheResource, name: 'Renamed Cache' }, oauthResource]);
    expect(snackBar.success).toHaveBeenCalledWith('Resource updated!');
  });

  it('enables and disables a resource through the full resources list', async () => {
    await openForUpdate();
    await show();

    await harness.clickToggle(1);
    await saveAndExpectResources([cacheResource, { ...oauthResource, enabled: true }]);
    expect(snackBar.success).toHaveBeenCalledWith('Resource updated!');
    expect(fixture.nativeElement.textContent).toContain('Enabled');
  });

  it('asks for confirmation before removing a resource and saves the remaining list', async () => {
    await openForUpdate();
    await show();

    await harness.clickRemove(0);
    const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    expect(fixture.nativeElement.ownerDocument.body.textContent).toContain('Are you sure you want to remove this resource?');
    await confirmDialog.confirm();

    await saveAndExpectResources([oauthResource]);
    expect(snackBar.success).toHaveBeenCalledWith('Resource removed!');
    expect(fixture.nativeElement.textContent).not.toContain('MyCache');
  });

  it('leaves the list unchanged when removal is cancelled', async () => {
    await openForUpdate();
    await show();

    await harness.clickRemove(0);
    const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    await confirmDialog.cancel();
    fixture.detectChanges();

    expect(httpTestingController.match(req => req.method === 'PUT')).toHaveLength(0);
    expect(fixture.nativeElement.textContent).toContain('MyCache');
  });

  it('shows the server error and leaves the list unchanged when save fails', async () => {
    await openForUpdate();
    await show();
    const versionBefore = TestBed.inject(ApiProductV2Service).planStateVersion();

    await harness.clickToggle(0);
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`, method: 'GET' })
      .flush(product);
    const update = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
      method: 'PUT',
    });
    expect(update.request.body.resources).toEqual([{ ...cacheResource, enabled: false }, oauthResource]);
    update.flush({ message: 'Resource type [cache] is not supported on an API Product' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(snackBar.error).toHaveBeenCalledWith('Resource type [cache] is not supported on an API Product');
    expect(TestBed.inject(ApiProductV2Service).planStateVersion()).toBe(versionBefore);
    expect(fixture.nativeElement.textContent).toContain('MyCache');
    expect(fixture.nativeElement.textContent).toContain('Enabled');
    expect(fixture.nativeElement.textContent).toContain('MyOAuth');
    expect(fixture.nativeElement.textContent).toContain('Disabled');
  });

  it('is read-only when the user cannot update the product', async () => {
    await setup(['api_product-definition-r']);
    await show();

    expect(await harness.getAddButton()).toBeNull();
    expect(await harness.getToggleButton(0)).toBeNull();
    expect(await harness.getEditButton(0)).toBeNull();
    expect(await harness.getRemoveButton(0)).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('MyCache');
    expect(fixture.nativeElement.textContent).toContain('Enabled');
    expect(fixture.nativeElement.textContent).toContain('Disabled');
  });
});
