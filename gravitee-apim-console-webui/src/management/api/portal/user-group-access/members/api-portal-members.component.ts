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
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEmpty, uniqueId } from 'lodash';

import { Api, ApiMember } from '../../../../../entities/api';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiMemberService } from '../../../../../services-ngx/api-member.service';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { RoleService } from '../../../../../services-ngx/role.service';
import { Role } from '../../../../../entities/role/role';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../../../entities/user/searchableUser';

class MemberDataSource {
  id: string;
  reference: string;
  role: string;
  displayName: string;
  picture: string;
}
interface GroupData {
  groupId: string;
  isVisible: boolean;
}
@Component({
  selector: 'api-portal-members',
  template: require('./api-portal-members.component.html'),
  styles: [require('./api-portal-members.component.scss')],
})
export class ApiPortalMembersComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  form: FormGroup;

  roles: Role[];
  defaultRole?: Role;
  members: ApiMember[];
  membersToAdd: (ApiMember & { _viewId: string })[] = [];
  groupData: GroupData[];
  isReadOnly = false;

  dataSource: MemberDataSource[];
  displayedColumns = ['picture', 'displayName', 'role'];

  private apiId: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly apiMembersService: ApiMemberService,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: FormBuilder,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;

    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-member-u']);

    // Display the trash icon if the user is allowed to delete a member
    if (this.permissionService.hasAnyMatching(['api-member-d']) && !this.displayedColumns.includes('delete')) {
      this.displayedColumns.push('delete');
    }

    combineLatest([this.apiService.get(this.apiId), this.apiMembersService.getMembers(this.apiId), this.roleService.list('API')])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([api, members, roles]) => {
          this.members = members;
          this.roles = roles;
          this.defaultRole = roles.find((role) => role.default);
          this.groupData = api.groups?.map((groupId) => ({ groupId, isVisible: true }));
          this.initDataSource(members);
          this.initForm(api, members);
        }),
      )
      .subscribe();
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
        ...Object.entries((this.form.controls['members'] as FormGroup).controls)
          .filter(([_, formControl]) => formControl.dirty)
          .map(([memberFormId, roleFormControl]) => {
            return this.getSaveMemberQuery$(memberFormId, roleFormControl.value);
          }),
      );
    }
    combineLatest(queries)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => {
          this.snackBarService.success('Changes successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
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
        takeUntil(this.unsubscribe$),
        filter((selectedUsers) => !isEmpty(selectedUsers)),
        tap((selectedUsers) => {
          selectedUsers.forEach((user) => {
            this.addMemberToForm(user);
          });
        }),
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

  public onReset() {
    this.form = undefined;
    this.ngOnInit();
  }

  private initDataSource(members: ApiMember[]) {
    this.dataSource = members.map((member) => {
      return {
        id: member.id,
        reference: member.reference,
        role: member.role,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
      };
    });
  }

  private initForm(api: Api, members: ApiMember[]) {
    this.form = new FormGroup({
      isNotificationsEnabled: new FormControl({
        value: !api.disable_membership_notifications,
        disabled: this.isReadOnly,
      }),
      members: this.formBuilder.group(
        members.reduce((formGroup, member) => {
          return {
            ...formGroup,
            [member.id]: this.formBuilder.control({ value: member.role, disabled: member.role === 'PRIMARY_OWNER' || this.isReadOnly }),
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
      role: this.defaultRole?.name,
    });

    this.dataSource = [
      ...this.dataSource,
      {
        id: member._viewId,
        reference: member.reference,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
        role: this.defaultRole?.name,
      },
    ];
    const roleFormControl = new FormControl({ value: this.defaultRole?.name, disabled: this.isReadOnly }, [Validators.required]);
    roleFormControl.markAsDirty();
    roleFormControl.markAsTouched();
    const membersForm = this.form.get('members') as FormGroup;
    membersForm.addControl(member._viewId, roleFormControl);
    membersForm.markAsDirty();
  }
  private deleteMember(member: ApiMember) {
    this.apiMembersService.deleteMember(this.apiId, member.id).subscribe({
      next: () => {
        // remove from members
        this.members = this.members.filter((m) => m.id !== member.id);
        this.initDataSource(this.members);
        // remove from form
        // reset before removing to discard save bar if changes only on this element
        (this.form.get('members') as FormGroup).get(member.id).reset();
        (this.form.get('members') as FormGroup).removeControl(member.id);
        // remove from form initial value

        this.snackBarService.success(`Member ${member.displayName} has been removed.`);
      },
      error: (error) => {
        this.snackBarService.error(error.message);
      },
    });
  }

  private getSaveMemberQuery$(memberFormId: string, newRole: string): Observable<void> {
    const memberToAdd = this.membersToAdd.find((m) => m._viewId === memberFormId);
    const memberToUpdate = this.members.find((m) => m.id === memberFormId);

    const memberToSave = memberToAdd || memberToUpdate;

    return this.apiMembersService.addOrUpdateMember(this.apiId, {
      id: memberToSave.id,
      role: newRole,
      reference: memberToSave.reference,
    });
  }

  private getSaveChangeOnApiNotificationsQuery$(): Observable<Api> {
    return this.apiService.get(this.apiId).pipe(
      switchMap((api) => {
        const updatedApi = {
          ...api,
          disable_membership_notifications: !this.form.value.isNotificationsEnabled,
        };
        return this.apiService.update(updatedApi);
      }),
    );
  }
}
