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
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { of } from 'rxjs';

import { AddMembersDialogComponent } from './add-members-dialog.component';

import { GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { Group } from '../../../../../entities/group/group';
import { Member } from '../membershipState';
import { Role } from '../../../../../entities/role/role';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { AddOrInviteMembersDialogData } from '../group.component';

describe('AddMembersDialogComponent', () => {
  const GROUP: Group = {
    id: 'group-1',
    name: 'Group 1',
    manageable: true,
    system_invitation: true,
    email_invitation: false,
    max_invitation: 10,
    lock_api_role: false,
    lock_api_product_role: false,
    lock_application_role: false,
    disable_membership_notifications: false,
    event_rules: [],
    roles: {},
  };

  const ROLES_API: Role[] = [
    { id: 'r-api-owner', name: 'OWNER', scope: 'API' },
    { id: 'r-api-po', name: 'PRIMARY_OWNER', scope: 'API' },
    { id: 'r-api-reviewer', name: 'REVIEWER', scope: 'API' },
    { id: 'r-api-user', name: 'USER', scope: 'API' },
  ];
  const ROLES_API_PRODUCT: Role[] = [
    { id: 'r-ap-owner', name: 'OWNER', scope: 'API_PRODUCT' },
    { id: 'r-ap-po', name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' },
    { id: 'r-ap-user', name: 'USER', scope: 'API_PRODUCT' },
  ];
  const ROLES_APPLICATION: Role[] = [
    { id: 'r-app-owner', name: 'OWNER', scope: 'APPLICATION' },
    { id: 'r-app-user', name: 'USER', scope: 'APPLICATION' },
  ];
  const ROLES_INTEGRATION: Role[] = [{ id: 'r-int-user', name: 'USER', scope: 'INTEGRATION' }];
  const ROLES_CLUSTER: Role[] = [{ id: 'r-cl-user', name: 'USER', scope: 'CLUSTER' }];

  const HYBRID_SETTINGS = { api: { primaryOwnerMode: 'HYBRID' }, apiProduct: { primaryOwnerMode: 'HYBRID' } };

  const ALICE: SearchableUser = { id: 'a', reference: 'ref-a', email: 'a@x.com', displayName: 'Alice' };
  const BOB: SearchableUser = { id: 'b', reference: 'ref-b', email: 'b@x.com', displayName: 'Bob' };

  const fakeSelection = (user: SearchableUser): MatAutocompleteSelectedEvent =>
    ({ option: { value: user } }) as unknown as MatAutocompleteSelectedEvent;

  let fixture: ComponentFixture<AddMembersDialogComponent>;
  let component: AddMembersDialogComponent;
  let dialogRefSpy: { close: jest.Mock };

  const setup = (overrides: Partial<AddOrInviteMembersDialogData> = {}, settings = HYBRID_SETTINGS, members: Member[] = []) => {
    dialogRefSpy = { close: jest.fn() };
    const data: AddOrInviteMembersDialogData = {
      group: GROUP,
      members,
      defaultAPIRoles: ROLES_API,
      defaultAPIProductRoles: ROLES_API_PRODUCT,
      defaultApplicationRoles: ROLES_APPLICATION,
      defaultIntegrationRoles: ROLES_INTEGRATION,
      defaultClusterRoles: ROLES_CLUSTER,
      ...overrides,
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [AddMembersDialogComponent, MatDialogModule, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => settings } },
        { provide: UsersService, useValue: { search: () => of([ALICE, BOB]) } },
        { provide: GioTestingPermissionProvider, useValue: ['environment-group-u'] },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddMembersDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  describe('initial state', () => {
    it('starts with empty selection and submit disabled', () => {
      setup();

      expect(component.selectedUsers).toEqual([]);
      expect(component.disableSubmit).toBe(true);
    });

    it('seeds form values from the group default roles', () => {
      setup({ group: { ...GROUP, roles: { API: 'REVIEWER', API_PRODUCT: 'OWNER', APPLICATION: 'OWNER' } } });

      expect(component.addMemberForm.controls.defaultAPIRole.value).toBe('REVIEWER');
      expect(component.addMemberForm.controls.defaultAPIProductRole.value).toBe('OWNER');
      expect(component.addMemberForm.controls.defaultApplicationRole.value).toBe('OWNER');
    });
  });

  describe('selectUser / deselectUser', () => {
    it('adds a user to selectedUsers and enables submit', () => {
      setup();

      component.selectUser(fakeSelection(ALICE));

      expect(component.selectedUsers).toEqual([ALICE]);
      expect(component.disableSubmit).toBe(false);
    });

    it('does not add the same user twice', () => {
      setup();

      component.selectUser(fakeSelection(ALICE));
      component.selectUser(fakeSelection(ALICE));

      expect(component.selectedUsers).toEqual([ALICE]);
    });

    it('removes a user and disables submit when none remain', () => {
      setup();

      component.selectUser(fakeSelection(ALICE));
      component.deselectUser(ALICE);

      expect(component.selectedUsers).toEqual([]);
      expect(component.disableSubmit).toBe(true);
    });
  });

  describe('onDefaultRoleChange', () => {
    it('clears all selected users on role change', () => {
      setup();
      component.selectUser(fakeSelection(ALICE));
      component.selectUser(fakeSelection(BOB));

      component.onDefaultRoleChange();

      expect(component.selectedUsers).toEqual([]);
      expect(component.disableSubmit).toBe(true);
    });
  });

  describe('PRIMARY_OWNER single-user constraint', () => {
    it('disables search when API role is PRIMARY_OWNER and a user is chipped', () => {
      setup();
      component.addMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.selectUser(fakeSelection(ALICE));

      expect(component.addMemberForm.controls.searchTerm.disabled).toBe(true);
    });

    it('disables search when API_PRODUCT role is PRIMARY_OWNER and a user is chipped', () => {
      setup();
      component.addMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.selectUser(fakeSelection(ALICE));

      expect(component.addMemberForm.controls.searchTerm.disabled).toBe(true);
    });

    it('keeps search enabled with required validator when PRIMARY_OWNER is selected and no user is chipped', () => {
      setup();
      component.addMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onDefaultRoleChange();

      expect(component.addMemberForm.controls.searchTerm.enabled).toBe(true);
    });
  });

  describe('submit', () => {
    it('builds a membership for each selected user using current form values', () => {
      setup();
      component.addMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.addMemberForm.controls.defaultAPIProductRole.setValue('OWNER');
      component.addMemberForm.controls.defaultApplicationRole.setValue('OWNER');
      component.addMemberForm.controls.defaultIntegrationRole.setValue('USER');
      component.addMemberForm.controls.defaultClusterRole.setValue('USER');
      component.selectUser(fakeSelection(ALICE));
      component.selectUser(fakeSelection(BOB));

      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(2);
      expect(memberships.map(m => m.id).sort()).toEqual(['a', 'b']);
      memberships.forEach(m => {
        expect(m.roles).toContainEqual({ name: 'OWNER', scope: 'API' });
        expect(m.roles).toContainEqual({ name: 'OWNER', scope: 'API_PRODUCT' });
        expect(m.roles).toContainEqual({ name: 'OWNER', scope: 'APPLICATION' });
        expect(m.roles).toContainEqual({ name: 'USER', scope: 'INTEGRATION' });
        expect(m.roles).toContainEqual({ name: 'USER', scope: 'CLUSTER' });
      });
    });

    it('reflects role changes that happen AFTER the user was selected (lazy form-value mapping)', () => {
      // Reproduces the original APIM-13783 bug: role values must be read at submit time, not at selection time.
      setup();
      component.addMemberForm.controls.defaultAPIProductRole.setValue('USER');
      component.selectUser(fakeSelection(ALICE));

      // Bypassing onDefaultRoleChange() (which would clear the chip in real UI) to exercise the pure mapping logic.
      component.addMemberForm.controls.defaultAPIProductRole.setValue('OWNER');
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API_PRODUCT' });
    });

    it('closes with an empty memberships list when no users are selected', () => {
      setup();

      component.submit();

      expect(dialogRefSpy.close).toHaveBeenCalledWith({ memberships: [] });
    });
  });

  describe('disabling roles', () => {
    it('disables PRIMARY_OWNER for API when another member already holds it', () => {
      const existingPo: Member = {
        id: 'm1',
        displayName: 'Existing PO',
        roles: { API: 'PRIMARY_OWNER', API_PRODUCT: 'USER', APPLICATION: 'USER', INTEGRATION: 'USER', CLUSTER: 'USER' },
      };
      setup({}, HYBRID_SETTINGS, [existingPo]);

      expect(component.disabledAPIRoles.has('r-api-po')).toBe(true);
    });

    it('disables PRIMARY_OWNER for API_PRODUCT when another member already holds it', () => {
      const existingPo: Member = {
        id: 'm1',
        displayName: 'Existing PO',
        roles: { API: 'USER', API_PRODUCT: 'PRIMARY_OWNER', APPLICATION: 'USER', INTEGRATION: 'USER', CLUSTER: 'USER' },
      };
      setup({}, HYBRID_SETTINGS, [existingPo]);

      expect(component.disabledAPIProductRoles.has('r-ap-po')).toBe(true);
    });

    it('disables PRIMARY_OWNER for both scopes when primaryOwnerMode is USER', () => {
      setup({}, { api: { primaryOwnerMode: 'USER' }, apiProduct: { primaryOwnerMode: 'USER' } });

      expect(component.disabledAPIRoles.has('r-api-po')).toBe(true);
      expect(component.disabledAPIProductRoles.has('r-ap-po')).toBe(true);
    });

    it('does not disable PRIMARY_OWNER when mode is HYBRID and no member holds it yet', () => {
      setup();

      expect(component.disabledAPIRoles.has('r-api-po')).toBe(false);
      expect(component.disabledAPIProductRoles.has('r-ap-po')).toBe(false);
    });
  });
});
