/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { CdkAccordionModule } from '@angular/cdk/accordion';
import { AfterViewInit, Component, DestroyRef, forwardRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { MatOption } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';

import { RetryFormType, RetryFormValues } from './consumer-configuration-retry.model';
import { RetryOptions, RetryOptionsType, RetryStrategies, RetryStrategiesType } from '../../../../entities/subscription';
import { AccordionModule } from '../../../accordion/accordion.module';

@Component({
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatOption,
    MatSelect,
    CdkAccordionModule,
    MatIconModule,
    AccordionModule,
  ],
  selector: 'app-consumer-configuration-retry',
  standalone: true,
  templateUrl: './consumer-configuration-retry.component.html',
  styleUrls: ['./consumer-configuration-retry.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConsumerConfigurationRetryComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConsumerConfigurationRetryComponent),
      multi: true,
    },
  ],
})
export class ConsumerConfigurationRetryComponent implements ControlValueAccessor, Validator, AfterViewInit {
  defaultRetryOption: RetryOptionsType = 'No Retry';
  retryForm: RetryFormType = new FormGroup({
    retryOption: new FormControl<RetryOptionsType>(this.defaultRetryOption, { validators: [Validators.required], nonNullable: true }),
  });
  retryOptions = RetryOptions;
  retryStrategies = RetryStrategies;
  private destroyRef = inject(DestroyRef);

  constructor() {
    this.retryForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      this.updateFormControls();
      this._onChange(this.toRetryFormValues(value));
    });
  }

  ngAfterViewInit() {
    this.retryForm.patchValue({});
  }

  writeValue(value: RetryFormValues | null): void {
    if (!value) {
      this.retryForm.reset({ retryOption: this.defaultRetryOption }, { emitEvent: false });
      return;
    }

    this.retryForm.controls.retryOption.setValue(value.retryOption ?? this.defaultRetryOption, { emitEvent: false });
    this.updateFormControls();
    if (value.retryOption !== this.defaultRetryOption) {
      this.retryForm.patchValue(
        {
          retryStrategy: value.retryStrategy ?? 'LINEAR',
          maxAttempts: value.maxAttempts ?? null,
          initialDelaySeconds: value.initialDelaySeconds ?? null,
          maxDelaySeconds: value.maxDelaySeconds ?? null,
        },
        { emitEvent: false },
      );
    }
  }

  registerOnChange(fn: (value: RetryFormValues | null) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.retryForm.disable() : this.retryForm?.enable();
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.retryForm.valid ? null : { invalidForm: { valid: false, message: 'Retry form is invalid' } };
  }

  private updateFormControls(): void {
    if (this.retryForm.controls.retryOption.value !== this.defaultRetryOption) {
      this.retryForm.addControl('retryStrategy', new FormControl<RetryStrategiesType>('LINEAR', Validators.required), { emitEvent: false });
      this.retryForm.addControl('maxAttempts', new FormControl<number | null>(null, [Validators.required, Validators.min(1)]), {
        emitEvent: false,
      });
      this.retryForm.addControl('initialDelaySeconds', new FormControl<number | null>(null, [Validators.required, Validators.min(1)]), {
        emitEvent: false,
      });
      this.retryForm.addControl('maxDelaySeconds', new FormControl<number | null>(null, [Validators.required, Validators.min(1)]), {
        emitEvent: false,
      });
    } else {
      this.retryForm.removeControl('retryStrategy', { emitEvent: false });
      this.retryForm.removeControl('maxAttempts', { emitEvent: false });
      this.retryForm.removeControl('initialDelaySeconds', { emitEvent: false });
      this.retryForm.removeControl('maxDelaySeconds', { emitEvent: false });
    }
  }

  private toRetryFormValues(values: Partial<RetryFormValues>): RetryFormValues {
    return {
      retryOption: values.retryOption ?? this.defaultRetryOption,
      retryStrategy: values.retryStrategy ?? null,
      maxAttempts: values.maxAttempts ?? null,
      initialDelaySeconds: values.initialDelaySeconds ?? null,
      maxDelaySeconds: values.maxDelaySeconds ?? null,
    };
  }

  private _onTouched: () => void = () => ({});
  private _onChange: (value: RetryFormValues | null) => void = () => ({});
}
