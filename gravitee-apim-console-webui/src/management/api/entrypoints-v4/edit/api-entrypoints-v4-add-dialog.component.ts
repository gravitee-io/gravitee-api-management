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

import { ConnectorVM } from '../../creation-v4/models/ConnectorVM';

export type ApiEntrypointsV4AddDialogComponentData = {
  entrypoints: ConnectorVM[];
};
@Component({
  selector: 'api-entrypoints-v4-add-dialog',
  template: require('./api-entrypoints-v4-add-dialog.component.html'),
  styles: [require('./api-entrypoints-v4-add-dialog.component.scss')],
})
export class ApiEntrypointsV4AddDialogComponent implements OnInit {
  public entrypoints: ConnectorVM[];
  public formGroup: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<ApiEntrypointsV4AddDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: ApiEntrypointsV4AddDialogComponentData,
    private formBuilder: FormBuilder,
  ) {
    this.entrypoints = data.entrypoints;
  }

  ngOnInit(): void {
    this.formGroup = this.formBuilder.group({
      selectedEntrypointsIds: this.formBuilder.control([], [Validators.required]),
    });
  }

  save() {
    this.dialogRef.close(this.formGroup.getRawValue().selectedEntrypointsIds);
  }

  cancel() {
    this.dialogRef.close([]);
  }
}
