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
import { Component, DestroyRef, inject, Inject, OnInit, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatInput } from '@angular/material/input';
import { Observable, of, startWith } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { MatChip, MatChipRemove, MatChipSet } from '@angular/material/chips';
import { GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { RoleName } from '../membershipState';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { Role } from '../../../../../entities/role/role';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { UsersService } from '../../../../../services-ngx/users.service';
import { Member } from '../../../../../entities/management-api-v2';
import { EditMemberDialogData } from '../group.component';
import { Group } from '../../../../../entities/group/group';

@Component({
  selector: 'edit-member-dialog',
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
    MatChip,
    MatChipRemove,
    MatChipSet,
    GioFormSlideToggleModule,
  ],
  templateUrl: './edit-member-dialog.component.html',
  styleUrl: './edit-member-dialog.component.scss',
})
export class EditMemberDialogComponent implements OnInit {
  group: Group = null;
  member: Member = null;
  members: Member[] = [];
  defaultAPIRoles: Role[] = [];
  defaultApplicationRoles: Role[] = [];
  defaultIntegrationRoles: Role[] = [];
  editMemberForm: FormGroup<{
    displayName: FormControl<string>;
    groupAdmin: FormControl<boolean>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
    searchTerm: FormControl<string>;
  }>;
  ownershipTransferMessage: string = null;
  filteredMembers$: Observable<Member[]> = of([]);
  selectedPrimaryOwner: Member = null;
  memberships: GroupMembership[] = [];
  downgradedMember: Member = null;
  disableSubmit = signal(true);
  disabledAPIRoles = signal(new Set<string>());
  disableRemovePrimaryOwner = signal(false);

  private user: SearchableUser = null;
  private initialValues: any = null;
  private destroyRef = inject(DestroyRef);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: EditMemberDialogData,
    private usersService: UsersService,
    private matDialogRef: MatDialogRef<EditMemberDialogComponent>,
    private snackBarService: SnackBarService,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit(): void {
    this.initializeDataFromInput();
    this.getUserDetails();
    this.initializeForm();
    this.initialValues = this.editMemberForm.getRawValue();
    this.initializeFilterFn();
    this.disableControlsForUser();
  }

  private initializeDataFromInput() {
    this.group = this.data.group;
    this.member = this.data.member;
    this.members = this.data.members;
    this.defaultAPIRoles = this.data.defaultAPIRoles;
    this.defaultApplicationRoles = this.data.defaultApplicationRoles;
    this.defaultIntegrationRoles = this.data.defaultIntegrationRoles;
  }

  private initializeForm() {
    this.editMemberForm = new FormGroup({
      displayName: new FormControl<string>(this.member.displayName),
      groupAdmin: new FormControl<boolean>(this.member.roles['GROUP'] === 'ADMIN'),
      defaultAPIRole: new FormControl<string>(this.member.roles['API']),
      defaultApplicationRole: new FormControl<string>(this.member.roles['APPLICATION']),
      defaultIntegrationRole: new FormControl<string>(this.member.roles['INTEGRATION']),
      searchTerm: new FormControl<string>({ value: '', disabled: true }),
    });
  }

