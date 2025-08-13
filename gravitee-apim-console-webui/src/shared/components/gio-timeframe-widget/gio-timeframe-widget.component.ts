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
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';
import { MatRadioButton } from '@angular/material/radio';

import { customTimeFrames, DATE_TIME_FORMATS, timeFrames } from '../../utils/timeFrameRanges';

export interface ApiAnalyticsProxyFilters {
  period: string;
  from?: number | null;
  to?: number | null;
}

@Component({
  selector: 'gio-timeframe-widget',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    MatRadioButton,
  ],
  providers: [
    { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioTimeframeWidgetComponent),
      multi: true,
    },
  ],
  templateUrl: './gio-timeframe-widget.component.html',
  styleUrl: './gio-timeframe-widget.component.scss',
})
export class GioTimeframeWidgetComponent implements OnInit, ControlValueAccessor {
  @Input() activeFilters: ApiAnalyticsProxyFilters = { period: '1d' };
  @Input() refresh = () => {};

  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  protected readonly customPeriod = 'custom';

  // These will be bound from parent form controls
  @Input() periodControl: FormControl<string>;
  @Input() fromControl: FormControl<Moment | null>;
  @Input() toControl: FormControl<Moment | null>;

  minDate: Moment;
  nowDate: Moment = moment().add(1, 'd');

  private onChange = (_value: ApiAnalyticsProxyFilters) => {};
  private onTouched = () => {};

  ngOnInit() {
    // No form logic here - just UI setup
  }

  applyCustomTimeframe() {
    // Emit the custom timeframe value
    const from = this.fromControl.value?.valueOf();
    const to = this.toControl.value?.valueOf();

    if (from && to) {
      this.updateValue({
        ...this.activeFilters,
        from,
        to,
        period: this.customPeriod,
      });
    }
  }

  refreshFilters() {
    this.refresh();
  }

  // ControlValueAccessor implementation
  writeValue(value: ApiAnalyticsProxyFilters): void {
    if (value) {
      this.activeFilters = value;
    }
  }

  registerOnChange(fn: (value: ApiAnalyticsProxyFilters) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.periodControl?.disable();
      this.fromControl?.disable();
      this.toControl?.disable();
    } else {
      this.periodControl?.enable();
      this.fromControl?.enable();
      this.toControl?.enable();
    }
  }

  private updateValue(value: ApiAnalyticsProxyFilters): void {
    this.onChange(value);
    this.onTouched();
  }
}
