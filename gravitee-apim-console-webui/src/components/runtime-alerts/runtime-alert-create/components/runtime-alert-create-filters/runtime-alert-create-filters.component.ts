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
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { Metrics, Scope } from '../../../../../entities/alert';
import { Rule } from '../../../../../entities/alerts/rule.metrics';
import { ApiMetrics } from '../../../../../entities/alerts/api.metrics';
import { HealthcheckMetrics } from '../../../../../entities/alerts/healthcheck.metrics';

@Component({
  selector: 'runtime-alert-create-filters',
  templateUrl: './runtime-alert-create-filters.component.html',
  styleUrls: ['./runtime-alert-create-filters.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RuntimeAlertCreateFiltersComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RuntimeAlertCreateFiltersComponent),
      multi: true,
    },
  ],
})
export class RuntimeAlertCreateFiltersComponent implements OnDestroy, ControlValueAccessor, Validator {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private _onChange: (value: unknown) => void = () => ({});
  private _onTouched: () => void = () => ({});

  @Input({ required: true }) referenceType: Scope;
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) set rule(value: Rule) {
    this.selectedRule = value;
    this.removeAllControls();
    if (value) {
      // Metrics are depending on the source of the trigger
      if (value.source === 'REQUEST') {
        this.metrics = Metrics.filterByScope(ApiMetrics.METRICS, this.referenceType);
      } else if (value.source === 'ENDPOINT_HEALTH_CHECK') {
        this.metrics = Metrics.filterByScope(HealthcheckMetrics.METRICS, this.referenceType);
      }
    }
  }

  protected selectedRule: Rule;
  protected metrics: Metrics[];
  protected types: string[];
  protected form: FormArray = new FormArray([]);

  constructor() {
    this.form.valueChanges
      .pipe(
        tap((value) => {
          this._onChange(value);
          this._onTouched();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addControl() {
    this.form.push(
      new FormGroup({
        metric: new FormControl<Metrics>(null, [Validators.required]),
        type: new FormControl<string>({ value: null, disabled: true }, [Validators.required]),
      }),
    );
  }

  removeControl(index: number) {
    this.form.removeAt(index);
  }

  writeValue(value: any): void {
    if (value) {
      this.form.setValue(value, { emitEvent: false });
    }
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.form.disable() : this.form.enable();
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.form.valid ? null : { invalidForm: { valid: false, message: 'Filters form is invalid' } };
  }

  private removeAllControls() {
    while (this.form.length !== 0) {
      this.form.removeAt(0);
    }
  }
}
