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

import { OrgSettingsRolesComponent } from './org-settings-roles.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeRole } from '../../../entities/role/role.fixture';

describe('OrgSettingsRolesComponent', () => {
  let fixture: ComponentFixture<OrgSettingsRolesComponent>;
  let component: OrgSettingsRolesComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRolesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should init properly', () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ORGANIZATION/roles`).flush([
      fakeRole({
        id: 'role-1',
        name: 'Role 1',
        description: 'Role 1 description',
        scope: 'ORGANIZATION',
      }),
    ]);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ENVIRONMENT/roles`).flush([
      fakeRole({
        id: 'role-2',
        name: 'Role 2',
        description: 'Role 2 description',
        scope: 'ENVIRONMENT',
      }),
    ]);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API/roles`).flush([
      fakeRole({
        id: 'role-3',
        name: 'Role 3',
        description: 'Role 3 description',
        scope: 'API',
        default: false,
        system: false,
      }),
    ]);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/APPLICATION/roles`).flush([
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
    ]);

    expect(component.rolesByScope).toStrictEqual([
      {
        scope: 'Organization',
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

  afterEach(() => {
    httpTestingController.verify();
  });
});
