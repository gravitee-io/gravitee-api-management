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

import { Component, DoCheck, Input, OnDestroy, OnInit, forwardRef } from '@angular/core';
import {
  AbstractControl,
  ControlContainer,
  ControlValueAccessor,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { isEmpty } from 'lodash';
import { filter, map, takeUntil } from 'rxjs/operators';

import { JKSKeyStore, PEMKeyStore, PKCS12KeyStore, KeyStore } from '../../../../../entities/management-api-v2';

type InternalFormValue = {
  type: KeyStore['type'];

  jksPath?: string;
  jksContent?: string;
  jksPassword?: string;

  pkcs12Path?: string;
  pkcs12Content?: string;
  pkcs12Password?: string;

  pemKeyPath?: string;
  pemKeyContent?: string;
  pemCertPath?: string;
  pemCertContent?: string;
};

export const KEYSTORE_TYPE_LABELS: { label: string; value: KeyStore['type'] }[] = [
  {
    label: 'None',
    value: 'NONE',
  },
  {
    label: 'Java Trust Store (.jks)',
    value: 'JKS',
  },
  {
    label: 'PKCS#12 (.p12) / PFX (.pfx)',
    value: 'PKCS12',
  },
  {
    label: 'PEM (.pem)',
    value: 'PEM',
  },
];

@Component({
  selector: 'ssl-keystore-form',
  templateUrl: './ssl-keystore-form.component.html',
  styleUrls: ['./ssl-keystore-form.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => SslKeyStoreFormComponent),
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SslKeyStoreFormComponent),
      multi: true,
    },
  ],
})
export class SslKeyStoreFormComponent implements OnInit, DoCheck, OnDestroy, ControlValueAccessor, Validator {
  private unsubscribe$ = new Subject<boolean>();

  private _onChange: (value: KeyStore) => void = () => ({});

  private _onTouched: () => void = () => ({});

  private parentIsTouched = false;

  @Input() formControlName!: string;
  @Input() formGroupName!: string;

  public types = KEYSTORE_TYPE_LABELS;

  public internalFormGroup = new UntypedFormGroup({
    // Base fields
    type: new UntypedFormControl('NONE'),

    // JKS fields
    jksPath: new UntypedFormControl(),
    jksContent: new UntypedFormControl(),
    jksPassword: new UntypedFormControl(),

    // PKCS12 fields
    pkcs12Password: new UntypedFormControl(),
    pkcs12Path: new UntypedFormControl(),
    pkcs12Content: new UntypedFormControl(),

    // PEM fields
    pemKeyPath: new UntypedFormControl(),
    pemKeyContent: new UntypedFormControl(),
    pemCertPath: new UntypedFormControl(),
    pemCertContent: new UntypedFormControl(),
  });
  public isDisabled = false;

  constructor(private controlContainer: ControlContainer) {}

  ngOnInit(): void {
    this.internalFormGroup
      .get('type')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        // Clear all validators
        this.internalFormGroup.get('jksPassword').clearValidators();
        this.internalFormGroup.get('pkcs12Password').clearValidators();
        this.internalFormGroup.clearValidators();

        switch (type) {
          case 'JKS': {
            this.internalFormGroup.get('jksPassword').setValidators([Validators.required]);
            this.internalFormGroup.setValidators([pathOrContentRequired('jksPath', 'jksContent')]);
            break;
          }
          case 'PKCS12': {
            this.internalFormGroup.get('pkcs12Password').setValidators([Validators.required]);
            this.internalFormGroup.setValidators([pathOrContentRequired('pkcs12Path', 'pkcs12Content')]);
            break;
          }
          case 'PEM': {
            this.internalFormGroup.setValidators([
              pathOrContentRequired('pemKeyPath', 'pemKeyContent'),
              pathOrContentRequired('pemCertPath', 'pemCertContent'),
            ]);
            break;
          }
        }

        // Update validators
        Object.keys(this.internalFormGroup.controls).forEach((controlName) => {
          this.internalFormGroup.get(controlName)?.updateValueAndValidity({ emitEvent: false });
        });
        this.internalFormGroup.updateValueAndValidity();
      });

    this.internalFormGroup.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
      this._onChange(internalFormValueToKeyStore(value));
    });

    this.internalFormGroup.statusChanges
      .pipe(
        map(() => this.internalFormGroup?.touched),
        filter((touched) => touched === true && this.isDisabled === false),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this._onTouched();
      });
  }

  ngDoCheck() {
    const parentFormControl = this.controlContainer?.control?.get(this.formControlName);
    if (parentFormControl) {
      // If parent form control is touched, mark all internal fields as touched
      if (parentFormControl.touched && !this.parentIsTouched) {
        this.internalFormGroup.markAllAsTouched();
      }
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  registerOnChange(fn: (value: KeyStore) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.internalFormGroup.disable({ emitEvent: false }) : this.internalFormGroup.enable({ emitEvent: false });
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.internalFormGroup.valid
      ? null
      : {
          invalidKeyStore: true,
        };
  }

  writeValue(value: KeyStore): void {
    this.internalFormGroup.reset(keyStoreTypeToInternalFormValue(value));
  }
}

const pathOrContentRequired: (pathControlName: string, contentControlName: string) => ValidatorFn = (
  pathControlName: string,
  contentControlName: string,
) => {
  return (formGroup: UntypedFormGroup): ValidationErrors | null => {
    const pathControl = formGroup.get(pathControlName);
    const contentControl = formGroup.get(contentControlName);

    if (isEmpty(pathControl.value) && isEmpty(contentControl.value)) {
      pathControl.setErrors({ pathOrContentRequired: true }, { emitEvent: false });
      contentControl.setErrors({ pathOrContentRequired: true }, { emitEvent: false });
      return { pathOrContentRequired: true };
    }

    pathControl.setErrors(null, { emitEvent: false });
    contentControl.setErrors(null, { emitEvent: false });
    return null;
  };
};

const keyStoreTypeToInternalFormValue: (keyStore?: KeyStore) => InternalFormValue = (trustStore) => {
  switch (trustStore?.type) {
    case 'JKS': {
      const jksKeyStore = trustStore as JKSKeyStore;
      return {
        type: trustStore.type,
        jksPath: jksKeyStore.path,
        jksContent: jksKeyStore.content,
        jksPassword: jksKeyStore.password,
      };
    }
    case 'PKCS12': {
      const pkcs12KeyStore = trustStore as PKCS12KeyStore;
      return {
        type: trustStore.type,
        pkcs12Path: pkcs12KeyStore.path,
        pkcs12Content: pkcs12KeyStore.content,
        pkcs12Password: pkcs12KeyStore.password,
      };
    }
    case 'PEM': {
      const pemKeyStore = trustStore as PEMKeyStore;
      return {
        type: trustStore.type,
        pemKeyPath: pemKeyStore.keyPath,
        pemKeyContent: pemKeyStore.keyContent,
        pemCertPath: pemKeyStore.certPath,
        pemCertContent: pemKeyStore.certContent,
      };
    }
    default: {
      return {
        type: 'NONE',
      };
    }
  }
};

const internalFormValueToKeyStore: (internalFormValue: InternalFormValue) => KeyStore = (internalFormValue) => {
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
        keyPath: internalFormValue.pemKeyPath,
        keyContent: internalFormValue.pemKeyContent,
        certPath: internalFormValue.pemCertPath,
        certContent: internalFormValue.pemCertContent,
      };
    default:
      return {
        type: internalFormValue.type,
      };
  }
};
