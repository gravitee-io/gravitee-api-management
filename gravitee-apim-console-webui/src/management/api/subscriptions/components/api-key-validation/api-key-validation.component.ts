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
import { Component, ElementRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  AsyncValidatorFn,
  ControlValueAccessor,
  UntypedFormControl,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { filter, map, startWith, take, takeUntil, tap } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';
import { FocusMonitor } from '@angular/cdk/a11y';

import { ApiSubscriptionV2Service } from '../../../../../services-ngx/api-subscription-v2.service';

@Component({
  selector: 'api-key-validation',
  templateUrl: './api-key-validation.component.html',
  styleUrls: ['./api-key-validation.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ApiKeyValidationComponent),
      multi: true,
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => ApiKeyValidationComponent),
      multi: true,
    },
  ],
})
export class ApiKeyValidationComponent implements OnInit, ControlValueAccessor, Validator {
  @Input()
  apiId: string;

  @Input()
  applicationId: string;

  public apiKeyFormControl = new UntypedFormControl('', [], [this.apiKeyAsyncValidator()]);
  private apiKey: string;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private _onChange: (_apiKey: string | null) => void = () => ({});
  private _onTouched: () => void = () => ({});

  constructor(
    private readonly apiSubscriptionService: ApiSubscriptionV2Service,
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
  ) {}

  ngOnInit() {
    this.apiKeyFormControl?.valueChanges
      .pipe(
        tap((apiKey) => this._onChange(apiKey)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.fm
      .monitor(this.elRef.nativeElement, true)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this._onTouched();
      });
  }

  // From ControlValueAccessor interface
  registerOnChange(fn: (apiKey: string | null) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor interface
  validate(_: AbstractControl): Observable<ValidationErrors | null> {
    return this.apiKeyFormControl.statusChanges.pipe(
      startWith(this.apiKeyFormControl.status),
      filter(() => !this.apiKeyFormControl.pending),
      map(() => (this.apiKeyFormControl.valid ? null : { invalid: true })),
      take(1),
    );
  }

  // From ControlValueAccessor interface
  writeValue(value: string | null): void {
    if (!value) {
      return;
    }
    this.apiKey = value;
    this.apiKeyFormControl.setValue(this.apiKey);
  }

  private apiKeyAsyncValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (control.value === '') {
        return of(null);
      }
      if (control.errors && !control.errors.unique) {
        return of(control.errors);
      }

      return this.apiSubscriptionService
        .verify(this.apiId, { applicationId: this.applicationId, apiKey: control.value })
        .pipe(map((isUnique) => (!isUnique.ok ? { unique: true } : null)));
    };
  }
}
