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
import { JKSTrustStore, PKCS12TrustStore } from 'src/entities/management-api-v2';

import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { ClientRegistrationProvidersService } from '../../../../services-ngx/client-registration-providers.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ClientRegistrationProvider } from '../../../../entities/client-registration-provider/clientRegistrationProvider';

@Component({
  selector: 'client-registration-provider',
  templateUrl: './client-registration-provider.component.html',
  styleUrls: ['./client-registration-provider.component.scss'],
  standalone: false,
})
export class ClientRegistrationProviderComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();
  public updateMode: boolean;
  public providerForm: UntypedFormGroup;
  public initialAccessTokenTypes: { name: string; value: ClientRegistrationProvider['initial_access_token_type'] }[] = [
    {
      name: 'Client Credentials',
      value: 'CLIENT_CREDENTIALS',
    },
    {
      name: 'Initial Access Token',
      value: 'INITIAL_ACCESS_TOKEN',
    },
  ];
  public renewClientSecretMethods = ['POST', 'PATCH', 'PUT'];
  public renewClientSecretEndpointUrlExample: string = 'https://authorization_server/oidc/dcr/{#client_id}/renew_secret';

  public formInitialValues: unknown;

  public trustStoreTypes = [
    { label: 'None', value: 'NONE' },
    { label: 'Java Trust Store (.jks)', value: 'JKS' },
    { label: 'PKCS#12 (.p12) / PFX (.pfx)', value: 'PKCS12' },
  ];

  public keyStoreTypes = [
    { label: 'None', value: 'NONE' },
    { label: 'Java Key Store (.jks)', value: 'JKS' },
    { label: 'PKCS#12 (.p12) / PFX (.pfx)', value: 'PKCS12' },
  ];

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly clientRegistrationProvidersService: ClientRegistrationProvidersService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    if (this.activatedRoute?.snapshot?.params?.providerId) {
      this.clientRegistrationProvidersService
        .get(this.activatedRoute.snapshot.params.providerId)
        .pipe(
          tap(clientRegistrationProvider => {
            this.updateMode = true;
            this.initProviderForm(clientRegistrationProvider);
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      this.initProviderForm();
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (!this.providerForm?.valid) {
      throw new Error('Form is not valid');
    }

    const providerFormValueToSave = {
      ...this.providerForm.value,
      trust_store: this.getTrustStoreFromForm(),
      key_store: this.getKeyStoreFromForm(),
    };

    const createUpdate$ = this.updateMode
      ? this.clientRegistrationProvidersService
          .update({ ...providerFormValueToSave, id: this.activatedRoute.snapshot.params.providerId })
          .pipe(
            tap(clientRegistrationProvider => {
              this.snackBarService.success(`Client registration provider  ${clientRegistrationProvider.name} has been updated.`);
              this.providerForm.markAsPristine();
            }),
          )
      : this.clientRegistrationProvidersService.create(providerFormValueToSave).pipe(
          tap(clientRegistrationProvider => {
            this.snackBarService.success(`Client registration provider  ${clientRegistrationProvider.name} has been created.`);
            this.providerForm.markAsPristine();
          }),
        );

    createUpdate$
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(clientRegistrationProvider => {
          this.router.navigate(['../', clientRegistrationProvider.id], { relativeTo: this.activatedRoute });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initProviderForm(clientRegistrationProvider?: ClientRegistrationProvider) {
    const trustStoreGroup = this.createTrustStoreGroup(clientRegistrationProvider?.trust_store);
    const keyStoreGroup = this.createKeyStoreGroup(clientRegistrationProvider?.key_store);
    this.providerForm = this.createProviderForm(clientRegistrationProvider, trustStoreGroup, keyStoreGroup);

    this.setupTrustStoreValidation(trustStoreGroup);
    this.setupKeyStoreValidation(keyStoreGroup);

    // trigger initial validators
    trustStoreGroup.get('type')!.updateValueAndValidity({ emitEvent: true });
    keyStoreGroup.get('type')!.updateValueAndValidity({ emitEvent: true });

    this.providerForm
      .get('initial_access_token_type')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(value => {
        // Clear or add validators depending on the selected value
        if (value === 'INITIAL_ACCESS_TOKEN') {
          this.providerForm.get('client_id').clearValidators();
          this.providerForm.get('client_secret').clearValidators();

          this.providerForm.get('initial_access_token').setValidators([Validators.required]);
        }
        if (value === 'CLIENT_CREDENTIALS') {
          this.providerForm.get('initial_access_token').clearValidators();

          this.providerForm.get('client_id').setValidators([Validators.required]);
          this.providerForm.get('client_secret').setValidators([Validators.required]);
        }

        // Needed by angular after changing validators
        this.providerForm.get('client_id').updateValueAndValidity();
        this.providerForm.get('client_secret').updateValueAndValidity();
        this.providerForm.get('initial_access_token').updateValueAndValidity();
        this.providerForm.updateValueAndValidity();
      });
    this.formInitialValues = this.providerForm.value;
  }

  isClientCredentials(): boolean {
    return this.providerForm?.get('initial_access_token_type')?.value === 'CLIENT_CREDENTIALS';
  }

  isInitialAccessToken(): boolean {
    return this.providerForm?.get('initial_access_token_type')?.value === 'INITIAL_ACCESS_TOKEN';
  }

  get trustStoreForm(): UntypedFormGroup {
    return this.providerForm.get('trust_store') as UntypedFormGroup;
  }

  isTrustStoreType(type: string): boolean {
    return this.trustStoreForm?.get('type')?.value === type;
  }

  get keyStoreForm(): UntypedFormGroup {
    return this.providerForm.get('key_store') as UntypedFormGroup;
  }

  isKeyStoreType(type: string): boolean {
    return this.keyStoreForm.get('type')?.value === type;
  }

  private createTrustStoreGroup(trust_store) {
    const trustStore = trust_store || { type: 'NONE' };
    let initialPathOrContent = 'PATH';
    if (trustStore?.path) {
      initialPathOrContent = 'PATH';
    } else if (trustStore?.content) {
      initialPathOrContent = 'CONTENT';
    }
    return new UntypedFormGroup({
      type: new UntypedFormControl(trustStore.type),
      pathOrContent: new UntypedFormControl(initialPathOrContent),
      jksPath: new UntypedFormControl((trustStore as JKSTrustStore)?.path),
      jksContent: new UntypedFormControl((trustStore as JKSTrustStore)?.content),
      jksPassword: new UntypedFormControl((trustStore as JKSTrustStore)?.password),
      pkcs12Path: new UntypedFormControl((trustStore as PKCS12TrustStore)?.path),
      pkcs12Content: new UntypedFormControl((trustStore as PKCS12TrustStore)?.content),
      pkcs12Password: new UntypedFormControl((trustStore as PKCS12TrustStore)?.password),
    });
  }

  private createKeyStoreGroup(key_store) {
    const keyStore = key_store || { type: 'NONE' };
    let initialPathOrContent = 'PATH';
    if (keyStore?.path) {
      initialPathOrContent = 'PATH';
    } else if (keyStore?.content) {
      initialPathOrContent = 'CONTENT';
    }
    return new UntypedFormGroup({
      type: new UntypedFormControl(keyStore.type),
      pathOrContent: new UntypedFormControl(initialPathOrContent),
      jksPassword: new UntypedFormControl((keyStore as any)?.password),
      jksPath: new UntypedFormControl((keyStore as any)?.path),
      jksContent: new UntypedFormControl((keyStore as any)?.content),
      pkcs12Password: new UntypedFormControl((keyStore as any)?.password),
      pkcs12Path: new UntypedFormControl((keyStore as any)?.path),
      pkcs12Content: new UntypedFormControl((keyStore as any)?.content),
      alias: new UntypedFormControl((keyStore as any).alias),
      keyPassword: new UntypedFormControl((keyStore as any).keyPassword),
    });
  }

  private createProviderForm(clientRegistrationProvider: ClientRegistrationProvider, trustStoreGroup, keyStoreGroup) {
    return new UntypedFormGroup({
      name: new UntypedFormControl(clientRegistrationProvider?.name, [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
      ]),
      description: new UntypedFormControl(clientRegistrationProvider?.description),
      discovery_endpoint: new UntypedFormControl(clientRegistrationProvider?.discovery_endpoint, [Validators.required]),
      initial_access_token_type: new UntypedFormControl(clientRegistrationProvider?.initial_access_token_type, [Validators.required]),
      client_id: new UntypedFormControl(clientRegistrationProvider?.client_id),
      client_secret: new UntypedFormControl(clientRegistrationProvider?.client_secret),
      renew_client_secret_method: new UntypedFormControl(clientRegistrationProvider?.renew_client_secret_method),
      scopes: new UntypedFormControl(clientRegistrationProvider?.scopes),
      software_id: new UntypedFormControl(clientRegistrationProvider?.software_id),
      initial_access_token: new UntypedFormControl(clientRegistrationProvider?.initial_access_token),
      renew_client_secret_support: new UntypedFormControl(clientRegistrationProvider?.renew_client_secret_support),
      renew_client_secret_endpoint: new UntypedFormControl(clientRegistrationProvider?.renew_client_secret_endpoint),
      trust_store: trustStoreGroup,
      key_store: keyStoreGroup,
    });
  }

  private setupTrustStoreValidation(trustStoreGroup: UntypedFormGroup) {
    trustStoreGroup.setValidators(null);

    const updateValidators = () => {
      const type = trustStoreGroup.get('type').value;
      const pathOrContent = trustStoreGroup.get('pathOrContent').value;

      // Clear validators
      trustStoreGroup.get('jksPath')?.clearValidators();
      trustStoreGroup.get('jksContent')?.clearValidators();
      trustStoreGroup.get('pkcs12Path')?.clearValidators();
      trustStoreGroup.get('pkcs12Content')?.clearValidators();

      if (type === 'JKS') {
        trustStoreGroup.get('jksPassword')?.setValidators([Validators.required]);
        if (pathOrContent === 'PATH') {
          trustStoreGroup.get('jksPath')?.setValidators([Validators.required]);
        } else if (pathOrContent === 'CONTENT') {
          trustStoreGroup.get('jksContent')?.setValidators([Validators.required]);
        }
      } else if (type === 'PKCS12') {
        trustStoreGroup.get('pkcs12Password')?.setValidators([Validators.required]);
        if (pathOrContent === 'PATH') {
          trustStoreGroup.get('pkcs12Path')?.setValidators([Validators.required]);
        } else if (pathOrContent === 'CONTENT') {
          trustStoreGroup.get('pkcs12Content')?.setValidators([Validators.required]);
        }
      }

      Object.values(trustStoreGroup.controls).forEach(ctrl => ctrl.updateValueAndValidity({ emitEvent: false }));
    };

    trustStoreGroup.get('type').valueChanges.subscribe(updateValidators);
    trustStoreGroup.get('pathOrContent').valueChanges.subscribe(updateValidators);
    updateValidators();
  }

  private setupKeyStoreValidation(keyStoreGroup: UntypedFormGroup) {
    keyStoreGroup.setValidators(null);

    const updateValidators = () => {
      const type = keyStoreGroup.get('type').value;
      const pathOrContent = keyStoreGroup.get('pathOrContent').value;

      // Clear validators
      keyStoreGroup.get('jksPath')?.clearValidators();
      keyStoreGroup.get('jksContent')?.clearValidators();
      keyStoreGroup.get('pkcs12Path')?.clearValidators();
      keyStoreGroup.get('pkcs12Content')?.clearValidators();

      if (type === 'JKS') {
        keyStoreGroup.get('jksPassword')?.setValidators([Validators.required]);
        if (pathOrContent === 'PATH') {
          keyStoreGroup.get('jksPath')?.setValidators([Validators.required]);
        } else if (pathOrContent === 'CONTENT') {
          keyStoreGroup.get('jksContent')?.setValidators([Validators.required]);
        }
      } else if (type === 'PKCS12') {
        keyStoreGroup.get('pkcs12Password')?.setValidators([Validators.required]);
        if (pathOrContent === 'PATH') {
          keyStoreGroup.get('pkcs12Path')?.setValidators([Validators.required]);
        } else if (pathOrContent === 'CONTENT') {
          keyStoreGroup.get('pkcs12Content')?.setValidators([Validators.required]);
        }
      }

      Object.values(keyStoreGroup.controls).forEach(ctrl => ctrl.updateValueAndValidity({ emitEvent: false }));
    };

    keyStoreGroup.get('type').valueChanges.subscribe(updateValidators);
    keyStoreGroup.get('pathOrContent').valueChanges.subscribe(updateValidators);
    updateValidators();
  }

  private getTrustStoreFromForm() {
    const value = this.trustStoreForm.value;
    const pathOrContent = value.pathOrContent; // Updated name

    switch (value.type) {
      case 'JKS':
        return {
          type: 'JKS',
          password: value.jksPassword,
          path: pathOrContent === 'PATH' ? value.jksPath : null,
          content: pathOrContent === 'CONTENT' ? value.jksContent : null,
        } as JKSTrustStore;
      case 'PKCS12':
        return {
          type: 'PKCS12',
          password: value.pkcs12Password,
          path: pathOrContent === 'PATH' ? value.pkcs12Path : null,
          content: pathOrContent === 'CONTENT' ? value.pkcs12Content : null,
        } as PKCS12TrustStore;
      default:
        return { type: 'NONE' };
    }
  }

  private getKeyStoreFromForm() {
    const value = this.keyStoreForm.value;
    const pathOrContent = value.pathOrContent; // Updated name

    switch (value.type) {
      case 'JKS':
        return {
          type: 'JKS',
          password: value.jksPassword,
          path: pathOrContent === 'PATH' ? value.jksPath : null,
          content: pathOrContent === 'CONTENT' ? value.jksContent : null,
          alias: value.alias,
          keyPassword: value.keyPassword,
        };

      case 'PKCS12':
        return {
          type: 'PKCS12',
          password: value.pkcs12Password,
          path: pathOrContent === 'PATH' ? value.pkcs12Path : null,
          content: pathOrContent === 'CONTENT' ? value.pkcs12Content : null,
          alias: value.alias,
          keyPassword: value.keyPassword,
        };

      default:
        return { type: 'NONE' };
    }
  }
}
