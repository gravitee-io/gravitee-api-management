import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { MatSelectModule } from '@angular/material/select';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Group } from '../../../../../entities/group/group';
import { Role } from '../../../../../entities/role/role';
import { MatCardModule } from '@angular/material/card';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { GroupService } from '../../../../../services-ngx/group.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Invitation } from '../../../../../entities/invitation/invitation';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { RoleName } from '../membershipState';

@Component({
  selector: 'invite-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    GioPermissionModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
  ],
  templateUrl: './invite-member-dialog.component.html',
  styleUrl: './invite-member-dialog.component.scss',
})
export class InviteMemberDialogComponent implements OnInit {
  group: Group;
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  inviteMemberForm: FormGroup<{
    email: FormControl<string>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
  }>;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
    private matDialogRef: MatDialogRef<InviteMemberDialogComponent>,
    private groupService: GroupService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.group = this.data['group'];
    this.defaultAPIRoles = this.data['defaultAPIRoles'];
    this.defaultApplicationRoles = this.data['defaultApplicationRoles'];
    this.inviteMemberForm = new FormGroup({
      defaultAPIRole: new FormControl<string>(undefined),
      defaultApplicationRole: new FormControl<string>(undefined),
      defaultIntegrationRole: new FormControl<string>(undefined),
      email: new FormControl<string>(undefined, Validators.required),
    });
  }

  canChangeDefaultAPIRole() {
    return this.isSuperAdmin() || !this.group.lock_api_role;
  }

  canChangeDefaultApplicationRole() {
    return this.isSuperAdmin() || !this.group.lock_application_role;
  }

  isSuperAdmin() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  sendInvitation() {
    const addMemberFormControls = this.inviteMemberForm.controls;
    const invitation: Invitation = {
      reference_type: 'GROUP',
      reference_id: this.group.id,
      email: addMemberFormControls['email'].value,
      api_role: addMemberFormControls['defaultAPIRole'].value,
      application_role: addMemberFormControls['defaultApplicationRole'].value,
    };

    this.groupService.inviteMember(this.group.id, invitation).subscribe({
      next: () => {
        this.snackBarService.success(`Invitation sent to ${addMemberFormControls['email'].value}`);
      },
      error: () => {
        this.snackBarService.error(`Error while inviting member ${addMemberFormControls['email'].value} to the group ${this.group.name}`);
      },
    });

    this.matDialogRef.close();
  }

  isAPIRoleDisabled(role: Role) {
    if (this.checkPrimaryOwner()) {
      return role.name === RoleName.PRIMARY_OWNER;
    }

    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  private checkPrimaryOwner() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  isEmailValid() {
    const controls = this.inviteMemberForm.controls;
    return (
      !!controls['email'].value &&
      controls['email'].value
        .toLowerCase()
        .match(
          /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
        )
    );
  }
}
