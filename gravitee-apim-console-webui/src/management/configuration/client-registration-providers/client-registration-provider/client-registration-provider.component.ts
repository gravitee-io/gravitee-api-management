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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { EMPTY, Subject } from 'rxjs';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Constants } from '../../../../entities/Constants';
import { ClientRegistrationProvidersService } from '../../../../services-ngx/client-registration-providers.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ClientRegistrationProvider } from '../../../../entities/client-registration-provider/clientRegistrationProvider';

@Component({
  selector: 'client-registration-provider',
  template: require('./client-registration-provider.component.html'),
  styles: [require('./client-registration-provider.component.scss')],
})
export class ClientRegistrationProviderComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject();
  public updateMode: boolean;
  public providerForm: FormGroup;
  public initialAccessTokenTypes = [
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
  public renewClientSecretEndpointUrlExample: 'https://authorization_server/oidc/dcr/{#client_id}/renew_secret';
  public title = 'Create a new client registration provider';
  public invalidStateSaveBar = false;
  private clientRegistrationProvider?: ClientRegistrationProvider;

  constructor(
    @Inject(UIRouterStateParams) private ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly clientRegistrationProvidersService: ClientRegistrationProvidersService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    if (this.ajsStateParams.id) {
      this.clientRegistrationProvidersService
        .get(this.ajsStateParams.id)
        .pipe(
          tap((clientRegistrationProvider) => {
            this.updateMode = true;
            this.clientRegistrationProvider = clientRegistrationProvider;
            this.title = clientRegistrationProvider.name;
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
    if (!this.providerForm.valid) {
      return;
    }
    const clientRegistrationProvider = this.providerForm.value;
    if (this.updateMode) {
      this.clientRegistrationProvidersService
        .update({ ...clientRegistrationProvider, id: this.ajsStateParams.id })
        .pipe(
          tap((clientRegistrationProvider) => {
            this.snackBarService.success(`Client registration provider  ${clientRegistrationProvider.name} has been updated`);
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
          tap(() => {
            this.ajsState.go(
              'management.settings.clientregistrationproviders.clientregistrationprovider',
              { id: clientRegistrationProvider.id },
              { reload: true },
            );
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      this.clientRegistrationProvidersService
        .create(clientRegistrationProvider)
        .pipe(
          tap((clientRegistrationProvider) => {
            this.snackBarService.success(`Client registration provider  ${clientRegistrationProvider.name} has been created`);
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
          tap(() => {
            this.ajsState.go(
              'management.settings.clientregistrationproviders.clientregistrationprovider',
              { id: clientRegistrationProvider.id },
              { reload: true },
            );
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }
  }

  private initProviderForm(clientRegistrationProvider?: ClientRegistrationProvider) {
    this.providerForm = new FormGroup({
      name: new FormControl(clientRegistrationProvider?.name, [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      description: new FormControl(clientRegistrationProvider?.description),
      discovery_endpoint: new FormControl(clientRegistrationProvider?.discovery_endpoint, [Validators.required]),
      initial_access_token_type: new FormControl(clientRegistrationProvider?.initial_access_token_type, [Validators.required]),
      client_id: new FormControl(clientRegistrationProvider?.client_id, [Validators.required]),
      client_secret: new FormControl(clientRegistrationProvider?.client_secret, [Validators.required]),
      renew_client_secret_method: new FormControl(clientRegistrationProvider?.renew_client_secret_method),
      scopes: new FormControl(clientRegistrationProvider?.scopes),
      software_id: new FormControl(clientRegistrationProvider?.software_id),
      initial_access_token: new FormControl(clientRegistrationProvider?.initial_access_token, [Validators.required]),
      renew_client_secret_support: new FormControl(clientRegistrationProvider?.renew_client_secret_support),
      renew_client_secret_endpoint: new FormControl(clientRegistrationProvider?.renew_client_secret_endpoint),
    });

    this.providerForm.statusChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((status) => {
      this.invalidStateSaveBar = status !== 'VALID';
    });
  }

  onReset() {
    this.providerForm = undefined;
    this.ngOnInit();
  }
}
