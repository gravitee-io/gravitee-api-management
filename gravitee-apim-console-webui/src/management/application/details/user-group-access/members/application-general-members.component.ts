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
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEmpty, uniqueId } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { UsersService } from '../../../../../services-ngx/users.service';
import { RoleService } from '../../../../../services-ngx/role.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Role } from '../../../../../entities/management-api-v2';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { GroupData } from '../../../../api/user-group-access/members/api-general-members.component';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { Member } from '../../../../../entities/members/members';
import { Application } from '../../../../../entities/application/Application';
import { ApplicationMembersService } from '../../../../../services-ngx/application-members.service';

class MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
  notSaved?: boolean;
}

@Component({
  selector: 'application-general-members',
  templateUrl: './application-general-members.component.html',
  styleUrls: ['./application-general-members.component.scss'],
})
export class ApplicationGeneralMembersComponent {
  form: UntypedFormGroup;
  members: Member[];
  application: Application;
  membersTable: MemberDataSource[];
  roles: string[];
  groupData: GroupData[];
  membersToAdd: (Member & { _viewId: string })[] = [];
  defaultRole?: Role;
  isReadOnly = false;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public isLoadingData = true;
  displayedColumns = ['picture', 'displayName', 'role'];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly applicationMembersService: ApplicationMembersService,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly applicationService: ApplicationService,
  ) {}

  ngOnInit(): void {
    this.isLoadingData = true;

    if (this.permissionService.hasAnyMatching(['application-member-d']) && !this.displayedColumns.includes('delete')) {
      this.displayedColumns.push('delete');
    }

    combineLatest([
      this.applicationService.getById(this.activatedRoute.snapshot.params.applicationId),
      this.applicationMembersService.get(this.activatedRoute.snapshot.params.applicationId),
      this.groupService.list(1, 9999),
      this.roleService.list('APPLICATION'),
    ])
      .pipe(
        tap(([application, members, groups, roles]) => {
          this.isReadOnly = application.origin === 'KUBERNETES';
          this.members = members;
          this.application = application;
          this.roles = roles.map((r) => r.name) ?? [];
          this.defaultRole = roles.find((role) => role.default);
          this.membersTable = this.members.map((member) => {
            return {
              id: member.id,
              role: member.role,
              displayName: member.displayName,
              picture: this.userService.getUserAvatar(member.id),
              notSaved: false,
            };
          });
          this.groupData = application.groups?.map((id) => ({
            id,
            name: groups?.data.find((g) => g.id === id)?.name,
            isVisible: true,
          }));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.form = new UntypedFormGroup({
          isNotificationsEnabled: new UntypedFormControl(!this.application.disable_membership_notifications),
          members: this.formBuilder.group(
            this.members.reduce((formGroup, member) => {
              return {
                ...formGroup,
                [member.id]: new UntypedFormControl({
                  value: member.role,
                  disabled: member.role === 'PRIMARY_OWNER',
                }),
              };
            }, {}),
          ),
        });
        this.isLoadingData = false;

        if (this.isReadOnly) {
          this.form.disable({ emitEvent: false });
        }
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
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
    if (member.notSaved) {
      this.membersTable = this.membersTable.filter((member) => !member.notSaved);
      (this.form.get('members') as UntypedFormGroup).get(member.id).reset();
      (this.form.get('members') as UntypedFormGroup).removeControl(member.id);
    } else {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
          width: '500px',
          data: {
            title: `Remove Application member`,
            content: `Are you sure you want to remove "<b>${member.displayName}</b>" from this Application members? <br>This action cannot be undone!`,
            confirmButton: 'Remove',
          },
          role: 'alertdialog',
          id: 'confirmMemberDeleteDialog',
        })
        .afterClosed()
        .pipe(
          filter((confirm) => confirm === true),
          switchMap(() => this.applicationMembersService.delete(this.activatedRoute.snapshot.params.applicationId, member.id)),
          tap(() => {
            this.snackBarService.success(`"${member.displayName}" has been deleted`);
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe(() => this.ngOnInit());
    }
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
      displayName: member.displayName,
      role: this.defaultRole.name,
    });

    this.membersTable = [
      ...this.membersTable,
      {
        id: member._viewId,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
        role: this.defaultRole.name,
        notSaved: true,
      },
    ];

    const roleFormControl = new UntypedFormControl(
      {
        value: this.defaultRole?.name,
        disabled: this.isReadOnly,
      },
      [Validators.required],
    );
    roleFormControl.markAsDirty();
    roleFormControl.markAsTouched();

    const membersForm = this.form.get('members') as UntypedFormGroup;
    membersForm.addControl(member._viewId, roleFormControl);
    membersForm.markAsDirty();
  }

  public onApplicationNotificationChange() {
    const updatedApplication = {
      ...this.application,
      disable_membership_notifications: !this.form.value.isNotificationsEnabled,
    };
    return this.applicationService.update(updatedApplication);
  }

  private onApplicationMembersChange(memberFormId: string, role: string) {
    const memberToUpdate = this.members.find((member) => member.id === memberFormId);
    if (memberToUpdate) {
      return this.applicationMembersService.update(this.activatedRoute.snapshot.params.applicationId, {
        id: memberToUpdate.id,
        role: role,
      });
    } else {
      const memberToAdd = this.membersToAdd.find((member) => member._viewId === memberFormId);
      return this.applicationMembersService.update(this.activatedRoute.snapshot.params.applicationId, {
        id: memberToAdd.id,
        role: role,
      });
    }
  }

  public onSubmit() {
    const queries = [];
    if (this.form.controls['isNotificationsEnabled'].dirty) {
      queries.push(this.onApplicationNotificationChange());
    }
    if (this.form.controls['members'].dirty) {
      queries.push(
        ...Object.entries((this.form.controls['members'] as UntypedFormGroup).controls)
          .filter(([_, formControl]) => formControl.dirty)
          .map(([memberFormId, roleFormControl]) => {
            return this.onApplicationMembersChange(memberFormId, roleFormControl.value);
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  public onReset() {
    this.form = undefined;
    this.ngOnInit();
  }
}
