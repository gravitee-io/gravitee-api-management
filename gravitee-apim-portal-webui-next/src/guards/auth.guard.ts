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
import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { isEmpty } from 'lodash';

import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';

export const authGuard: CanActivateFn = (route, state) => {
  return checkUserAuthenticated() || forceLogin(state);
};

function checkUserAuthenticated() {
  if (inject(CurrentUserService).isAuthenticated()) {
    const redirectPath = getOAuthRedirectPath();
    return isEmpty(redirectPath) || inject(Router).parseUrl(redirectPath);
  }
  return false;
}

function getOAuthRedirectPath() {
  const oAuthService = inject(OAuthService);
  const redirectPath = decodeURIComponent(oAuthService.state ?? '');
  oAuthService.state = '';
  return redirectPath;
}

function forceLogin(state: RouterStateSnapshot) {
  if (isForceLoginEnabled() && !isRouteAnonymous(state.url)) {
    return inject(Router).navigate(['/log-in'], { queryParams: { redirectUrl: state.url } });
  }
  return true;
}

function isForceLoginEnabled(): boolean {
  return inject(ConfigService).configuration.authentication?.forceLogin?.enabled ?? false;
}

function isRouteAnonymous(url: string): boolean {
  return url.startsWith('/log-in') || url.startsWith('/sign-up') || url.startsWith('/log-in/reset-password');
}
