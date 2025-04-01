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
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { Rule } from '../../../../../entities/alerts/rule.metrics';
import { Scope } from '../../../../../entities/alert';
import { ALERT_SEVERITIES, AlertSeverity } from '../../../../../entities/alerts/alertTriggerEntity';

export type GeneralFormValue = {
  name: string;
  enabled: boolean;
  rule: Rule;
  severity: AlertSeverity;
  description: string;
};

@Component({
  selector: 'runtime-alert-create-general',
  templateUrl: './runtime-alert-create-general.component.html',
  styleUrls: ['./runtime-alert-create-general.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RuntimeAlertCreateGeneralComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RuntimeAlertCreateGeneralComponent),
      multi: true,
    },
  ],
})
export class RuntimeAlertCreateGeneralComponent implements OnDestroy, ControlValueAccessor, Validator {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private _onChange: (value: GeneralFormValue) => void = () => ({});
  private _onTouched: () => void = () => ({});

  @Input({ required: true }) set referenceType(value: Scope) {
    this.rules = Rule.findByScope(value);
  }
  protected form!: FormGroup;
  protected rules: Rule[];
  protected alertSeverities = ALERT_SEVERITIES;

  constructor() {
    this.form = new FormGroup({
      name: new FormControl<string>(null, [Validators.required]),
      enabled: new FormControl<boolean>(false),
      rule: new FormControl<Rule>(null, [Validators.required]),
      severity: new FormControl<AlertSeverity>('INFO', [Validators.required, Validators.max(256)]),
      description: new FormControl<string>(null),
    });
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

  writeValue(value: GeneralFormValue): void {
    if (value) {
      this.form.setValue(value, { emitEvent: false });
    }
  }

  registerOnChange(fn: (value: GeneralFormValue) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.form.disable() : this.form.enable();
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.form.valid ? null : { invalidForm: { valid: false, message: 'General form is invalid' } };
  }
}
