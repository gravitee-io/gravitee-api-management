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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { shareReplay } from 'rxjs/operators';

import { Entrypoint } from '../../../../entities/entrypoint/entrypoint';
import { TagService } from '../../../../services-ngx/tag.service';

export type OrgSettingAddMappingDialogData = {
  entrypoint?: Entrypoint;
};

@Component({
  selector: 'org-settings-add-mapping-dialog',
  templateUrl: './org-settings-add-mapping-dialog.component.html',
  styleUrls: ['./org-settings-add-mapping-dialog.component.scss'],
  standalone: false,
})
export class OrgSettingAddMappingDialogComponent {
  entrypoint?: Entrypoint;
  isUpdate = false;
  mappingForm: UntypedFormGroup;
  tags$? = this.tagService.list().pipe(shareReplay(1));

  constructor(
    public dialogRef: MatDialogRef<OrgSettingAddMappingDialogComponent>,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingAddMappingDialogData,
    private readonly tagService: TagService,
  ) {
    this.entrypoint = confirmDialogData.entrypoint;
    this.isUpdate = !!this.entrypoint;

    this.mappingForm = new UntypedFormGroup({
      value: new UntypedFormControl(this.entrypoint?.value, [Validators.required]),
      tags: new UntypedFormControl(this.entrypoint?.tags ?? []),
    });
  }

  onSubmit() {
    const updatedEntrypoint = {
      ...this.entrypoint,
      ...this.mappingForm.getRawValue(),
    };
    this.dialogRef.close(updatedEntrypoint);
  }
}
