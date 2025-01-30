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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { Group } from '../../../../../entities/group/group';
import { Role } from '../../../../../entities/role/role';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
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
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  invitationForm: FormGroup<{
    email: FormControl<string>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
  }>;

  private group: Group;

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
    this.invitationForm = new FormGroup({
      defaultAPIRole: new FormControl<string>('USER'),
      defaultApplicationRole: new FormControl<string>('USER'),
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
    const addMemberFormControls = this.invitationForm.controls;
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
    const controls = this.invitationForm.controls;
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
