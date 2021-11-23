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
import { Component, Inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { map, shareReplay } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { RoleService } from '../../../../services-ngx/role.service';
import { GroupService } from '../../../../services-ngx/group.service';
import { Group } from '../../../../entities/group/group';

export type OrgSettingsUserDetailAddGroupDialogReturn = {
  groupId: string;
  apiRole: string;
  applicationRole?: string;
  isAdmin?: boolean;
};

export type OrgSettingsUserDetailAddGroupDialogData = {
  groupIdAlreadyAdded: string[];
};

@Component({
  selector: 'org-settings-user-detail-add-group-dialog',
  template: require('./org-settings-user-detail-add-group-dialog.component.html'),
  styles: [require('./org-settings-user-detail-add-group-dialog.component.scss')],
})
export class OrgSettingsUserDetailAddGroupDialogComponent {
  isUpdate = false;
  addGroupForm: FormGroup;
  groups$: Observable<Group[]>;
  apiRoles$ = this.roleService.list('API').pipe(shareReplay());
  applicationRoles$ = this.roleService.list('APPLICATION').pipe(shareReplay());

  constructor(
    public readonly dialogRef: MatDialogRef<OrgSettingsUserDetailAddGroupDialogComponent, OrgSettingsUserDetailAddGroupDialogReturn>,
    @Inject(MAT_DIALOG_DATA) private readonly userDetailData: OrgSettingsUserDetailAddGroupDialogData,
    private readonly groupService: GroupService,
    private readonly roleService: RoleService,
  ) {
    this.groups$ = this.groupService.listByOrganization().pipe(
      shareReplay(),
      map((groups) => groups.filter((g) => !this.userDetailData.groupIdAlreadyAdded.includes(g.id))),
    );

    this.addGroupForm = new FormGroup(
      {
        groupId: new FormControl(null, [Validators.required]),
        isAdmin: new FormControl(null),
        apiRole: new FormControl(null),
        applicationRole: new FormControl(null),
      },
      [leastOneGroupRoleIsRequiredValidator],
    );
  }

  onSubmit() {
    const groupToAdd = this.addGroupForm.getRawValue();

    this.dialogRef.close(groupToAdd);
  }
}

const leastOneGroupRoleIsRequiredValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const groupRolesFormGroup = control as FormGroup;

  const isAdmin = groupRolesFormGroup.get('isAdmin').value;
  const apiRoleValue = groupRolesFormGroup.get('apiRole').value;
  const applicationRoleValue = groupRolesFormGroup.get('applicationRole').value;

  if (isAdmin || apiRoleValue || applicationRoleValue) {
    return null;
  }

  return {
    leastOneIsRequired: true,
  };
};
