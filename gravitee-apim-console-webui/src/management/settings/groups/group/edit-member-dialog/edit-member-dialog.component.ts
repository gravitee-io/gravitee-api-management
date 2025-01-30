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
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatInput } from '@angular/material/input';
import { MatList, MatListItem } from '@angular/material/list';
import { Observable, startWith } from 'rxjs';

import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { RoleName } from '../membershipState';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { Role } from '../../../../../entities/role/role';
import { GroupMembership } from '../../../../../entities/group/groupMember';

import { GroupService } from '../../../../../services-ngx/group.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { UsersService } from '../../../../../services-ngx/users.service';


import { Group, Member } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'edit-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatAutocomplete,
    MatAutocompleteTrigger,
    MatInput,
    MatList,
    MatListItem,
  ],
  templateUrl: './edit-member-dialog.component.html',
  styleUrl: './edit-member-dialog.component.scss',
})
export class EditMemberDialogComponent implements OnInit {
  group: Group;
  member: Member;
  members: Member[];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  editMemberForm: FormGroup<{
    displayName: FormControl<string>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
    searchTerm: FormControl<string>;
  }>;

  private user: SearchableUser;
  private initialValues: any;
  private updatedMemberships: GroupMembership[] = [];
  ownershipTransferMessage: string;
  filteredMembers: Observable<Member[]>;
  selectedPrimaryOwner: Member;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private groupService: GroupService,
    private usersService: UsersService,
    private matDialogRef: MatDialogRef<EditMemberDialogComponent>,
    private snackBarService: SnackBarService,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit(): void {
    this.group = this.data['group'];
    this.member = this.data['member'];
    this.members = this.data['members'];
    this.usersService.search(this.member.id).subscribe({
      next: (response) => {
        this.user = response.length > 0 ? response.find((u) => u.id === this.member.id) : undefined;
      },
      error: () => {
        this.snackBarService.error(`Error occurred while searching details of the user ${this.member.displayName}`);
      },
    });
    this.defaultAPIRoles = this.data['defaultAPIRoles'];
    this.defaultApplicationRoles = this.data['defaultApplicationRoles'];
    this.defaultIntegrationRoles = this.data['defaultIntegrationRoles'];
    this.editMemberForm = new FormGroup({
      displayName: new FormControl<string>(this.member.displayName),
      defaultAPIRole: new FormControl<string>(this.member.roles['API']),
      defaultApplicationRole: new FormControl<string>(this.member.roles['APPLICATION']),
      defaultIntegrationRole: new FormControl<string>(this.member.roles['INTEGRATION']),
      searchTerm: new FormControl<string>(''),
    });
    this.initialValues = this.editMemberForm.getRawValue();
    this.filteredMembers = this.editMemberForm.controls['searchTerm'].valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map((searchTerm: string) => this.filterMembers(searchTerm)),
    );
  }

  canChangeDefaultAPIRole() {
    return this.isSuperAdmin() || !this.group.lockApiRole;
  }

  isSuperAdmin() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  canChangeDefaultApplicationRole() {
    return this.isSuperAdmin() || !this.group.lockApplicationRole;
  }

  canChangeDefaultIntegrationRole(): boolean {
    return this.isSuperAdmin();
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

  isSelectedAPIRolePrimaryOwner() {
    return this.editMemberForm.controls['defaultAPIRole'].value === RoleName.PRIMARY_OWNER;
  }

  isUpgradeRole() {
    return this.isSelectedAPIRolePrimaryOwner() && this.initialValues['defaultAPIRole'] !== RoleName.PRIMARY_OWNER;
  }

  isDowngradeRole() {
    return !this.isSelectedAPIRolePrimaryOwner() && this.initialValues['defaultAPIRole'] === RoleName.PRIMARY_OWNER;
  }

  save() {
    this.groupService.addOrUpdateMemberships(this.group.id, this.updatedMemberships).subscribe({
      next: () => {
        this.snackBarService.success(`Successfully edited member ${this.user.displayName} in the group ${this.group.name}`);
      },
      error: () => {
        this.snackBarService.error(`Error occurred while updating members of the group ${this.group.name}`);
      },
    });

    this.matDialogRef.close();
  }

  updateMemberships() {
    if (this.isUpgradeRole()) {
      const currentPrimaryOwner = this.members.find((member) => member.roles['API'] === RoleName.PRIMARY_OWNER);

      if (currentPrimaryOwner) {
        this.usersService.search(currentPrimaryOwner.displayName).subscribe({
          next: (response) => {
            const reference = response.find((u) => u.id === currentPrimaryOwner.id).reference;
            const ownerMembership = this.mapGroupMembership(currentPrimaryOwner.id, reference, RoleName.OWNER);
            const primaryOwnership = this.mapGroupMembership(this.user.id, this.user.reference, RoleName.PRIMARY_OWNER);
            this.updatedMemberships = [ownerMembership, primaryOwnership];
            this.ownershipTransferMessage = `${currentPrimaryOwner.displayName} is the API primary owner. The primary ownership will be transferred to ${this.member.displayName} and ${currentPrimaryOwner.displayName} will be updated as owner.`;
          },
        });
      }
    } else if (this.isDowngradeRole()) {
      if (this.selectedPrimaryOwner) {
        this.usersService.search(this.selectedPrimaryOwner.displayName).subscribe({
          next: (response) => {
            const reference = response.find((user) => user.id === this.selectedPrimaryOwner.id).reference;
            const groupMembership = this.mapGroupMembership(
              this.user.id,
              this.user.reference,
              this.editMemberForm.controls['defaultAPIRole'].value,
            );
            const primaryOwnership = this.mapGroupMembership(this.selectedPrimaryOwner.id, reference, RoleName.PRIMARY_OWNER);
            this.updatedMemberships = [groupMembership, primaryOwnership];
            this.ownershipTransferMessage = `${this.member.displayName} is the API primary owner. The primary ownership will be transferred to ${this.selectedPrimaryOwner.displayName} and ${this.member.displayName} will be updated as owner.`;
          },
          error: () => {
            this.snackBarService.error(`Error occurred while fetching user details.`);
          },
        });
      }
    } else {
      const ownerMembership = this.mapGroupMembership(
        this.user.id,
        this.user.reference,
        this.editMemberForm.controls['defaultAPIRole'].value,
      );
      this.updatedMemberships = [ownerMembership];
    }
  }

  mapGroupMembership(id: string, reference: string, defaultAPIRole: string): GroupMembership {
    return {
      id: id,
      reference: reference,
      roles: [
        {
          name: defaultAPIRole,
          scope: 'API',
        },
        {
          name: this.editMemberForm.controls['defaultApplicationRole'].value,
          scope: 'APPLICATION',
        },
        {
          name: this.editMemberForm.controls['defaultIntegrationRole'].value,
          scope: 'INTEGRATION',
        },
      ],
    };
  }

  selectPrimaryOwner($event: MatAutocompleteSelectedEvent) {
    this.selectedPrimaryOwner = $event.option.value;
    this.editMemberForm.controls['searchTerm'].setValue('');
    this.updateMemberships();
  }

  deselectPrimaryOwner() {
    this.selectedPrimaryOwner = null;
  }

  private filterMembers(searchTerm: string) {
    if (!searchTerm.trim()) {
      return [];
    }

    const filterValue = searchTerm.toLowerCase();
    return this.members.filter((member) => member.displayName.toLowerCase().includes(filterValue) && member.id !== this.member.id);
  }

  disableSubmit() {
    return this.isDowngradeRole() && !this.selectedPrimaryOwner;
  }
}
