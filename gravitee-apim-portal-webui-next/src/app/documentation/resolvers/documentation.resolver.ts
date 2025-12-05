/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {forkJoin, map, tap} from 'rxjs';

import {PortalNavigationItemsService} from "../../../services/portal-navigation-items.service";
import {of} from "rxjs/internal/observable/of";

export const documentationResolver = ((route: ActivatedRouteSnapshot) => {
  const itemsService = inject(PortalNavigationItemsService);
  const navItem = itemsService.topNavbarItems().find(item => item.id === route.params['navId']);

  if (!navItem) {
    inject(Router).navigate(['/404']);
    return of(null);
  } else {
    return of({ navItem });
  }

  // if (!navItem) {
  //   inject(Router).navigate(['/404']);
  //   return of(null);
  // } else if (navItem.type === 'FOLDER') {
  //   const selectedPageId = route.queryParams['pageId'];
  //   // console.log('selectedPageId', selectedPageId);
  //   return forkJoin([
  //     itemsService.getNavigationItems('TOP_NAVBAR', true, navItem.id),
  //     selectedPageId ? itemsService.getNavigationItemContent(selectedPageId) : of(''),
  //   ]).pipe(
  //     // tap(([_, selectedPageContent]) => console.log(selectedPageContent)),
  //     map(([children, selectedPageContent]) => ({
  //       navItem,
  //       children,
  //       selectedPageContent: '',
  //     }))
  //   );
  // } else {
  //   return of({ navItem });
  // }
});
