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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { AbstractControl, FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { isEmpty } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiMemberService } from '../../../../../services-ngx/api-member.service';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { GroupService } from '../../../../../services-ngx/group.service';
import { Group } from '../../../../../entities/group/group';
import { RoleService } from '../../../../../services-ngx/role.service';
import { Role } from '../../../../../entities/role/role';
import { Constants } from '../../../../../entities/Constants';
import { SearchableUser } from '../../../../../entities/user/searchableUser';

@Component({
  selector: 'api-portal-transfer-ownership',
  template: require('./api-portal-transfer-ownership.component.html'),
  styles: [require('./api-portal-transfer-ownership.component.scss')],
})
export class ApiPortalTransferOwnershipComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  mode: 'USER' | 'GROUP' | 'HYBRID';
  warnUseGroupAsPrimaryOwner = false;

  form: FormGroup;
  formInitialValues: unknown;

  poGroups: Group[];

  poRoles: Role[];

  private apiId: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly apiMembersService: ApiMemberService,
    private readonly userService: UsersService,
    private readonly groupService: GroupService,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;

    this.mode = this.constants.env.settings.api.primaryOwnerMode.toUpperCase() as 'USER' | 'GROUP' | 'HYBRID';

    combineLatest([this.apiService.get(this.apiId), this.groupService.list(), this.roleService.list('API')])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([api, groups, roles]) => {
          this.poGroups = groups.filter((group) => group.apiPrimaryOwner != null);
          if (api.owner.type === 'GROUP') {
            this.poGroups = this.poGroups.filter((group) => group.id !== api.owner.id);
          }

          this.warnUseGroupAsPrimaryOwner = (this.mode === 'HYBRID' || this.mode === 'GROUP') && isEmpty(this.poGroups);

          this.poRoles = roles.filter((role) => role.name !== 'PRIMARY_OWNER');
          const defaultRolePO = roles.find((role) => role.default);
          this.initForm(defaultRolePO, this.mode);
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public onSubmit() {
    const newRole = this.form.get('roleId').value;

    const confirm = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
      data: {
        title: `Transfer API ownership`,
        content: `This action cannot be undone. If you are the primary owner of this API, your role will be set to <code>${newRole}</code>.`,
        confirmButton: 'Transfer',
      },
      role: 'alertdialog',
      id: 'confirmTransferDialog',
    });

    const user: SearchableUser = this.form.get('user').value;
    const transferOwnershipToUser = {
      id: user?.id,
      reference: user?.reference,
      role: newRole,
      type: 'USER',
    };
    const transferOwnershipToGroup = {
      id: this.form.get('groupId').value,
      reference: null,
      role: newRole,
      type: 'GROUP',
    };

    const isUserMode = this.form.get('userOrGroup').value === 'user';

    confirm
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirmed) => confirmed),
        switchMap(() => this.apiService.transferOwnership(this.apiId, isUserMode ? transferOwnershipToUser : transferOwnershipToGroup)),
        tap(
          () => this.snackBarService.success('Transfer ownership done.'),
          ({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          },
        ),
        tap(() => this.ngOnInit()),
      )
      .subscribe();
  }

  private initForm(defaultRolePO: Role, mode: 'USER' | 'GROUP' | 'HYBRID') {
    this.form = new FormGroup(
      {
        userOrGroup: new FormControl(mode === 'HYBRID' ? undefined : mode === 'USER' ? 'user' : 'group'),
        user: new FormControl(),
        groupId: new FormControl(),
        roleId: new FormControl(defaultRolePO.name),
      },
      [
        (control: AbstractControl): ValidationErrors | null => {
          const errors: ValidationErrors = {};
          if (!control.get('userOrGroup').value) {
            errors.userOrGroupRequired = true;
          }

          const isUserMode = control.get('userOrGroup').value === 'user';
          const isGroupMode = control.get('userOrGroup').value === 'group';

          if (isUserMode && isEmpty(control.get('user').value)) {
            errors.userRequired = true;
          }
          if (isGroupMode && isEmpty(control.get('groupId').value)) {
            errors.groupRequired = true;
          }
          if (!control.get('roleId').value) {
            errors.roleRequired = true;
          }

          return errors ? errors : null;
        },
      ],
    );
    this.formInitialValues = this.form.getRawValue();
  }
}
