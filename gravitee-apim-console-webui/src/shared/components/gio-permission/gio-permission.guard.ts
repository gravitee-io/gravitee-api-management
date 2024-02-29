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
import {
  ActivatedRouteSnapshot,
  CanActivateChildFn,
  CanActivateFn,
  createUrlTreeFromSnapshot,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { of } from 'rxjs';

import { GioPermissionService } from './gio-permission.service';

const hasPermission = (gioPermissionService: GioPermissionService, permissions: string[]): boolean => {
  if (!permissions) {
    return true;
  }
  return gioPermissionService.hasAnyMatching(permissions);
};

export const PermissionGuard: {
  checkRouteDataPermissions: CanActivateFn | CanActivateChildFn;
} = {
  checkRouteDataPermissions: (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    const router = inject(Router);
    const permissions = route.data.permissions?.anyOf;
    const unauthorizedFallbackTo = route.data.permissions?.unauthorizedFallbackTo;
    if (hasPermission(gioPermissionService, permissions)) {
      return of(true);
    }
    if (unauthorizedFallbackTo) {
      const urlTree = createUrlTreeFromSnapshot(route, [unauthorizedFallbackTo]);
      router.navigateByUrl(urlTree);
      return of(false);
    }

    // TODO : redirect to 403 page
    return of(false);
  },
};
