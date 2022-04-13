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

import { OrgSettingsRolesComponent } from './org-settings-roles.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeRole } from '../../../entities/role/role.fixture';
import { CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';
import { Role } from '../../../entities/role/role';

describe('OrgSettingsRolesComponent', () => {
  let fixture: ComponentFixture<OrgSettingsRolesComponent>;
  let component: OrgSettingsRolesComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  const currentUser = new User();
  currentUser.userPermissions = [];
  currentUser.userApiPermissions = [];
  currentUser.userEnvironmentPermissions = [];
  currentUser.userApplicationPermissions = [];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRolesComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    currentUser.userPermissions = [];
    currentUser.userApiPermissions = [];
    currentUser.userEnvironmentPermissions = [];
    currentUser.userApplicationPermissions = [];
  });

  it('should init properly', () => {
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
          },
          {
            canBeDeleted: false,
            description: 'Role 5 description',
            hasUserRoleManagement: false,
            icon: 'list',
            isDefault: true,
            isSystem: false,
            name: 'Role 5',
          },
        ],
      },
    ]);
  });

  describe('onDeleteRoleClicked', () => {
    beforeEach(() => {
      currentUser.userPermissions = ['organization-role-d'];
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

      respondToGetRolesRequests([], [], [], []);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToGetRolesRequests(orgRoles: Role[], envRoles: Role[], apiRoles: Role[], appRoles: Role[]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ORGANIZATION/roles`).flush(orgRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ENVIRONMENT/roles`).flush(envRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API/roles`).flush(apiRoles);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/APPLICATION/roles`).flush(appRoles);
  }
});
