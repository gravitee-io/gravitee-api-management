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
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';

import { OrgSettingsRolesComponent } from './org-settings-roles.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeRole } from '../../../entities/role/role.fixture';
import { Role } from '../../../entities/role/role';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('OrgSettingsRolesComponent', () => {
  let fixture: ComponentFixture<OrgSettingsRolesComponent>;
  let component: OrgSettingsRolesComponent;
  let httpTestingController: HttpTestingController;
  let permissionsService: GioPermissionService;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, MatIconTestingModule, GioLicenseTestingModule],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    permissionsService = TestBed.inject(GioPermissionService);
    fixture = TestBed.createComponent(OrgSettingsRolesComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  it('should init properly', () => {
    fixture.detectChanges();
    respondToGetRolesRequests(
      [
        fakeRole({
          id: 'role-1',
          name: 'Role 1',
          description: 'Role 1 description',
          scope: 'ORGANIZATION',
        }),
      ],
      [
        fakeRole({
          id: 'role-2',
          name: 'Role 2',
          description: 'Role 2 description',
          scope: 'ENVIRONMENT',
        }),
      ],
      [
        fakeRole({
          id: 'role-3',
          name: 'Role 3',
          description: 'Role 3 description',
          scope: 'API',
          default: false,
          system: false,
        }),
      ],
      [
        fakeRole({
          id: 'role-4',
          name: 'Role 4',
          description: 'Role 4 description',
          scope: 'APPLICATION',
        }),
        fakeRole({
          id: 'role-5',
          name: 'Role 5',
          description: 'Role 5 description',
          scope: 'APPLICATION',
        }),
      ],
      [
        fakeRole({
          id: 'role-6',
          name: 'Role 6',
          description: 'Role 6 description',
          scope: 'INTEGRATION',
        }),
      ],
    );

    expect(component.rolesByScope).toStrictEqual([
      {
        scope: 'Organization',
        scopeId: 'ORGANIZATION',
        roles: [
          {
            canBeDeleted: false,
            description: 'Role 1 description',
            hasUserRoleManagement: true,
            icon: 'corporate_fare',
            isDefault: true,
            isSystem: false,
            isReadOnly: false,
            name: 'Role 1',
          },
        ],
      },
      {
        scope: 'Environment',
        scopeId: 'ENVIRONMENT',
        roles: [
          {
            canBeDeleted: false,
            description: 'Role 2 description',
            hasUserRoleManagement: false,
            icon: 'dns',
            isDefault: true,
            isSystem: false,
            name: 'Role 2',
            isReadOnly: false,
          },
        ],
      },
      {
        scope: 'API',
        scopeId: 'API',
        roles: [
          {
            canBeDeleted: true,
            description: 'Role 3 description',
            hasUserRoleManagement: false,
            icon: 'dashboard',
            isDefault: false,
            isSystem: false,
            name: 'Role 3',
            isReadOnly: false,
          },
        ],
      },
      {
        scope: 'Application',
        scopeId: 'APPLICATION',
        roles: [
          {
            canBeDeleted: false,
            description: 'Role 4 description',
            hasUserRoleManagement: false,
            icon: 'list',
            isDefault: true,
            isSystem: false,
            name: 'Role 4',
            isReadOnly: false,
          },
          {
            canBeDeleted: false,
            description: 'Role 5 description',
            hasUserRoleManagement: false,
            icon: 'list',
            isDefault: true,
            isSystem: false,
            name: 'Role 5',
            isReadOnly: false,
          },
        ],
      },
      {
        scope: 'Integration',
        scopeId: 'INTEGRATION',
        roles: [
          {
            canBeDeleted: false,
            description: 'Role 6 description',
            hasUserRoleManagement: false,
            icon: 'list',
            isDefault: true,
            isReadOnly: false,
            isSystem: false,
            name: 'Role 6',
          },
        ],
      },
    ]);
  });

  describe('onDeleteRoleClicked', () => {
    beforeEach(() => {
      permissionsService._setPermissions(['organization-role-d']);
      fixture.detectChanges();
    });

    it('should send a DELETE request', async () => {
      respondToGetRolesRequests(
        [
          fakeRole({
            id: 'role-1',
            name: 'Role 1',
            description: 'Role 1 description',
            scope: 'ORGANIZATION',
            default: false,
            system: false,
          }),
        ],
        [],
        [],
        [],
        [],
      );

      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a role"]' }));
      await deleteButton.click();

      const dialogConfirmButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await dialogConfirmButton.click();

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ORGANIZATION/roles/Role 1`,
        })
        .flush(null);

      respondToGetRolesRequests([], [], [], [], []);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToGetRolesRequests(orgRoles: Role[], envRoles: Role[], apiRoles: Role[], appRoles: Role[], integrationRoles: Role[]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ORGANIZATION/roles`).flush(orgRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ENVIRONMENT/roles`).flush(envRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API/roles`).flush(apiRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/APPLICATION/roles`).flush(appRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/INTEGRATION/roles`).flush(integrationRoles);
  }
});
