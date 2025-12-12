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
import { ActivatedRouteSnapshot, BaseRouteReuseStrategy } from '@angular/router';

export class DocumentationRouteReuseStrategy extends BaseRouteReuseStrategy {
  /**
   * Avoid reusing the existing route when navId has stayed the same but the pageId has been reset to null.
   * This allows to reload the component after the pageId has been reset.
   *
   * This only happens when the currently selected top level item is clicked.
   */
  override shouldReuseRoute(future: ActivatedRouteSnapshot, current: ActivatedRouteSnapshot): boolean {
    if (this.isDocumentationRoute(current)) {
      const [currentNavId, futureNavId] = [this.getNavId(current), this.getNavId(future)];
      const isSameNavId = currentNavId === futureNavId && currentNavId !== null && futureNavId !== null;

      if (isSameNavId) {
        const [currentPageId, newPageId] = [this.getPageId(current), this.getPageId(future)];
        const isPageIdReset = currentPageId !== null && newPageId === null;

        if (isPageIdReset) {
          return false;
        }
      }
    }

    return future.routeConfig === current.routeConfig;
  }

  isDocumentationRoute(route: ActivatedRouteSnapshot): boolean {
    return route.routeConfig?.path === ':navId' && route.parent?.routeConfig?.path === 'documentation';
  }

  getNavId(route: ActivatedRouteSnapshot) {
    return route.paramMap.get('navId');
  }

  getPageId(route: ActivatedRouteSnapshot) {
    return route.queryParamMap.get('pageId');
  }
}
