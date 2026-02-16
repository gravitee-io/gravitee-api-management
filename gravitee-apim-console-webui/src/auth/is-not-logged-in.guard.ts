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

import { AuthService } from './auth.service';

import { CurrentUserService } from '../services-ngx/current-user.service';

export const IsNotLoggedInGuard: CanActivateFn = (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
  const authService = inject(AuthService);
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);

  return authService.checkAuth().pipe(
    switchMap(() => currentUserService.current()),
    map(user => {
      return !!user;
    }),
    catchError(() => {
      // If the user is not logged in, we can continue
      return of(false);
    }),
    map(isLoggedIn => {
      if (isLoggedIn) {
        const redirect = route.queryParams['redirect'];

        router.navigateByUrl(redirect ?? '/');
      }
      return !isLoggedIn;
    }),
  );
};
