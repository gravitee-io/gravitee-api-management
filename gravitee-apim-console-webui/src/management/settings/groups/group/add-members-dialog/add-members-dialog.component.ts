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

import { Component, Inject, OnInit, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { Role } from '../../../../../entities/role/role';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { Member } from '../../../../../entities/management-api-v2';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { RoleName } from '../membershipState';
import { UsersService } from '../../../../../services-ngx/users.service';
import { AddOrInviteMembersDialogData } from '../group.component';
import { Group } from '../../../../../entities/group/group';

@Component({
  selector: 'add-member-dialog',
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
  filteredUsers$: Observable<SearchableUser[]>;
  selectedUsers: SearchableUser[] = [];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  addMemberForm: FormGroup<{
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
    searchTerm: FormControl<string>;
  }>;
  disabledAPIRoles = signal(new Set<string>());
  disableSubmit = signal(false);

  private group: Group;
  private members: Member[] = [];
  private memberships: GroupMembership[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: AddOrInviteMembersDialogData,
    private permissionService: GioPermissionService,
    private usersService: UsersService,
    private settingsService: EnvironmentSettingsService,
    private matDialogRef: MatDialogRef<AddMembersDialogComponent>,
  ) {}

  ngOnInit(): void {
    this.initializeDialogDataFromInput();
    this.initializeForm();
    this.initializeFilterFn();
    this.disableControlsForUser();
  }

  private initializeDialogDataFromInput() {
    this.group = this.data.group;
    this.members = this.data.members;
    this.defaultAPIRoles = this.data.defaultAPIRoles;
    this.defaultApplicationRoles = this.data.defaultApplicationRoles;
    this.defaultIntegrationRoles = this.data.defaultIntegrationRoles;
  }

  private initializeForm() {
    this.addMemberForm = new FormGroup({
      defaultAPIRole: new FormControl({ value: 'USER', disabled: false }),
      defaultApplicationRole: new FormControl<string>({ value: 'USER', disabled: false }),
      defaultIntegrationRole: new FormControl<string>({ value: 'USER', disabled: false }),
      searchTerm: new FormControl<string>({ value: '', disabled: false }),
    });
    this.disableSearch();
    this.disableSubmit.set(true);
  }

  private disableSearch() {
    if (this.group.max_invitation <= this.members.length + this.selectedUsers.length) {
      this.addMemberForm.controls.searchTerm.disable();
    }
  }

  private initializeFilterFn() {
    this.filteredUsers$ = this.addMemberForm.controls.searchTerm.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((searchTerm: string) => this.searchUsers(searchTerm)),
    );
  }

  private disableControlsForUser() {
    this.disableDefaultAPIRole();
    this.disableDefaultApplicationRole();
    this.disableDefaultIntegrationRole();
    this.disableAPIRoleOptions();
  }

  private disableDefaultAPIRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_api_role) {
      this.addMemberForm.controls.defaultAPIRole.disable();
    }
  }

  private disableDefaultApplicationRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_application_role) {
      this.addMemberForm.controls.defaultApplicationRole.disable();
    }
  }

  private disableDefaultIntegrationRole(): void {
    if (!this.canUpdateGroup()) {
      this.addMemberForm.controls.defaultIntegrationRole.disable();
    }
  }

  private disableAPIRoleOptions() {
    this.disabledAPIRoles.set(
      new Set(
        this.defaultAPIRoles.filter((role) => this.isPrimaryOwnerDisabled(role) || this.isSystemRoleDisabled(role)).map((role) => role.id),
      ),
    );
  }

  private canUpdateGroup() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  private isPrimaryOwnerDisabled(role: Role): boolean {
    return (this.checkPrimaryOwnerMode() || this.isPrimaryOwnerPresent()) && role.name === RoleName.PRIMARY_OWNER;
  }

  private checkPrimaryOwnerMode() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  private isPrimaryOwnerPresent() {
    return this.members.some((member) => member.roles['API'] === RoleName.PRIMARY_OWNER);
  }

  private isSystemRoleDisabled(role: Role): boolean {
    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  submit() {
    this.matDialogRef.close({ memberships: this.memberships });
  }

  mapGroupMembership(user: SearchableUser): GroupMembership {
    return {
      id: user.id,
      reference: user.reference,
      roles: [
        {
          name: this.addMemberForm.controls.defaultAPIRole.value,
          scope: 'API',
        },
        {
          name: this.addMemberForm.controls.defaultApplicationRole.value,
          scope: 'APPLICATION',
        },
        {
          name: this.addMemberForm.controls.defaultIntegrationRole.value,
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
    this.changeFormState();
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
    this.changeFormState();
  }

  private changeFormState() {
    const noOfPrimaryOwners = this.filterPrimaryOwners().length;
    this.addMemberForm.controls.searchTerm.setValue('');
    this.disableSearch();

    if (this.addMemberForm.controls.defaultAPIRole.value === RoleName.PRIMARY_OWNER) {
      if (noOfPrimaryOwners > 0) {
        this.addMemberForm.controls.searchTerm.disable();
        this.addMemberForm.controls.searchTerm.removeValidators(Validators.required);
      } else if (noOfPrimaryOwners < 1) {
        this.addMemberForm.controls.searchTerm.enable();
        this.addMemberForm.controls.searchTerm.addValidators(Validators.required);
      }
    }
    this.disableSubmit.set(this.memberships.length === 0);
  }

  private filterPrimaryOwners() {
    return this.memberships.filter((membership) => membership.roles.find((role) => role.scope === 'API').name === RoleName.PRIMARY_OWNER);
  }

  onAPIRoleChange() {
    this.selectedUsers = [];
    this.memberships = [];
    this.changeFormState();
  }
}
