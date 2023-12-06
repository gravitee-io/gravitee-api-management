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
import { Inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, CanDeactivate, Router, RouterStateSnapshot } from '@angular/router';
import { switchMap, tap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

import { ManagementComponent } from './management.component';

import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { EnvironmentService } from '../services-ngx/environment.service';
import { Constants } from '../entities/Constants';
import { EnvironmentSettingsService } from '../services-ngx/environment-settings.service';

@Injectable({
  providedIn: 'root',
})
export class HasEnvironmentPermissionGuard implements CanActivate, CanActivateChild, CanDeactivate<ManagementComponent> {
  constructor(
    private readonly gioPermissionService: GioPermissionService,
    private readonly environmentService: EnvironmentService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
    @Inject('Constants') private constants: Constants,
    private router: Router,
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const paramEnv = route.params.envId;

    return this.environmentService.list().pipe(
      switchMap((environments) => {
        this.constants.org.environments = environments;

        const currentEnvironment = environments.find((e) => e.id === paramEnv || e.hrids?.includes(paramEnv));

        if (!currentEnvironment) {
          this.router.navigate([environments[0].hrids[0] ?? environments[0].id]);
        }

        this.constants.org.currentEnv = currentEnvironment;

        if (paramEnv === currentEnvironment.id && currentEnvironment.hrids?.length > 0) {
          // Replace environment ID by hrid but keep url path
          this.router.navigateByUrl(state.url.replace(currentEnvironment.id, currentEnvironment.hrids[0]));
        }

        return this.gioPermissionService.loadEnvironmentPermissions(paramEnv);
      }),
      switchMap(() => this.environmentSettingsService.get()),
      tap((settings) => {
        // FIXME: this is a hack to make the environment settings available in the constants. Try to remove it.
        this.constants.env.settings = settings;
      }),
      switchMap(() => this.canActivateChild(route, state)),
    );
  }

  canActivateChild(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<boolean> {
    const permissions = route.data.apiPermissions?.only;
    if (!permissions) {
      return of(true);
    }
    if (this.gioPermissionService.hasAnyMatching(permissions)) {
      return of(true);
    }

    // TODO : redirect to 403 page
    this.router.navigate(['_login']);
    return of(false);
  }

  canDeactivate(_component: ManagementComponent, _currentRoute: ActivatedRouteSnapshot, _currentState: RouterStateSnapshot): boolean {
    this.gioPermissionService.clearEnvironmentPermissions();
    return true;
  }
}
