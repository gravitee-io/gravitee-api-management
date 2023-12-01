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
import { OAuthService } from 'angular-oauth2-oidc';

import { Role } from '../model/role.enum';

import { CurrentUserService } from './current-user.service';

/**
 * Checks whether the user can activate a route based on authentication and expected user role.
 *
 * @param {ActivatedRouteSnapshot} route - The activated route snapshot.
 * @param {CurrentUserService} currentUserService - Service to get the current user information.
 * @param {Router} router - The router service.
 * @param {OAuthService} oauthService - The OAuth service for authentication and authorization.
 *
 * @return {Promise<boolean | UrlTree>} A promise that resolves to either `true` if the user can activate the route or a `UrlTree` object representing a redirect URL.
 */
export function canActivateBasedOnAuth(
  route: ActivatedRouteSnapshot,
  currentUserService: CurrentUserService,
  router: Router,
  oauthService: OAuthService,
): Promise<boolean | UrlTree> {
  if (route && route.data) {
    const expectedRole = route.data.expectedRole;
    if (expectedRole) {
      return new Promise(resolve => {
        const user = currentUserService.get().getValue();
        if ((expectedRole === Role.AUTH_USER && user == null) || (expectedRole === Role.GUEST && user)) {
          // 📝 Check OAuth state to find redirectUrl if exist
          resolve(router.parseUrl(decodeURIComponent(oauthService.state ?? '/')));
        } else {
          resolve(true);
        }
      });
    }
    return Promise.resolve(true);
  }
  return Promise.resolve(true);
}

export const authGuard = ((route: ActivatedRouteSnapshot): Promise<boolean | UrlTree> => {
  const currentUserService: CurrentUserService = inject(CurrentUserService);
  const router: Router = inject(Router);
  const oauthService: OAuthService = inject(OAuthService);
  return canActivateBasedOnAuth(route, currentUserService, router, oauthService);
}) satisfies CanActivateFn;
