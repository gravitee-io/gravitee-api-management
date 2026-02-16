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
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { map, shareReplay, switchMap, take } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { RoleService } from '../../../../services-ngx/role.service';
import { GroupService } from '../../../../services-ngx/group.service';
import { Group } from '../../../../entities/group/group';

export type OrgSettingsUserDetailAddGroupDialogReturn = {
  environmentId: any;
  groupId: string;
  apiRole: string;
  applicationRole?: string;
  integrationRole?: string;
  isAdmin?: boolean;
};

export type OrgSettingsUserDetailAddGroupDialogData = {
  groupIdAlreadyAdded: string[];
};

@Component({
  selector: 'org-settings-user-detail-add-group-dialog',
  templateUrl: './org-settings-user-detail-add-group-dialog.component.html',
  styleUrls: ['./org-settings-user-detail-add-group-dialog.component.scss'],
  standalone: false,
})
export class OrgSettingsUserDetailAddGroupDialogComponent {
  isUpdate = false;
  addGroupForm: UntypedFormGroup;
  groups$: Observable<Group[]>;
  apiRoles$ = this.roleService.list('API').pipe(shareReplay(1));
  applicationRoles$ = this.roleService.list('APPLICATION').pipe(shareReplay(1));
  integrationRoles$ = this.roleService.list('INTEGRATION').pipe(shareReplay(1));

  constructor(
    public readonly dialogRef: MatDialogRef<OrgSettingsUserDetailAddGroupDialogComponent, OrgSettingsUserDetailAddGroupDialogReturn>,
    @Inject(MAT_DIALOG_DATA) private readonly userDetailData: OrgSettingsUserDetailAddGroupDialogData,
    private readonly groupService: GroupService,
    private readonly roleService: RoleService,
  ) {
    this.groups$ = this.groupService.listByOrganization().pipe(
      shareReplay(1),
      map(groups => groups.filter(g => !this.userDetailData.groupIdAlreadyAdded.includes(g.id))),
    );

    this.addGroupForm = new UntypedFormGroup(
      {
        groupId: new UntypedFormControl(null, [Validators.required]),
        isAdmin: new UntypedFormControl(null),
        apiRole: new UntypedFormControl(null),
        applicationRole: new UntypedFormControl(null),
        integrationRole: new UntypedFormControl(null),
        environmentId: new UntypedFormControl(null),
      },
      [leastOneGroupRoleIsRequiredValidator],
    );
    this.addGroupForm
      .get('groupId')
      .valueChanges.pipe(
        switchMap(selectedGroupId =>
          this.groups$.pipe(
            take(1),
            map(groups => {
              const selectedGroup = groups.find(g => g.id === selectedGroupId);
              return selectedGroup?.environmentId ?? null;
            }),
          ),
        ),
      )
      .subscribe(environmentId => {
        this.addGroupForm.patchValue({ environmentId });
      });
  }

  onSubmit() {
    const groupToAdd = this.addGroupForm.getRawValue();
    this.dialogRef.close(groupToAdd);
  }
}

const leastOneGroupRoleIsRequiredValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const groupRolesFormGroup = control as UntypedFormGroup;

  const isAdmin = groupRolesFormGroup.get('isAdmin').value;
  const apiRoleValue = groupRolesFormGroup.get('apiRole').value;
  const applicationRoleValue = groupRolesFormGroup.get('applicationRole').value;
  const integrationRoleValue = groupRolesFormGroup.get('integrationRole').value;

  if (isAdmin || apiRoleValue || applicationRoleValue || integrationRoleValue) {
    return null;
  }

  return {
    leastOneIsRequired: true,
  };
};
