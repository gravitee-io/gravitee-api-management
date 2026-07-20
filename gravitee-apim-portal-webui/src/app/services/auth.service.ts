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
import { HttpClient } from '@angular/common/http';
import { Injectable, Injector } from '@angular/core';
import { Router } from '@angular/router';

import { AuthenticationService, IdentityProvider, PortalService } from '../../../projects/portal-webclient-sdk/src/lib';

import {
  clearOidcTransaction,
  consumeOidcTransaction,
  isSafeOidcLogoutUrl,
  OidcTransaction,
  storeOidcTransaction,
} from './oidc-transaction.util';
import { ConfigurationService } from './configuration.service';
import { CurrentUserService } from './current-user.service';
import { NotificationService } from './notification.service';

export const OIDC_REDIRECT_STATE_KEY = 'oidc-redirect-state';
const USER_PROVIDER_ID_KEY = 'user-provider-id';
const OIDC_REDIRECT_URI_KEY = 'oidc-redirect-uri';
const OIDC_CODE_PROCESSED_PREFIX = 'oidc-code-processed:';

interface LogoutResponse {
  logout_url?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private oidcCallbackInFlight: Promise<boolean> | null = null;
  private authenticationService: AuthenticationService;
  private portalService: PortalService;
  private router: Router;

  constructor(
    private readonly configurationService: ConfigurationService,
    private readonly http: HttpClient,
    private readonly currentUserService: CurrentUserService,
    private readonly notificationService: NotificationService,
    private readonly injector: Injector,
  ) {}

  completeOidcLoginIfPresent(): Promise<boolean> {
    this.ensureSdkServices();

    const providerId = this.getProviderId();
    const urlParams = new URLSearchParams(window.location.search);
    const authorizationCode = urlParams.get('code');
    const state = urlParams.get('state');

    if (!providerId || !authorizationCode) {
      return Promise.resolve(true);
    }

    const processedKey = `${OIDC_CODE_PROCESSED_PREFIX}${authorizationCode}`;
    if (sessionStorage.getItem(processedKey)) {
      return Promise.resolve(true);
    }

    if (this.oidcCallbackInFlight) {
      return this.oidcCallbackInFlight;
    }

    const transaction = consumeOidcTransaction(state);
    if (!transaction || transaction.providerId !== providerId) {
      this.removeProviderId();
      sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
      clearOidcTransaction();
      return Promise.reject(new Error('Invalid OIDC state'));
    }

    this.clearOidcQueryParams();

    this.oidcCallbackInFlight = this.completeOidcCallback(providerId, authorizationCode, transaction)
      .then(() => {
        sessionStorage.setItem(processedKey, '1');
        return true;
      })
      .catch(() => {
        this.removeProviderId();
        sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
        clearOidcTransaction();
        return true;
      })
      .finally(() => {
        this.oidcCallbackInFlight = null;
      });

    return this.oidcCallbackInFlight;
  }

  login(username: string, password: string, redirectUrl = ''): Promise<boolean> {
    this.ensureSdkServices();

    return new Promise(resolve => {
      const authorization: string = 'Basic ' + this.encode(`${username}:${password}`);
      return this.authenticationService.login({ authorization }).subscribe(
        () => {
          this.removeProviderId();
          this.currentUserService.load().then(() => {
            this.router.navigate([redirectUrl]);
          });
        },
        () => {
          this.notificationService.error('login.notification.error');
          resolve(false);
        },
        () => resolve(true),
      );
    });
  }

