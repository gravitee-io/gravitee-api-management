/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { CurrentUserService } from '../services/current-user.service';

/**
 * Guard that always requires authentication.
 * If user is not authenticated, redirects to login page.
 */
export const requireAuthGuard: CanActivateFn = (_route, state: RouterStateSnapshot) => {
  const authResult = checkUserAuthenticated();

  if (authResult === false) {
    const router = inject(Router);
    return router.createUrlTree(['/log-in'], { queryParams: { redirectUrl: state.url } });
  }

  return authResult;
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
