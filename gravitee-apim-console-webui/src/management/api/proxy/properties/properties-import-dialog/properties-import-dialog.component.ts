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
import { FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type PropertiesImportDialogData = undefined;

export type PropertiesImportDialogResult = undefined;

@Component({
  selector: 'properties-import-dialog',
  template: require('./properties-import-dialog.component.html'),
  styles: [require('./properties-import-dialog.component.scss')],
})
export class PropertiesImportDialogComponent {
  public formGroup = new FormGroup({});

  constructor(
    private readonly dialogRef: MatDialogRef<PropertiesImportDialogData, PropertiesImportDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: PropertiesImportDialogData,
  ) {}

  public onSave(): void {
    this.dialogRef.close();
  }
}
