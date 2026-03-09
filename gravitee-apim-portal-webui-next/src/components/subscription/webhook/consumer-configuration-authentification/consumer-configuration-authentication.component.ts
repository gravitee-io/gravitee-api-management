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
import { AfterViewInit, Component, DestroyRef, forwardRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  FormGroup,
  FormsModule,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatError, MatFormFieldModule, MatHint } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { tap } from 'rxjs';

import { AUTHENTICATION_TYPES, AuthFormValue } from './consumer-configuration-authentication.model';
import {
  BasicAuthConfiguration,
  Oauth2AuthConfiguration,
  TokenAuthConfiguration,
  WebhookSubscriptionConfigurationAuth,
  WebhookSubscriptionConfigurationAuthType,
} from '../../../../entities/subscription';
import { AccordionModule } from '../../../accordion/accordion.module';

@Component({
  selector: 'app-consumer-configuration-authentication',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatError,
    MatHint,
    MatInputModule,
    AccordionModule,
    MatChipsModule,
    MatIcon,
  ],
  templateUrl: './consumer-configuration-authentication.component.html',
  styleUrls: ['consumer-configuration-authentication.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => ConsumerConfigurationAuthenticationComponent),
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConsumerConfigurationAuthenticationComponent),
      multi: true,
    },
  ],
})
export class ConsumerConfigurationAuthenticationComponent implements AfterViewInit, ControlValueAccessor, Validator {
  scopes = signal<string[]>([]);

  authForm = new FormGroup({
    type: new FormControl<WebhookSubscriptionConfigurationAuthType>('none', { nonNullable: true }),

    // for basic authentication
    username: new FormControl(),
    password: new FormControl(),

    // for token authentication
    token: new FormControl(),

    // for oauth2 authentication
    endpoint: new FormControl(),
    clientId: new FormControl(),
    clientSecret: new FormControl(),
    scopes: new FormControl(),
  });
  readonly types = AUTHENTICATION_TYPES;
  private readonly destroyRef = inject(DestroyRef);
  private isDisabled = false;

  constructor() {
    this.authForm.controls.type.valueChanges
      .pipe(
        tap(type => {
          this.updateValidators(type);
          this.updateValueAndValidity();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.authForm.valueChanges
      .pipe(
        tap(values => {
          this._onChange(this.toConfigurationAuthentication(values as AuthFormValue));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngAfterViewInit(): void {
    this.authForm.patchValue({});
    if (this.authForm.controls.type.value === 'oauth2' && this.authForm.controls.scopes.getRawValue()) {
      this.scopes.set(this.authForm.controls.scopes.getRawValue());
    }
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.authForm.valid ? null : { invalidAuth: true };
  }

  writeValue(value: WebhookSubscriptionConfigurationAuth): void {
    const formValue = this.toFormValue(value);
    this.authForm.patchValue(formValue, { emitEvent: false });

    if (formValue.type === 'oauth2' && formValue.scopes) {
      this.scopes.set([...formValue.scopes]);
    } else {
      this.scopes.set([]);
    }
  }

  registerOnChange(fn: (value: WebhookSubscriptionConfigurationAuth) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.authForm.disable({ emitEvent: false }) : this.authForm.enable({ emitEvent: false });
  }

  removeScope(scopeToRemove: string) {
    this.scopes.set([...this.scopes().filter(scope => scope !== scopeToRemove)]);
  }

  addScope(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();
    if (value) {
      this.scopes.set([...this.scopes(), value]);
    }
    event.chipInput.clear();
  }

  private toFormValue(auth?: WebhookSubscriptionConfigurationAuth): AuthFormValue {
    switch (auth?.type) {
      case 'basic':
        return { type: auth.type, username: auth.basic?.username, password: auth.basic?.password };
      case 'token':
        return { type: auth.type, token: auth.token?.value };
      case 'oauth2':
        return {
          type: auth.type,
          endpoint: auth.oauth2?.endpoint,
          clientId: auth.oauth2?.clientId,
          clientSecret: auth.oauth2?.clientSecret,
          scopes: auth.oauth2?.scopes,
        };
      default:
        return { type: 'none' };
    }
  }

  private toConfigurationAuthentication(authFormValue: AuthFormValue): WebhookSubscriptionConfigurationAuth {
    switch (authFormValue.type) {
      case 'basic':
        return {
          type: authFormValue.type,
          basic: { username: authFormValue.username, password: authFormValue.password } as BasicAuthConfiguration,
        };
      case 'token':
        return { type: authFormValue.type, token: { value: authFormValue.token } as TokenAuthConfiguration };
      case 'oauth2':
        return {
          type: authFormValue.type,
          oauth2: {
            endpoint: authFormValue.endpoint,
            clientId: authFormValue.clientId,
            clientSecret: authFormValue.clientSecret,
            scopes: authFormValue.scopes,
          } as Oauth2AuthConfiguration,
        };
      default:
        return { type: 'none' };
    }
  }

  private updateValidators(type: WebhookSubscriptionConfigurationAuthType) {
    Object.keys(this.authForm.controls).forEach(controlName => {
      this.authForm.get(controlName)?.clearValidators();
    });
    this.authForm.clearValidators();

    switch (type) {
      case 'basic': {
        this.authForm.controls.username.setValidators([Validators.required]);
        this.authForm.controls.password.setValidators([Validators.required]);
        break;
      }
      case 'token': {
        this.authForm.controls.token.setValidators([Validators.required]);
        break;
      }
      case 'oauth2': {
        this.authForm.controls.endpoint.setValidators([Validators.required]);
        this.authForm.controls.clientId.setValidators([Validators.required]);
        this.authForm.controls.clientSecret.setValidators([Validators.required]);
        this.authForm.controls.scopes.setValidators([]);
        break;
      }
    }
  }

  private updateValueAndValidity() {
    Object.keys(this.authForm.controls).forEach(controlName => {
      this.authForm.get(controlName)?.updateValueAndValidity({ emitEvent: false });
    });
    this.authForm.updateValueAndValidity();
  }

  private _onChange: (value: WebhookSubscriptionConfigurationAuth) => void = () => ({});
  private _onTouched: () => void = () => ({});
}