  encode = str => {
    return btoa(
      encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, function toSolidBytes(match, p1) {
        return String.fromCharCode(Number('0x' + p1));
      }),
    );
  };

  authenticate(provider: IdentityProvider, redirectUrl: string) {
    if (provider?.id && provider.authorizationEndpoint && provider.client_id) {
      void this.redirectToIdentityProvider(provider, redirectUrl);
    }
  }

  logout(): Promise<boolean> {
    this.ensureSdkServices();

    return new Promise(resolve => {
      this.performLogout()
        .catch(() => resolve(false))
        .finally(() => resolve(true));
    });
  }

  removeProviderId() {
    localStorage.removeItem(USER_PROVIDER_ID_KEY);
  }

  storeProviderId(providerId: string) {
    localStorage.setItem(USER_PROVIDER_ID_KEY, providerId);
  }

  getProviderId(): string | null {
    return localStorage.getItem(USER_PROVIDER_ID_KEY);
  }

  private async redirectToIdentityProvider(provider: IdentityProvider, redirectUrl: string): Promise<void> {
    this.storeProviderId(provider.id);
    const redirectUri = this.getRedirectUri();
    sessionStorage.setItem(OIDC_REDIRECT_URI_KEY, redirectUri);

    const { state, codeChallenge } = await storeOidcTransaction({
      providerId: provider.id,
      redirectUrl,
      redirectUri,
    });

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: provider.client_id,
      redirect_uri: redirectUri,
      scope: provider.scopes?.join(' ') ?? 'openid',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    window.location.href = `${provider.authorizationEndpoint}?${params.toString()}`;
  }

  private completeOidcCallback(providerId: string, authorizationCode: string, transaction: OidcTransaction): Promise<void> {
    return this.fetchIdentityProvider(providerId).then(identityProvider => {
      if (!identityProvider?.id || !identityProvider.client_id) {
        return Promise.reject(new Error(`Identity provider ${providerId} not found!`));
      }

      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        code: authorizationCode,
        redirect_uri: transaction.redirectUri,
        client_id: identityProvider.client_id,
        code_verifier: transaction.codeVerifier,
      });

      const baseURL = this.configurationService.get('baseURL');

      return this.http
        .post(`${baseURL}/auth/oauth2/${identityProvider.id}`, body.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          responseType: 'text',
          withCredentials: true,
        })
        .toPromise()
        .then(() => {
          sessionStorage.removeItem(OIDC_REDIRECT_URI_KEY);
          return this.currentUserService.load();
        })
        .then(() => {
          const user = this.currentUserService.getUser();
          if (!user) {
            return Promise.reject(new Error('OIDC login failed'));
          }

          if (transaction.redirectUrl) {
            sessionStorage.setItem(OIDC_REDIRECT_STATE_KEY, transaction.redirectUrl);
          }
        });
    });
  }

  private performLogout(): Promise<void> {
    const baseURL = this.configurationService.get('baseURL');
    const postLogoutRedirectUri = this.getRedirectUri();

    return this.http
      .post<LogoutResponse>(`${baseURL}/auth/logout`, { post_logout_redirect_uri: postLogoutRedirectUri }, { withCredentials: true })
      .toPromise()
      .catch((): LogoutResponse => ({}))
      .then(logoutResponse => {
        this.currentUserService.revokeUser();
        this.removeProviderId();

        const logoutUrl = logoutResponse?.logout_url;
        if (isSafeOidcLogoutUrl(logoutUrl)) {
          window.location.href = logoutUrl!;
          return;
        }

        return this.router.navigate(['']).then(() => undefined);
      });
  }

  private fetchIdentityProvider(providerId: string): Promise<IdentityProvider | null> {
    this.ensureSdkServices();

    return this.portalService
      .getPortalIdentityProvider({ identityProviderId: providerId })
      .toPromise()
      .then(identityProvider => identityProvider ?? null)
      .catch(() => null);
  }

  private getRedirectUri(): string {
    return window.location.origin + (window.location.pathname === '/' ? '' : window.location.pathname.replace('/user/logout', '/'));
  }

  private clearOidcQueryParams() {
    window.history.replaceState({}, '', window.location.pathname);
  }

  private ensureSdkServices() {
    if (!this.authenticationService) {
      this.authenticationService = this.injector.get(AuthenticationService);
      this.portalService = this.injector.get(PortalService);
      this.router = this.injector.get(Router);
    }
  }
}
