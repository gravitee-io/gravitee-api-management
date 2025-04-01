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
import { UntypedFormControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { isEmpty } from 'lodash';

import { Property } from '../../../../../entities/management-api-v2';
import { parsePropertiesStringFormat } from '../../../../../shared/utils';

export type PropertiesImportDialogData = {
  properties: Property[];
};

export type PropertiesImportDialogResult = (existingProperties: Property[]) => Property[];

@Component({
  selector: 'properties-import-dialog',
  templateUrl: './properties-import-dialog.component.html',
  styleUrls: ['./properties-import-dialog.component.scss'],
  standalone: false,
})
export class PropertiesImportDialogComponent {
  public formGroup = new UntypedFormGroup({
    properties: new UntypedFormControl(''),
  });

  constructor(
    private readonly dialogRef: MatDialogRef<PropertiesImportDialogData, PropertiesImportDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: PropertiesImportDialogData,
  ) {
    this.formGroup.get('properties').addValidators([this.validateProperties(dialogData?.properties ?? [])]);
    this.formGroup.get('properties').updateValueAndValidity();
  }

  public onSave(): void {
    const propertiesParsed = parsePropertiesStringFormat(this.formGroup.get('properties').value);

    this.dialogRef.close((existingProperties: Property[]) => {
      const existingKeysEncrypted = existingProperties.filter((p) => p.encrypted).map((p) => p.key);

      // Remove encrypted properties from import
      const propertiesToImport = propertiesParsed.properties.filter((p) => !existingKeysEncrypted.includes(p.key));

      // Get existing properties not updated by import
      const existingPropertiesToKeep = existingProperties.filter((p) => !propertiesToImport.map((p) => p.key).includes(p.key));

      return [...existingPropertiesToKeep, ...propertiesToImport];
    });
  }

  // Validate properties format
  public validateProperties(properties: Property[]): ValidatorFn {
    return (control) => {
      const result: Record<string, string> = {};
      const value = control.value;

      if (!value) {
        return null;
      }

      const propertiesParsed = parsePropertiesStringFormat(value);
      if (propertiesParsed.errors.length > 0) {
        result.propertiesFormat = propertiesParsed.errors.join('<br>');
      }

      if (propertiesParsed.properties.length > 0) {
        const existingKeysNotEncrypted = properties.filter((p) => !p.encrypted).map((p) => p.key);
        const duplicateKeysReplaced = propertiesParsed.properties.map((p) => p.key).filter((k) => existingKeysNotEncrypted.includes(k));
        if (duplicateKeysReplaced.length > 0) {
          result.duplicateKeysReplaced = `Overwritten keys: ${duplicateKeysReplaced.join(', ')}`;
        }

        const existingKeysEncrypted = properties.filter((p) => p.encrypted).map((p) => p.key);
        const duplicateKeysNotReplaced = propertiesParsed.properties.map((p) => p.key).filter((k) => existingKeysEncrypted.includes(k));
        if (duplicateKeysNotReplaced.length > 0) {
          result.duplicateKeysNotReplaced = `Skipped keys (encrypted): ${duplicateKeysNotReplaced.join(', ')}`;
        }
      }

      return isEmpty(result) ? null : result;
    };
  }
}
