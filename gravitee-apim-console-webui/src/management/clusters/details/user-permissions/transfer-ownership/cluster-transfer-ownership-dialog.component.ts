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
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { isEmpty } from 'lodash';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule, NgForOf } from '@angular/common';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { Role } from '../../../../../entities/role/role';
import { Member } from '../../../../../entities/management-api-v2';
import { GioFormUserAutocompleteModule } from '../../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.module';

export interface TransferOwnershipDialogData {
  roles: Role[];
  members: Member[];
}

export interface TransferOwnership {
  newPrimaryOwnerId: string;
  userReference: string;
  currentPrimaryOwnerNewRole: string;
}

export interface TransferOwnershipDialogResult {
  transferOwnershipToUser: TransferOwnership;
}

type TransferOwnershipMode = 'ENTITY_MEMBER' | 'OTHER_USER';

@Component({
  selector: 'cluster-transfer-ownership-dialog',
  templateUrl: './cluster-transfer-ownership-dialog.component.html',
  styleUrls: ['./cluster-transfer-ownership-dialog.component.scss'],
  imports: [
    CommonModule,
    MatFormFieldModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatButtonModule,
    NgForOf,
    GioPermissionModule,
    MatButtonToggle,
    GioBannerModule,
    GioFormUserAutocompleteModule,
    MatButtonToggleGroup,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
  ],
})
export class ClusterTransferOwnershipDialogComponent implements OnInit {
  form: FormGroup;
  mode: TransferOwnershipMode = 'ENTITY_MEMBER';
  poRoles: Role[];
  entityMembers: Member[];

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: TransferOwnershipDialogData,
    public dialogRef: MatDialogRef<TransferOwnershipDialogData, TransferOwnershipDialogResult>,
  ) {
    this.poRoles = dialogData.roles.filter(role => role.name !== 'PRIMARY_OWNER');
    this.entityMembers = dialogData.members.filter(member => !member.roles?.map(r => r.name)?.includes('PRIMARY_OWNER'));
  }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm() {
    const defaultRolePO = this.poRoles.find(role => role.default);
    this.form = new FormGroup(
      {
        transferMode: new FormControl('ENTITY_MEMBER'),
        user: new FormControl(),
        entityMember: new FormControl(),
        roleId: new FormControl(defaultRolePO.name),
      },
      {
        validators: (control: AbstractControl): ValidationErrors | null => {
          const group = control as FormGroup;
          const errors: ValidationErrors = {};

          const transferMode = group.get('transferMode').value;

          if (transferMode === 'ENTITY_MEMBER' && isEmpty(group.get('entityMember').value)) {
            errors.entityMemberRequired = true;
          }

          if (transferMode === 'OTHER_USER' && isEmpty(group.get('user').value)) {
            errors.userRequired = true;
          }

          return errors ?? null;
        },
      },
    );
  }

  onSubmit() {
    const transferMode = this.form.get('transferMode').value;
    const newRole = this.form.get('roleId').value;
    const user: SearchableUser = transferMode === 'ENTITY_MEMBER' ? this.form.get('entityMember').value : this.form.get('user').value;
    const transferOwnershipToUser: TransferOwnership = {
      newPrimaryOwnerId: user?.id,
      userReference: user?.reference,
      currentPrimaryOwnerNewRole: newRole,
    };
    this.dialogRef.close({ transferOwnershipToUser });
  }
}
