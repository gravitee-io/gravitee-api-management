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
import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { OAuthService } from 'angular-oauth2-oidc';
import { of } from 'rxjs';

import { PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { AuthService } from '../../services/auth.service';
import { ConfigurationService } from '../../services/configuration.service';
import { NotificationService } from '../../services/notification.service';

import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  const createComponent = createComponentFactory({
    component: LoginComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, FormsModule, ReactiveFormsModule, HttpClientTestingModule],
    providers: [mockProvider(NotificationService), mockProvider(ConfigurationService)],
  });

  let spectator: Spectator<LoginComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getProviderStyle', () => {
    it('should return empty style when color is unset', () => {
      expect(component.getProviderStyle({ type: 'OIDC' } as never)).toBe('');
    });

    it('should return empty style when color is whitespace only', () => {
      expect(component.getProviderStyle({ type: 'OIDC', color: '   ' } as never)).toBe('');
    });

    it('should include background and contrasting text color for dark hex', () => {
      expect(component.getProviderStyle({ type: 'OIDC', color: '#000000' } as never)).toBe(
        '--gv-button-oidc--bgc:#000000;--gv-button--c:#ffffff;',
      );
    });

    it('should include background and contrasting text color for light hex', () => {
      expect(component.getProviderStyle({ type: 'OIDC', color: '#ffffff' } as never)).toBe(
        '--gv-button-oidc--bgc:#ffffff;--gv-button--c:#000000;',
      );
    });
  });
});

describe('LoginComponent identity provider code exchange failure', () => {
  const provider = {
    id: 'pulsar-repro-idp',
    name: 'Gravitee AM',
    type: 'GRAVITEEIO_AM',
    client_id: 'pulsar-repro-client',
    authorizationEndpoint: 'http://localhost/authorize',
    tokenIntrospectionEndpoint: 'http://localhost/introspect',
    userLogoutEndpoint: 'http://localhost/logout',
    scopes: ['openid', 'profile', 'email'],
  };
  const tryLoginCodeFlow = jest.fn();

  const createComponent = createComponentFactory({
    component: LoginComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, HttpClientTestingModule],
    // The sign-in page must not be initialized before the code exchange has been attempted, as it happens at startup.
    detectChanges: false,
    providers: [
      mockProvider(NotificationService),
      mockProvider(ConfigurationService, { get: () => '/portal', hasFeature: () => true }),
      mockProvider(PortalService, {
        getPortalIdentityProvider: () => of(provider),
        getPortalIdentityProviders: () => of({ data: [provider] }),
      }),
      { provide: OAuthService, useValue: { configure: jest.fn(), tryLoginCodeFlow } },
    ],
  });

  let spectator: Spectator<LoginComponent>;

  beforeEach(() => {
    localStorage.setItem('user-provider-id', provider.id);
    spectator = createComponent();
  });

  afterEach(() => {
    localStorage.removeItem('user-provider-id');
    tryLoginCodeFlow.mockReset();
  });

  async function loadThenOpenSignInPage() {
    await spectator.inject(AuthService).load();
    spectator.detectChanges();
  }

  it('should notify the user when APIM rejects the code exchange', async () => {
    tryLoginCodeFlow.mockRejectedValue(new HttpErrorResponse({ status: 401, error: { error: 'invalid_client' } }));

    await loadThenOpenSignInPage();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: 'invalid_client',
    });
  });

  it('should include the hint APIM returns with the error', async () => {
    tryLoginCodeFlow.mockRejectedValue(
      new HttpErrorResponse({ status: 401, error: { error: 'invalid_client', hint: 'Check the client authentication method.' } }),
    );

    await loadThenOpenSignInPage();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: 'invalid_client - Check the client authentication method.',
    });
  });

  it('should fall back to the HTTP status when APIM answers without an error code', async () => {
    tryLoginCodeFlow.mockRejectedValue(new HttpErrorResponse({ status: 502, statusText: 'Bad Gateway' }));

    await loadThenOpenSignInPage();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: '502 Bad Gateway',
    });
  });

  it('should show the failure only once', async () => {
    tryLoginCodeFlow.mockRejectedValue(new HttpErrorResponse({ status: 401, error: { error: 'invalid_client' } }));
    await loadThenOpenSignInPage();

    spectator.component.ngOnInit();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledTimes(1);
  });

  it('should not notify when the code flow fails before reaching APIM', async () => {
    tryLoginCodeFlow.mockRejectedValue(new Error('Validating access_token failed, wrong state/nonce.'));

    await loadThenOpenSignInPage();

    expect(spectator.inject(NotificationService).error).not.toHaveBeenCalled();
  });

  it('should not notify when the code exchange succeeds', async () => {
    tryLoginCodeFlow.mockResolvedValue(undefined);

    await loadThenOpenSignInPage();

    expect(spectator.inject(NotificationService).error).not.toHaveBeenCalled();
  });
});
