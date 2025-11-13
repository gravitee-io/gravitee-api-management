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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { catchError, map, Observable, switchMap } from 'rxjs';
import { fromPromise } from 'rxjs/internal/observable/innerFrom';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import { IdentityProviderService } from './identity-provider.service';
import { IdentityProvider } from '../entities/configuration/identity-provider';

interface Token {
  token_type: string;
  token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
    private readonly oauthService: OAuthService,
    private readonly identityProviderService: IdentityProviderService,
  ) {}

  login(username: string, password: string) {
    return this.http.post<Token>(
      `${this.configService.baseURL}/auth/login`,
      {},
      {
        headers: {
          Authorization: `Basic ${btoa(username + ':' + password)}`,
        },
      },
    );
  }

  logout(): Observable<unknown> {
    return this.http.post<Token>(`${this.configService.baseURL}/auth/logout`, {});
  }

  authenticateSSO(provider: IdentityProvider, redirectUrl: string) {
    if (provider) {
      this.storeProviderId(provider.id!);
      this._configure(provider);
      this.oauthService.initCodeFlow(redirectUrl);
    }
  }

  storeProviderId(providerId: string) {
    localStorage.setItem('user-provider-id', providerId);
  }

  getProviderId(): string {
    return localStorage.getItem('user-provider-id')!;
  }

  load() {
    if (this.getProviderId()) {
      return this._fetchProviderAndConfigure().pipe(
        switchMap(() => {
          return fromPromise(
            this.oauthService.tryLoginCodeFlow({
              // 📝 The clear of the hash doesn't work correctly and keeps a piece of string which distorts angular routing and
              // displays a 404. Disabling it solves the problem and the clear will be done with an angular internal redirection.
              preventClearHashAfterLogin: true,
            }),
          );
        }),
      );
    } else {
      return of(undefined);
    }
  }

  private _fetchProviderAndConfigure() {
    return this.identityProviderService.getPortalIdentityProvider(this.getProviderId()).pipe(
      map(identityProvider => {
        if (identityProvider) {
          this._configure(identityProvider);
        }
      }),
      catchError(() => of(undefined)),
    );
  }

  private _configure(provider: IdentityProvider) {
    const redirectUri =
      window.location.origin + (window.location.pathname === '/' ? '' : window.location.pathname.replace('/user/logout', '/'));
    this.oauthService.configure({
      clientId: provider.client_id,
      loginUrl: provider.authorizationEndpoint,
      tokenEndpoint: this.configService.baseURL + '/auth/oauth2/' + provider.id,
      requireHttps: false,
      issuer: provider.tokenIntrospectionEndpoint,
      logoutUrl: provider.userLogoutEndpoint,
      postLogoutRedirectUri: redirectUri,
      scope: provider.scopes?.join(' '),
      responseType: 'code',
      redirectUri,
      /*
       added because with our current OIDC configuration, we don't know the real issuer.
       For example, with keycloak, the issuer is "https://[host]:[port]/auth/realms/[realm_id].
       But in our configuration, we only have these endpoints:
        - https://[host]:[port]/auth/realms/[realm_id]/protocol/openid-connect/token
        - https://[host]:[port]/auth/realms/[realm_id]/protocol/openid-connect/token/introspect
        - https://[host]:[port]/auth/realms/[realm_id]/protocol/openid-connect/auth
        - https://[host]:[port]/auth/realms/[realm_id]/protocol/openid-connect/userinfo
        - https://[host]:[port]/auth/realms/[realm_id]/protocol/openid-connect/logout
       */
      skipIssuerCheck: true,
    });
  }
}
