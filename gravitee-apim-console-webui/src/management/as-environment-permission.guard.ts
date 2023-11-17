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
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, CanDeactivate, Router, RouterStateSnapshot } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

import { ManagementComponent } from './management.component';

import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';

@Injectable({
  providedIn: 'root',
})
export class AsEnvironmentPermissionGuard implements CanActivate, CanActivateChild, CanDeactivate<ManagementComponent> {
  constructor(private readonly gioPermissionService: GioPermissionService, private readonly router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this.gioPermissionService
      .loadEnvironmentPermissions(route.params.envId)
      .pipe(switchMap(() => this.canActivateChild(route, state)));
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    const permissions = route.data.apiPermissions?.only;
    if (!permissions) {
      return of(true);
    }
    if (this.gioPermissionService.hasAnyMatching(permissions)) {
      return of(true);
    }

    // TODO : redirect to 403 page
    this.router.navigate(['_login'], { queryParams: { redirect: state.url } });
    return of(false);
  }

  canDeactivate(_component: ManagementComponent, _currentRoute: ActivatedRouteSnapshot, _currentState: RouterStateSnapshot): boolean {
    this.gioPermissionService.clearEnvironmentPermissions();
    return true;
  }
}
