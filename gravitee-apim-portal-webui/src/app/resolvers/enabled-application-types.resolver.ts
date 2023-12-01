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

import { ApplicationType, PortalService } from '../../../projects/portal-webclient-sdk/src/lib';

export const enabledApplicationTypesResolver = ((
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  portalService: PortalService = inject(PortalService),
): Promise<ApplicationType[]> =>
  portalService
    .getEnabledApplicationTypes()
    .toPromise()
    .then(response => response.data)) satisfies ResolveFn<ApplicationType[]>;
