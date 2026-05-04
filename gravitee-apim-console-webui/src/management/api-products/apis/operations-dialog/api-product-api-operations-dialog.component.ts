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
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { ApiProductOperation } from '../../../../entities/management-api-v2/api-product/apiProduct';

export interface ApiProductApiOperationsDialogData {
  apiName: string;
  /** Context path of the API, e.g. "/my-api" — shown to users so they know to enter relative paths */
  apiContextPath: string;
  /** Current allowed operations for this API, or null/undefined for full access */
  operations: ApiProductOperation[] | null | undefined;
}

export type ApiProductApiOperationsDialogResult = { action: 'save'; operations: ApiProductOperation[] | null } | { action: 'cancel' };

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', '*'];

@Component({
  selector: 'api-product-api-operations-dialog',
  templateUrl: './api-product-api-operations-dialog.component.html',
  styleUrls: ['./api-product-api-operations-dialog.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSelectModule,
    MatTooltipModule,
    GioIconsModule,
  ],
})
export class ApiProductApiOperationsDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ApiProductApiOperationsDialogComponent>);
  readonly data: ApiProductApiOperationsDialogData = inject(MAT_DIALOG_DATA);

  readonly httpMethods = HTTP_METHODS;

  /** true = filter mode (specific ops); false = all operations allowed */
  isFiltered: boolean;

  form: FormGroup;

  constructor() {
    const initial = this.data.operations;
    this.isFiltered = Array.isArray(initial) && initial.length > 0;

    const ops = this.isFiltered ? (initial as ApiProductOperation[]) : [];
    this.form = this.fb.group({
      operations: this.fb.array(ops.map(op => this.createOperationGroup(op))),
    });
  }

  get operationsArray(): FormArray {
    return this.form.get('operations') as FormArray;
  }

  private createOperationGroup(op?: ApiProductOperation): FormGroup {
    return this.fb.group({
      path: [op?.path ?? '', [Validators.required, Validators.pattern(/^\/.*$/)]],
      method: [op?.method ?? 'GET', Validators.required],
    });
  }

  addOperation(): void {
    this.operationsArray.push(this.createOperationGroup());
    this.isFiltered = true;
  }

  removeOperation(index: number): void {
    this.operationsArray.removeAt(index);
    if (this.operationsArray.length === 0) {
      this.isFiltered = false;
    }
  }

  allowAll(): void {
    this.operationsArray.clear();
    this.isFiltered = false;
  }

  save(): void {
    if (!this.isFiltered || this.operationsArray.length === 0) {
      this.dialogRef.close({ action: 'save', operations: null });
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const operations: ApiProductOperation[] = this.operationsArray.value;
    this.dialogRef.close({ action: 'save', operations });
  }

  cancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }
}
