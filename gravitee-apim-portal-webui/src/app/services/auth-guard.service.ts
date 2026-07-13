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
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';

import { Role } from '../model/role.enum';

import { OIDC_REDIRECT_STATE_KEY } from './auth.service';
import { CurrentUserService } from './current-user.service';

/**
 * Checks whether the user can activate a route based on authentication and expected user role.
 */
export function canActivateBasedOnAuth(
  route: ActivatedRouteSnapshot,
  currentUserService: CurrentUserService,
  router: Router,
): Promise<boolean | UrlTree> {
  if (route && route.data) {
    const expectedRole = route.data.expectedRole;
    if (expectedRole) {
      return new Promise(resolve => {
        const user = currentUserService.get().getValue();
        if ((expectedRole === Role.AUTH_USER && user == null) || (expectedRole === Role.GUEST && user)) {
          resolve(router.parseUrl(getOAuthRedirectPath() || '/'));
        } else {
          resolve(true);
        }
      });
    }
    return Promise.resolve(true);
  }
  return Promise.resolve(true);
}

function getOAuthRedirectPath(): string {
  const redirectPath = sessionStorage.getItem(OIDC_REDIRECT_STATE_KEY) ?? '';
  sessionStorage.removeItem(OIDC_REDIRECT_STATE_KEY);
  return decodeURIComponent(redirectPath);
}

export const authGuard = ((route: ActivatedRouteSnapshot): Promise<boolean | UrlTree> => {
  const currentUserService: CurrentUserService = inject(CurrentUserService);
  const router: Router = inject(Router);
  return canActivateBasedOnAuth(route, currentUserService, router);
}) satisfies CanActivateFn;
