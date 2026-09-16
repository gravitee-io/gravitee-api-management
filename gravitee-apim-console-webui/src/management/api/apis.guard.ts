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
import { ActivatedRouteSnapshot, CanActivateFn, CanDeactivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { map } from 'rxjs/operators';

import { ApiNavigationComponent } from './api-navigation/api-navigation.component';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';
import { NewtAIService } from '../../services-ngx/newtai.service';

export const ApisGuard: {
  loadPermissions: CanActivateFn;
  clearPermissions: CanDeactivateFn<unknown>;
  denyNativeApi: CanActivateFn;
} = {
  /**
   * Refuses a route for a NATIVE API, sending the user to the API's own pages instead.
   *
   * Hiding a menu entry only hides the entry. The legacy Alerts screen offers HTTP conditions that a
   * Kafka API's gateway can never satisfy, so without this a bookmark or a pasted link would still let
   * someone save a trigger that never fires.
   *
   * `type` lives on the V4 shape alone, so the union is narrowed on `definitionVersion` first — which
   * is also what the check means: NATIVE is a V4-only API type.
   */
  denyNativeApi: (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
    const router = inject(Router);
    return inject(ApiV2Service)
      .get(route.params.apiId)
      .pipe(map(api => (api.definitionVersion === 'V4' && api.type === 'NATIVE' ? router.parseUrl(`/apis/${route.params.apiId}`) : true)));
  },

  loadPermissions: (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    inject(NewtAIService).addToContext('apiId', route.params.apiId);

    return gioPermissionService.loadApiPermissions(route.params.apiId).pipe(
      map(() => {
        return true;
      }),
    );
  },

  clearPermissions: (_component: ApiNavigationComponent, _currentRoute: ActivatedRouteSnapshot, _currentState: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    inject(NewtAIService).removeToContext('apiId');
    gioPermissionService.clearApiPermissions();
    return true;
  },
};
