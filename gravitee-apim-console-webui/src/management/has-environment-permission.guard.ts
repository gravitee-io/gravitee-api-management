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
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, CanDeactivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { GioMenuSearchService } from '@gravitee/ui-particles-angular';

import { ManagementComponent } from './management.component';
import { SettingsNavigationService } from './settings/settings-navigation/settings-navigation.service';

import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { EnvironmentService } from '../services-ngx/environment.service';
import { Constants } from '../entities/Constants';
import { EnvironmentSettingsService } from '../services-ngx/environment-settings.service';

const hasEnvironmentPermission = (gioPermissionService: GioPermissionService, permissions: string[]): boolean => {
  if (!permissions) {
    return true;
  }
  return gioPermissionService.hasAnyMatching(permissions);
};

const canActivate: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const router = inject(Router);
  const constants = inject(Constants);
  const environmentService = inject(EnvironmentService);
  const environmentSettingsService = inject(EnvironmentSettingsService);
  const gioPermissionService = inject(GioPermissionService);
  const gioMenuSearchService = inject(GioMenuSearchService);
  const settingsNavigationService = inject(SettingsNavigationService);

  const paramEnv = route.params.envId;

  return environmentService.list().pipe(
    switchMap((environments) => {
      constants.org.environments = environments;

      const currentEnvironment = environments.find((e) => e.id === paramEnv || e.hrids?.includes(paramEnv));

      if (!currentEnvironment) {
        router.navigate([environments[0].hrids[0] ?? environments[0].id]);
      }

      constants.org.currentEnv = currentEnvironment;

      if (paramEnv === currentEnvironment.id && currentEnvironment.hrids?.length > 0) {
        // Replace environment ID by hrid but keep url path
        router.navigateByUrl(state.url.replace(currentEnvironment.id, currentEnvironment.hrids[0]));
      }

      return gioPermissionService.loadEnvironmentPermissions(paramEnv);
    }),
    switchMap(() => environmentSettingsService.load()),
    switchMap(() => {
      const permissions = route.data.apiPermissions?.only;
      return of(hasEnvironmentPermission(gioPermissionService, permissions));
    }),
    map((canActivateChild) => {
      if (canActivateChild) {
        gioMenuSearchService.addMenuSearchItems(settingsNavigationService.getSettingsNavigationSearchItems(route.params.envId));
        return true;
      }
      // TODO : redirect to 403 page
      router.navigate(['_login']);
      return false;
    }),
  );
};

export const HasEnvironmentPermissionGuard: {
  canActivate: CanActivateFn;
  canActivateChild: CanActivateChildFn;
  canDeactivate: CanDeactivateFn<unknown>;
} = {
  canActivate,

  canActivateChild: (childRoute: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    const router = inject(Router);
    const permissions = childRoute.data.apiPermissions?.only;

    if (hasEnvironmentPermission(gioPermissionService, permissions)) {
      return of(true);
    }

    // TODO : redirect to 403 page
    router.navigate(['_login']);
    return of(false);
  },

  canDeactivate: (_component: ManagementComponent, currentRoute: ActivatedRouteSnapshot, _currentState: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    const gioMenuSearchService = inject(GioMenuSearchService);

    gioPermissionService.clearEnvironmentPermissions();
    gioMenuSearchService.removeMenuSearchItems([currentRoute.params.envId]);
    return true;
  },
};
