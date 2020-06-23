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
import { Injectable, Injector } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { CurrentUserService } from './current-user.service';
import { NotificationService } from './notification.service';
import { ConfigurationService } from './configuration.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { AuthenticationService, PortalService } from '@gravitee/ng-portal-webclient';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authenticationService: AuthenticationService;
  private portalService: PortalService;
  private router: Router;

  constructor(
    private configurationService: ConfigurationService,
    private oauthService: OAuthService,
    private currentUserService: CurrentUserService,
    private notificationService: NotificationService,
    private injector: Injector,
  ) {}

  load() {
    // lazy injection to wait for base path injection and router init
    this.authenticationService = this.injector.get(AuthenticationService);
    this.portalService = this.injector.get(PortalService);
    this.router = this.injector.get(Router);
    return new Promise((resolve) => {
      if (this.getProviderId()) {
        this._fetchProviderAndConfigure().then(() => {
          this.oauthService.tryLoginCodeFlow().finally(() => resolve(true));
        });
      } else {
        resolve(true);
      }
    });
  }

  login(username: string, password: string, redirectUrl: string = ''): Promise<boolean> {
    return new Promise((resolve) => {
      const authorization: string = 'Basic ' + btoa(`${username}:${password}`);
      return this.authenticationService.login({ Authorization: authorization }).subscribe(
        () => {
          this.currentUserService.load().then(() => {
            this.router.navigate([redirectUrl]);
          });
        },
        () => {
          this.notificationService.error(i18n('login.notification.error'));
          resolve(false);
        },
        () => resolve(true)
      );
    });
  }

  authenticate(provider) {
    if (provider) {
      this.storeProviderId(provider.id);
      this._configure(provider);
      this.oauthService.initCodeFlow();
    }
  }

  logout(): Promise<boolean> {
    return new Promise((resolve) => {
      if (this.getProviderId()) {
        this._fetchProviderAndConfigure().finally(() => {
          this.removeProviderId();
          this._logout(resolve);
        });
      } else {
        this._logout(resolve);
      }
    });
  }

  private _logout(resolve) {
    this.authenticationService.logout().toPromise().then(() => {
      this.currentUserService.revokeUser();
      if (this.getProviderId()) {
        this.oauthService.logOut();
      }
      this.router.navigate(['']);
    })
      .catch(() => resolve(false))
      .finally(() => resolve(true));
  }

  private _configure(provider) {
    const redirectUri = window.location.origin;
    this.oauthService.configure({
      clientId: provider.client_id,
      loginUrl: provider.authorizationEndpoint,
      tokenEndpoint: this.configurationService.get('baseURL') + '/auth/oauth2/' + provider.id,
      requireHttps: false,
      issuer: provider.tokenIntrospectionEndpoint,
      logoutUrl: provider.userLogoutEndpoint,
      scope: provider.scopes.join(' '),
      responseType: 'code',
      redirectUri,
      postLogoutRedirectUri: redirectUri,
    });
  }

  private _fetchProviderAndConfigure(): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.portalService.getPortalIdentityProvider({ identityProviderId: this.getProviderId() }).toPromise().then(
        (identityProvider) => {
          if (identityProvider) {
            this._configure(identityProvider);
            resolve(true);
          } else {
            resolve(false);
          }
        },
        () => resolve(false)
      );
    });
  }

  removeProviderId() {
    localStorage.removeItem('user-provider-id');
  }

  storeProviderId(providerId) {
    localStorage.setItem('user-provider-id', providerId);
  }

  getProviderId(): string {
    return localStorage.getItem('user-provider-id');
  }
}
