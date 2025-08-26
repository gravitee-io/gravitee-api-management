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
import { ActivatedRouteSnapshot, CanActivateFn, CanDeactivateFn, RouterStateSnapshot } from '@angular/router';
import { map } from 'rxjs/operators';

import { ApiNavigationComponent } from './api-navigation/api-navigation.component';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { NewtAIService } from '../../services-ngx/newtai.service';

export const ApisGuard: {
  loadPermissions: CanActivateFn;
  clearPermissions: CanDeactivateFn<unknown>;
} = {
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
