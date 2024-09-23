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
import { UserManager, WebStorageStateStore, Log } from 'oidc-client-ts';
import { Router } from '@angular/router';
import { from, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { LocationStrategy } from '@angular/common';

import { Constants } from '../entities/Constants';
import { CsrfInterceptor } from '../shared/interceptors/csrf.interceptor';
import { HeaderXRequestedWithInterceptor } from '../shared/interceptors/header-x-requested-with.interceptor';
import { CurrentUserService } from '../services-ngx/current-user.service';
import { CustomUserFields } from '../entities/customUserFields';
import { environment } from '../environments/environment';

const USER_PROVIDER_ID_SELECTED = 'user-provider-id-selected';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private oidcManagers: Record<string, UserManager> = {};

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
  ) {
    if (!environment.production) {
      Log.setLogger(console);
      Log.setLevel(Log.DEBUG);
    }

    // Initialize manager for each identity provider
    constants.org.identityProviders?.forEach((idp) => {
      this.oidcManagers[idp.id] = new UserManager({
        authority: idp.authorizationEndpoint,
        client_id: idp.clientId,
        redirect_uri: `${(window.location.origin + window.location.pathname).replace(/\/$/, '')}`,
        scope: idp.scopes?.join(idp.scopeDelimiter ?? ' '),
        response_type: 'code',
        post_logout_redirect_uri: `${(window.location.origin + window.location.pathname).replace(/\/$/, '') + this.locationStrategy.prepareExternalUrl('/_login')}`,
        userStore: new WebStorageStateStore({ store: window.localStorage }),
        loadUserInfo: false,
        extraHeaders: {
          // Needed for Gravitee APIM POST method
          [CsrfInterceptor.xsrfTokenHeaderName]: CsrfInterceptor.xsrfToken,
          [HeaderXRequestedWithInterceptor.xRequestedWithHeaderName]: HeaderXRequestedWithInterceptor.xRequestedWithValue,
        },
        fetchRequestCredentials: 'include',
        response_mode: 'query',

        metadata: {
          introspection_endpoint: idp.tokenIntrospectionEndpoint,
          authorization_endpoint: idp.authorizationEndpoint,
          end_session_endpoint:
            idp.userLogoutEndpoint ??
            `${(window.location.origin + window.location.pathname).replace(/\/$/, '') + this.locationStrategy.prepareExternalUrl('/_login')}`,
          token_endpoint: `${constants.org.baseURL}/auth/oauth2/${idp.id}`,
        },
      });
    });
  }

  checkAuth(): Observable<void> {
    return new Observable<void>((observer) => {
      const providerIdSelected = this.providerIdSelectedStore;
      if (!providerIdSelected) {
        // No provider selected, we can't check auth
        observer.next();
        observer.complete();
        return;
      }

      const oidcManager = this.oidcManagers[providerIdSelected];
      if (!oidcManager) {
        observer.error(new Error(`Identity provider ${providerIdSelected} not found!`));
        return;
      }

      oidcManager
        .getUser()
        .then((user) => {
          // If user still logged in
          if (user && !user.expired) {
            return user;
          }
          // If user not logged in, try call signinRedirectCallback
          return oidcManager.signinRedirectCallback().then((user) => {
            const redirectUrl = (user.state as string) ?? '/';
            return this.router.navigateByUrl(redirectUrl).then(() => user);
          });
        })
        .then((_user) => {
          // Ok we are authenticated
          observer.next();
          observer.complete();
        })
        .catch((error) => {
          // KO User should restart authentication
          this.providerIdSelectedStore = null;
          observer.error(error);
        });
    });
  }

  loginWithProvider(providerId: string, redirectUrl: string): Observable<void> {
    if (!this.oidcManagers[providerId]) {
      throw new Error(`Identity provider ${providerId} not found!`);
    }
    this.providerIdSelectedStore = providerId;

    return from(
      this.oidcManagers[providerId].signinRedirect({
        state: redirectUrl,
      }),
    );
  }

  loginWithApim({ username, password }: { username: string; password: string }, redirectUrl: string): Observable<void> {
    this.providerIdSelectedStore = null;

    return this.http
      .post<{
        access_token: string;
      }>(
        `${this.constants.org.baseURL}/user/login`,
        {},
        {
          headers: {
            Authorization: `Basic ${base64Encoder(`${username}:${password}`)}`,
          },
        },
      )
      .pipe(
        switchMap((_response) => {
          return this.router.navigateByUrl(redirectUrl ?? '/');
        }),
        map(() => null),
      );
  }

  logout(options: { disableRedirect?: boolean; redirectToCurrentUrl?: boolean } = {}) {
    return this.http.post(`${this.constants.org.baseURL}/user/logout`, {}).pipe(
      catchError(() => {
        // If logout failed, we can continue
        return of({});
      }),
      switchMap(() => {
        this.currentUserService.clearCurrent();

        const oidcManager = this.oidcManagers[this.providerIdSelectedStore];
        if (!oidcManager) {
          return of({});
        }
        this.providerIdSelectedStore = null;

        return from([oidcManager.removeUser(), oidcManager.signoutRedirect().catch(() => null), oidcManager.clearStaleState()]);
      }),
      switchMap(() => {
        if (!options.disableRedirect) {
          // If not logged with provider or if signoutRedirect() not configured for selected provider
          // redirect to login page

          if (options.redirectToCurrentUrl) {
            return this.router.navigateByUrl(`/_login?redirect=${this.router.routerState.snapshot.url}`);
          }

          return this.router.navigateByUrl('/_login');
        }
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
