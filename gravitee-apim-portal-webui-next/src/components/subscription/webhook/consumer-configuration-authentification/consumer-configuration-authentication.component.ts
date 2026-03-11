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
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatError, MatFormFieldModule, MatHint } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { tap } from 'rxjs';

import { AUTHENTICATION_TYPES, AuthFormValue } from './consumer-configuration-authentication.model';
import {
  BasicAuthConfiguration,
  JwtProfileOauth2AuthConfiguration,
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
    MatCheckboxModule,
    MatButtonModule,
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
  customClaims = signal<{ name: string; value: string }[]>([]);
  customClaimError = signal<string | null>(null);

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

    // for jwtProfileOauth2 authentication
    issuer: new FormControl(),
    subject: new FormControl(),
    audience: new FormControl(),
    expirationTime: new FormControl(),
    expirationTimeUnit: new FormControl(),
    signatureAlgorithm: new FormControl(),
    keySource: new FormControl(),
    jwtId: new FormControl(),
    secretBase64Encoded: new FormControl(),
    x509CertChain: new FormControl(),
    alias: new FormControl(),
    storePassword: new FormControl(),
    keyPassword: new FormControl(),
    keyId: new FormControl(),
    keyContent: new FormControl(),
    customClaims: new FormControl(),
  });
  readonly types = AUTHENTICATION_TYPES;
  private readonly destroyRef = inject(DestroyRef);

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

    this.authForm.controls.signatureAlgorithm.valueChanges
      .pipe(
        tap(algorithm => {
          this.updateKeySourceValidators(algorithm);
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
      this.authForm.controls.scopes.setValue(this.scopes(), { emitEvent: false });
    }
    if (this.authForm.controls.type.value === 'jwtProfileOauth2' && this.authForm.controls.customClaims.getRawValue()) {
      this.customClaims.set(this.authForm.controls.customClaims.getRawValue());
      this.authForm.controls.customClaims.setValue(this.customClaims(), { emitEvent: false });
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
    this.authForm.controls.scopes.setValue(this.scopes(), { emitEvent: false });

    if (formValue.type === 'jwtProfileOauth2' && formValue.customClaims) {
      this.customClaims.set([...formValue.customClaims]);
    } else {
      this.customClaims.set([]);
    }
    this.authForm.controls.customClaims.setValue(this.customClaims(), { emitEvent: false });
  }

  registerOnChange(fn: (value: WebhookSubscriptionConfigurationAuth) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.authForm.disable({ emitEvent: false }) : this.authForm.enable({ emitEvent: false });
  }

  removeScope(scopeToRemove: string) {
    this.scopes.set([...this.scopes().filter(scope => scope !== scopeToRemove)]);
    this.authForm.controls.scopes.setValue(this.scopes());
  }

  addScope(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();
    if (value) {
      this.scopes.set([...this.scopes(), value]);
      this.authForm.controls.scopes.setValue(this.scopes());
    }
    event.chipInput.clear();
  }

  removeCustomClaim(claimToRemove: { name: string; value: string }) {
    this.customClaims.set([
      ...this.customClaims().filter(claim => !(claim.name === claimToRemove.name && claim.value === claimToRemove.value)),
    ]);
    this.authForm.controls.customClaims.setValue(this.customClaims());
  }

  addCustomClaim(nameInput: HTMLInputElement, valueInput: HTMLInputElement) {
    const name = nameInput.value.trim();
    const value = valueInput.value.trim();
    if (name && value) {
      // Check for duplicate claim name
      const existingClaim = this.customClaims().find(claim => claim.name === name);
      if (existingClaim) {
        this.customClaimError.set('A claim with this name already exists');
        return;
      }

      this.customClaims.set([...this.customClaims(), { name, value }]);
      this.authForm.controls.customClaims.setValue(this.customClaims());
      this.customClaimError.set(null);
      nameInput.value = '';
      valueInput.value = '';
    }
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
      case 'jwtProfileOauth2':
        return {
          type: auth.type,
          issuer: auth.jwtProfileOauth2?.issuer,
          subject: auth.jwtProfileOauth2?.subject,
          audience: auth.jwtProfileOauth2?.audience,
          expirationTime: auth.jwtProfileOauth2?.expirationTime ?? 30,
          expirationTimeUnit: auth.jwtProfileOauth2?.expirationTimeUnit ?? 'SECONDS',
          signatureAlgorithm: auth.jwtProfileOauth2?.signatureAlgorithm ?? 'RSA_RS256',
          keySource: auth.jwtProfileOauth2?.keySource ?? 'INLINE',
          jwtId: auth.jwtProfileOauth2?.jwtId,
          secretBase64Encoded: auth.jwtProfileOauth2?.secretBase64Encoded ?? false,
          x509CertChain: auth.jwtProfileOauth2?.x509CertChain ?? 'NONE',
          alias: auth.jwtProfileOauth2?.keystoreOptions?.alias,
          storePassword: auth.jwtProfileOauth2?.keystoreOptions?.storePassword,
          keyPassword: auth.jwtProfileOauth2?.keystoreOptions?.keyPassword,
          keyId: auth.jwtProfileOauth2?.keyId,
          keyContent: auth.jwtProfileOauth2?.keyContent,
          customClaims: auth.jwtProfileOauth2?.customClaims,
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
      case 'jwtProfileOauth2': {
        const jwtConfig = {
          issuer: authFormValue.issuer,
          subject: authFormValue.subject,
          audience: authFormValue.audience,
          expirationTime: authFormValue.expirationTime,
          expirationTimeUnit: authFormValue.expirationTimeUnit,
          signatureAlgorithm: authFormValue.signatureAlgorithm,
          keySource: authFormValue.keySource,
          jwtId: authFormValue.jwtId,
          secretBase64Encoded: authFormValue.secretBase64Encoded,
          x509CertChain: authFormValue.x509CertChain,
          keyId: authFormValue.keyId,
          keyContent: authFormValue.keyContent,
          customClaims: authFormValue.customClaims,
        } as JwtProfileOauth2AuthConfiguration;

        // Only add keystoreOptions for JKS and PKCS12 key sources
        if (authFormValue.keySource === 'JKS' || authFormValue.keySource === 'PKCS12') {
          jwtConfig.keystoreOptions = {
            alias: authFormValue.alias,
            storePassword: authFormValue.storePassword,
            keyPassword: authFormValue.keyPassword,
          };
        }

        return {
          type: authFormValue.type,
          jwtProfileOauth2: jwtConfig,
        };
      }
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
      case 'jwtProfileOauth2': {
        this.authForm.controls.issuer.setValidators([Validators.required]);
        this.authForm.controls.subject.setValidators([Validators.required]);
        this.authForm.controls.audience.setValidators([Validators.required]);
        this.authForm.controls.expirationTime.setValidators([Validators.required]);
        this.authForm.controls.expirationTimeUnit.setValidators([Validators.required]);
        this.authForm.controls.signatureAlgorithm.setValidators([Validators.required]);
        this.authForm.controls.keyContent.setValidators([Validators.required]);
        // keySource validation is handled dynamically by updateKeySourceValidators
        this.updateKeySourceValidators(this.authForm.controls.signatureAlgorithm.value);
        break;
      }
    }
  }

  private updateKeySourceValidators(algorithm: string | null | undefined) {
    // Only require keySource for RSA_RS256 algorithm
    if (algorithm === 'RSA_RS256') {
      this.authForm.controls.keySource.setValidators([Validators.required]);
    } else {
      this.authForm.controls.keySource.clearValidators();
    }
    this.authForm.controls.keySource.updateValueAndValidity({ emitEvent: false });
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
