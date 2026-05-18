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

import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { MaskingType, PayloadFormat, PayloadMaskingRule, PayloadPhase } from '../../../../../../entities/management-api-v2';

export type PayloadMaskingRuleDialogData = {
  rule: (PayloadMaskingRule & { _id: string }) | null;
};

export type PayloadMaskingRuleDialogResult = PayloadMaskingRule | undefined;

type DialogFormControls = {
  path: FormControl<string>;
  format: FormControl<PayloadFormat>;
  phase: FormControl<PayloadPhase>;
  maskingType: FormControl<MaskingType>;
  fullReplacement: FormControl<string>;
  prefixLength: FormControl<number>;
  suffixLength: FormControl<number>;
  maskChar: FormControl<string>;
};

const PREVIEW_SAMPLE = 'ABCDEFGHIJ1234';

@Component({
  selector: 'payload-masking-rule-dialog',
  templateUrl: './payload-masking-rule-dialog.component.html',
  styleUrls: ['./payload-masking-rule-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    GioIconsModule,
  ],
})
export class PayloadMaskingRuleDialogComponent {
  private readonly dialogRef = inject<MatDialogRef<PayloadMaskingRuleDialogComponent, PayloadMaskingRuleDialogResult>>(MatDialogRef);
  protected readonly data = inject<PayloadMaskingRuleDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.rule !== null;

  protected readonly form = new FormGroup<DialogFormControls>({
    path: new FormControl<string>(this.data.rule?.path ?? '', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    format: new FormControl<PayloadFormat>(this.data.rule?.format ?? 'AUTO', { nonNullable: true }),
    phase: new FormControl<PayloadPhase>(this.data.rule?.phase ?? 'BOTH', { nonNullable: true }),
    maskingType: new FormControl<MaskingType>((this.data.rule?.maskingStrategy?.type as MaskingType) ?? 'FULL', { nonNullable: true }),
    fullReplacement: new FormControl<string>(
      this.data.rule?.maskingStrategy?.type !== 'PARTIAL' ? (this.data.rule?.maskingStrategy?.replacement ?? '') : '',
      { nonNullable: true },
    ),
    prefixLength: new FormControl<number>(this.data.rule?.maskingStrategy?.prefixLength ?? 0, {
      nonNullable: true,
      validators: [Validators.min(0)],
    }),
    suffixLength: new FormControl<number>(this.data.rule?.maskingStrategy?.suffixLength ?? 0, {
      nonNullable: true,
      validators: [Validators.min(0)],
    }),
    maskChar: new FormControl<string>(
      this.data.rule?.maskingStrategy?.type === 'PARTIAL' ? (this.data.rule?.maskingStrategy?.replacement ?? '*') : '*',
      { nonNullable: true, validators: [Validators.maxLength(1)] },
    ),
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly isPartial = computed(() => this.formValue().maskingType === 'PARTIAL');

  protected readonly partialPreview = computed(() => {
    const { prefixLength, suffixLength, maskChar } = this.formValue();
    const prefix = Math.max(0, prefixLength ?? 0);
    const suffix = Math.max(0, suffixLength ?? 0);
    const char = maskChar || '*';
    const sample = PREVIEW_SAMPLE;
    const len = sample.length;
    if (prefix + suffix >= len) {
      return sample;
    }
    return sample.slice(0, prefix) + char.repeat(len - prefix - suffix) + sample.slice(len - suffix);
  });

  onSave(): void {
    if (this.form.invalid) return;

    const { path, format, phase, maskingType, fullReplacement, prefixLength, suffixLength, maskChar } = this.form.getRawValue();

    const rule: PayloadMaskingRule = { path, format, phase };

    if (maskingType === 'PARTIAL') {
      rule.maskingStrategy = {
        type: 'PARTIAL',
        prefixLength,
        suffixLength,
        replacement: maskChar || '*',
      };
    } else {
      rule.maskingStrategy = {
        type: 'FULL',
        ...(fullReplacement ? { replacement: fullReplacement } : {}),
      };
    }

    this.dialogRef.close(rule);
  }

  onCancel(): void {
    this.dialogRef.close(undefined);
  }
}
