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
import { Component, DestroyRef, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { filter, shareReplay, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Tag } from '../../../../entities/tag/tag';
import { GroupService } from '../../../../services-ngx/group.service';
import { sanitizeKeyBase, sanitizeKeyFinal } from '../../../../shared/utils/key-sanitizer.util';

export type OrgSettingAddTagDialogData = {
  tag?: Tag;
};

@Component({
  selector: 'org-settings-add-tag-dialog',
  templateUrl: './org-settings-add-tag-dialog.component.html',
  styleUrls: ['./org-settings-add-tag-dialog.component.scss'],
  standalone: false,
})
export class OrgSettingAddTagDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<OrgSettingAddTagDialogComponent>);
  private readonly confirmDialogData = inject<OrgSettingAddTagDialogData>(MAT_DIALOG_DATA);
  private readonly groupService = inject(GroupService);
  private readonly destroyRef = inject(DestroyRef);

  tag?: Tag = this.confirmDialogData.tag;
  isUpdate = !!this.tag;
  tagForm = new FormGroup({
    name: new FormControl<string>(this.tag?.name, [Validators.required, Validators.minLength(1), Validators.maxLength(64)]),
    key: new FormControl<string>({ value: this.tag?.key ?? '', disabled: this.isUpdate }, [
      Validators.required,
      Validators.minLength(1),
      Validators.maxLength(64),
    ]),
    description: new FormControl<string>(this.tag?.description),
    restrictedGroups: new FormControl<string[]>(this.tag?.restricted_groups ?? []),
  });
  groups$ = this.groupService.listByOrganization().pipe(shareReplay(1));

  constructor() {
    this.tagForm.controls.key.valueChanges
      .pipe(
        filter(key => key !== null),
        tap(key => {
          const sanitized = sanitizeKeyBase(key);
          if (sanitized !== key) {
            this.tagForm.controls.key.setValue(sanitized, { emitEvent: false });
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
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

  onKeyBlur(): void {
    const value = this.tagForm.controls.key.value;
    if (value == null) {
      return;
    }
    const sanitized = sanitizeKeyFinal(value);
    if (sanitized !== value) {
      this.tagForm.controls.key.setValue(sanitized, { emitEvent: false });
    }
  }
}
