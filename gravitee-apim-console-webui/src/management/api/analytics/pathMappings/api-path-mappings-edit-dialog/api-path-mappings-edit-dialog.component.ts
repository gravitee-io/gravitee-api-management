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
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

import { ApiPathMappingsEditDialogData } from '../api-path-mappings.model';

@Component({
  selector: 'api-path-mappings-edit-dialog',
  template: require('./api-path-mappings-edit-dialog.component.html'),
  styles: [require('./api-path-mappings-edit-dialog.component.scss')],
})
export class ApiPathMappingsEditDialogComponent {
  public pathFormGroup: FormGroup;
  constructor(@Inject(MAT_DIALOG_DATA) dialogData: ApiPathMappingsEditDialogData) {
    this.pathFormGroup = new FormGroup({
      path: new FormControl(dialogData.path, [Validators.required, this.isUnique(dialogData.api.path_mappings)]),
    });
  }

  public save(): void {
    // Do something
  }

  private isUnique(paths: string[]): ValidatorFn | null {
    return (control: AbstractControl): ValidationErrors | null => {
      const formControlValue = control.value;
      return paths.includes(formControlValue) ? { isUnique: true } : null;
    };
  }
}
