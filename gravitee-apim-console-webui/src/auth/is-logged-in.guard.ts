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
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { GioPendoService } from '@gravitee/ui-analytics';

import { AuthService } from './auth.service';

import { CurrentUserService as AjsCurrentUserService } from '../ajs-upgraded-providers';
import { CurrentUserService } from '../services-ngx/current-user.service';
import { User } from '../entities/user';
import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { GioRoleService } from '../shared/components/gio-role/gio-role.service';
import { Constants } from '../entities/Constants';

export const IsLoggedInGuard: CanActivateFn = (_route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const authService = inject(AuthService);
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);
  const gioPendoService = inject(GioPendoService);
  const permissionService = inject(GioPermissionService);
  const roleService = inject(GioRoleService);
  const constants = inject(Constants);
  const ajsCurrentUserService: any = inject(AjsCurrentUserService);

  return authService.checkAuth().pipe(
    switchMap(() => currentUserService.current()),
    map(user => {
      if (!user) {
        throw new Error('User not logged in!');
      }

      // For legacy angularJs permission
      ajsCurrentUserService.currentUser = Object.assign(new User(), user);
      ajsCurrentUserService.currentUser.userApiPermissions = [];
      ajsCurrentUserService.currentUser.userEnvironmentPermissions = [];

      // Load Organization permissions
      permissionService.loadOrganizationPermissions(user);
      roleService.loadCurrentUserRoles(user);

      // Init Analytics with Pendo
      gioPendoService.initialize(
        {
          id: `${user.sourceId}`,
          email: `${user.email}`,
        },
        {
          id: constants.org?.settings?.analyticsPendo?.accountId,
          hrid: constants.org?.settings?.analyticsPendo?.accountHrid,
          type: constants.org?.settings?.analyticsPendo?.accountType,
        },
      );

      return true;
    }),
    catchError(() => {
      // If the user is not logged in, we logout (to clean auth stuff) and redirect to login page
      return authService
        .logout({
          disableRedirect: true,
        })
        .pipe(
          catchError(() => of({})),
          switchMap(() => {
            router.navigate(['/_login'], { queryParams: { redirect: state.url } });
            return of(false);
          }),
        );
    }),
  );
};
