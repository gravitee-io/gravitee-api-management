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
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { RuntimeAlertCreateConditionsFactory } from './runtime-alert-create-conditions.factory';

import { Metrics, Scope } from '../../../../../entities/alert';
import { Rule } from '../../../../../entities/alerts/rule.metrics';

@Component({
  selector: 'runtime-alert-create-conditions',
  templateUrl: './runtime-alert-create-conditions.component.html',
  styleUrls: ['./runtime-alert-create-conditions.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RuntimeAlertCreateConditionsComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RuntimeAlertCreateConditionsComponent),
      multi: true,
    },
  ],
})
export class RuntimeAlertCreateConditionsComponent implements OnDestroy, ControlValueAccessor, Validator {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private _onChange: (value: unknown) => void;
  private _onTouched: () => void;

  @Input({ required: true }) referenceType: Scope;
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) set rule(value: Rule) {
    if (value) {
      this.ruleType = `${value.source}@${value.type}`;
      this.conditionsForm = RuntimeAlertCreateConditionsFactory.create(this.ruleType);

      // The control value accessor validity is calculated when the component is instantiated.
      // By default, it's considered as invalid because we want to link the alert to a condition on a metric.
      // After the selection of the Rule in the general form, the condition form is created.
      // We trigger the _onChange method to force the check of the form validity a first time.
      // For example, by default the endpoint health condition form is valid.
      this._onChange(this.conditionsForm.getRawValue());

      this.metrics = Metrics.filterByScope(Rule.findByScopeAndType(this.referenceType, this.ruleType)?.metrics ?? [], this.referenceType);
      this.conditionsForm.valueChanges
        .pipe(
          tap((value) => {
            this._onChange(value);
            this._onTouched();
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }
  }
  protected ruleType: string;
  protected metrics: Metrics[];
  protected types: string[];
  protected conditionsForm: FormGroup;

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  writeValue(value: unknown): void {
    this.conditionsForm?.setValue(value, { emitEvent: false });
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.conditionsForm?.disable() : this.conditionsForm?.enable();
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.conditionsForm?.valid ? null : { invalidForm: { valid: false, message: 'Conditions form is invalid' } };
  }
}
