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
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { isEmpty } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApiMemberService } from '../../../../services-ngx/api-member.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GroupService } from '../../../../services-ngx/group.service';
import { Group } from '../../../../entities/group/group';
import { RoleService } from '../../../../services-ngx/role.service';
import { Role } from '../../../../entities/role/role';
import { Constants } from '../../../../entities/Constants';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiTransferOwnership } from '../../../../entities/management-api-v2/api/apiTransferOwnership';

@Component({
  selector: 'api-general-transfer-ownership',
  templateUrl: './api-general-transfer-ownership.component.html',
  styleUrls: ['./api-general-transfer-ownership.component.scss'],
})
export class ApiGeneralTransferOwnershipComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  mode: 'USER' | 'GROUP' | 'HYBRID';
  warnUseGroupAsPrimaryOwner = false;

  form: UntypedFormGroup;

  poGroups: Group[];

  poRoles: Role[];

  apiMembers: SearchableUser[];

  private apiId: string;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiMembersService: ApiMemberService,
    private readonly groupService: GroupService,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.apiId = this.activatedRoute.snapshot.params.apiId;

    this.mode = this.constants.env.settings.api.primaryOwnerMode.toUpperCase() as 'USER' | 'GROUP' | 'HYBRID';

    combineLatest([
      this.apiV2Service.get(this.apiId),
      this.groupService.list(),
      this.roleService.list('API'),
      this.apiMembersService.getMembers(this.apiId),
    ])
      .pipe(
        tap(([api, groups, roles, apiMembers]) => {
          this.poGroups = groups.filter((group) => group.apiPrimaryOwner != null);
          if (api.primaryOwner.type === 'GROUP') {
            this.poGroups = this.poGroups.filter((group) => group.id !== api.primaryOwner.id);
          }

          this.warnUseGroupAsPrimaryOwner = (this.mode === 'HYBRID' || this.mode === 'GROUP') && isEmpty(this.poGroups);

          this.poRoles = roles.filter((role) => role.name !== 'PRIMARY_OWNER');
          const defaultRolePO = roles.find((role) => role.default);

          this.apiMembers = apiMembers
            .filter((member) => member.role !== 'PRIMARY_OWNER')
            .map((member) => ({
              reference: member.reference,
              id: member.id,
              displayName: member.displayName,
            }));

          this.initForm(defaultRolePO);
        }),
        takeUntil(this.unsubscribe$),
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
    const transferOwnershipToUser: ApiTransferOwnership = {
      userId: user?.id,
      userReference: user?.reference,
      poRole: newRole,
      userType: 'USER',
    };
    const transferOwnershipToGroup: ApiTransferOwnership = {
      userId: this.form.get('groupId').value,
      userReference: null,
      poRole: newRole,
      userType: 'GROUP',
    };

    const userMode = this.form.get('userOrGroup').value;
    const isUserMode = userMode === 'user' || userMode === 'apiMember';

    confirm
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => this.apiV2Service.transferOwnership(this.apiId, isUserMode ? transferOwnershipToUser : transferOwnershipToGroup)),
        tap(
          () => this.snackBarService.success('Transfer ownership done.'),
          ({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          },
        ),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm(defaultRolePO: Role) {
    this.form = new UntypedFormGroup(
      {
        userOrGroup: new UntypedFormControl(this.mode === 'GROUP' ? 'group' : 'apiMember'),
        user: new UntypedFormControl(),
        groupId: new UntypedFormControl(),
        roleId: new UntypedFormControl(defaultRolePO.name),
      },
      [
        (control: AbstractControl): ValidationErrors | null => {
          const errors: ValidationErrors = {};
          if (!control.get('userOrGroup').value) {
            errors.userOrGroupRequired = true;
          }

          const userMode = control.get('userOrGroup').value;

          const isUserMode = userMode === 'user' || userMode === 'apiMember';
          const isGroupMode = userMode === 'group';

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

    this.form
      .get('userOrGroup')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this.form.get('user').reset();
        this.form.get('groupId').reset();
      });
  }
}
