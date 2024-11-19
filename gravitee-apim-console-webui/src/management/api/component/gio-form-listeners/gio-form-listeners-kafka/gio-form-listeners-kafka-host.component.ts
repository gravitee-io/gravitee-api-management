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
import { Component, DestroyRef, ElementRef, forwardRef, inject, Input, OnInit } from '@angular/core';
import {
  AsyncValidator,
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormControlStatus,
  FormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { isEmpty } from 'lodash';
import { filter, map, observeOn, startWith, take, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { asyncScheduler, Observable } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { KafkaHost } from '../../../../../entities/management-api-v2';
import { hostAsyncValidator } from '../../../../../shared/validators/host/host-async-validator.directive';
import { hostSyncValidator } from '../../../../../shared/validators/host/host-sync-validator.directive';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';

export type KafkaHostData = KafkaHost;

@Component({
  selector: 'gio-form-listeners-kafka-host',
  templateUrl: './gio-form-listeners-kafka-host.component.html',
  styleUrls: ['../gio-form-listeners.common.scss', './gio-form-listeners-kafka-host.component.scss'],
  imports: [MatInputModule, MatFormFieldModule, ReactiveFormsModule, MatIconModule, GioIconsModule, MatButtonModule, MatTooltipModule],
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormListenersKafkaHostComponent),
      multi: true,
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => GioFormListenersKafkaHostComponent),
      multi: true,
    },
  ],
})
export class GioFormListenersKafkaHostComponent implements OnInit, ControlValueAccessor, AsyncValidator {
  @Input()
  public apiId?: string;

  public host: KafkaHost;

  public hostFormControl: FormControl<string>;

  public mainForm: FormGroup<{ host: FormControl<string> }>;
  public isDisabled = false;

  private destroyRef = inject(DestroyRef);

  protected _onChange: (_listener: KafkaHostData) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
    private readonly apiV2Service: ApiV2Service,
    private readonly fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.hostFormControl = this.fb.control('', {
      validators: [hostSyncValidator, Validators.required],
      asyncValidators: [hostAsyncValidator(this.apiV2Service, this.apiId)],
    });

    this.mainForm = this.fb.group({
      host: this.hostFormControl,
    });

    this.mainForm.valueChanges
      .pipe(
        tap((value) => this._onChange({ host: value.host })),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.fm
      .monitor(this.elRef.nativeElement, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this._onTouched();
      });
  }

  // From ControlValueAccessor interface
  public writeValue(data: KafkaHostData | null = {}): void {
    if (!data || isEmpty(data)) {
      return;
    }

    this.host = data;
    this.initForm();
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (host: KafkaHostData | null) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(onTouched: () => void): void {
    this._onTouched = onTouched;
  }

  setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.mainForm.disable();
    } else {
      this.mainForm.enable();
    }
  }

  public validate(): Observable<ValidationErrors | null> {
    return this.validateHostFormControl().pipe(
      map(() => (this.mainForm.controls.host.valid ? null : { invalid: true })),
      take(1),
    );
  }

  protected getValue(): KafkaHost {
    const formData = this.mainForm.getRawValue();
    return { host: formData.host };
  }

  private initForm(): void {
    // Reset with current values
    this.mainForm.reset(this.host);

    // Update controls
    this.hostFormControl.updateValueAndValidity();
  }

  private validateHostFormControl(): Observable<FormControlStatus> {
    return this.hostFormControl.statusChanges.pipe(
      observeOn(asyncScheduler),
      startWith(this.hostFormControl.status),
      filter(() => !this.mainForm.controls.host.pending),
    );
  }
}
