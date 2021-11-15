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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { shareReplay } from 'rxjs/operators';

import { Tag } from '../../../entities/tag/tag';
import { GroupService } from '../../../services-ngx/group.service';

export type OrgSettingAddTagDialogData = {
  tag?: Tag;
};

@Component({
  selector: 'org-settings-add-tag-dialog',
  template: require('./org-settings-add-tag-dialog.component.html'),
  styles: [require('./org-settings-add-tag-dialog.component.scss')],
})
export class OrgSettingAddTagDialogComponent {
  tag?: Tag;
  isUpdate = false;
  tagForm: FormGroup;
  groups$? = this.groupService.listByOrganization().pipe(shareReplay());

  constructor(
    public dialogRef: MatDialogRef<OrgSettingAddTagDialogComponent>,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingAddTagDialogData,
    private readonly groupService: GroupService,
  ) {
    this.tag = confirmDialogData.tag;
    this.isUpdate = !!this.tag;

    this.tagForm = new FormGroup({
      name: new FormControl(this.tag?.name, [Validators.required, Validators.minLength(1), Validators.maxLength(64)]),
      description: new FormControl(this.tag?.description),
      restrictedGroups: new FormControl(this.tag?.restricted_groups ?? []),
    });
  }

  onSubmit() {
    const { restrictedGroups, ...formRawValue } = this.tagForm.getRawValue();
    const updatedTag = {
      ...this.tag,
      ...formRawValue,
      restricted_groups: restrictedGroups,
    };
    this.dialogRef.close(updatedTag);
  }
}
