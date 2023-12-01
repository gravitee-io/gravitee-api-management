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

import { CurrentUserService } from './current-user.service';

export function checkPermission(route: ActivatedRouteSnapshot, currentUserService: CurrentUserService, router: Router) {
  let canActivate: boolean | UrlTree = true;
  const routePermissions = route.data.permissions || {};
  const userPermissions = (currentUserService.get().getValue() && currentUserService.get().getValue().permissions) || {};
  // concat permission permission related to the current page with the user permission
  // otherwise some permission validations may fails.
  const permissions = { ...routePermissions, ...userPermissions };
  if (permissions && route.data && route.data.expectedPermissions) {
    const expectedPermissions = route.data.expectedPermissions;
    const expectedPermissionsObject = {};
    expectedPermissions.map(perm => {
      const splittedPerms = perm.split('-');
      if (expectedPermissionsObject[splittedPerms[0]]) {
        expectedPermissionsObject[splittedPerms[0]].push(splittedPerms[1]);
      } else {
        expectedPermissionsObject[splittedPerms[0]] = [splittedPerms[1]];
      }
    });
    Object.keys(expectedPermissionsObject).forEach(perm => {
      const applicationRights = permissions[perm];
      if (!applicationRights || (applicationRights && !includesAll(applicationRights, expectedPermissionsObject[perm]))) {
        canActivate = router.parseUrl('/');
      }
    });
  }
  return canActivate;
}

export const permissionGuard = ((route: ActivatedRouteSnapshot): boolean | UrlTree => {
  const currentUserService: CurrentUserService = inject(CurrentUserService);
  const router: Router = inject(Router);
  return checkPermission(route, currentUserService, router);
}) satisfies CanActivateFn;

function includesAll(applicationRights: string[], expectedRights: string[]): boolean {
  let includesAll = true;
  expectedRights.forEach(r => {
    if (!applicationRights.includes(r)) {
      includesAll = false;
    }
  });
  return includesAll;
}
