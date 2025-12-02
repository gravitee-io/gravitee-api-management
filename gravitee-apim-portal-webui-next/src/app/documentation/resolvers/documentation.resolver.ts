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
import {ActivatedRoute, ActivatedRouteSnapshot, ResolveFn, Router, RouterStateSnapshot} from '@angular/router';
import {map, switchMap, tap, timer} from 'rxjs';

import {PortalNavigationItemsService} from "../../../services/portal-navigation-items.service";
import {PortalPageContentService} from "../../../services/portal-page-content.service";
import {PortalNavigationItem, PortalNavigationPage} from "../../../entities/portal-navigation/portal-navigation-item";

// load everything under the selected folder and unfold all of its children
// path param for parent id
// query param for the selected child
// id = the root for that view
// only the page content is loaded incrementally
// maybe cache the page content
export const documentationResolver = ((route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const itemsService = inject(PortalNavigationItemsService);
  const contentService = inject(PortalPageContentService);
  const navId = route.params['navId'];
  const selectedNavId = route.queryParams['selectedNavId'];
  console.log('selectedNavId', selectedNavId);
  const currentItem = itemsService.topNavbarItems().find(item => item.id === navId);

  return itemsService
    .getNavigationItems('TOP_NAVBAR', !!navId, navId)
    .pipe(
      tap(res => {
        const seletedItem = res.find(item => item.id === selectedNavId) as PortalNavigationPage;
        if (seletedItem) {
          contentService.getPageContent(seletedItem.portalPageContentId).subscribe(res => console.log('res', res))
        }
      }),
      map(res => {
        return {
          currentItem,
          children: res
        }
      })
    )
});
