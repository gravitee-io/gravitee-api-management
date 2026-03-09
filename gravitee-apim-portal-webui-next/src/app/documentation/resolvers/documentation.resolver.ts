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
import { ActivatedRouteSnapshot, ResolveFn, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from '../../../services/portal-navigation-items.service';

export const documentationResolver = ((route: ActivatedRouteSnapshot): Observable<PortalNavigationItem | null> => {
  const topNavbarItems = inject(PortalNavigationItemsService).topNavbarItems();
  const router = inject(Router);
  const navId = route.params['navId'];

  if (!navId) {
    router.navigate(['/']);
    return of(null);
  }

  const navItem = topNavbarItems.find(item => item.id === navId);

  if (!navItem) {
    router.navigate(['/404']);
    return of(null);
  }

  return of(navItem);
}) satisfies ResolveFn<PortalNavigationItem | null>;
