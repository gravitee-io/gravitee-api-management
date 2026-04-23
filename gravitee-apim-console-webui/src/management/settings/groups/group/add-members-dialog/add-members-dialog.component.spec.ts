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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AddMembersDialogComponent } from './add-members-dialog.component';

import { Group } from '../../../../../entities/group/group';
import { Role } from '../../../../../entities/role/role';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { GioTestingModule } from '../../../../../shared/testing';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { GroupMembership } from '../../../../../entities/group/groupMember';

describe('AddMembersDialogComponent', () => {
  const SETTINGS_SNAPSHOT = {
    api: {
      primaryOwnerMode: 'USER',
    },
  };

  const GROUP: Group = {
    id: 'g1',
    manageable: true,
    max_invitation: 10,
    lock_api_role: false,
    lock_api_product_role: false,
    lock_application_role: false,
    roles: { API: 'USER', API_PRODUCT: 'USER', APPLICATION: 'USER' },
  };

  const DEFAULT_API_ROLES: Role[] = [{ id: '1', name: 'USER', scope: 'API' }];
  const DEFAULT_API_PRODUCT_ROLES: Role[] = [
    { id: 'ap1', name: 'USER', scope: 'API_PRODUCT' },
    { id: 'ap2', name: 'REVIEWER', scope: 'API_PRODUCT' },
  ];
  const DEFAULT_APP_ROLES: Role[] = [{ id: 'a1', name: 'USER', scope: 'APPLICATION' }];
  const DEFAULT_INTEGRATION_ROLES: Role[] = [{ id: 'i1', name: 'USER', scope: 'INTEGRATION' }];
  const DEFAULT_CLUSTER_ROLES: Role[] = [{ id: 'c1', name: 'USER', scope: 'CLUSTER' }];

  const SEARCH_USER: SearchableUser = {
    id: 'u99',
    reference: 'ref-u99',
    displayName: 'New Member',
  };

  let fixture: ComponentFixture<AddMembersDialogComponent>;
  let component: AddMembersDialogComponent;
  let matDialogRef: MatDialogRef<AddMembersDialogComponent>;

  beforeEach(async () => {
    matDialogRef = { close: jest.fn() } as unknown as MatDialogRef<AddMembersDialogComponent>;

    await TestBed.configureTestingModule({
      imports: [AddMembersDialogComponent, MatDialogModule, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            group: GROUP,
            members: [],
            defaultAPIRoles: DEFAULT_API_ROLES,
            defaultAPIProductRoles: DEFAULT_API_PRODUCT_ROLES,
            defaultApplicationRoles: DEFAULT_APP_ROLES,
            defaultIntegrationRoles: DEFAULT_INTEGRATION_ROLES,
            defaultClusterRoles: DEFAULT_CLUSTER_ROLES,
          },
        },
        { provide: MatDialogRef, useValue: matDialogRef },
        { provide: GioTestingPermissionProvider, useValue: ['environment-group-u'] },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => SETTINGS_SNAPSHOT } },
        { provide: UsersService, useValue: { search: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddMembersDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should clear selected users and memberships when default role selection changes', () => {
    component.selectedUsers = [SEARCH_USER];
    const internal = component as unknown as { memberships: GroupMembership[] };
    internal.memberships = [component.mapGroupMembership(SEARCH_USER)];
    component.disableSubmit = false;

    component.onDefaultRoleChange();

    expect(component.selectedUsers).toEqual([]);
    expect(internal.memberships).toEqual([]);
    expect(component.disableSubmit).toBe(true);
  });

  it('should close with memberships built from current form values on submit', () => {
    component.addMemberForm.patchValue({ defaultAPIProductRole: 'REVIEWER' });
    component.selectedUsers = [SEARCH_USER];
    const internal = component as unknown as { memberships: GroupMembership[] };
    internal.memberships = [component.mapGroupMembership(SEARCH_USER)];
    component.disableSubmit = false;

    component.submit();

    expect(matDialogRef.close).toHaveBeenCalledWith({
      memberships: [
        expect.objectContaining({
          id: SEARCH_USER.id,
          roles: expect.arrayContaining([{ name: 'REVIEWER', scope: 'API_PRODUCT' }]),
        }),
      ],
    });
  });
});
