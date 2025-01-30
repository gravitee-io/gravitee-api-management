import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Group, Member } from '../../../../../entities/management-api-v2';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { RoleName } from '../membershipState';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { Role } from '../../../../../entities/role/role';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { CommonModule } from '@angular/common';
import { GroupService } from '../../../../../services-ngx/group.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { UsersService } from '../../../../../services-ngx/users.service';

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
  ],
  templateUrl: './edit-member-dialog.component.html',
  styleUrl: './edit-member-dialog.component.scss',
})
export class EditMemberDialogComponent implements OnInit {
  group: Group;
  member: Member;
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  editMemberForm: FormGroup<{
    displayName: FormControl<string | null>;
    defaultAPIRole: FormControl<string | null>;
    defaultApplicationRole: FormControl<string | null>;
    defaultIntegrationRole: FormControl<string | null>;
  }>;
  memberships: GroupMembership;
  private user: SearchableUser;

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
    this.usersService.search(this.member.id).subscribe({
      next: (data) => {
        this.user = data.length > 0 ? data.find((u) => u.id === this.member.id) : undefined;
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
    });
  }

  isDefaultAPIRolePrimaryOwner = (): boolean => {
    return this.getDefaultAPIRole() === RoleName.PRIMARY_OWNER;
  };

  private getDefaultAPIRole() {
    return this.defaultAPIRoles.find((role) => role.scope === 'API').name;
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

  isAtLeastOneRoleSelected() {
    const controls = this.editMemberForm.controls;

    return !!controls['defaultAPIRole'].value || !!controls['defaultIntegrationRole'].value || !!controls['defaultApplicationRole'].value;
  }

  isFormInvalid() {
    return this.isDefaultAPIRolePrimaryOwner() || !this.isAtLeastOneRoleSelected();
  }

  private mapGroupMembership(user: SearchableUser): void {
    this.memberships = {
      id: user.id,
      reference: user.reference,
      roles: [
        {
          name: this.editMemberForm.controls['defaultAPIRole'].value,
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

  save() {
    this.mapGroupMembership(this.user);
    const updatedMemberships = [];
    updatedMemberships.push(this.memberships);
    this.groupService.addOrUpdateMemberships(this.group.id, updatedMemberships).subscribe({
      next: () => {
        this.snackBarService.success(`Successfully edited member ${this.user.displayName} in the group ${this.group.name}`);
      },
      error: () => {
        this.snackBarService.error(`Error occurred while adding members to group ${this.group.name}`);
      },
    });

    this.matDialogRef.close();
  }
}
