import { Component, DestroyRef, inject, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { GroupService } from '../../../../../services-ngx/group.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Group } from '../../../../../entities/group/group';
import { Role } from '../../../../../entities/role/role';
import { CommonModule } from '@angular/common';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../../../entities/user/searchableUser';

import { filter, map } from 'rxjs/operators';
import { isEmpty } from 'lodash';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Member } from '../../../../../entities/management-api-v2';
import { switchMap } from 'rxjs';
import { GroupMembership } from '../../../../../entities/group/groupMember';
import { RoleName } from '../membershipState';

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
  ],
  templateUrl: './edit-members-dialog.component.html',
  styleUrl: './edit-members-dialog.component.scss',
})
export class EditMembersDialogComponent implements OnInit {
  group: Group;
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  addMemberForm: FormGroup<{
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
  }>;
  memberships: GroupMembership[] = [];
  members: Member[] = [];

  private destroyRef = inject(DestroyRef);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
    private matDialogRef: MatDialogRef<EditMembersDialogComponent>,
    private matDialog: MatDialog,
    private groupService: GroupService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.group = this.data['group'];
    this.defaultAPIRoles = this.data['defaultAPIRoles'];
    this.defaultApplicationRoles = this.data['defaultApplicationRoles'];
    this.defaultIntegrationRoles = this.data['defaultIntegrationRoles'];
    this.addMemberForm = new FormGroup({
      defaultAPIRole: new FormControl<string>(undefined),
      defaultApplicationRole: new FormControl<string>(undefined),
      defaultIntegrationRole: new FormControl<string>(undefined),
    });
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

  addMembers() {
    this.groupService.addOrUpdateMemberships(this.group.id, this.memberships).subscribe({
      next: () => {
        this.snackBarService.success(`Successfully added selected members to group ${this.group.name}`);
      },
      error: () => {
        this.snackBarService.success(`Error while adding members to group ${this.group.name}`);
      },
    });

    this.matDialogRef.close();
  }

  selectMembers() {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        data: {
          userFilterPredicate: (user) => !this.members.some((member) => member.id === user.id),
        },
        role: 'alertdialog',
        id: 'gioUsersSelectorDialog',
        hasBackdrop: true,
        autoFocus: true,
      })
      .afterClosed()
      .pipe(
        filter((members) => !isEmpty(members)),
        map((members) =>
          members.map((user): GroupMembership => {
            return this.mapGroupMembership(user);
          }),
        ),
        switchMap((memberships) => (this.memberships = memberships)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private mapGroupMembership(user: SearchableUser): GroupMembership {
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

  isDefaultAPIRolePrimaryOwner = (): boolean => {
    return this.getDefaultAPIRole() === RoleName.PRIMARY_OWNER && this.members.length > 0;
  };

  private getDefaultAPIRole() {
    return this.defaultAPIRoles.find((role) => role.default).name;
  }

  isAtLeastOneRoleSelected() {
    const controls = this.addMemberForm.controls;

    return !!controls['defaultAPIRole'].value || !!controls['defaultIntegrationRole'].value || !!controls['defaultApplicationRole'].value;
  }

  isFormInvalid() {
    return this.isDefaultAPIRolePrimaryOwner() || !this.isAtLeastOneRoleSelected() || this.memberships.length === 0;
  }
}