  private initializeFilterFn() {
    this.filteredMembers$ = this.editMemberForm.controls.searchTerm.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map((searchTerm: string) => this.filterMembers(searchTerm)),
    );
  }

  private getUserDetails() {
    this.usersService
      .search(this.member.displayName)
      .pipe(
        map((users: SearchableUser[]) => (this.user = users.length > 0 ? users.find((u) => u.id === this.member.id) : undefined)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private disableControlsForUser() {
    this.disableDefaultAPIRole();
    this.disableDefaultApplicationRole();
    this.disableDefaultIntegrationRole();
    this.disableAPIRoleOptions();
  }

  private disableDefaultAPIRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_api_role) {
      this.editMemberForm.controls.defaultAPIRole.disable();
    }
  }

  private disableDefaultApplicationRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_application_role) {
      this.editMemberForm.controls.defaultApplicationRole.disable();
    }
  }

  private disableDefaultIntegrationRole(): void {
    if (!this.canUpdateGroup()) {
      this.editMemberForm.controls.defaultIntegrationRole.disable();
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
    return this.checkPrimaryOwnerMode() && role.name === RoleName.PRIMARY_OWNER;
  }

  private checkPrimaryOwnerMode() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  private isSystemRoleDisabled(role: Role): boolean {
    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  submit() {
    if (this.isUpgradeRole() && this.ifPrimaryOwnerPresent()) {
      this.usersService
        .search(this.downgradedMember.displayName)
        .pipe(
          map((users) => {
            const ownerId = this.downgradedMember.id;
            const reference = users.find((u) => u.id === ownerId).reference;
            const ownerMembership = this.mapGroupMembership(ownerId, reference, RoleName.OWNER);
            const primaryOwnerMembership = this.mapGroupMembership(this.user.id, this.user.reference, RoleName.PRIMARY_OWNER);
            this.memberships = [ownerMembership, primaryOwnerMembership];
            this.matDialogRef.close({ memberships: this.memberships });
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else if (this.isDowngradeRole()) {
      const ownerMembership = this.mapGroupMembership(this.user.id, this.user.reference, this.editMemberForm.controls.defaultAPIRole.value);
      this.usersService
        .search(this.selectedPrimaryOwner.displayName)
        .pipe(
          map((users) => {
            const reference = users.find((user) => user.id === this.selectedPrimaryOwner.id).reference;
            const primaryOwnerMembership = this.mapGroupMembership(this.selectedPrimaryOwner.id, reference, RoleName.PRIMARY_OWNER);
            this.memberships = [ownerMembership, primaryOwnerMembership];
            this.matDialogRef.close({ memberships: this.memberships });
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else {
      const groupMembership = this.mapGroupMembership(this.user.id, this.user.reference, this.editMemberForm.controls.defaultAPIRole.value);
      this.memberships = [groupMembership];
      this.matDialogRef.close({ memberships: this.memberships });
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
          name: this.editMemberForm.controls.defaultApplicationRole.value,
          scope: 'APPLICATION',
        },
        {
          name: this.editMemberForm.controls.defaultIntegrationRole.value,
          scope: 'INTEGRATION',
        },
      ],
    };
  }

  selectPrimaryOwner($event: MatAutocompleteSelectedEvent) {
    if (this.isDowngradeRole()) {
      this.selectedPrimaryOwner = $event.option.value;
      this.ownershipTransferMessage = `${this.member.displayName} is the API primary owner. The primary ownership will be transferred to ${this.selectedPrimaryOwner.displayName} and ${this.member.displayName} will be updated as owner.`;
      this.editMemberForm.controls.searchTerm.disable();
      this.editMemberForm.controls.searchTerm.setValue('');
      this.editMemberForm.controls.searchTerm.removeValidators(Validators.required);
      this.disableSubmit.set(false);
    }
  }

  deselectPrimaryOwner() {
    if (this.isDowngradeRole()) {
      this.selectedPrimaryOwner = null;
      this.ownershipTransferMessage = null;
      this.editMemberForm.controls.searchTerm.enable();
      this.editMemberForm.controls.searchTerm.addValidators(Validators.required);
      this.disableSubmit.set(true);
    }
  }

  private filterMembers(searchTerm: string) {
    if (!searchTerm.trim()) {
      return [];
    }

    const filterValue = searchTerm.toLowerCase();
    return this.members.filter((member) => member.displayName.toLowerCase().includes(filterValue) && member.id !== this.member.id);
  }

  onRoleChange() {
    this.selectedPrimaryOwner = null;
    this.ownershipTransferMessage = null;
    this.memberships = [];

    if (this.isUpgradeRole() && this.ifPrimaryOwnerPresent()) {
      this.editMemberForm.controls.searchTerm.disable();
      this.editMemberForm.controls.searchTerm.removeValidators(Validators.required);
      this.downgradedMember = this.members.find((member) => member.roles['API'] === RoleName.PRIMARY_OWNER);
      this.selectedPrimaryOwner = this.member;
      this.ownershipTransferMessage = `${this.downgradedMember.displayName} is the API primary owner. The primary ownership will be transferred to ${this.member.displayName} and ${this.downgradedMember.displayName} will be updated as owner.`;
      this.disableRemovePrimaryOwner.set(true);
      this.disableSubmit.set(false);
    } else if (this.isDowngradeRole()) {
      this.editMemberForm.controls.searchTerm.enable();
      this.editMemberForm.controls.searchTerm.addValidators(Validators.required);
      this.downgradedMember = this.member;
    } else {
      this.disableSubmit.set(false);
    }
  }

  private ifPrimaryOwnerPresent() {
    return this.members.some((member) => member.roles['API'] === RoleName.PRIMARY_OWNER);
  }

  private isDowngradeRole() {
    return (
      this.initialValues.defaultAPIRole === RoleName.PRIMARY_OWNER &&
      this.editMemberForm.controls.defaultAPIRole.value !== RoleName.PRIMARY_OWNER
    );
  }

  private isUpgradeRole() {
    return (
      this.editMemberForm.controls.defaultAPIRole.value === RoleName.PRIMARY_OWNER &&
      this.initialValues.defaultAPIRole !== RoleName.PRIMARY_OWNER
    );
  }
}
