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
import { Role } from '../model/role.enum';
import { CurrentUserService } from './current-user.service';

@Injectable({ providedIn: 'root' })
export class PermissionGuardService implements CanActivate {

  constructor(private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const permissions = route.data.permissions;
    let canActivate: boolean | UrlTree = true;
    if (route.data && route.data.expectedPermissions && permissions) {
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
        if (!applicationRights ||
          (applicationRights && !this.includesAll(applicationRights, expectedPermissionsObject[perm]))) {
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
    })
    return includesAll;
  }
}
