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
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatCellHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { OrgSettingsTenantsComponent } from './org-settings-tenants.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeTenant } from '../../../entities/tenant/tenant.fixture';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { Tenant } from '../../../entities/tenant/tenant';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('OrgSettingsTenantsComponent', () => {
  let fixture: ComponentFixture<OrgSettingsTenantsComponent>;
  let httpTestingController: HttpTestingController;
  let permissionsService: GioPermissionService;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true,
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    permissionsService = TestBed.inject(GioPermissionService);
    fixture = TestBed.createComponent(OrgSettingsTenantsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  it('should display tenants in table', async () => {
    permissionsService._setPermissions(['organization-tenant-c', 'organization-tenant-u', 'organization-tenant-d']);
    fixture.detectChanges();

    const tenants = [
      fakeTenant({ id: 'tenant-1', name: 'Tenant 1', description: 'Tenant 1 description' }),
      fakeTenant({ id: 'tenant-2', name: 'Tenant 2', description: 'Tenant 2 description' }),
    ];

    respondToGetTenants(tenants);

    await loader.getHarness(MatCellHarness.with({ text: 'tenant-1' }));
    await loader.getHarness(MatCellHarness.with({ text: 'Tenant 1' }));
    await loader.getHarness(MatCellHarness.with({ text: 'Tenant 1 description' }));
    await loader.getHarness(MatCellHarness.with({ text: 'tenant-2' }));
    await loader.getHarness(MatCellHarness.with({ text: 'Tenant 2' }));
    await loader.getHarness(MatCellHarness.with({ text: 'Tenant 2 description' }));
  });

  it('should filter tenants displayed in table', async () => {
    permissionsService._setPermissions(['organization-tenant-c', 'organization-tenant-u', 'organization-tenant-d']);
    fixture.detectChanges();

    const tenants = [
      fakeTenant({ id: 'tenant-1', name: 'Tenant 1', description: 'Tenant 1 description' }),
      fakeTenant({ id: 'tenant-2', name: 'Tenant 2', description: 'Tenant 2 description' }),
    ];

    respondToGetTenants(tenants);

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
    await tableWrapper.setSearchValue('Tenant 2');

    const cells = await loader.getAllHarnesses(MatCellHarness);
    for (const cell of cells) {
      expect(await cell.getText()).not.toContain('1');
    }
  });

  it('should create a new tenant', async () => {
    permissionsService._setPermissions(['organization-tenant-c']);
    fixture.detectChanges();

    respondToGetTenants([fakeTenant()]);
    fixture.detectChanges();

    const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to add a tenant"]' }));
    await addButton.click();

    const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
    await nameInput.setValue('External');

    const descriptionInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
    await descriptionInput.setValue('External tenant');

    const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    await submitButton.click();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
    });
    expect(req.request.body).toEqual([
      {
        name: 'External',
        description: 'External tenant',
      },
    ]);

    const serverResponse = [
      {
        id: 'external',
        name: 'External',
        description: 'External tenant',
      },
    ];
    req.flush(serverResponse);
    respondToGetTenants(serverResponse);
  });

  it('should edit a tenant', async () => {
    permissionsService._setPermissions(['organization-tenant-u']);
    fixture.detectChanges();

    respondToGetTenants([fakeTenant({ id: 'tenant-1', name: 'Tenant 1', description: 'Tenant 1 description' })]);

    fixture.detectChanges();

    const addButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to edit a tenant"]' }));
    await addButton.click();

    const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
    await nameInput.setValue('External');

    const descriptionInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
    await descriptionInput.setValue('External tenant');

    const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    await submitButton.click();

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
    });
    expect(req.request.body).toEqual([
      {
        id: 'tenant-1',
        name: 'External',
        description: 'External tenant',
      },
    ]);

    const serverResponse = [
      {
        id: 'external',
        name: 'External',
        description: 'External tenant',
      },
    ];
    req.flush(serverResponse);
    respondToGetTenants(serverResponse);
  });

  it('should delete a tenant', async () => {
    permissionsService._setPermissions(['organization-tenant-d']);
    fixture.detectChanges();

    respondToGetTenants([fakeTenant({ id: 'tenant-1', name: 'Tenant 1', description: 'Tenant 1 description' })]);

    fixture.detectChanges();

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a tenant"]' }));
    await deleteButton.click();

    const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
    await confirmDialogButton.click();

    httpTestingController
      .expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants/tenant-1`,
      })
      .flush(null);

    respondToGetTenants([]);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToGetTenants(tenants: Tenant[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
      })
      .flush(tenants);
  }

  it('should not submit tenant form if name is too long', async () => {
    permissionsService._setPermissions(['organization-tenant-c']);
    fixture.detectChanges();
    respondToGetTenants([fakeTenant({ id: 'tenant-1', name: 'Tenant 1', description: 'Tenant 1 description' })]);
    fixture.detectChanges();
    const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to add a tenant"]' }));
    await addButton.click();
    const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
    await nameInput.setValue('A'.repeat(41)); // greater than allowed characters
    const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    expect(await submitButton.isDisabled()).toBeTruthy();

    httpTestingController.expectNone({
      method: 'POST',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
    });
  });
});
