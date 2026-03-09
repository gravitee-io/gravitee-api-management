/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterStateSnapshot } from '@angular/router';
import { catchError, EMPTY, switchMap } from 'rxjs';

import { PortalPageContent } from '../entities/portal-navigation/portal-page-content';
import { PortalNavigationItemsService } from '../services/portal-navigation-items.service';

export const homepageContentResolver = ((
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot,
  portalNavigationItemsService: PortalNavigationItemsService = inject(PortalNavigationItemsService),
  router: Router = inject(Router),
) => {
  return portalNavigationItemsService.getNavigationItems('HOMEPAGE', false).pipe(
    switchMap(homepages => {
      if (!homepages?.length) {
        router.navigate(['catalog']);
        return EMPTY;
      }

      return portalNavigationItemsService.getNavigationItemContent(homepages[0].id);
    }),
    catchError(_ => {
      router.navigate(['catalog']);
      return EMPTY;
    }),
  );
}) satisfies ResolveFn<PortalPageContent>;
