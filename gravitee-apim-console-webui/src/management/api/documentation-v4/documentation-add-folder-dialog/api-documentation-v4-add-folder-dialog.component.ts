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
import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'api-documentation-v4-add-folder-dialog',
  template: require('./api-documentation-v4-add-folder-dialog.component.html'),
  styles: [require('./api-documentation-v4-add-folder-dialog.component.scss')],
})
export class ApiDocumentationV4AddFolderDialog implements OnInit {
  public formGroup: FormGroup;

  constructor(public dialogRef: MatDialogRef<ApiDocumentationV4AddFolderDialog>, private formBuilder: FormBuilder) {}

  ngOnInit(): void {
    this.formGroup = this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required]),
      visibility: this.formBuilder.control('PUBLIC', [Validators.required]),
    });
  }

  save() {
    this.dialogRef.close(this.formGroup.getRawValue());
  }
  cancel() {
    this.dialogRef.close(null);
  }
}
