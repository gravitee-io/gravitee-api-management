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

import { MaskingType, RedactionRule } from '../../../../../../entities/management-api-v2';

export type RedactionRuleDialogData = {
  rule: (RedactionRule & { _id: string }) | null;
  existingPatterns: string[];
};

export type RedactionRuleDialogResult = RedactionRule | undefined;

type DialogFormControls = {
  attributeNamePattern: FormControl<string>;
  maskingType: FormControl<MaskingType>;
  fullReplacement: FormControl<string>;
  prefixLength: FormControl<number>;
  suffixLength: FormControl<number>;
  maskChar: FormControl<string>;
  valuePattern: FormControl<string>;
};

const PREVIEW_SAMPLE = 'ABCDEFGHIJ1234';

@Component({
  selector: 'redaction-rule-dialog',
  templateUrl: './redaction-rule-dialog.component.html',
  styleUrls: ['./redaction-rule-dialog.component.scss'],
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
export class RedactionRuleDialogComponent {
  private readonly dialogRef = inject<MatDialogRef<RedactionRuleDialogComponent, RedactionRuleDialogResult>>(MatDialogRef);
  protected readonly data = inject<RedactionRuleDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.rule !== null;

  protected readonly form = new FormGroup<DialogFormControls>({
    attributeNamePattern: new FormControl<string>(this.data.rule?.attributeNamePattern ?? '', {
      nonNullable: true,
      validators: [Validators.required, control => (this.data.existingPatterns.includes(control.value) ? { duplicate: true } : null)],
    }),
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
    valuePattern: new FormControl<string>(this.data.rule?.valuePattern ?? '', { nonNullable: true }),
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

    const { attributeNamePattern, maskingType, fullReplacement, prefixLength, suffixLength, maskChar, valuePattern } =
      this.form.getRawValue();

    const rule: RedactionRule = { attributeNamePattern };

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

    if (valuePattern) {
      rule.valuePattern = valuePattern;
    }

    this.dialogRef.close(rule);
  }

  onCancel(): void {
    this.dialogRef.close(undefined);
  }
}
