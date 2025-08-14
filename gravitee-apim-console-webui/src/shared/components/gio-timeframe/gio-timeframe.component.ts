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
import { Component, forwardRef, input, output, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  ControlValueAccessor,
  FormsModule,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';
import { MatRadioButton } from '@angular/material/radio';

import { DATE_TIME_FORMATS } from '../../utils/timeFrameRanges';

export interface GioTimeframeValue {
  period: string;
  from: Moment | null;
  to: Moment | null;
}

@Component({
  selector: 'gio-timeframe',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    GioIconsModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    MatRadioButton,
  ],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => GioTimeframeComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => GioTimeframeComponent), multi: true },
    { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
  ],
  templateUrl: './gio-timeframe.component.html',
  styleUrls: ['./gio-timeframe.component.scss'],
})
export class GioTimeframeComponent implements ControlValueAccessor, OnDestroy, Validator {
  timeFrames = input.required<{ id: string; label: string }[]>();
  customPeriod = input('custom');

  apply = output<void>();
  refresh = output<void>();

  value: GioTimeframeValue = { period: '', from: null, to: null };
  disabled = false;
  minDate: Moment | undefined;
  nowDate: Moment = moment().add(1, 'd');

  private onChange: (val: GioTimeframeValue) => void = () => {};
  private onTouched: () => void = () => {};

  validate(control: AbstractControl): ValidationErrors | null {
    const val: GioTimeframeValue = (control?.value as GioTimeframeValue) ?? this.value;
    const from = val?.from;
    const to = val?.to;
    if (from && to && from.isAfter(to)) {
      return { dateRange: true };
    }
    return null;
  }

  hasDateRangeError(): boolean {
    const from = this.value?.from;
    const to = this.value?.to;
    return !!(from && to && from.isAfter(to));
  }

  onPickerClosed() {
    // Defer cleanup to ensure it runs after CDK overlay finished its own teardown.
    setTimeout(() => this.clearCdkScrollblock(), 0);
  }

  private clearCdkScrollblock() {
    try {
      const targets: HTMLElement[] = [];
      if (document.body) targets.push(document.body);
      const htmlEl = document.documentElement as HTMLElement | null;
      if (htmlEl) targets.push(htmlEl);

      const stylesToClear = ['paddingRight', 'overflow', 'overflowY', 'position', 'top', 'width'] as const;

      targets.forEach((el) => {
        el.classList.remove('cdk-global-scrollblock');
        stylesToClear.forEach((prop) => {
          el.style[prop] = '';
        });
      });
    } catch {
      // no-op
    }
  }

  writeValue(obj: GioTimeframeValue | null): void {
    this.value = obj ?? { period: '', from: null, to: null };
    this.updateMin();
  }

  registerOnChange(fn: (val: GioTimeframeValue) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onPeriodChange(period: string) {
    this.value = { ...this.value, period };
    this.onChange(this.value);
    this.onTouched();
  }

  onFromChange(from: Moment | null) {
    this.value = { ...this.value, from };
    this.updateMin();
    this.onChange(this.value);
    this.onTouched();
  }

  onToChange(to: Moment | null) {
    this.value = { ...this.value, to };
    this.onChange(this.value);
    this.onTouched();
  }

  onApplyClicked() {
    this.apply.emit();
    // Ensure any leftover overlay scroll-block styles are cleared when applying.
    this.clearCdkScrollblock();
  }

  private updateMin() {
    this.minDate = this.value.from ?? undefined;
  }

  ngOnDestroy(): void {
    // Cleanup in case the component is destroyed while overlays were open
    this.clearCdkScrollblock();
  }
}
