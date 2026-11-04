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
import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { pathMappingValidators } from './path-mapping.validators';

export interface ApiV4PathMappingsEditDialogData {
  path: string;
  existing: string[];
}

@Component({
  selector: 'api-v4-path-mappings-edit-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './api-v4-path-mappings-edit-dialog.component.html',
  styleUrl: './api-v4-path-mappings-edit-dialog.component.scss',
})
export class ApiV4PathMappingsEditDialogComponent {
  private readonly dialogData = inject<ApiV4PathMappingsEditDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ApiV4PathMappingsEditDialogComponent, string>);

  protected readonly errorStateMatcher: ErrorStateMatcher = {
    isErrorState: control => !!control && control.invalid && (control.dirty || control.touched || !!control.value),
  };

  protected readonly pathForm = new FormGroup({
    path: new FormControl(this.dialogData.path, {
      nonNullable: true,
      validators: pathMappingValidators(this.dialogData.existing, this.dialogData.path),
    }),
  });

  protected save(): void {
    if (this.pathForm.invalid) {
      this.pathForm.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.pathForm.controls.path.value.trim());
  }
}
