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
import { Component, DestroyRef, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Moment } from 'moment';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';

import { DEFAULT_PERIOD, PERIODS, SimpleFilter } from '../../../../../runtime-logs/models';
import { DATE_TIME_FORMATS } from '../../../../../../../../shared/utils/timeFrameRanges';
import { WebhookMoreFiltersForm } from '../../../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-more-filters-form',
  templateUrl: './webhook-logs-more-filters-form.component.html',
  styleUrls: ['./webhook-logs-more-filters-form.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatIconModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
  ],
  providers: [
    { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WebhookLogsMoreFiltersFormComponent),
      multi: true,
    },
  ],
})
export class WebhookLogsMoreFiltersFormComponent implements ControlValueAccessor {
  @Input() callbackUrls: string[] = [];

  readonly periods = PERIODS;
  disabled = false;
  minDate: Moment | null = null;

  form: FormGroup<{
    period: FormControl<SimpleFilter | null>;
    from: FormControl<Moment | null>;
    to: FormControl<Moment | null>;
    callbackUrls: FormControl<string[]>;
  }>;

  private onChange: (value: WebhookMoreFiltersForm) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    this.form = this.fb.group({
      period: this.fb.control<SimpleFilter | null>(DEFAULT_PERIOD),
      from: this.fb.control<Moment | null>(null),
      to: this.fb.control<Moment | null>(null),
      callbackUrls: this.fb.control<string[]>([], { nonNullable: true }),
    });

    // Subscribe to form value changes and notify parent
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const value = this.getFormValue();
      this.onChange(value);
    });

    // Handle period changes - reset dates when period is selected
    this.form.controls.period.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.controls.from.setValue(null, { emitEvent: false, onlySelf: true });
      this.form.controls.to.setValue(null, { emitEvent: false, onlySelf: true });
      this.minDate = null;
    });

    // Handle from date changes - update minDate for 'to' date picker and reset period
    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((from) => {
      this.minDate = from ?? null;
      this.form.controls.period.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
    });

    // Handle to date changes - reset period
    this.form.controls.to.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.controls.period.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
    });
  }

  // ControlValueAccessor implementation
  writeValue(obj: WebhookMoreFiltersForm | null): void {
    const value: WebhookMoreFiltersForm = obj ?? {
      period: DEFAULT_PERIOD,
      from: null,
      to: null,
      callbackUrls: [],
    };

    this.form.patchValue(
      {
        period: value.period ?? DEFAULT_PERIOD,
        from: value.from ?? null,
        to: value.to ?? null,
        callbackUrls: value.callbackUrls ?? [],
      },
      { emitEvent: false },
    );
    this.minDate = value.from ?? null;
  }

  registerOnChange(fn: (value: WebhookMoreFiltersForm) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.form.disable({ emitEvent: false });
    } else {
      this.form.enable({ emitEvent: false });
    }
  }

  private getFormValue(): WebhookMoreFiltersForm {
    const rawValue = this.form.getRawValue();
    return {
      period: rawValue.period ?? undefined,
      from: rawValue.from !== undefined ? rawValue.from : null,
      to: rawValue.to !== undefined ? rawValue.to : null,
      callbackUrls: rawValue.callbackUrls ?? [],
    };
  }
}
