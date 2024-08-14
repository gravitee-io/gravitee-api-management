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
import { Component, DestroyRef, inject, Inject, OnInit } from "@angular/core";
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { isEmpty } from 'lodash';

import { Constants } from '../../../../entities/Constants';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { ApiTransferOwnership , Api, Group, Member } from "../../../../entities/management-api-v2";
import { Role } from '../../../../entities/role/role';


export interface ApiOwnershipDialogData {
  api: Api;
  groups: Group[];
  roles: Role[];
  members: Member[];
}
export interface ApiOwnershipDialogResult {
  isUserMode: boolean;
  transferOwnershipToUser?: ApiTransferOwnership;
  transferOwnershipToGroup: ApiTransferOwnership;
}

type TransferOwnershipMode = 'USER' | 'GROUP' | 'HYBRID';

@Component({
  selector: 'integration-general-transfer-ownership',
  templateUrl: './integration-general-transfer-ownership.component.html',
  styleUrls: ['./integration-general-transfer-ownership.component.scss'],
})
export class IntegrationGeneralTransferOwnershipComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  private api: Api;
  private groups: Group[];
  private roles: Role[];

  mode: TransferOwnershipMode;
  warnUseGroupAsPrimaryOwner = false;
  form: UntypedFormGroup;
  poGroups: Group[];
  poRoles: Role[];
  apiMembers: Member[];

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    @Inject(Constants) private readonly constants: Constants,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiOwnershipDialogData,
    public dialogRef: MatDialogRef<ApiOwnershipDialogData, ApiOwnershipDialogResult>,
  ) {
    this.api = dialogData.api;
    this.groups = dialogData.groups;
    this.roles = dialogData.roles;
    this.apiMembers = dialogData.members.filter((member) => !member.roles?.map((r) => r.name)?.includes('PRIMARY_OWNER'));
  }

  ngOnInit(): void {
    this.mode = this.constants.env.settings.api.primaryOwnerMode.toUpperCase() as TransferOwnershipMode;
    this.poGroups = this.groups.filter((group) => group.apiPrimaryOwner != null);
    if (this.api.primaryOwner.type === 'GROUP') {
      this.poGroups = this.poGroups.filter((group) => group.id !== this.api.primaryOwner.id);
    }
    this.warnUseGroupAsPrimaryOwner = (this.mode === 'HYBRID' || this.mode === 'GROUP') && isEmpty(this.poGroups);
    this.poRoles = this.roles.filter((role) => role.name !== 'PRIMARY_OWNER');
    const defaultRolePO = this.roles.find((role) => role.default);
    this.initForm(defaultRolePO);
  }


  public onSubmit() {
    const newRole = this.form.get('roleId').value;
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
    this.dialogRef.close({ isUserMode, transferOwnershipToGroup, transferOwnershipToUser });
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

          return errors ?? null;
        },
      ],
    );

    this.form
      .get('userOrGroup')
      .valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.form.get('user').reset();
        this.form.get('groupId').reset();
      });
  }
}
