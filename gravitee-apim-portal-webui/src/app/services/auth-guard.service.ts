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
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Role } from '../model/role.enum';
import { CurrentUserService } from './current-user.service';

@Injectable({ providedIn: 'root' })
export class AuthGuardService implements CanActivate {
  constructor(private currentUserService: CurrentUserService, private router: Router, private oauthService: OAuthService) {}

  canActivate(route: ActivatedRouteSnapshot): Promise<boolean | UrlTree> {
    if (route && route.data) {
      const expectedRole = route.data.expectedRole;
      if (expectedRole) {
        return new Promise(resolve => {
          const user = this.currentUserService.get().getValue();
          if ((expectedRole === Role.AUTH_USER && user == null) || (expectedRole === Role.GUEST && user)) {
            // 📝 Check OAuth state to find redirectUrl if exist
            resolve(this.router.parseUrl(decodeURIComponent(this.oauthService.state ?? '/')));
          } else {
            resolve(true);
          }
        });
      }
      return Promise.resolve(true);
    }
    return Promise.resolve(true);
  }
}
