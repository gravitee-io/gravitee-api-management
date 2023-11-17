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
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Inject, Injectable } from '@angular/core';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { GioPendoService } from '@gravitee/ui-analytics';

import { AuthService } from './auth.service';

import { CurrentUserService as AjsCurrentUserService } from '../ajs-upgraded-providers';
import { CurrentUserService } from '../services-ngx/current-user.service';
import UserService from '../services/user.service';
import { User } from '../entities/user';
import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';

@Injectable({
  providedIn: 'root',
})
export class IsLoggedInGuard implements CanActivate {
  constructor(
    private readonly authService: AuthService,
    private readonly currentUserService: CurrentUserService,
    private readonly router: Router,
    private readonly gioPendoService: GioPendoService,
    private readonly permissionService: GioPermissionService,
    @Inject('Constants') private readonly constants,
    @Inject(AjsCurrentUserService) private ajsCurrentUserService: UserService,
  ) {}

  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this.authService.checkAuth().pipe(
      switchMap(() => this.currentUserService.current()),
      map((user) => {
        if (!user) {
          throw new Error('User not logged in!');
        }

        // For legacy angularJs permission
        this.ajsCurrentUserService.currentUser = Object.assign(new User(), user);
        this.ajsCurrentUserService.currentUser.userApiPermissions = [];
        this.ajsCurrentUserService.currentUser.userEnvironmentPermissions = [];

        // Load Organization permissions
        this.permissionService.loadOrganizationPermissions(user);

        // Init Analytics with Pendo
        this.gioPendoService.initialize(
          {
            id: `${user.sourceId}`,
            email: `${user.email}`,
          },
          {
            id: this.constants.org?.settings?.analyticsPendo?.accountId,
            hrid: this.constants.org?.settings?.analyticsPendo?.accountHrid,
            type: this.constants.org?.settings?.analyticsPendo?.accountType,
          },
        );

        return true;
      }),
      catchError(() => {
        this.router.navigate(['/_login'], { queryParams: { redirect: state.url } });
        return of(false);
      }),
    );
  }
}
