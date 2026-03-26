/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { Component, computed, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

function notBlank(control: AbstractControl): ValidationErrors | null {
  const value: unknown = control.value;
  if (typeof value === 'string' && value.trim().length === 0) {
    return { blank: true };
  }
  return null;
}

type LabelGroup = FormGroup<{ key: FormControl<string>; value: FormControl<string> }>;

export interface DashboardMetadataDialogData {
  name: string;
  labels: Record<string, string>;
}

export interface DashboardMetadataDialogResult {
  name: string;
  labels: Record<string, string>;
}

@Component({
  selector: 'gd-dashboard-metadata-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './dashboard-metadata-dialog.component.html',
  styleUrl: './dashboard-metadata-dialog.component.scss',
})
export class DashboardMetadataDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<DashboardMetadataDialogComponent>);
  readonly data: DashboardMetadataDialogData = inject(MAT_DIALOG_DATA);

  readonly form = new FormGroup({
    name: new FormControl(this.data.name, { nonNullable: true, validators: [Validators.required, notBlank] }),
    labels: new FormArray<LabelGroup>(Object.entries(this.data.labels ?? {}).map(([key, value]) => this.buildLabelRow(key, value))),
  });

  get labelsArray(): FormArray<LabelGroup> {
    return this.form.controls.labels;
  }

  // Converts form value changes to a signal so computed() can react to them.
  private readonly formValue = toSignal(this.form.valueChanges.pipe(takeUntilDestroyed()), { initialValue: this.form.getRawValue() });

  readonly hasChanges = computed(() => {
    const value = this.formValue();

    if ((value.name ?? '').trim() !== this.data.name) return true;

    const original = this.data.labels ?? {};
    const originalKeys = Object.keys(original);
    const currentRows = value.labels ?? [];

    // Detect added/removed rows (even if still empty)
    if (currentRows.length !== originalKeys.length) return true;

    const currentLabels = this.labelsFromValue(currentRows);
    const currentKeys = Object.keys(currentLabels);

    if (originalKeys.length !== currentKeys.length) return true;
    return currentKeys.some(k => original[k] !== currentLabels[k]);
  });

  addLabel(): void {
    this.labelsArray.push(this.buildLabelRow('', ''));
  }

  removeLabel(index: number): void {
    this.labelsArray.removeAt(index);
  }

  save(): void {
    if (this.form.invalid || !this.hasChanges()) return;
    this.dialogRef.close({
      name: this.form.controls.name.value.trim(),
      labels: this.currentLabelsSnapshot(),
    } satisfies DashboardMetadataDialogResult);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private currentLabelsSnapshot(): Record<string, string> {
    const labels: Record<string, string> = {};
    this.labelsArray.controls.forEach(ctrl => {
      const key = ctrl.controls.key.value?.trim();
      const value = ctrl.controls.value.value?.trim();
      if (key) labels[key] = value ?? '';
    });
    return labels;
  }

  private labelsFromValue(rows: Array<{ key?: string; value?: string }>): Record<string, string> {
    const labels: Record<string, string> = {};
    rows.forEach(row => {
      const key = (row.key ?? '').trim();
      const value = (row.value ?? '').trim();
      if (key) labels[key] = value;
    });
    return labels;
  }

  private buildLabelRow(key: string, value: string): LabelGroup {
    return new FormGroup({
      key: new FormControl(key, { nonNullable: true, validators: [Validators.required, notBlank] }),
      value: new FormControl(value, { nonNullable: true, validators: [Validators.required, notBlank] }),
    });
  }
}
