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
import { Component, input, output, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';

import { dateRangeGroupValidator } from './utils/date-range.validator';
import { DATE_TIME_FORMATS } from './utils/timeframe-ranges';

export interface TimeframeValue {
  period: string;
  from: Moment | null;
  to: Moment | null;
}

@Component({
  selector: 'gd-timeframe-selector',
  imports: [
    MatButton,
    MatError,
    MatFormField,
    MatIcon,
    MatIconButton,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    MatSuffix,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    ReactiveFormsModule,
  ],
  providers: [
    { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: TimeframeSelectorComponent,
      multi: true,
    },
  ],
  templateUrl: './timeframe-selector.component.html',
  styleUrl: './timeframe-selector.component.scss',
})
export class TimeframeSelectorComponent implements ControlValueAccessor {
  timeFrames = input.required<{ id: string; label: string }[]>();
  customPeriod = input<string>('custom');
  defaultPeriod = input<string>('1h');

  apply = output<void>();
  refresh = output<void>();

  form: FormGroup<{ period: FormControl<string>; from: FormControl<Moment | null>; to: FormControl<Moment | null> }>;
  disabled = false;
  minDate: Moment | null = null;
  nowDate: Moment = moment().add(1, 'd');

  constructor(
    private readonly fb: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    this.form = this.fb.group(
      {
        period: this.fb.control<string>(this.defaultPeriod(), { nonNullable: true }),
        from: this.fb.control<Moment | null>(null),
        to: this.fb.control<Moment | null>(null),
      },
      { validators: dateRangeGroupValidator() },
    );

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      this.onChange({ period: value.period ?? '', from: value.from ?? null, to: value.to ?? null });
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(from => {
      this.minDate = from ?? null;
      this.form.updateValueAndValidity({ onlySelf: false, emitEvent: false });
    });
  }

  get value() {
    const v = this.form.value;
    return { period: v.period ?? '', from: v.from ?? null, to: v.to ?? null };
  }

  writeValue(obj: TimeframeValue | null): void {
    const val: TimeframeValue = obj ?? { period: this.defaultPeriod(), from: null, to: null };
    this.form.setValue({ period: val.period ?? this.defaultPeriod(), from: val.from ?? null, to: val.to ?? null }, { emitEvent: false });
    this.minDate = val.from ?? null;
  }

  registerOnChange(fn: (value: TimeframeValue) => void): void {
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

  onPeriodChange(period: string) {
    this.form.controls.period.setValue(period);
    this.onTouched();
  }

  onFromChange(date: Moment | null) {
    this.form.controls.from.setValue(date);
  }

  onToChange(date: Moment | null) {
    this.form.controls.to.setValue(date);
  }

  onApplyClicked() {
    this.apply.emit();
  }

  onRefreshClicked() {
    this.refresh.emit();
  }

  private onChange: (value: TimeframeValue) => void = () => {};
  private onTouched: () => void = () => {};
}
