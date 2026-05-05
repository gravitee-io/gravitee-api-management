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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { EditMemberDialogComponent } from './edit-member-dialog.component';

import { GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { Group } from '../../../../../entities/group/group';
import { Member } from '../membershipState';
import { Role } from '../../../../../entities/role/role';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { EditMemberDialogData } from '../group.component';

describe('EditMemberDialogComponent', () => {
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

  const USERS: SearchableUser[] = [
    { id: '1', reference: 'ref-1', email: '1@x.com', displayName: 'Member One' },
    { id: '2', reference: 'ref-2', email: '2@x.com', displayName: 'Member Two' },
    { id: '3', reference: 'ref-3', email: '3@x.com', displayName: 'Member Three' },
  ];

  const HYBRID_SETTINGS = { api: { primaryOwnerMode: 'HYBRID' }, apiProduct: { primaryOwnerMode: 'HYBRID' } };

  const mkMember = (id: string, displayName: string, roles: Record<string, string> = {}): Member => ({
    id,
    displayName,
    roles: {
      API: roles.API ?? 'USER',
      API_PRODUCT: roles.API_PRODUCT ?? 'USER',
      APPLICATION: roles.APPLICATION ?? 'USER',
      INTEGRATION: roles.INTEGRATION ?? 'USER',
      CLUSTER: roles.CLUSTER ?? 'USER',
      ...roles,
    },
  });

  let fixture: ComponentFixture<EditMemberDialogComponent>;
  let component: EditMemberDialogComponent;
  let dialogRefSpy: { close: jest.Mock };
  let snackBarSpy: { error: jest.Mock; success: jest.Mock };
  let usersSearchSpy: jest.Mock;

  const setup = (member: Member, members: Member[] = [member], settings = HYBRID_SETTINGS, users: SearchableUser[] = USERS) => {
    dialogRefSpy = { close: jest.fn() };
    snackBarSpy = { error: jest.fn(), success: jest.fn() };
    usersSearchSpy = jest.fn(() => of(users));
    const data: EditMemberDialogData = {
      group: GROUP,
      member,
      members,
      defaultAPIRoles: ROLES_API,
      defaultAPIProductRoles: ROLES_API_PRODUCT,
      defaultApplicationRoles: ROLES_APPLICATION,
      defaultIntegrationRoles: ROLES_INTEGRATION,
      defaultClusterRoles: ROLES_CLUSTER,
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [EditMemberDialogComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => settings } },
        { provide: UsersService, useValue: { search: usersSearchSpy } },
        { provide: SnackBarService, useValue: snackBarSpy },
        { provide: GioTestingPermissionProvider, useValue: ['environment-group-u'] },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EditMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  describe('upgrade / downgrade detection', () => {
    it('detects API role upgrade', () => {
      setup(mkMember('1', 'Member One', { API: 'OWNER' }));
      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');

      expect(component['isRoleUpgrade']('API')).toBe(true);
      expect(component['isRoleDowngrade']('API')).toBe(false);
    });

    it('detects API role downgrade', () => {
      setup(mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' }));
      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');

      expect(component['isRoleDowngrade']('API')).toBe(true);
      expect(component['isRoleUpgrade']('API')).toBe(false);
    });

    it('detects API_PRODUCT role upgrade', () => {
      setup(mkMember('1', 'Member One', { API_PRODUCT: 'USER' }));
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');

      expect(component['isRoleUpgrade']('API_PRODUCT')).toBe(true);
      expect(component['isRoleDowngrade']('API_PRODUCT')).toBe(false);
    });

    it('detects API_PRODUCT role downgrade', () => {
      setup(mkMember('1', 'Member One', { API_PRODUCT: 'PRIMARY_OWNER' }));
      component.editMemberForm.controls.defaultAPIProductRole.setValue('USER');

      expect(component['isRoleDowngrade']('API_PRODUCT')).toBe(true);
      expect(component['isRoleUpgrade']('API_PRODUCT')).toBe(false);
    });

    it('reports neither upgrade nor downgrade for non-PO transitions', () => {
      setup(mkMember('1', 'Member One', { API: 'USER', API_PRODUCT: 'USER' }));
      component.editMemberForm.controls.defaultAPIRole.setValue('REVIEWER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('OWNER');

      expect(component['isRoleUpgrade']('API')).toBe(false);
      expect(component['isRoleDowngrade']('API')).toBe(false);
      expect(component['isRoleUpgrade']('API_PRODUCT')).toBe(false);
      expect(component['isRoleDowngrade']('API_PRODUCT')).toBe(false);
    });
  });

  describe('existing PO lookups', () => {
    it('finds another member holding API PRIMARY_OWNER', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo]);

      expect(component['findExistingPrimaryOwner']('API')?.id).toBe('2');
    });

    it('returns null when the only API PRIMARY_OWNER is the edit user themselves', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser]);

      expect(component['findExistingPrimaryOwner']('API')).toBeNull();
    });

    it('finds another member holding API_PRODUCT PRIMARY_OWNER', () => {
      const editUser = mkMember('1', 'Member One');
      const apiProductPo = mkMember('2', 'Member Two', { API_PRODUCT: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiProductPo]);

      expect(component['findExistingPrimaryOwner']('API_PRODUCT')?.id).toBe('2');
    });
  });

  describe('upgrade banner', () => {
    it('shows fresh-assignment phrasing when no existing PO holds the upgraded scope', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      setup(editUser, [editUser]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();

      expect(component.ownershipTransferMessage).toContain('Member One will become the API primary owner of this group.');
      expect(component.ownershipTransferMessage).not.toContain('will be transferred');
    });

    it('shows transfer phrasing when an existing PO holds the upgraded scope', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();

      expect(component.ownershipTransferMessage).toContain('Member Two is the API primary owner');
      expect(component.ownershipTransferMessage).toContain('transferred to Member One');
    });

    it('collapses to a single sentence when the same outgoing PO holds both scopes', () => {
      const editUser = mkMember('1', 'Member One');
      const dualPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, dualPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();

      expect(component.ownershipTransferMessage).toContain('Member Two is the API and API Product primary owner');
      expect(component.ownershipTransferMessage).not.toContain('The API primary ownership will be transferred');
      expect(component.ownershipTransferMessage).not.toContain('The API Product primary ownership will be transferred');
    });

    it('emits two sentences when API and API_PRODUCT POs are different members', () => {
      const editUser = mkMember('1', 'Member One');
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      const apiProductPo = mkMember('3', 'Member Three', { API_PRODUCT: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo, apiProductPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();

      expect(component.ownershipTransferMessage).toContain('Member Two is the API primary owner');
      expect(component.ownershipTransferMessage).toContain('Member Three is the API Product primary owner');
    });
  });

  describe('downgrade banner', () => {
    it('mentions only the API scope when downgrading API alone', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two');
      setup(editUser, [editUser, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);

      expect(component.ownershipTransferMessage).toContain('Member One is the API primary owner');
      expect(component.ownershipTransferMessage).toContain('transferred to Member Two');
      expect(component.ownershipTransferMessage).not.toContain('API Product');
    });

    it('mentions only the API Product scope when downgrading API_PRODUCT alone', () => {
      const editUser = mkMember('1', 'Member One', { API_PRODUCT: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two');
      setup(editUser, [editUser, successor]);

      component.editMemberForm.controls.defaultAPIProductRole.setValue('USER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);

      expect(component.ownershipTransferMessage).toContain('Member One is the API Product primary owner');
      expect(component.ownershipTransferMessage).toContain('transferred to Member Two');
    });

    it('collapses into one sentence when downgrading both scopes', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two');
      setup(editUser, [editUser, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('USER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);

      expect(component.ownershipTransferMessage).toContain('Member One is the API and API Product primary owner');
    });
  });

  describe('mixed downgrade + upgrade banner', () => {
    it('mentions BOTH the downgrade transfer and the upgrade transfer when scopes move in opposite directions', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER', API_PRODUCT: 'OWNER' });
      const apiProductPo = mkMember('2', 'Member Two', { API_PRODUCT: 'PRIMARY_OWNER' });
      const successor = mkMember('3', 'Member Three');
      setup(editUser, [editUser, apiProductPo, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);

      expect(component.ownershipTransferMessage).toContain('Member One is the API primary owner');
      expect(component.ownershipTransferMessage).toContain('transferred to Member Three');
      expect(component.ownershipTransferMessage).toContain('Member Two is the API Product primary owner');
      expect(component.ownershipTransferMessage).toContain('transferred to Member One');
    });

    it('shows only the upgrade message before a successor is picked, then adds the downgrade message afterwards', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER', API_PRODUCT: 'OWNER' });
      const apiProductPo = mkMember('2', 'Member Two', { API_PRODUCT: 'PRIMARY_OWNER' });
      const successor = mkMember('3', 'Member Three');
      setup(editUser, [editUser, apiProductPo, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();

      // Before successor picked: only the upgrade-side message is visible.
      expect(component.ownershipTransferMessage).toContain('Member Two is the API Product primary owner');
      expect(component.ownershipTransferMessage).not.toContain('Member One is the API primary owner');

      component.selectPrimaryOwner({ option: { value: successor } } as any);

      // After successor picked: both messages present.
      expect(component.ownershipTransferMessage).toContain('Member One is the API primary owner');
      expect(component.ownershipTransferMessage).toContain('Member Two is the API Product primary owner');
    });
  });

  describe('submit ordering and payload shape', () => {
    it('sends a single membership with the edited form values and no PO transfer when only application/integration/cluster change', () => {
      // Pure non-PO save: only APPLICATION/INTEGRATION/CLUSTER are touched, no API or API_PRODUCT
      // role transition. The submit must not produce any demotion or successor membership and must
      // forward the edit user's new values verbatim — guarding against regressions that would route
      // simple edits through the transfer machinery.
      const editUser = mkMember('1', 'Member One', { API: 'OWNER', API_PRODUCT: 'OWNER' });
      setup(editUser, [editUser]);

      component.editMemberForm.controls.defaultApplicationRole.setValue('USER');
      component.editMemberForm.controls.defaultIntegrationRole.setValue('USER');
      component.editMemberForm.controls.defaultClusterRole.setValue('USER');
      component.onChange();
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(1);
      expect(memberships[0].id).toBe('1');
      expect(memberships[0].roles).toEqual(
        expect.arrayContaining([
          { name: 'OWNER', scope: 'API' },
          { name: 'OWNER', scope: 'API_PRODUCT' },
          { name: 'USER', scope: 'APPLICATION' },
          { name: 'USER', scope: 'INTEGRATION' },
          { name: 'USER', scope: 'CLUSTER' },
        ]),
      );
    });

    it('sends only the edit user when promoting to PO with no existing PO for that scope', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      setup(editUser, [editUser]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      const closeArg = dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] };
      expect(closeArg.memberships).toHaveLength(1);
      expect(closeArg.memberships[0].id).toBe('1');
      expect(closeArg.memberships[0].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
    });

    it('demotes the previous API PO before promoting the edit user', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(2);
      expect(memberships[0].id).toBe('2');
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(memberships[1].id).toBe('1');
      expect(memberships[1].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
    });

    it('collapses the demotion into a single membership when one member holds both POs', () => {
      const editUser = mkMember('1', 'Member One');
      const dualPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER', APPLICATION: 'OWNER' });
      setup(editUser, [editUser, dualPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(2);
      expect(memberships[0].id).toBe('2');
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API_PRODUCT' });
      // Demoted member's other-scope roles must NOT be overwritten with the edit user's form values.
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'APPLICATION' });
    });

    it('produces two demotions when API and API_PRODUCT POs are different members', () => {
      const editUser = mkMember('1', 'Member One');
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER', APPLICATION: 'OWNER' });
      const apiProductPo = mkMember('3', 'Member Three', { API_PRODUCT: 'PRIMARY_OWNER', APPLICATION: 'OWNER' });
      setup(editUser, [editUser, apiPo, apiProductPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(3);
      const m2 = memberships.find(m => m.id === '2');
      const m3 = memberships.find(m => m.id === '3');
      const m1 = memberships.find(m => m.id === '1');
      expect(m2?.roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(m3?.roles).toContainEqual({ name: 'OWNER', scope: 'API_PRODUCT' });
      expect(m1?.roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
      expect(m1?.roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' });
      // Promotion of the edit user must come last in the payload.
      expect(memberships[memberships.length - 1].id).toBe('1');
    });

    it('promotes the chosen successor and demotes the edit user on downgrade', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two', { APPLICATION: 'OWNER' });
      setup(editUser, [editUser, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(2);
      expect(memberships[0].id).toBe('1');
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(memberships[1].id).toBe('2');
      expect(memberships[1].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
      // Successor's APPLICATION role must be preserved at OWNER, not overwritten with the edit user's form value.
      expect(memberships[1].roles).toContainEqual({ name: 'OWNER', scope: 'APPLICATION' });
    });

    it('promotes the successor for both scopes when downgrading both', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two');
      setup(editUser, [editUser, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('USER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      const successorMembership = memberships.find(m => m.id === '2');
      expect(successorMembership?.roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
      expect(successorMembership?.roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' });
    });

    it('handles api downgrade + api product upgrade in a single save (mixed case)', () => {
      // The case the reviewer flagged: edit user is API PO and changes API to OWNER (downgrade) while
      // simultaneously promoting their API_PRODUCT role from OWNER to PRIMARY_OWNER (upgrade). Another
      // member currently holds API_PRODUCT=PRIMARY_OWNER and must be demoted; the operator picks a
      // successor for the API role. The submit must produce three memberships ordered as
      // [demote-existing-API_PRODUCT-PO, edit-user, promote-successor-as-new-API-PO].
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER', API_PRODUCT: 'OWNER' });
      const apiProductPo = mkMember('2', 'Member Two', {
        API_PRODUCT: 'PRIMARY_OWNER',
        APPLICATION: 'OWNER',
        INTEGRATION: 'USER',
        CLUSTER: 'USER',
      });
      const successor = mkMember('3', 'Member Three');
      setup(editUser, [editUser, apiProductPo, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(3);
      // 1. The previous API_PRODUCT PO is demoted FIRST so the edit user's API_PRODUCT promotion doesn't
      //    trip the server's PRIMARY_OWNER uniqueness check.
      expect(memberships[0].id).toBe('2');
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API_PRODUCT' });
      // The demoted member's other-scope roles must stay intact.
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'APPLICATION' });
      expect(memberships[0].roles).toContainEqual({ name: 'USER', scope: 'INTEGRATION' });
      expect(memberships[0].roles).toContainEqual({ name: 'USER', scope: 'CLUSTER' });
      // 2. The edit user's combined demotion + promotion comes next.
      expect(memberships[1].id).toBe('1');
      expect(memberships[1].roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(memberships[1].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' });
      // 3. The successor is promoted to the new API PO last.
      expect(memberships[2].id).toBe('3');
      expect(memberships[2].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
    });

    it('handles api upgrade + api product downgrade in a single save (mixed case, inverse)', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER', API_PRODUCT: 'PRIMARY_OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER', APPLICATION: 'OWNER' });
      const successor = mkMember('3', 'Member Three');
      setup(editUser, [editUser, apiPo, successor]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.editMemberForm.controls.defaultAPIProductRole.setValue('USER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      expect(memberships).toHaveLength(3);
      expect(memberships[0].id).toBe('2');
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'API' });
      expect(memberships[0].roles).toContainEqual({ name: 'OWNER', scope: 'APPLICATION' });
      expect(memberships[1].id).toBe('1');
      expect(memberships[1].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API' });
      expect(memberships[1].roles).toContainEqual({ name: 'USER', scope: 'API_PRODUCT' });
      expect(memberships[2].id).toBe('3');
      expect(memberships[2].roles).toContainEqual({ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' });
    });
  });

  describe('successor search filter', () => {
    it('returns no candidates when the term is empty (autocomplete only filters on typed input)', () => {
      const editUser = mkMember('1', 'Member One');
      const m2 = mkMember('2', 'Member Two');
      const m3 = mkMember('3', 'Member Three');
      setup(editUser, [editUser, m2, m3]);

      expect(component['filterMembers']('')).toEqual([]);
      expect(component['filterMembers']('   ')).toEqual([]);
    });

    it('filters by display name case-insensitively', () => {
      const editUser = mkMember('1', 'Member One');
      const m2 = mkMember('2', 'Member Two');
      const m3 = mkMember('3', 'Other Person');
      setup(editUser, [editUser, m2, m3]);

      const result = component['filterMembers']('member');

      expect(result.map(m => m.id)).toEqual(['2']);
    });

    it('coerces a Member object value into a search string instead of throwing', () => {
      const editUser = mkMember('1', 'Member One');
      const m2 = mkMember('2', 'Member Two');
      setup(editUser, [editUser, m2]);

      // mat-autocomplete may transiently write the picked Member object into the form control.
      const result = component['filterMembers'](m2 as unknown as string);

      expect(result.map(m => m.id)).toEqual(['2']);
    });
  });

  describe('displayMember', () => {
    it('returns the displayName for a Member', () => {
      const editUser = mkMember('1', 'Member One');
      setup(editUser);

      expect(component.displayMember(mkMember('2', 'Member Two'))).toBe('Member Two');
    });

    it('returns the string verbatim for a string value', () => {
      const editUser = mkMember('1', 'Member One');
      setup(editUser);

      expect(component.displayMember('typed text')).toBe('typed text');
    });

    it('returns an empty string for null/undefined', () => {
      const editUser = mkMember('1', 'Member One');
      setup(editUser);

      expect(component.displayMember(null)).toBe('');
    });
  });

  describe('downgradedMember reset (review #1)', () => {
    it('clears downgradedMember when the role transitions back to PRIMARY_OWNER', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, mkMember('2', 'Member Two')]);

      // Trigger downgrade — downgradedMember = edit user, search rendered.
      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.onChange();
      expect(component.downgradedMember?.id).toBe('1');

      // Revert to PRIMARY_OWNER — must reset, otherwise the search field stays rendered.
      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();

      expect(component.downgradedMember).toBeNull();
    });

    it('clears downgradedMember on any non-transfer role change', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, mkMember('2', 'Member Two')]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.onChange();
      expect(component.downgradedMember?.id).toBe('1');

      // No transfer happening — reset back to the seed value, downgradedMember should clear.
      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      expect(component.downgradedMember).toBeNull();
    });
  });

  describe('error handling on submit (review #2 and #3)', () => {
    it('surfaces a snackbar error and keeps the dialog open if the successor lookup returns no matching user', () => {
      const editUser = mkMember('1', 'Member One', { API: 'PRIMARY_OWNER' });
      const successor = mkMember('2', 'Member Two');
      // Mock returns a list that does NOT contain the successor — find() will yield undefined.
      setup(editUser, [editUser, successor], HYBRID_SETTINGS, [USERS[0]]);

      component.editMemberForm.controls.defaultAPIRole.setValue('OWNER');
      component.onChange();
      component.selectPrimaryOwner({ option: { value: successor } } as any);
      component.submit();

      expect(snackBarSpy.error).toHaveBeenCalledTimes(1);
      expect(dialogRefSpy.close).not.toHaveBeenCalled();
      expect(component.disableSubmit).toBe(false);
    });

    it('surfaces a snackbar error and keeps the dialog open if the demotion lookup returns no matching user', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo], HYBRID_SETTINGS, [USERS[0]]); // USERS[0] has id '1' only

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      expect(snackBarSpy.error).toHaveBeenCalledTimes(1);
      expect(dialogRefSpy.close).not.toHaveBeenCalled();
      expect(component.disableSubmit).toBe(false);
    });

    it('surfaces a snackbar error if the user search itself fails', () => {
      const editUser = mkMember('1', 'Member One', { API: 'OWNER' });
      const apiPo = mkMember('2', 'Member Two', { API: 'PRIMARY_OWNER' });
      setup(editUser, [editUser, apiPo]);
      // Replace the search after init so getUserDetails has already populated this.user.
      usersSearchSpy.mockReturnValueOnce(throwError(() => new Error('network down')));

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      expect(snackBarSpy.error).toHaveBeenCalledTimes(1);
      expect(dialogRefSpy.close).not.toHaveBeenCalled();
    });
  });

  describe('demoted membership shape (review #4)', () => {
    it('emits exactly the demoted member roles regardless of source object key order', () => {
      const editUser = mkMember('1', 'Member One');
      const apiPo: Member = {
        id: '2',
        displayName: 'Member Two',
        // Source roles intentionally inserted in a non-canonical order — assertion is order-insensitive.
        roles: { CLUSTER: 'USER', INTEGRATION: 'USER', APPLICATION: 'OWNER', API_PRODUCT: 'OWNER', API: 'PRIMARY_OWNER' },
      };
      setup(editUser, [editUser, apiPo]);

      component.editMemberForm.controls.defaultAPIRole.setValue('PRIMARY_OWNER');
      component.onChange();
      component.submit();

      const memberships = (dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] }).memberships;
      const demoted = memberships.find(m => m.id === '2');
      expect(demoted?.roles).toHaveLength(5);
      expect(demoted?.roles).toEqual(
        expect.arrayContaining([
          { name: 'OWNER', scope: 'API' },
          { name: 'OWNER', scope: 'API_PRODUCT' },
          { name: 'OWNER', scope: 'APPLICATION' },
          { name: 'USER', scope: 'INTEGRATION' },
          { name: 'USER', scope: 'CLUSTER' },
        ]),
      );
    });
  });
});
