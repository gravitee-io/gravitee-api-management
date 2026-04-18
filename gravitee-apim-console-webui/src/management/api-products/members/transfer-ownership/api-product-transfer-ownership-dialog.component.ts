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

import { Component, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { isEmpty } from 'lodash';

import { GioFormUserAutocompleteModule } from '../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.module';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { ApiProductTransferOwnership } from '../../../../entities/management-api-v2/api-product/apiProductTransferOwnership';
import { Group, Member } from '../../../../entities/management-api-v2';
import { Role } from '../../../../entities/role/role';
import { Constants } from '../../../../entities/Constants';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';

export interface ApiProductOwnershipDialogData {
  apiProduct: ApiProduct;
  groups: Group[];
  roles: Role[];
  members: Member[];
}

export interface ApiProductOwnershipDialogResult {
  isUserMode: boolean;
  transferOwnershipToUser: ApiProductTransferOwnership;
  transferOwnershipToGroup: ApiProductTransferOwnership;
}

type TransferOwnershipMode = 'USER' | 'GROUP' | 'HYBRID';

type UserOrGroupValue = 'user' | 'group' | 'apiProductMember';

type TransferOwnershipFormControls = {
  userOrGroup: FormControl<UserOrGroupValue | null>;
  user: FormControl<Member | SearchableUser | null>;
  groupId: FormControl<string | null>;
  roleId: FormControl<string | null>;
};

@Component({
  selector: 'api-product-transfer-ownership-dialog',
  standalone: true,
  templateUrl: './api-product-transfer-ownership-dialog.component.html',
  styleUrls: ['./api-product-transfer-ownership-dialog.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    GioBannerModule,
    GioFormUserAutocompleteModule,
  ],
})
export class ApiProductTransferOwnershipDialogComponent {
  private readonly constants = inject(Constants);
  private readonly dialogData = inject<ApiProductOwnershipDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ApiProductTransferOwnershipDialogComponent, ApiProductOwnershipDialogResult>);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly mode: TransferOwnershipMode =
    this.constants.env.settings.apiProduct.primaryOwnerMode.toUpperCase() as TransferOwnershipMode;
  protected readonly apiProductMembers: Member[] = this.dialogData.members.filter(
    member => !member.roles?.some(role => role.name === 'PRIMARY_OWNER'),
  );
  protected readonly poGroups: Group[] = this.computePoGroups();
  protected readonly poRoles: Role[] = this.dialogData.roles.filter(role => role.name !== 'PRIMARY_OWNER');
  protected readonly warnUseGroupAsPrimaryOwner = (this.mode === 'HYBRID' || this.mode === 'GROUP') && isEmpty(this.poGroups);
  protected readonly form: FormGroup<TransferOwnershipFormControls> = this.buildForm();

  protected readonly userOrGroupValue = toSignal(
    this.form.controls.userOrGroup.valueChanges.pipe(startWith(this.form.controls.userOrGroup.value)),
    { initialValue: this.form.controls.userOrGroup.value },
  );

  protected readonly selectedTransferUser = toSignal(this.form.controls.user.valueChanges.pipe(startWith(this.form.controls.user.value)), {
    initialValue: this.form.controls.user.value,
  });

  protected readonly selectedGroupId = toSignal(this.form.controls.groupId.valueChanges.pipe(startWith(this.form.controls.groupId.value)), {
    initialValue: this.form.controls.groupId.value,
  });

  protected readonly showRoleSection = computed(() => !isEmpty(this.selectedTransferUser()) || !isEmpty(this.selectedGroupId()));

  protected onSubmit(): void {
    const { userOrGroup, user, groupId, roleId } = this.form.getRawValue();
    const currentPrimaryOwnerNewRole = roleId ?? undefined;
    const transferOwnershipToUser: ApiProductTransferOwnership = {
      newPrimaryOwnerId: user?.id ?? undefined,
      userReference: user && 'reference' in user ? (user.reference ?? undefined) : undefined,
      currentPrimaryOwnerNewRole,
      userType: 'USER',
    };
    const transferOwnershipToGroup: ApiProductTransferOwnership = {
      newPrimaryOwnerId: groupId ?? undefined,
      currentPrimaryOwnerNewRole,
      userType: 'GROUP',
    };
    const isUserMode = userOrGroup === 'user' || userOrGroup === 'apiProductMember';
    this.dialogRef.close({ isUserMode, transferOwnershipToGroup, transferOwnershipToUser });
  }

  private computePoGroups(): Group[] {
    let groups = this.dialogData.groups.filter(group => group.apiProductPrimaryOwner != null);
    const { apiProduct } = this.dialogData;
    if (apiProduct.primaryOwner?.type === 'GROUP') {
      const poId = apiProduct.primaryOwner.id;
      if (poId != null) {
        groups = groups.filter(group => group.id !== poId);
      }
    }
    return groups;
  }

  private buildForm(): FormGroup<TransferOwnershipFormControls> {
    const defaultRolePO = this.dialogData.roles.find(role => role.default);
    const form = new FormGroup<TransferOwnershipFormControls>(
      {
        userOrGroup: new FormControl<UserOrGroupValue | null>(this.mode === 'GROUP' ? 'group' : 'apiProductMember'),
        user: new FormControl<Member | SearchableUser | null>(null),
        groupId: new FormControl<string | null>(null),
        roleId: new FormControl<string | null>(defaultRolePO?.name ?? null),
      },
      { validators: [control => this.transferOwnershipFormValidator(control)] },
    );

    form.controls.userOrGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      form.controls.user.reset();
      form.controls.groupId.reset();
    });

    return form;
  }

  private transferOwnershipFormValidator(control: AbstractControl): ValidationErrors | null {
    const fg = control as FormGroup<TransferOwnershipFormControls>;
    const errors: ValidationErrors = {};
    if (!fg.controls.userOrGroup.value) {
      errors['userOrGroupRequired'] = true;
    }

    const userMode = fg.controls.userOrGroup.value;
    const isUserMode = userMode === 'user' || userMode === 'apiProductMember';
    const isGroupMode = userMode === 'group';

    if (isUserMode && isEmpty(fg.controls.user.value)) {
      errors['userRequired'] = true;
    }
    if (isGroupMode && isEmpty(fg.controls.groupId.value)) {
      errors['groupRequired'] = true;
    }
    if (!fg.controls.roleId.value) {
      errors['roleRequired'] = true;
    }

    return Object.keys(errors).length ? errors : null;
  }
}
