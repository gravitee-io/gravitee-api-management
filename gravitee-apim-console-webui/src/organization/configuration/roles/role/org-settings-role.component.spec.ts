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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { OrgSettingsRoleComponent } from './org-settings-role.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { Role } from '../../../../entities/role/role';
import { fakePermissionsByScopes } from '../../../../entities/role/permission.fixtures';

describe('OrgSettingsRoleComponent', () => {
  const roleScope = 'ORGANIZATION';
  const role = 'USER';
  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<OrgSettingsRoleComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  afterEach(() => {
    httpTestingController.verify();
    jest.resetAllMocks();
  });

  describe('edit mode', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
        providers: [
          { provide: UIRouterState, useValue: fakeAjsState },
          { provide: UIRouterStateParams, useValue: { roleScope, role } },
        ],
      });
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(OrgSettingsRoleComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    });

    it('should update role', async () => {
      const role = fakeRole({
        id: 'roleId',
        scope: roleScope,
        permissions: {
          '1_USER_P': ['C', 'R', 'U', 'D'],
        },
      });
      expectRoleGetRequest(role);
      expectGetPermissionsByScopeRequest(['1_USER_P', '2_ROLE_P']);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      expect(await nameInput.isDisabled()).toEqual(true);

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      await descriptionInput.setValue('New description');

      const defaultToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=default]' }));
      await defaultToggle.toggle();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({
        ...role,
        description: 'New description',
        default: false,
        permissions: {
          '1_USER_P': ['C', 'R', 'U', 'D'],
          '2_ROLE_P': [],
        },
      });
      // No flush to stop test here
    });

    it('should update role permissions', async () => {
      const role = fakeRole({
        id: 'roleId',
        scope: roleScope,
        permissions: {
          '1_USER_P': ['C', 'R', 'U', 'D'],
        },
      });
      expectRoleGetRequest(role);
      expectGetPermissionsByScopeRequest(['1_USER_P', '2_ROLE_P']);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#rolePermissionsTable' }));
      const rows = await table.getRows();

      // Uncheck Create for the first row
      const firstRowCreateCell = (await rows[0].getCells({ columnName: 'create' }))[0];
      const firstRowCreateCheckbox = await firstRowCreateCell.getHarness(MatCheckboxHarness);
      await firstRowCreateCheckbox.uncheck();

      // Uncheck Read for the first row
      const firstRowReadCell = (await rows[0].getCells({ columnName: 'read' }))[0];
      const firstRowReadCheckbox = await firstRowReadCell.getHarness(MatCheckboxHarness);
      await firstRowReadCheckbox.uncheck();

      // Check Update for the second row
      const firstRowUpdateCell = (await rows[1].getCells({ columnName: 'update' }))[0];
      const firstRowUpdateCheckbox = await firstRowUpdateCell.getHarness(MatCheckboxHarness);
      await firstRowUpdateCheckbox.check();

      // Check Delete for the second row
      const firstRowDeleteCell = (await rows[1].getCells({ columnName: 'delete' }))[0];
      const firstRowDeleteCheckbox = await firstRowDeleteCell.getHarness(MatCheckboxHarness);
      await firstRowDeleteCheckbox.check();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({
        ...role,
        permissions: {
          '1_USER_P': ['U', 'D'],
          '2_ROLE_P': ['U', 'D'],
        },
      });
      // No flush to stop test here
    });

    it('should toggle select all create & update permission right', async () => {
      const role = fakeRole({
        id: 'roleId',
        scope: roleScope,
        permissions: {
          '1_USER_P': ['U', 'R'],
          '2_ROLE_P': ['R'],
        },
      });
      expectRoleGetRequest(role);
      expectGetPermissionsByScopeRequest(['1_USER_P', '2_ROLE_P']);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#rolePermissionsTable' }));
      const headerRow = (await table.getHeaderRows())[0];

      // Select all create
      const createHeaderCell = (await headerRow.getCells({ columnName: 'create' }))[0];
      const selectAllCreateCheckbox = await createHeaderCell.getHarness(MatCheckboxHarness);
      expect(await selectAllCreateCheckbox.isChecked()).toEqual(false);
      expect(await selectAllCreateCheckbox.isIndeterminate()).toEqual(false);
      await selectAllCreateCheckbox.check();

      // Expect read select all is checked
      const readHeaderCell = (await headerRow.getCells({ columnName: 'read' }))[0];
      const selectAllReadCheckbox = await readHeaderCell.getHarness(MatCheckboxHarness);
      expect(await selectAllReadCheckbox.isChecked()).toEqual(true);
      expect(await selectAllReadCheckbox.isIndeterminate()).toEqual(false);

      // Unselect all update
      const updateHeaderCell = (await headerRow.getCells({ columnName: 'update' }))[0];
      const selectAllUpdateCheckbox = await updateHeaderCell.getHarness(MatCheckboxHarness);
      expect(await selectAllUpdateCheckbox.isIndeterminate()).toEqual(true);
      await selectAllUpdateCheckbox.toggle(); // Check all
      await selectAllUpdateCheckbox.toggle(); // Unckeck all

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({
        ...role,
        permissions: {
          '1_USER_P': ['C', 'R'],
          '2_ROLE_P': ['C', 'R'],
        },
      });
      // No flush to stop test here
    });

    it('should disable form with a system role', async () => {
      const role = fakeRole({ id: 'roleId', system: true, scope: roleScope });
      expectRoleGetRequest(role);
      expectGetPermissionsByScopeRequest(['1_USER_P', '2_ROLE_P']);

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      expect(await descriptionInput.isDisabled()).toEqual(true);

      const defaultToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=default]' }));
      expect(await defaultToggle.isDisabled()).toEqual(true);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#rolePermissionsTable' }));
      const rows = await table.getRows();

      // Checkbox Create for the first row should be disabled
      const firstRowCreateCell = (await rows[0].getCells({ columnName: 'create' }))[0];
      const firstRowCreateCheckbox = await firstRowCreateCell.getHarness(MatCheckboxHarness);
      expect(await firstRowCreateCheckbox.isDisabled()).toEqual(true);

      // Checkbox Read for the first row should be disabled
      const firstRowReadCell = (await rows[0].getCells({ columnName: 'read' }))[0];
      const firstRowReadCheckbox = await firstRowReadCell.getHarness(MatCheckboxHarness);
      expect(await firstRowReadCheckbox.isDisabled()).toEqual(true);

      // Checkbox Update for the first row should be disabled
      const firstRowUpdateCell = (await rows[0].getCells({ columnName: 'update' }))[0];
      const firstRowUpdateCheckbox = await firstRowUpdateCell.getHarness(MatCheckboxHarness);
      expect(await firstRowUpdateCheckbox.isDisabled()).toEqual(true);

      // Checkbox Delete for the first row should be disabled
      const firstRowDeleteCell = (await rows[0].getCells({ columnName: 'delete' }))[0];
      const firstRowDeleteCheckbox = await firstRowDeleteCell.getHarness(MatCheckboxHarness);
      expect(await firstRowDeleteCheckbox.isDisabled()).toEqual(true);
    });
  });

  describe('create mode', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
        providers: [
          { provide: UIRouterState, useValue: fakeAjsState },
          { provide: UIRouterStateParams, useValue: { roleScope } },
        ],
      });
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(OrgSettingsRoleComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    });

    it('should create role', async () => {
      expectGetPermissionsByScopeRequest(['1_USER_P', '2_ROLE_P']);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('New name');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      await descriptionInput.setValue('New description');

      const defaultToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=default]' }));
      await defaultToggle.toggle();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#rolePermissionsTable' }));
      const rows = await table.getRows();

      // Check Create for the first row
      const firstRowCreateCell = (await rows[0].getCells({ columnName: 'create' }))[0];
      const firstRowCreateCheckbox = await firstRowCreateCell.getHarness(MatCheckboxHarness);
      await firstRowCreateCheckbox.check();

      // Check Read for the first row
      const firstRowReadCell = (await rows[0].getCells({ columnName: 'read' }))[0];
      const firstRowReadCheckbox = await firstRowReadCell.getHarness(MatCheckboxHarness);
      await firstRowReadCheckbox.check();

      // Check Update for the first row
      const firstRowUpdateCell = (await rows[0].getCells({ columnName: 'update' }))[0];
      const firstRowUpdateCheckbox = await firstRowUpdateCell.getHarness(MatCheckboxHarness);
      await firstRowUpdateCheckbox.check();

      // Check Delete for the first row
      const firstRowDeleteCell = (await rows[0].getCells({ columnName: 'delete' }))[0];
      const firstRowDeleteCheckbox = await firstRowDeleteCell.getHarness(MatCheckboxHarness);
      await firstRowDeleteCheckbox.check();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({
        name: 'NEW NAME',
        description: 'New description',
        default: true,
        scope: roleScope,
        permissions: {
          '1_USER_P': ['C', 'R', 'U', 'D'],
          '2_ROLE_P': [],
        },
        system: false,
      });
      req.flush({
        name: 'CREATED_ROLE_NAME',
      });
      fixture.detectChanges();

      expect(fakeAjsState.go).toHaveBeenCalledWith('organization.settings.ng-roleedit', {
        role: 'CREATED_ROLE_NAME',
        roleScope: 'ORGANIZATION',
      });
    });
  });

  function expectRoleGetRequest(role: Role) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`, method: 'GET' })
      .flush(role);
    fixture.detectChanges();
  }

  function expectGetPermissionsByScopeRequest(permissions: string[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes`, method: 'GET' })
      .flush(fakePermissionsByScopes({ [roleScope]: permissions }));
    fixture.detectChanges();
  }
});
