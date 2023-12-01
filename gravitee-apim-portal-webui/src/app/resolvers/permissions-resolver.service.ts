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
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';

import { PermissionsResponse, PermissionsService } from '../../../projects/portal-webclient-sdk/src/lib';
import { CurrentUserService } from '../services/current-user.service';
import { ConfigurationService } from '../services/configuration.service';
import { FeatureEnum } from '../model/feature.enum';

export const permissionsResolver = ((
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  permissionsService: PermissionsService = inject(PermissionsService),
  currentUserService: CurrentUserService = inject(CurrentUserService),
  configurationService: ConfigurationService = inject(ConfigurationService),
): Promise<PermissionsResponse | void> => {
  if (currentUserService.exist()) {
    const params = route.params;
    if (params.applicationId) {
      const applicationId = params.applicationId;
      return permissionsService
        .getCurrentUserPermissions({ applicationId })
        .toPromise()
        .catch(() => ({}));
    } else if (params.apiId && configurationService.hasFeature(FeatureEnum.rating)) {
      const apiId = params.apiId;
      return permissionsService
        .getCurrentUserPermissions({ apiId })
        .toPromise()
        .catch(() => ({}));
    }
  }
  return Promise.resolve({});
}) satisfies ResolveFn<PermissionsResponse | void>;
