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
import { Component, inject } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { map, shareReplay } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

import { leastOneGroupRoleIsRequiredValidator } from './group-role-validators';

import { RoleService } from '../../../../../services-ngx/role.service';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';

export type OrgSettingsUserDetailAddGroupDialogReturn = {
  environmentId: string;
  groupId: string;
  groupName: string;
  apiRole: string;
  applicationRole?: string;
  integrationRole?: string;
  isAdmin?: boolean;
};

export type OrgSettingsUserDetailAddGroupDialogData = {
  environmentId: string;
  groupIdAlreadyAdded: string[];
};

@Component({
  selector: 'org-settings-user-detail-add-group-dialog',
  templateUrl: './org-settings-user-detail-add-group-dialog.component.html',
  styleUrls: ['./org-settings-user-detail-add-group-dialog.component.scss'],
  standalone: false,
})
export class OrgSettingsUserDetailAddGroupDialogComponent {
  readonly dialogRef =
    inject<MatDialogRef<OrgSettingsUserDetailAddGroupDialogComponent, OrgSettingsUserDetailAddGroupDialogReturn>>(MatDialogRef);
  private readonly userDetailData = inject<OrgSettingsUserDetailAddGroupDialogData>(MAT_DIALOG_DATA);
  private readonly groupV2Service = inject(GroupV2Service);
  private readonly roleService = inject(RoleService);

  isUpdate = false;
  addGroupForm: UntypedFormGroup;

  readonly groups = toSignal(
    this.groupV2Service
      .listByEnvironmentId(this.userDetailData.environmentId, 1, 9999)
      .pipe(map((response) => (response.data ?? []).filter((g) => !this.userDetailData.groupIdAlreadyAdded.includes(g.id)))),
  );

  readonly apiRoles = toSignal(this.roleService.list('API').pipe(shareReplay(1)), { initialValue: [] });
  readonly applicationRoles = toSignal(this.roleService.list('APPLICATION').pipe(shareReplay(1)), { initialValue: [] });
  readonly integrationRoles = toSignal(this.roleService.list('INTEGRATION').pipe(shareReplay(1)), { initialValue: [] });

  constructor() {
    this.addGroupForm = new UntypedFormGroup(
      {
        groupId: new UntypedFormControl(null, [Validators.required]),
        isAdmin: new UntypedFormControl(null),
        apiRole: new UntypedFormControl(null),
        applicationRole: new UntypedFormControl(null),
        integrationRole: new UntypedFormControl(null),
      },
      [leastOneGroupRoleIsRequiredValidator],
    );
  }

  onSubmit() {
    const formValue = this.addGroupForm.getRawValue();
    const selectedGroup = this.groups()?.find((g) => g.id === formValue.groupId);
    this.dialogRef.close({
      ...formValue,
      groupName: selectedGroup?.name ?? formValue.groupId,
      environmentId: this.userDetailData.environmentId,
    });
  }
}
