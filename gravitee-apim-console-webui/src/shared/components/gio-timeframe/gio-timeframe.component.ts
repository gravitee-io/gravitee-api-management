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
import { Component, input, output, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DATE_TIME_FORMATS } from '../../utils/timeFrameRanges';
import { dateRangeGroupValidator } from '../../validators/date-range.validator';

interface TimeframeValue {
  period: string;
  from: Moment | null;
  to: Moment | null;
}

@Component({
  selector: 'gio-timeframe',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    GioIconsModule,
  ],
  providers: [
    { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: GioTimeframeComponent,
      multi: true,
    },
  ],
  templateUrl: './gio-timeframe.component.html',
  styleUrl: './gio-timeframe.component.scss',
})
export class GioTimeframeComponent implements ControlValueAccessor {
  timeFrames = input.required<{ id: string; label: string }[]>();
  customPeriod = input<string>('custom');

  apply = output<void>();
  refresh = output<void>();

  form: FormGroup<{ period: FormControl<string>; from: FormControl<Moment | null>; to: FormControl<Moment | null> }>;
  disabled = false;
  minDate: Moment | null = null;
  nowDate: Moment = moment().add(1, 'd');

  get value() {
    const v = this.form.value;
    return { period: v.period ?? '', from: v.from ?? null, to: v.to ?? null };
  }

  private onChange: (value: TimeframeValue) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    this.form = this.fb.group(
      {
        period: this.fb.control<string>(''),
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

  writeValue(obj: TimeframeValue | null): void {
    const val: TimeframeValue = obj ?? { period: '', from: null, to: null };
    this.form.setValue({ period: val.period ?? '', from: val.from ?? null, to: val.to ?? null }, { emitEvent: false });
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
}
