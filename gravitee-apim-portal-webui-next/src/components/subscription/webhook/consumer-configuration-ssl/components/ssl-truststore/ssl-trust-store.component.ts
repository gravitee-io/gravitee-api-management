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

import { Component, DestroyRef, forwardRef, inject, OnInit } from '@angular/core';
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
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { tap } from 'rxjs';

import { SslTruststoreFormValue, TRUSTSTORE_TYPE_LABELS } from './ssl-trust-store.model';
import { MobileClassDirective } from '../../../../../../directives/mobile-class.directive';
import { JKSTrustStore, PEMTrustStore, PKCS12TrustStore, SslTrustStore, SslTrustStoreType } from '../../../../../../entities/ssl';
import { pathOrContentRequired } from '../validators/ssl-trust-store.validators';

@Component({
  selector: 'app-ssl-truststore',
  templateUrl: './ssl-trust-store.component.html',
  styleUrls: ['./ssl-trust-store.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => SslTrustStoreComponent),
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SslTrustStoreComponent),
      multi: true,
    },
  ],
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatSelectModule, MatInputModule, MobileClassDirective],
})
export class SslTrustStoreComponent implements OnInit, ControlValueAccessor, Validator {
  types = TRUSTSTORE_TYPE_LABELS;
  isDisabled = false;
  trustStoreForm = new FormGroup({
    type: new FormControl<SslTrustStoreType>('', { nonNullable: true }),

    // JKS fields
    jksPath: new FormControl(),
    jksContent: new FormControl(),
    jksPassword: new FormControl(),

    // PKCS12 fields
    pkcs12Path: new FormControl(),
    pkcs12Content: new FormControl(),
    pkcs12Password: new FormControl(),

    // PEM fields
    pemPath: new FormControl(),
    pemContent: new FormControl(),
  });

  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.trustStoreForm.controls.type.valueChanges
      .pipe(
        tap(type => {
          this.updateValidators(type);
          this.updateValueAndValidity();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.trustStoreForm.valueChanges
      .pipe(
        tap(values => this._onChange(this.toTruststore(values as SslTrustStore))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  registerOnChange(fn: (value: SslTrustStore) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.trustStoreForm.disable({ emitEvent: false }) : this.trustStoreForm.enable({ emitEvent: false });
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.trustStoreForm.valid ? null : { invalidTrustStore: true };
  }

  writeValue(value: SslTrustStore): void {
    this.trustStoreForm.patchValue(this.toFormValue(value));
  }

  private toFormValue(trustStore?: SslTrustStore): SslTruststoreFormValue {
    switch (trustStore?.type) {
      case 'JKS': {
        const jksTrustStore = trustStore as JKSTrustStore;
        return {
          type: trustStore.type,
          jksPath: jksTrustStore.path,
          jksContent: jksTrustStore.content,
          jksPassword: jksTrustStore.password,
        };
      }
      case 'PKCS12': {
        const pkcs12TrustStore = trustStore as PKCS12TrustStore;
        return {
          type: trustStore.type,
          pkcs12Path: pkcs12TrustStore.path,
          pkcs12Content: pkcs12TrustStore.content,
          pkcs12Password: pkcs12TrustStore.password,
        };
      }
      case 'PEM': {
        const pemTrustStore = trustStore as PEMTrustStore;
        return {
          type: trustStore.type,
          pemPath: pemTrustStore.path,
          pemContent: pemTrustStore.content,
        };
      }
      default: {
        return {
          type: '',
        };
      }
    }
  }

  private toTruststore(internalFormValue: SslTruststoreFormValue): SslTrustStore {
    switch (internalFormValue.type) {
      case 'JKS':
        return {
          type: internalFormValue.type,
          path: internalFormValue.jksPath,
          content: internalFormValue.jksContent,
          password: internalFormValue.jksPassword,
        };
      case 'PKCS12':
        return {
          type: internalFormValue.type,
          path: internalFormValue.pkcs12Path,
          content: internalFormValue.pkcs12Content,
          password: internalFormValue.pkcs12Password,
        };
      case 'PEM':
        return {
          type: internalFormValue.type,
          path: internalFormValue.pemPath,
          content: internalFormValue.pemContent,
        };
      default:
        return {
          type: '' as SslTrustStoreType,
        };
    }
  }

  private updateValueAndValidity() {
    Object.keys(this.trustStoreForm.controls).forEach(controlName => {
      this.trustStoreForm.get(controlName)?.updateValueAndValidity({ emitEvent: false });
    });
    this.trustStoreForm.updateValueAndValidity();
  }

  private updateValidators(type: SslTrustStoreType) {
    this.trustStoreForm.clearValidators();
    this.trustStoreForm.controls.jksPassword.clearValidators();
    this.trustStoreForm.controls.pkcs12Password.clearValidators();

    switch (type) {
      case 'JKS': {
        this.trustStoreForm.controls.jksPassword.setValidators([Validators.required]);
        this.trustStoreForm.setValidators([pathOrContentRequired('jksPath', 'jksContent')]);
        break;
      }
      case 'PKCS12': {
        this.trustStoreForm.controls.pkcs12Password.setValidators([Validators.required]);
        this.trustStoreForm.setValidators([pathOrContentRequired('pkcs12Path', 'pkcs12Content')]);
        break;
      }
      case 'PEM': {
        this.trustStoreForm.setValidators([pathOrContentRequired('pemPath', 'pemContent')]);
        break;
      }
    }
  }

  private _onChange: (value: SslTrustStore) => void = () => ({});
  private _onTouched: () => void = () => ({});
}
