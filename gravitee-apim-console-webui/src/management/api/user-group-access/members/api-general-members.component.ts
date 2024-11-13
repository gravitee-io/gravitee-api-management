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
import { Component, OnInit } from '@angular/core';
import { combineLatest, EMPTY, forkJoin, Observable, of, Subject, throwError } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEmpty, uniqueId } from 'lodash';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../services-ngx/users.service';
import { RoleService } from '../../../../services-ngx/role.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiMemberV2Service } from '../../../../services-ngx/api-member-v2.service';
import { Api, ApiV4, Group, Member } from '../../../../entities/management-api-v2';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';
import { ApiGeneralGroupsComponent, ApiGroupsDialogData, ApiGroupsDialogResult } from '../groups/api-general-groups.component';
import {
  ApiGeneralTransferOwnershipComponent,
  ApiOwnershipDialogData,
  ApiOwnershipDialogResult,
} from '../transfer-ownership/api-general-transfer-ownership.component';
import { Role } from '../../../../entities/role/role';
import { GioRoleService } from '../../../../shared/components/gio-role/gio-role.service';

class MemberDataSource {
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
  selector: 'api-general-members',
  templateUrl: './api-general-members.component.html',
  styleUrls: ['./api-general-members.component.scss'],
})
export class ApiGeneralMembersComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private apiId: string;
  public isKubernetesOrigin: boolean;

  form: UntypedFormGroup;
  roles: Role[];
  roleNames: string[];
  defaultRole?: Role;
  members: Member[];
  membersToAdd: (Member & { _viewId: string; reference: string })[] = [];
  groupData: GroupData[];
  isReadOnly = false;
  dataSource: MemberDataSource[];
  displayedColumns = ['picture', 'displayName', 'role'];
  api: Api;
  groups: Group[];
  canTransferOwnership: boolean;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiMemberService: ApiMemberV2Service,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly matDialog: MatDialog,
    private readonly gioRoleService: GioRoleService,
  ) {}

  ngOnInit(): void {
    this.apiId = this.activatedRoute.snapshot.params.apiId;

    // Display the trash icon if the user is allowed to delete a member
    if (this.permissionService.hasAnyMatching(['api-member-d']) && !this.displayedColumns.includes('delete')) {
      this.displayedColumns.push('delete');
    }
    // Get group list, map id + name

    forkJoin([
      this.apiService.get(this.apiId),
      this.apiMemberService.getMembers(this.apiId),
      this.roleService.list('API'),
      this.groupService.list(1, 9999),
    ])
      .pipe(
        tap(([api, members, roles, groups]) => {
          this.api = api;
          this.groups = groups.data;
          this.members = members.data ?? [];
          this.roles = roles ?? [];
          this.defaultRole = roles.find((role) => role.default);
          this.roleNames = roles.map((r) => r.name) ?? [];
          this.groupData = api.groups?.map((id) => ({
            id,
            name: groups.data.find((g) => g.id === id)?.name,
            isVisible: true,
          }));
          this.initDataSource();
          this.initForm(api);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.canTransferOwnership = this.gioRoleService.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u']);
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public onSubmit() {
    const queries = [];
    if (this.form.controls['isNotificationsEnabled'].dirty) {
      queries.push(this.getSaveChangeOnApiNotificationsQuery$());
    }
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
      .pipe(
        tap(() => {
          this.snackBarService.success('Changes successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public removeMember(member: MemberDataSource) {
    const confirm = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
      data: {
        title: `Remove API member`,
        content: `Are you sure you want to remove "<b>${member.displayName}</b>" from this API members? <br>This action cannot be undone!`,
        confirmButton: 'Remove',
      },
      role: 'alertdialog',
      id: 'confirmMemberDeleteDialog',
    });

    confirm
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((shouldDeleteMember) => {
        if (shouldDeleteMember) {
          this.deleteMember(member);
        }
      });
  }

  public updateGroups(): void {
    this.matDialog
      .open<ApiGeneralGroupsComponent, ApiGroupsDialogData, ApiGroupsDialogResult>(ApiGeneralGroupsComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        role: 'alertdialog',
        id: 'addGroupsDialog',
        data: {
          api: this.api,
          groups: this.groups,
          isKubernetesOrigin: this.isKubernetesOrigin,
        },
      })
      .afterClosed()
      .pipe(
        filter(() => !this.isKubernetesOrigin),
        switchMap((apiDialogResult) => {
          return combineLatest([of(apiDialogResult), this.apiService.get(this.activatedRoute.snapshot.params.apiId)]);
        }),
        switchMap(([apiDialogResult, api]) => {
          return api.definitionVersion === 'V1'
            ? throwError({ message: `You cannot modify a ${api.definitionVersion} API.` })
            : this.apiService.update(api.id, { ...api, groups: apiDialogResult?.groups });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  public transferOwnership(): void {
    this.matDialog
      .open<ApiGeneralTransferOwnershipComponent, ApiOwnershipDialogData, ApiOwnershipDialogResult>(ApiGeneralTransferOwnershipComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        role: 'alertdialog',
        id: 'transferOwnershipDialog',
        data: {
          api: this.api,
          groups: this.groups,
          roles: this.roles,
          members: this.members,
        },
      })
      .afterClosed()
      .pipe(
        switchMap((apiDialogResult) => {
          return this.apiService.transferOwnership(
            this.apiId,
            apiDialogResult.isUserMode ? apiDialogResult.transferOwnershipToUser : apiDialogResult.transferOwnershipToGroup,
          );
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  public onReset() {
    this.form = undefined;
    this.ngOnInit();
  }

  private initDataSource() {
    this.dataSource = this.members?.map((member) => {
      // The data structure for roles allows multiple role for one user, but at API level, we only manage one role per user. Throw error if data is incorrect
      if (member.roles.length !== 1) {
        throw new Error('Cannot manage more than one role at API level');
      }
      return {
        id: member.id,
        role: member.roles[0].name,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
      };
    });
  }

  private initForm(api: Api) {
    this.isKubernetesOrigin = api.originContext?.origin === 'KUBERNETES';
    this.isReadOnly =
      !this.permissionService.hasAnyMatching(['api-member-u']) ||
      api.definitionVersion === 'V1' ||
      this.isKubernetesOrigin ||
      (api as ApiV4).type === 'NATIVE';
    this.form = new UntypedFormGroup({
      isNotificationsEnabled: new UntypedFormControl({
        value: !api.disableMembershipNotifications,
        disabled: this.isReadOnly,
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
    this.apiMemberService.deleteMember(this.apiId, member.id).subscribe({
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
      return this.apiMemberService.updateMember(this.apiId, {
        memberId: memberToUpdate.id,
        roleName: newRole,
      });
    } else {
      const memberToAdd = this.membersToAdd.find((m) => m._viewId === memberFormId);
      return this.apiMemberService.addMember(this.apiId, {
        userId: memberToAdd.id,
        roleName: newRole,
        externalReference: memberToAdd.reference,
      });
    }
  }

  private getSaveChangeOnApiNotificationsQuery$(): Observable<Api> {
    return this.apiService.get(this.apiId).pipe(
      switchMap((api) => {
        if (api.definitionVersion === 'V2' || api.definitionVersion === 'V4') {
          const updatedApi = {
            ...api,
            disableMembershipNotifications: !this.form.value.isNotificationsEnabled,
          };
          return this.apiService.update(api.id, updatedApi);
        } else {
          // Update V1 API is not supported
          return EMPTY;
        }
      }),
    );
  }
}
