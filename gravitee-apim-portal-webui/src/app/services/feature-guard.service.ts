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
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';

import { ConfigurationService } from './configuration.service';

@Injectable({ providedIn: 'root' })
export class FeatureGuardService implements CanActivate {
  constructor(private config: ConfigurationService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (route.data && route.data.expectedFeature) {
      if (!this.config.hasFeature(route.data.expectedFeature)) {
        return this.router.parseUrl(route.data.fallbackRedirectTo);
      }
    }
    return true;
  }
}
