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

import { KEYSTORE_TYPE_LABELS, SslKeyStoreFormValue } from './ssl-key-store.model';
import { MobileClassDirective } from '../../../../../../directives/mobile-class.directive';
import { JKSKeyStore, PEMKeyStore, PKCS12KeyStore, SslKeyStore } from '../../../../../../entities/ssl';
import { pathOrContentRequired } from '../validators/ssl-trust-store.validators';

@Component({
  selector: 'app-ssl-keystore',
  templateUrl: './ssl-key-store.component.html',
  styleUrls: ['./ssl-key-store.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => SslKeyStoreComponent),
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SslKeyStoreComponent),
      multi: true,
    },
  ],
  standalone: true,
  imports: [MatFormFieldModule, ReactiveFormsModule, MatInputModule, MatSelectModule, MobileClassDirective],
})
export class SslKeyStoreComponent implements OnInit, ControlValueAccessor, Validator {
  public types = KEYSTORE_TYPE_LABELS;
  public keyStoreForm = new FormGroup({
    type: new FormControl('', { nonNullable: true }),

    // JKS fields
    jksPath: new FormControl(),
    jksContent: new FormControl(),
    jksPassword: new FormControl(),
    jksAlias: new FormControl(),
    jksKeyPassword: new FormControl(),

    // PKCS12 fields
    pkcs12Password: new FormControl(),
    pkcs12Path: new FormControl(),
    pkcs12Content: new FormControl(),
    pkcs12Alias: new FormControl(),
    pkcs12KeyPassword: new FormControl(),

    // PEM fields
    pemKeyPath: new FormControl(),
    pemKeyContent: new FormControl(),
    pemCertPath: new FormControl(),
    pemCertContent: new FormControl(),
  });
  isDisabled = false;
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.keyStoreForm.controls.type.valueChanges
      .pipe(
        tap(type => {
          this.updateValidators(type);
          this.updateValueAndValidity();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.keyStoreForm.valueChanges
      .pipe(
        tap(values => this._onChange(this.toSslKeyStore(values as SslKeyStoreFormValue))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  registerOnChange(fn: (value: SslKeyStore) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.keyStoreForm.disable({ emitEvent: false }) : this.keyStoreForm.enable({ emitEvent: false });
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.keyStoreForm.valid
      ? null
      : {
          invalidKeyStore: true,
        };
  }

  writeValue(value: SslKeyStore): void {
    this.keyStoreForm.patchValue(this.toSslKeyStoreFormValue(value));
  }

  private updateValueAndValidity() {
    Object.keys(this.keyStoreForm.controls).forEach(controlName => {
      this.keyStoreForm.get(controlName)?.updateValueAndValidity({ emitEvent: false });
    });
    this.keyStoreForm.updateValueAndValidity();
  }

  private toSslKeyStoreFormValue(keyStore?: SslKeyStore): SslKeyStoreFormValue {
    switch (keyStore?.type) {
      case 'JKS': {
        const jksKeyStore = keyStore as JKSKeyStore;
        return {
          type: keyStore.type,
          jksPath: jksKeyStore.path,
          jksContent: jksKeyStore.content,
          jksPassword: jksKeyStore.password,
          jksAlias: jksKeyStore.alias,
          jksKeyPassword: jksKeyStore.keyPassword,
        };
      }
      case 'PKCS12': {
        const pkcs12KeyStore = keyStore as PKCS12KeyStore;
        return {
          type: keyStore.type,
          pkcs12Path: pkcs12KeyStore.path,
          pkcs12Content: pkcs12KeyStore.content,
          pkcs12Password: pkcs12KeyStore.password,
          pkcs12Alias: pkcs12KeyStore.alias,
          pkcs12KeyPassword: pkcs12KeyStore.keyPassword,
        };
      }
      case 'PEM': {
        const pemKeyStore = keyStore as PEMKeyStore;
        return {
          type: keyStore.type,
          pemKeyPath: pemKeyStore.keyPath,
          pemKeyContent: pemKeyStore.keyContent,
          pemCertPath: pemKeyStore.certPath,
          pemCertContent: pemKeyStore.certContent,
        };
      }
      default: {
        return {
          type: '',
        };
      }
    }
  }

  private toSslKeyStore(values: SslKeyStoreFormValue): SslKeyStore {
    switch (values.type) {
      case 'JKS':
        return {
          type: values.type,
          path: values.jksPath,
          content: values.jksContent,
          password: values.jksPassword,
          alias: values.jksAlias,
          keyPassword: values.jksKeyPassword,
        };
      case 'PKCS12':
        return {
          type: values.type,
          path: values.pkcs12Path,
          content: values.pkcs12Content,
          password: values.pkcs12Password,
          alias: values.pkcs12Alias,
          keyPassword: values.pkcs12KeyPassword,
        };
      case 'PEM':
        return {
          type: values.type,
          keyPath: values.pemKeyPath,
          keyContent: values.pemKeyContent,
          certPath: values.pemCertPath,
          certContent: values.pemCertContent,
        };
      default:
        return {
          type: values.type,
        };
    }
  }

  private updateValidators(type: string) {
    this.keyStoreForm.controls.jksPassword.clearValidators();
    this.keyStoreForm.controls.pkcs12Password.clearValidators();
    this.keyStoreForm.clearValidators();

    switch (type) {
      case 'JKS': {
        this.keyStoreForm.controls.jksPassword.setValidators([Validators.required]);
        this.keyStoreForm.setValidators([pathOrContentRequired('jksPath', 'jksContent')]);
        break;
      }
      case 'PKCS12': {
        this.keyStoreForm.controls.pkcs12Password.setValidators([Validators.required]);
        this.keyStoreForm.setValidators([pathOrContentRequired('pkcs12Path', 'pkcs12Content')]);
        break;
      }
      case 'PEM': {
        this.keyStoreForm.setValidators([
          pathOrContentRequired('pemKeyPath', 'pemKeyContent'),
          pathOrContentRequired('pemCertPath', 'pemCertContent'),
        ]);
        break;
      }
    }
  }

  private _onChange: (value: SslKeyStore) => void = () => ({});
  private _onTouched: () => void = () => ({});
}
