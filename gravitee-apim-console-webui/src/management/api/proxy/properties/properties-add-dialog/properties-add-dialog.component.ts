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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { Property } from '../../../../../entities/management-api-v2';
import { isUnique } from '../../../../../shared/utils';

export type PropertiesAddDialogData = {
  properties: Property[];
};

export type PropertiesAddDialogResult = Property;

@Component({
  selector: 'properties-add-dialog',
  template: require('./properties-add-dialog.component.html'),
  styles: [require('./properties-add-dialog.component.scss')],
})
export class PropertiesAddDialogComponent {
  private existingKeys: string[] = [];

  public formGroup = new FormGroup({
    key: new FormControl('', [Validators.required]),
    value: new FormControl(''),
    toEncrypt: new FormControl(false),
  });

  constructor(
    private readonly dialogRef: MatDialogRef<PropertiesAddDialogData, PropertiesAddDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: PropertiesAddDialogData,
  ) {
    this.existingKeys = dialogData.properties.map((p) => p.key);
    this.formGroup.get('key').addValidators([isUnique(this.existingKeys)]);
    this.formGroup.get('key').updateValueAndValidity();
  }

  public onSave(): void {
    this.dialogRef.close({
      key: this.formGroup.get('key').value,
      value: this.formGroup.get('value').value,
      encryptable: this.formGroup.get('toEncrypt').value,
    });
  }
}
