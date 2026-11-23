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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { provideRouter } from '@angular/router';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { OAuthService } from 'angular-oauth2-oidc';
import { of } from 'rxjs';

import { PortalService } from '../../../projects/portal-webclient-sdk/src/lib';
import { LoginComponent } from '../pages/login/login.component';

import { AuthService } from './auth.service';
import { ConfigurationService } from './configuration.service';
import { NotificationService } from './notification.service';

describe('AuthService when APIM rejects the authorization code exchange', () => {
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
  const clientAuthenticationHint =
    'The identity provider rejected the client credentials. Check that the tokenEndpointAuthMethod configured on ' +
    'the identity provider matches the method the provider expects (client_secret_basic, client_secret_post, or none); ' +
    'the token and introspection endpoints must both accept it.';
  const tryLoginCodeFlow = jest.fn();

  // provideRouter, not RouterTestingModule: no initial navigation runs, so the sign-in page is the only thing under test.
  const createSignInPage = createComponentFactory({
    component: LoginComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule],
    detectChanges: false,
    providers: [
      provideRouter([]),
      mockProvider(NotificationService),
      mockProvider(ConfigurationService, { get: () => '/portal/environments/DEFAULT', hasFeature: () => true }),
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
    spectator = createSignInPage();
  });

  afterEach(() => {
    localStorage.removeItem('user-provider-id');
    tryLoginCodeFlow.mockReset();
  });

  it('should pass the invalid_client error and its hint on to the sign-in page', async () => {
    tryLoginCodeFlow.mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
        url: `/portal/environments/DEFAULT/auth/oauth2/${provider.id}`,
        error: { error: 'invalid_client', hint: clientAuthenticationHint },
      }),
    );

    await spectator.inject(AuthService).load();
    spectator.detectChanges();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: `invalid_client - ${clientAuthenticationHint}`,
    });
  });

  // The notification is rendered through [innerHtml], and APIM relays the identity provider's error code verbatim.
  it('should escape markup in the error relayed from the identity provider', async () => {
    tryLoginCodeFlow.mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
        url: `/portal/environments/DEFAULT/auth/oauth2/${provider.id}`,
        error: { error: '<a href="https://example.test">reset</a>&' },
      }),
    );

    await spectator.inject(AuthService).load();
    spectator.detectChanges();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: '&lt;a href=&quot;https://example.test&quot;&gt;reset&lt;/a&gt;&amp;',
    });
  });

  it('should notify the user while loading, before any page is shown', async () => {
    tryLoginCodeFlow.mockRejectedValue(new HttpErrorResponse({ status: 401, error: { error: 'invalid_client' } }));

    await spectator.inject(AuthService).load();

    expect(spectator.inject(NotificationService).error).toHaveBeenCalledWith('login.notification.identityProviderError', {
      provider: 'Gravitee AM',
      error: 'invalid_client',
    });
  });
});
