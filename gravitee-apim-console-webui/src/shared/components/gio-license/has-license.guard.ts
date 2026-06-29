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
import { ActivatedRouteSnapshot, CanActivateChildFn, Router, RouterStateSnapshot } from '@angular/router';
import { map, switchMap } from 'rxjs/operators';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

export interface GioRequireLicenseRouterData {
  license: {
    feature: string;
    context?: string;
    requiredTier?: string;
  };
  redirect: string;
}

export const HasLicenseGuard: CanActivateChildFn = (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
  const gioLicenseService = inject(GioLicenseService);
  const router = inject(Router);

  const licenseRouterData: GioRequireLicenseRouterData | undefined = route.data.requireLicense;
  if (!licenseRouterData) {
    // no license required
    return of(true);
  }

  const { feature, context, requiredTier } = licenseRouterData.license;

  // If a specific tier is required, check it first before the feature check.
  // This handles cases where a lower tier (e.g. galaxy) has the feature flag set
  // but the actual capability still requires a higher tier (e.g. universe).
  if (requiredTier) {
    return gioLicenseService.getLicense$().pipe(
      switchMap(license => {
        if (license?.tier !== requiredTier) {
          // Open the EE dialog so the user understands the upgrade path, then redirect.
          gioLicenseService.openDialog({ feature, context });
          router.navigate([licenseRouterData.redirect]);
          return of(false);
        }
        // Tier matches — still run the normal feature check as a safety net.
        return gioLicenseService.isMissingFeature$(feature).pipe(
          map(notAllowed => {
            if (notAllowed) {
              router.navigate([licenseRouterData.redirect]);
              return false;
            }
            return true;
          }),
        );
      }),
    );
  }

  return gioLicenseService.isMissingFeature$(feature).pipe(
    map(notAllowed => {
      if (notAllowed) {
        router.navigate([licenseRouterData.redirect]);
        return false;
      }

      return true;
    }),
  );
};
