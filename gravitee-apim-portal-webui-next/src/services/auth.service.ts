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
import { CurrentUserService } from './current-user.service';
import { IdentityProviderService } from './identity-provider.service';
import { clearOidcTransaction, consumeOidcTransaction, OidcTransaction, storeOidcTransaction } from './oidc-transaction.util';
import { IdentityProvider } from '../entities/configuration/identity-provider';

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

    void this.redirectToIdentityProvider(provider, redirectUrl);
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

  completeOidcLoginIfPresent() {
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

    const transaction = consumeOidcTransaction(state);
    if (transaction?.providerId !== providerId) {
      this.removeProviderId();
      sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
      clearOidcTransaction();
      return throwError(() => new Error('Invalid OIDC state'));
    }

    this.clearOidcQueryParams();

    this.oidcCallbackInFlight = this.identityProviderService.getPortalIdentityProvider(providerId).pipe(
      switchMap(identityProvider => this.completeOidcCallback(identityProvider, authorizationCode, transaction)),
      tap(() => sessionStorage.setItem(processedKey, '1')),
      finalize(() => {
        this.oidcCallbackInFlight = null;
      }),
      shareReplay(1),
      catchError(() => {
        this.removeProviderId();
        sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
        clearOidcTransaction();
        return of(undefined);
      }),
    );

    return this.oidcCallbackInFlight;
  }

  private async redirectToIdentityProvider(provider: IdentityProvider, redirectUrl: string): Promise<void> {
    this.storeProviderId(provider.id!);
    const redirectUri = this.getRedirectUri();
    sessionStorage.setItem(OIDC_REDIRECT_URI_KEY, redirectUri);

    const { state, codeChallenge } = await storeOidcTransaction({
      providerId: provider.id!,
      redirectUrl,
      redirectUri,
    });

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: provider.client_id!,
      redirect_uri: redirectUri,
      scope: provider.scopes?.join(' ') ?? 'openid',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    window.location.href = `${provider.authorizationEndpoint}?${params.toString()}`;
  }

  private completeOidcCallback(identityProvider: IdentityProvider, authorizationCode: string, transaction: OidcTransaction) {
    if (!identityProvider?.id || !identityProvider.client_id) {
      return throwError(() => new Error(`Identity provider ${identityProvider?.id} not found!`));
    }

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code: authorizationCode,
      redirect_uri: transaction.redirectUri,
      client_id: identityProvider.client_id,
      code_verifier: transaction.codeVerifier,
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

          if (transaction.redirectUrl) {
            sessionStorage.setItem(OIDC_REDIRECT_STATE_KEY, transaction.redirectUrl);
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
