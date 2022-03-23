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
import { CurrentUserService } from './current-user.service';

@Injectable({ providedIn: 'root' })
export class PermissionGuardService implements CanActivate {
  constructor(private router: Router, private currentUserService: CurrentUserService) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    let canActivate: boolean | UrlTree = true;
    const routePermissions = route.data.permissions || {};
    const userPermissions = (this.currentUserService.get().getValue() && this.currentUserService.get().getValue().permissions) || {};
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
        if (!applicationRights || (applicationRights && !this.includesAll(applicationRights, expectedPermissionsObject[perm]))) {
          canActivate = this.router.parseUrl('/');
        }
      });
    }
    return canActivate;
  }

  includesAll(applicationRights, expectedRights): boolean {
    let includesAll = true;
    expectedRights.forEach(r => {
      if (!applicationRights.includes(r)) {
        includesAll = false;
      }
    });
    return includesAll;
  }
}
