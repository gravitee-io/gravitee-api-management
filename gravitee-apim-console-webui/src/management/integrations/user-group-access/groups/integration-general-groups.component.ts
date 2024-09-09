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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Group } from '../../../../entities/management-api-v2';
import { Integration } from '../../integrations.model';

export interface IntegrationGroupsDialogData {
  integration: Integration;
  groups: Group[];
}
export interface IntegrationGroupsDialogResult {
  groups: string[];
}

@Component({
  selector: 'integration-general-access-groups',
  templateUrl: './integration-general-groups.component.html',
  styleUrls: ['./integration-general-groups.component.scss'],
})
export class IntegrationGeneralGroupsComponent implements OnInit {
  public isReadOnly = true;
  public form: UntypedFormGroup;
  public integration: Integration;
  public groups: Group[];
  public readOnlyGroupList: string;

  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly formBuilder: UntypedFormBuilder,

    @Inject(MAT_DIALOG_DATA) dialogData: IntegrationGroupsDialogData,
    public dialogRef: MatDialogRef<IntegrationGroupsDialogData, IntegrationGroupsDialogResult>,
  ) {
    this.integration = dialogData.integration;
    this.groups = dialogData.groups;
  }

  ngOnInit() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['integration-member-u']);

    const userGroupList: Group[] = this.groups.filter((group) => this.integration.groups?.includes(group.id));
    this.form = this.formBuilder.group({
      selectedGroups: {
        value: userGroupList.map((g) => g.id),
        disabled: this.isReadOnly,
      },
    });
    this.readOnlyGroupList = userGroupList.length === 0 ? 'No groups associated' : userGroupList.map((g) => g.name).join(', ');
  }

  save(): void {
    this.dialogRef.close({
      groups: this.form.getRawValue()?.selectedGroups,
    });
  }
}
