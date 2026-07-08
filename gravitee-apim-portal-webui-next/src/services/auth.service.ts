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
import { catchError, finalize, map, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { ConfigService } from './config.service';
import { IdentityProviderService } from './identity-provider.service';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { CurrentUserService } from './current-user.service';

export const OIDC_REDIRECT_STATE_KEY = 'oidc-redirect-state';
const USER_PROVIDER_ID_KEY = 'user-provider-id';
const OIDC_REDIRECT_URI_KEY = 'oidc-redirect-uri';
const OIDC_CODE_PROCESSED_PREFIX = 'oidc-code-processed:';

interface Token {
  token_type: string;
  token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private oidcCallbackInFlight: Observable<unknown> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
    private readonly identityProviderService: IdentityProviderService,
    private readonly currentUserService: CurrentUserService,
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

  logout(): Observable<{ logout_url?: string }> {
    const postLogoutRedirectUri = this.getRedirectUri();

    return this.http
      .post<{ logout_url?: string }>(`${this.configService.baseURL}/auth/logout`, {
        post_logout_redirect_uri: postLogoutRedirectUri,
      })
      .pipe(
        catchError(() => of({})),
        map(response => {
          this.removeProviderId();
          return response;
        }),
      );
  }

  authenticateSSO(provider: IdentityProvider, redirectUrl: string) {
    if (!provider?.id || !provider.authorizationEndpoint || !provider.client_id) {
      return;
    }

    this.storeProviderId(provider.id);
    const redirectUri = this.getRedirectUri();
    sessionStorage.setItem(OIDC_REDIRECT_URI_KEY, redirectUri);
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: provider.client_id,
      redirect_uri: redirectUri,
      scope: provider.scopes?.join(' ') ?? 'openid',
      state: redirectUrl,
    });

    window.location.href = `${provider.authorizationEndpoint}?${params.toString()}`;
  }

  storeProviderId(providerId: string) {
    localStorage.setItem(USER_PROVIDER_ID_KEY, providerId);
  }

  getProviderId(): string | null {
    return localStorage.getItem(USER_PROVIDER_ID_KEY);
  }

  removeProviderId() {
    localStorage.removeItem(USER_PROVIDER_ID_KEY);
  }

  load() {
    const providerId = this.getProviderId();
    const urlParams = new URLSearchParams(window.location.search);
    const authorizationCode = urlParams.get('code');
    const state = urlParams.get('state');

    if (!providerId || !authorizationCode) {
      return of(undefined);
    }

    const processedKey = `${OIDC_CODE_PROCESSED_PREFIX}${authorizationCode}`;
    if (sessionStorage.getItem(processedKey)) {
      return of(undefined);
    }

    if (this.oidcCallbackInFlight) {
      return this.oidcCallbackInFlight;
    }

    this.clearOidcQueryParams();

    this.oidcCallbackInFlight = this.identityProviderService.getPortalIdentityProvider(providerId).pipe(
      switchMap(identityProvider => this.completeOidcCallback(identityProvider, authorizationCode, state)),
      tap(() => sessionStorage.setItem(processedKey, '1')),
      finalize(() => {
        this.oidcCallbackInFlight = null;
      }),
      shareReplay(1),
      catchError(() => of(undefined)),
    );

    return this.oidcCallbackInFlight;
  }

  private completeOidcCallback(identityProvider: IdentityProvider, authorizationCode: string, state: string | null) {
    if (!identityProvider?.id || !identityProvider.client_id) {
      return throwError(() => new Error(`Identity provider ${identityProvider?.id} not found!`));
    }

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code: authorizationCode,
      redirect_uri: sessionStorage.getItem(OIDC_REDIRECT_URI_KEY) ?? this.getRedirectUri(),
      client_id: identityProvider.client_id,
    });

    return this.http
      .post(`${this.configService.baseURL}/auth/oauth2/${identityProvider.id}`, body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        responseType: 'text',
      })
      .pipe(
        switchMap(() => {
          sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
          return this.currentUserService.loadUser();
        }),
        map(user => {
          if (!user) {
            throw new Error('OIDC login failed');
          }

          if (state) {
            sessionStorage.setItem(OIDC_REDIRECT_STATE_KEY, state);
          }

          return undefined;
        }),
      );
  }

  private getRedirectUri(): string {
    return window.location.origin + (window.location.pathname === '/' ? '' : window.location.pathname.replace('/user/logout', '/'));
  }

  private clearOidcQueryParams() {
    window.history.replaceState({}, '', window.location.pathname);
  }
}
