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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { from, Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { LocationStrategy } from '@angular/common';

import {
  clearOidcTransaction,
  consumeOidcTransaction,
  isSafeOidcLogoutUrl,
  OidcTransaction,
  storeOidcTransaction,
} from './oidc-transaction.util';

import { Constants } from '../entities/Constants';
import { CurrentUserService } from '../services-ngx/current-user.service';
import { CustomUserFields } from '../entities/customUserFields';
import { SocialIdentityProvider } from '../entities/organization/socialIdentityProvider';

const USER_PROVIDER_ID_SELECTED = 'user-provider-id-selected';
const OIDC_REDIRECT_URI_KEY = 'oidc-redirect-uri';
const OIDC_CODE_PROCESSED_PREFIX = 'oidc-code-processed:';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private oidcCallbackInFlight: Observable<void> | null = null;
  private set providerIdSelectedStore(providerId: string | null) {
    if (!providerId) {
      localStorage.removeItem(USER_PROVIDER_ID_SELECTED);
      return;
    }
    localStorage.setItem(USER_PROVIDER_ID_SELECTED, providerId);
  }

  private get providerIdSelectedStore(): string | null {
    return localStorage.getItem(USER_PROVIDER_ID_SELECTED) ?? null;
  }

  constructor(
    @Inject(Constants) public readonly constants: Constants,
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly currentUserService: CurrentUserService,
    private readonly locationStrategy: LocationStrategy,
  ) {}

  checkAuth(): Observable<void> {
    const providerIdSelected = this.providerIdSelectedStore;
    const urlParams = new URLSearchParams(window.location.search);
    const authorizationCode = urlParams.get('code');
    const state = urlParams.get('state');

    if (!providerIdSelected || !authorizationCode) {
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
    if (!transaction || transaction.providerId !== providerIdSelected) {
      this.providerIdSelectedStore = null;
      sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
      clearOidcTransaction();
      return throwError(() => new Error('Invalid OIDC state'));
    }

    // Remove ?code= from the URL before the exchange so route guards cannot trigger a second redemption.
    this.clearOidcQueryParams();

    this.oidcCallbackInFlight = this.completeOidcCallback(providerIdSelected, authorizationCode, transaction).pipe(
      tap(() => sessionStorage.setItem(processedKey, '1')),
      finalize(() => {
        this.oidcCallbackInFlight = null;
      }),
      shareReplay(1),
      catchError(error => {
        this.providerIdSelectedStore = null;
        sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
        clearOidcTransaction();
        return throwError(() => error);
      }),
    );

    return this.oidcCallbackInFlight;
  }

  loginWithProvider(providerId: string, redirectUrl: string): Observable<void> {
    const identityProvider = this.constants.org.identityProviders?.find(idp => idp.id === providerId);
    if (!identityProvider) {
      throw new Error(`Identity provider ${providerId} not found!`);
    }

    return from(this.redirectToIdentityProvider(identityProvider, providerId, redirectUrl));
  }

  private async redirectToIdentityProvider(
    identityProvider: SocialIdentityProvider,
    providerId: string,
    redirectUrl: string,
  ): Promise<void> {
    this.providerIdSelectedStore = providerId;

    const redirectUri = this.getRedirectUri();
    sessionStorage.setItem(OIDC_REDIRECT_URI_KEY, redirectUri);

    const { state, codeChallenge } = await storeOidcTransaction({
      providerId,
      redirectUrl: redirectUrl ?? '/',
      redirectUri,
    });

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: identityProvider.clientId!,
      redirect_uri: redirectUri,
      scope: identityProvider.scopes?.join(identityProvider.scopeDelimiter ?? ' ') ?? 'openid',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    window.location.href = `${identityProvider.authorizationEndpoint}?${params.toString()}`;
  }

  loginWithApim({ username, password }: { username: string; password: string }, redirectUrl: string): Observable<void> {
    this.providerIdSelectedStore = null;

    return this.http
      .post<void>(
        `${this.constants.org.baseURL}/user/login`,
        {},
        {
          headers: {
            Authorization: `Basic ${base64Encoder(`${username}:${password}`)}`,
          },
        },
      )
      .pipe(
        switchMap(() => this.router.navigateByUrl(redirectUrl ?? '/')),
        map(() => undefined),
      );
  }

  logout(options: { disableRedirect?: boolean; redirectToCurrentUrl?: boolean } = {}) {
    const postLogoutRedirectUri = this.getPostLogoutRedirectUri();

    return this.http
      .post<{ logout_url?: string }>(`${this.constants.org.baseURL}/user/logout`, {
        post_logout_redirect_uri: postLogoutRedirectUri,
      })
      .pipe(
        catchError(() => of({} as { logout_url?: string })),
        switchMap(logoutResponse => {
          this.currentUserService.clearCurrent();
          this.providerIdSelectedStore = null;

          if (isSafeOidcLogoutUrl(logoutResponse?.logout_url)) {
            window.location.href = logoutResponse.logout_url!;
            return of(undefined);
          }

          if (!options.disableRedirect) {
            if (options.redirectToCurrentUrl) {
              return this.router.navigateByUrl(`/_login?redirect=${this.router.routerState.snapshot.url}`);
            }

            return this.router.navigateByUrl('/_login');
          }

          return of(undefined);
        }),
      );
  }

  signUpCustomUserFields(): Observable<CustomUserFields> {
    return this.http.get<CustomUserFields>(`${this.constants.org.baseURL}/configuration/custom-user-fields`);
  }

  signUp(userToCreate: { firstName: string; lastName: string; email: string; customFields?: Record<string, string> }): Observable<void> {
    return this.http.post<void>(`${this.constants.org.baseURL}/users/registration`, {
      firstname: userToCreate.firstName,
      lastname: userToCreate.lastName,
      email: userToCreate.email,
      customFields: userToCreate.customFields,
    });
  }

  signUpConfirm(userToConfirm: { token: string; password: string; firstName: string; lastName: string }): Observable<void> {
    return this.http.post<void>(`${this.constants.org.baseURL}/users/registration/finalize`, {
      token: userToConfirm.token,
      password: userToConfirm.password,
      firstname: userToConfirm.firstName,
      lastname: userToConfirm.lastName,
    });
  }

  resetPassword(userToReset: { userId: string; token: string; password: string; firstName: string; lastName: string }): Observable<void> {
    return this.http.post<void>(`${this.constants.org.baseURL}/users/${userToReset.userId}/changePassword`, {
      token: userToReset.token,
      password: userToReset.password,
      firstname: userToReset.firstName,
      lastname: userToReset.lastName,
    });
  }

  private completeOidcCallback(providerId: string, authorizationCode: string, transaction: OidcTransaction): Observable<void> {
    const identityProvider = this.constants.org.identityProviders?.find(idp => idp.id === providerId);
    if (!identityProvider) {
      return throwError(() => new Error(`Identity provider ${providerId} not found!`));
    }

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code: authorizationCode,
      redirect_uri: transaction.redirectUri,
      client_id: identityProvider.clientId!,
      code_verifier: transaction.codeVerifier,
    });

    return this.http
      .post(`${this.constants.org.baseURL}/auth/oauth2/${providerId}`, body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        responseType: 'text',
      })
      .pipe(
        switchMap(() => {
          sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
          // Drop any cached anonymous /user response before validating the new session cookie.
          this.currentUserService.clearCurrent();
          return this.currentUserService.current();
        }),
        switchMap(user => {
          if (!user) {
            return throwError(() => new Error('OIDC login failed'));
          }

          const redirectUrl = transaction.redirectUrl || '/';
          return from(this.router.navigateByUrl(redirectUrl, { replaceUrl: true }));
        }),
        map(() => undefined),
      );
  }

  private getRedirectUri(): string {
    return `${(window.location.origin + window.location.pathname).replace(/\/$/, '')}`;
  }

  private clearOidcQueryParams() {
    window.history.replaceState({}, '', window.location.pathname);
  }

  private getPostLogoutRedirectUri(): string {
    return `${this.getRedirectUri() + this.locationStrategy.prepareExternalUrl('/_login')}`;
  }
}

// see https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding#The_Unicode_Problem
// https://stackoverflow.com/questions/30106476/using-javascripts-atob-to-decode-base64-doesnt-properly-decode-utf-8-strings/53433503#53433503
// https://github.com/anonyco/BestBase64EncoderDecoder
const base64Encoder = (str: string): string => {
  // first we use encodeURIComponent to get percent-encoded UTF-8,
  // then we convert the percent encodings into raw bytes which
  // can be fed into btoa.
  return btoa(
    encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (match, p1) => {
      return String.fromCharCode(Number('0x' + p1));
    }),
  );
};
