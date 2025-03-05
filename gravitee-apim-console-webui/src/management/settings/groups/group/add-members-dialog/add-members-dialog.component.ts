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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, map, startWith } from 'rxjs/operators';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Observable, of, switchMap } from 'rxjs';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';

import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { GroupService } from '../../../../../services-ngx/group.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Group } from '../../../../../entities/group/group';
import { Role } from '../../../../../entities/role/role';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { Member } from '../../../../../entities/management-api-v2';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { RoleName } from '../membershipState';
import { UsersService } from '../../../../../services-ngx/users.service';

@Component({
  selector: 'add-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    ReactiveFormsModule,
    MatIconModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatListModule,
    MatChipsModule,
  ],
  templateUrl: './add-members-dialog.component.html',
  styleUrl: './add-members-dialog.component.scss',
})
export class AddMembersDialogComponent implements OnInit {
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  addMemberForm: FormGroup<{
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
    searchTerm: FormControl<string>;
  }>;
  filteredUsers$: Observable<SearchableUser[]>;
  selectedUsers: SearchableUser[] = [];

  private group: Group;
  private memberships: GroupMembership[] = [];
  private members: Member[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private permissionService: GioPermissionService,
    private usersService: UsersService,
    private settingsService: EnvironmentSettingsService,
    private matDialogRef: MatDialogRef<AddMembersDialogComponent>,
    private groupService: GroupService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.group = this.data['group'];
    this.members = this.data['members'];
    this.defaultAPIRoles = this.data['defaultAPIRoles'];
    this.defaultApplicationRoles = this.data['defaultApplicationRoles'];
    this.defaultIntegrationRoles = this.data['defaultIntegrationRoles'];
    this.addMemberForm = new FormGroup({
      defaultAPIRole: new FormControl<string>('USER'),
      defaultApplicationRole: new FormControl<string>('USER'),
      defaultIntegrationRole: new FormControl<string>('USER'),
      searchTerm: new FormControl<string>(''),
    });
    this.filteredUsers$ = this.addMemberForm.controls['searchTerm'].valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term: string) => this.searchUsers(term)),
    );
  }

  canChangeDefaultAPIRole() {
    return this.isSuperAdmin() || !this.group.lock_api_role;
  }

  canChangeDefaultApplicationRole() {
    return this.isSuperAdmin() || !this.group.lock_application_role;
  }

  canChangeDefaultIntegrationRole(): boolean {
    return this.isSuperAdmin();
  }

  isSuperAdmin() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  isAPIRoleDisabled(role: Role) {
    if (this.isUserPrimaryOwner()) {
      return role.name === RoleName.PRIMARY_OWNER;
    }

    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  private isUserPrimaryOwner() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  isManyPrimaryOwnersSelected() {
    return (
      this.isSelectedAPIRolePrimaryOwner() &&
      this.memberships.filter((membership) => membership.roles.find((role) => role.scope === 'API').name === RoleName.PRIMARY_OWNER)
        .length > 1
    );
  }

  isSelectedAPIRolePrimaryOwner() {
    return this.addMemberForm.controls['defaultAPIRole'].value === RoleName.PRIMARY_OWNER;
  }

  isAnotherMemberPrimaryOwner() {
    return this.isSelectedAPIRolePrimaryOwner() && this.members.some((member) => member.roles['API'] === RoleName.PRIMARY_OWNER);
  }

  isFormInvalid() {
    return this.memberships.length === 0 || this.isAnotherMemberPrimaryOwner() || this.isManyPrimaryOwnersSelected();
  }

  addMembers() {
    this.groupService.addOrUpdateMemberships(this.group.id, this.memberships).subscribe({
      next: () => {
        this.snackBarService.success(`Successfully added selected members to group ${this.group.name}`);
      },
      error: () => {
        this.snackBarService.error(`Error occurred while adding members to group ${this.group.name}`);
      },
    });

    this.matDialogRef.close();
  }

  mapGroupMembership(user: SearchableUser): GroupMembership {
    return {
      id: user.id,
      reference: user.reference,
      roles: [
        {
          name: this.addMemberForm.controls['defaultAPIRole'].value,
          scope: 'API',
        },
        {
          name: this.addMemberForm.controls['defaultApplicationRole'].value,
          scope: 'APPLICATION',
        },
        {
          name: this.addMemberForm.controls['defaultIntegrationRole'].value,
          scope: 'INTEGRATION',
        },
      ],
    };
  }

  private searchUsers(searchTerm: string) {
    if (!searchTerm.trim()) {
      return of([]);
    }

    return this.usersService.search(searchTerm).pipe(
      map((users) => {
        const excludedIds = new Set([...this.members.map((member) => member.id), ...this.selectedUsers.map((user) => user.id)]);
        return users.filter((user) => !excludedIds.has(user.id));
      }),
    );
  }

  selectUser($event: MatAutocompleteSelectedEvent) {
    const selectedUser = $event.option.value;

    if (!this.selectedUsers.includes(selectedUser)) {
      this.selectedUsers.push(selectedUser);
      this.memberships.push(this.mapGroupMembership(selectedUser));
    }
    this.addMemberForm.controls['searchTerm'].setValue('');
  }

  deselectUser(user: SearchableUser) {
    const index = this.selectedUsers.indexOf(user);

    if (index >= 0) {
      this.selectedUsers.splice(index, 1);
      const membershipIndex = this.memberships.findIndex((member) => member.id === user.id);

      if (membershipIndex >= 0) {
        this.memberships.splice(index, 1);
      }
    }
  }
}
