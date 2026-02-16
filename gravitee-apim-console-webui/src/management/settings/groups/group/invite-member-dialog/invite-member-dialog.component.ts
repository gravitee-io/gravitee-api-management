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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { Role } from '../../../../../entities/role/role';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { Invitation } from '../../../../../entities/invitation/invitation';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { RoleName, Member } from '../membershipState';
import { AddOrInviteMembersDialogData } from '../group.component';
import { Group } from '../../../../../entities/group/group';

@Component({
  selector: 'invite-member-dialog',
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
  defaultAPIRoles: Role[] = [];
  defaultApplicationRoles: Role[] = [];
  invitationForm: FormGroup<{
    email: FormControl<string>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
  }>;

  private group: Group = null;
  private members: Member[] = [];
  disabledAPIRoles = signal(new Set<string>());

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: AddOrInviteMembersDialogData,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
    private matDialogRef: MatDialogRef<InviteMemberDialogComponent>,
  ) {}

  ngOnInit() {
    this.initializeDataFromInput();
    this.initializeForm();
    this.disableControlsForUser();
  }

  private initializeForm() {
    this.invitationForm = new FormGroup({
      defaultAPIRole: new FormControl({ value: this.data.group.roles['API'] ?? 'USER', disabled: false }),
      defaultApplicationRole: new FormControl<string>({
        value: this.data.group.roles['APPLICATION'] ?? 'USER',
        disabled: false,
      }),
      email: new FormControl<string>(null, [Validators.required, Validators.email]),
    });
  }

  private initializeDataFromInput() {
    this.group = this.data.group;
    this.members = this.data.members;
    this.defaultAPIRoles = this.data.defaultAPIRoles;
    this.defaultApplicationRoles = this.data.defaultApplicationRoles;
  }

  private disableControlsForUser() {
    this.disableDefaultAPIRole();
    this.disableDefaultApplicationRole();
    this.disableAPIRoleOptions();
  }

  private disableDefaultAPIRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_api_role) {
      this.invitationForm.controls.defaultAPIRole.disable();
    }
  }

  private disableDefaultApplicationRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_application_role) {
      this.invitationForm.controls.defaultApplicationRole.disable();
    }
  }

  private disableAPIRoleOptions() {
    this.disabledAPIRoles.set(
      new Set(
        this.defaultAPIRoles.filter(role => this.isPrimaryOwnerDisabled(role) || this.isSystemRoleDisabled(role)).map(role => role.id),
      ),
    );
  }

  private isPrimaryOwnerDisabled(role: Role): boolean {
    return (this.checkPrimaryOwnerMode() || this.isPrimaryOwnerPresent()) && role.name === RoleName.PRIMARY_OWNER;
  }

  private isPrimaryOwnerPresent() {
    return this.members.some(member => member.roles['API'] === RoleName.PRIMARY_OWNER);
  }

  private isSystemRoleDisabled(role: Role): boolean {
    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  private checkPrimaryOwnerMode() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  private canUpdateGroup() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  submit() {
    const invitationFormControls = this.invitationForm.controls;
    const invitation: Invitation = {
      reference_type: 'GROUP',
      reference_id: this.group.id,
      email: invitationFormControls['email'].value,
      api_role: invitationFormControls['defaultAPIRole'].value,
      application_role: invitationFormControls['defaultApplicationRole'].value,
    };

    this.matDialogRef.close({ invitation: invitation });
  }
}
