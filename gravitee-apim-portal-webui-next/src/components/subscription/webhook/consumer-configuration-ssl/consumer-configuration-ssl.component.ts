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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { tap } from 'rxjs';

import { SslKeyStoreComponent } from './components/ssl-keystore/ssl-key-store.component';
import { SslTrustStoreComponent } from './components/ssl-truststore/ssl-trust-store.component';
import { SslKeyStore, SslTrustStore } from '../../../../entities/ssl';
import { SslOptions } from '../../../../entities/subscription';
import { AccordionModule } from '../../../accordion/accordion.module';

@Component({
  selector: 'app-consumer-configuration-ssl',
  standalone: true,
  imports: [
    MatSlideToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,
    CdkAccordionModule,
    MatIconModule,
    SslTrustStoreComponent,
    SslKeyStoreComponent,
    AccordionModule,
  ],
  templateUrl: 'consumer-configuration-ssl.component.html',
  styleUrls: ['consumer-configuration-ssl.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => ConsumerConfigurationSslComponent),
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConsumerConfigurationSslComponent),
      multi: true,
    },
  ],
})
export class ConsumerConfigurationSslComponent implements ControlValueAccessor, Validator, AfterViewInit {
  isDisabled = false;
  sslForm = new FormGroup({
    hostnameVerifier: new FormControl<boolean>(false, { validators: [Validators.required], nonNullable: true }),
    trustAll: new FormControl<boolean>(false),
    trustStore: new FormControl<SslTrustStore | null>(null),
    keyStore: new FormControl<SslKeyStore | null>(null),
  });
  private destroyRef = inject(DestroyRef);

  constructor() {
    this.sslForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(values => {
      this._onChange(values as SslOptions);
    });

    this.sslForm.controls.trustAll.valueChanges
      .pipe(
        tap(trustAll => {
          if (trustAll) {
            this.sslForm.controls.trustStore.reset({ type: '' }, { emitEvent: false });
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngAfterViewInit() {
    this.sslForm.patchValue({});
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.sslForm.valid ? null : { invalidSsl: true };
  }

  writeValue(value: SslOptions): void {
    this.sslForm.patchValue(value);
  }

  registerOnChange(fn: (value: SslOptions) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.sslForm.disable({ emitEvent: false }) : this.sslForm.enable({ emitEvent: false });
  }

  private _onChange: (value: SslOptions) => void = () => ({});
  private _onTouched: () => void = () => ({});
}
