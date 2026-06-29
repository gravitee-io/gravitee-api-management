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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { EditMemberDialogComponent } from './edit-member-dialog.component';

import { GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { Group } from '../../../../../entities/group/group';
import { Member } from '../membershipState';
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
    lock_application_role: false,
    disable_membership_notifications: false,
    event_rules: [],
    roles: {},
  };

  const member: Member = {
    id: 'user-1',
    displayName: 'Alex River',
    roles: { API: 'USER', APPLICATION: 'USER', INTEGRATION: 'USER', CLUSTER: 'USER' },
  };

  const dialogRefSpy = { close: jest.fn() };

  const setup = () => {
    TestBed.resetTestingModule();
    // UsersService is intentionally NOT provided: the dialog must resolve the member from its
    // Gravitee id alone, never via GET /search/users.
    TestBed.configureTestingModule({
      imports: [EditMemberDialogComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            group: GROUP,
            member,
            members: [member],
            defaultAPIRoles: [],
            defaultApplicationRoles: [],
            defaultIntegrationRoles: [],
            defaultClusterRoles: [],
          } as EditMemberDialogData,
        },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => ({ api: { primaryOwnerMode: 'HYBRID' } }) } },
        { provide: GioTestingPermissionProvider, useValue: ['environment-group-u'] },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(EditMemberDialogComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  };

  it('sets Group Admin without any /search/users request, resolving the member by id', () => {
    const component = setup();
    const httpTestingController = TestBed.inject(HttpTestingController);

    component.editMemberForm.controls.groupAdmin.setValue(true);
    component.onChange();
    component.submit();

    // The bug: a directory search was used to recover the member; in large LDAP directories it
    // missed the member and Save threw. There must now be no such request at all.
    httpTestingController.expectNone((req) => req.url.includes('/search/users'));

    const { memberships } = dialogRefSpy.close.mock.calls[0][0] as { memberships: GroupMembership[] };
    expect(memberships).toHaveLength(1);
    expect(memberships[0].id).toBe('user-1');
    expect(memberships[0].roles).toContainEqual({ name: 'ADMIN', scope: 'GROUP' });
  });
});
