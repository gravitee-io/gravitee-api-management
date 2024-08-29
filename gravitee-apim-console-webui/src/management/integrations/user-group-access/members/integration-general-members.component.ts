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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, EMPTY, forkJoin, Observable, of } from 'rxjs';
import { filter, switchMap, tap } from 'rxjs/operators';
import { isEmpty, uniqueId } from 'lodash';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../services-ngx/users.service';
import { RoleService } from '../../../../services-ngx/role.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { Group, Member } from '../../../../entities/management-api-v2';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';
import { Role } from '../../../../entities/role/role';
import { IntegrationsService } from '../../../../services-ngx/integrations.service';
import { Integration } from '../../integrations.model';
import { IntegrationMemberService } from '../../../../services-ngx/integration-member.service';
import {
  IntegrationGeneralGroupsComponent,
  IntegrationGroupsDialogData,
  IntegrationGroupsDialogResult,
} from '../groups/integration-general-groups.component';

interface MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}
export interface GroupData {
  id: string;
  name?: string;
  isVisible?: boolean;
}
@Component({
  selector: 'integration-general-members',
  templateUrl: './integration-general-members.component.html',
  styleUrls: ['./integration-general-members.component.scss'],
})
export class IntegrationGeneralMembersComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  private integrationId: string;
  public isReadOnly = false;
  public displayedColumns = ['picture', 'displayName', 'role'];

  public form: UntypedFormGroup;

  public roles: Role[];
  public roleNames: string[];
  public defaultRole?: Role;

  public members: Member[];
  public membersToAdd: (Member & { _viewId: string; reference: string })[] = [];

  public groupData: GroupData[];
  public groups: Group[] = [];

  dataSource: MemberDataSource[];
  integration: Integration;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly integrationsService: IntegrationsService,
    private readonly integrationMemberService: IntegrationMemberService,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-integration-u']);
    this.integrationId = this.activatedRoute.snapshot.params.integrationId;

    if (this.permissionService.hasAnyMatching(['environment-integration-d']) && !this.displayedColumns.includes('delete')) {
      this.displayedColumns.push('delete');
    }

    forkJoin([
      this.integrationsService.getIntegration(this.integrationId),
      this.integrationMemberService.getMembers(this.integrationId),
      this.roleService.list('INTEGRATION'),
      this.groupService.list(1, 9999),
    ])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([integration, members, roles, groups]) => {
          this.integration = integration;

          this.groups = groups.data;

          this.members = members.data;
          this.roles = roles;
          this.defaultRole = roles.find((role) => role.default);
          this.roleNames = roles.map((r) => r.name) ?? [];

          this.groupData = integration.groups?.map((id) => ({
            id,
            name: groups.data.find((g) => g.id === id)?.name,
            isVisible: true,
          }));

          this.initDataSource();
          this.initForm();
        },
      });
  }

  public onSubmit() {
    const queries = [];

    if (this.form.controls['members'].dirty) {
      queries.push(
        ...Object.entries((this.form.controls['members'] as UntypedFormGroup).controls)
          .filter(([_, formControl]) => formControl.dirty)
          .map(([memberFormId, roleFormControl]) => {
            return this.getSaveMemberQuery$(memberFormId, roleFormControl.value);
          }),
      );
    }
    combineLatest(queries)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Changes successfully saved!');
          this.ngOnInit();
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        },
      });
  }

  public addMember() {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        width: '500px',
        data: {
          userFilterPredicate: (user) => !this.members.some((member) => member.id === user.id),
        },
        role: 'alertdialog',
        id: 'addUserDialog',
      })
      .afterClosed()
      .pipe(
        filter((selectedUsers) => !isEmpty(selectedUsers)),
        tap((selectedUsers) => {
          selectedUsers.forEach((user) => {
            this.addMemberToForm(user);
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public removeMember(member: MemberDataSource) {
    const confirm = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
      data: {
        title: `Remove Integration member`,
        content: `Are you sure you want to remove "<b>${member.displayName}</b>" from this Integration members? <br>This action cannot be undone!`,
        confirmButton: 'Remove',
      },
      role: 'alertdialog',
      id: 'confirmMemberDeleteDialog',
    });

    confirm
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((shouldDeleteMember) => {
        if (shouldDeleteMember) {
          this.deleteMember(member);
        }
      });
  }

  public updateGroups(): void {
    this.matDialog
      .open<IntegrationGeneralGroupsComponent, IntegrationGroupsDialogData, IntegrationGroupsDialogResult>(
        IntegrationGeneralGroupsComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          role: 'alertdialog',
          id: 'addGroupsDialog',
          data: {
            integration: this.integration,
            groups: this.groups,
          },
        },
      )
      .afterClosed()
      .pipe(
        switchMap((dialogResult) => {
          return combineLatest([of(dialogResult), this.integrationsService.getIntegration(this.integrationId)]);
        }),
        switchMap(([dialogResult]) => {
          return this.integrationsService.updateIntegration({ ...this.integration, groups: dialogResult?.groups }, this.integrationId);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  public onReset() {
    this.form = undefined;
    this.ngOnInit();
  }

  private initDataSource() {
    this.dataSource = this.members?.map((member) => {
      // The data structure for roles allows multiple role for one user, but at Integration level, we only manage one role per user. Throw error if data is incorrect
      if (member.roles.length !== 1) {
        throw new Error('Cannot manage more than one role at Integration level');
      }
      return {
        id: member.id,
        role: member.roles[0].name,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
      };
    });
  }

  private initForm() {
    this.form = new UntypedFormGroup({
      isNotificationsEnabled: new UntypedFormControl({
        value: false,
        disabled: true,
      }),
      members: this.formBuilder.group(
        this.members.reduce((formGroup, member) => {
          return {
            ...formGroup,
            [member.id]: new UntypedFormControl({
              value: member.roles[0].name,
              disabled: member.roles[0].name === 'PRIMARY_OWNER' || this.isReadOnly,
            }),
          };
        }, {}),
      ),
    });
  }

  private addMemberToForm(searchableUser: SearchableUser) {
    const member = {
      ...searchableUser,
      _viewId: `to-add-${uniqueId()}`,
      id: searchableUser.id,
    };
    this.membersToAdd.push({
      _viewId: member._viewId,
      id: member.id,
      reference: member.reference,
      displayName: member.displayName,
      roles: [this.defaultRole],
    });

    this.dataSource = [
      ...this.dataSource,
      {
        id: member._viewId,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
        role: this.defaultRole.name,
      },
    ];
    const roleFormControl = new UntypedFormControl({ value: this.defaultRole?.name, disabled: this.isReadOnly }, [Validators.required]);
    roleFormControl.markAsDirty();
    roleFormControl.markAsTouched();
    const membersForm = this.form.get('members') as UntypedFormGroup;
    membersForm.addControl(member._viewId, roleFormControl);
    membersForm.markAsDirty();
  }

  private deleteMember(member: Member) {
    this.integrationMemberService.deleteMember(this.integrationId, member.id).subscribe({
      next: () => {
        // remove from members
        this.members = this.members.filter((m) => m.id !== member.id);
        this.initDataSource();
        // remove from form
        // reset before removing to discard save bar if changes only on this element
        (this.form.get('members') as UntypedFormGroup).get(member.id).reset();
        (this.form.get('members') as UntypedFormGroup).removeControl(member.id);
        // remove from form initial value

        this.snackBarService.success(`Member ${member.displayName} has been removed.`);
      },
      error: (error) => {
        this.snackBarService.error(error.message);
      },
    });
  }

  private getSaveMemberQuery$(memberFormId: string, newRole: string): Observable<Member> {
    const memberToUpdate = this.members.find((m) => m.id === memberFormId);
    if (memberToUpdate) {
      return this.integrationMemberService.updateMember(this.integrationId, {
        memberId: memberToUpdate.id,
        roleName: newRole,
      });
    } else {
      const memberToAdd = this.membersToAdd.find((m) => m._viewId === memberFormId);
      return this.integrationMemberService.addMember(this.integrationId, {
        userId: memberToAdd.id,
        roleName: newRole,
        externalReference: memberToAdd.reference,
      });
    }
  }
}
