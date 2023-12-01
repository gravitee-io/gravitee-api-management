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
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';

import { ConfigurationService } from './configuration.service';

/**
 * Checks if the user has access to a specific feature based on the provided route and configuration.
 *
 * @param {ActivatedRouteSnapshot} route - The current route.
 * @param {ConfigurationService} config - The configuration service used to check for feature access.
 * @param {Router} router - The router instance used for navigation.
 *
 * @return {boolean | UrlTree} - Returns true if the user has access to the feature. If the user does not have access,
 *                                it returns a UrlTree that represents the fallback redirect URL.
 */
export function canAccessFeature(route: ActivatedRouteSnapshot, config: ConfigurationService, router: Router) {
  if (route.data && route.data.expectedFeature) {
    if (!config.hasFeature(route.data.expectedFeature)) {
      return router.parseUrl(route.data.fallbackRedirectTo);
    }
  }
  return true;
}

export const featureGuard = ((route: ActivatedRouteSnapshot): boolean | UrlTree => {
  const config: ConfigurationService = inject(ConfigurationService);
  const router: Router = inject(Router);
  return canAccessFeature(route, config, router);
}) satisfies CanActivateFn;
