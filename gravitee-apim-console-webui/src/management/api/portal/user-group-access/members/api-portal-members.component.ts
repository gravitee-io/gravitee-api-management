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
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { Api, ApiMember } from '../../../../../entities/api';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiMemberService } from '../../../../../services-ngx/api-member.service';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { RoleService } from '../../../../../services-ngx/role.service';
import { Role } from '../../../../../entities/role/role';

class MembersDataSource extends ApiMember {
  picture: string;
  name: string;
}
@Component({
  selector: 'api-portal-members',
  template: require('./api-portal-members.component.html'),
  styles: [require('./api-portal-members.component.scss')],
})
export class ApiPortalMembersComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  form: FormGroup;
  dataSource: MembersDataSource[];
  members: ApiMember[];
  displayedColumns = ['picture', 'displayName', 'role', 'delete'];
  roles: Role[];
  formInitialValues: { isNotificationsEnabled: boolean; members: { [memberId: string]: string } };
  private apiId: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly apiMembersService: ApiMemberService,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: FormBuilder,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
    combineLatest([this.apiService.get(this.apiId), this.apiMembersService.getMembers(this.apiId), this.roleService.list('API')])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([api, members, roles]) => {
          this.members = members;
          this.roles = roles;
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
      queries.push(this.saveChangeOnApiNotifications());
    }
    if (this.form.controls['members'].dirty) {
      queries.push(
        ...Object.entries((this.form.controls['members'] as FormGroup).controls)
          .filter(([_, formControl]) => formControl.dirty)
          .map(([memberId, roleFormControl]) => {
            return this.updateMember(memberId, roleFormControl.value);
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

  public getMemberName(member: ApiMember): string {
    if (!member.displayName) {
      return member.username;
    }

    return member.username ? member.displayName + ' (' + member.username + ')' : member.displayName;
  }

  public updateMember(memberId: string, newRole: string): Observable<void> {
    const memberToUpdate = this.members.find((m) => m.id === memberId);
    return this.apiMembersService.addOrUpdateMember(this.apiId, {
      id: memberToUpdate.id,
      role: newRole,
      reference: memberToUpdate.reference,
    });
  }

  public removeMember(member: ApiMember) {
    const confirm = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
      data: {
        title: `Remove member ${member.displayName}?`,
        content: `This will remove the member indefinitely. You cannot undo this action.`,
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

  public saveChangeOnApiNotifications(): Observable<Api> {
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

  private initDataSource(members: ApiMember[]) {
    this.dataSource = members.map((member) => {
      return {
        ...member,
        name: this.getMemberName(member),
        picture: this.userService.getUserAvatar(member.id),
      };
    });
  }

  private initForm(api: Api, members: ApiMember[]) {
    this.form = new FormGroup({
      isNotificationsEnabled: new FormControl(!api.disable_membership_notifications),
      members: this.formBuilder.group(
        members.reduce((formGroup, member) => {
          return {
            ...formGroup,
            [member.id]: this.formBuilder.control({ value: member.role, disabled: member.role === 'PRIMARY_OWNER' }),
          };
        }, {}),
      ),
    });
    this.formInitialValues = this.form.getRawValue();
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
        delete this.formInitialValues.members[member.id];

        this.snackBarService.success(`Member ${member.displayName} has been removed.`);
      },
      error: (error) => {
        this.snackBarService.error(error.message);
      },
    });
  }
}
