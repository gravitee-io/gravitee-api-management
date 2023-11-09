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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { tap } from 'rxjs/operators';

import { Visibility } from '../../../../../entities/management-api-v2/documentation/visibility';

export interface ApiDocumentationV4AddFolderDialogData {
  mode: 'create' | 'edit';
  name?: string;
  visibility?: Visibility;
}

@Component({
  selector: 'api-documentation-v4-add-folder-dialog',
  template: require('./api-documentation-v4-add-folder-dialog.component.html'),
  styles: [require('./api-documentation-v4-add-folder-dialog.component.scss')],
})
export class ApiDocumentationV4AddFolderDialog implements OnInit {
  public formGroup: FormGroup;
  public formValueChanged = false;
  public data: ApiDocumentationV4AddFolderDialogData;

  public title: string;
  public submitButtonText: string;

  constructor(
    public dialogRef: MatDialogRef<ApiDocumentationV4AddFolderDialog, ApiDocumentationV4AddFolderDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiDocumentationV4AddFolderDialogData,
    private formBuilder: FormBuilder,
  ) {
    this.data = dialogData;
  }

  ngOnInit(): void {
    this.title = this.data.mode === 'create' ? 'Add new folder' : 'Configure folder';
    this.submitButtonText = this.data.mode === 'create' ? 'Add folder' : 'Save';

    this.formGroup = this.formBuilder.group({
      name: this.formBuilder.control(this.data?.name ?? '', [Validators.required]),
      visibility: this.formBuilder.control(this.data?.visibility ?? 'PUBLIC', [Validators.required]),
    });

    this.formGroup.valueChanges
      .pipe(
        tap((value) => {
          this.formValueChanged = this.data.name !== value.name || this.data.visibility !== value.visibility;
        }),
      )
      .subscribe();
  }

  save() {
    this.dialogRef.close(this.formGroup.getRawValue());
  }
  cancel() {
    this.dialogRef.close(null);
  }
}
