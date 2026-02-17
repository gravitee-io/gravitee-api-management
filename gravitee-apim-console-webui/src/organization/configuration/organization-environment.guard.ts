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
import { CanActivateFn } from '@angular/router';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';

import { EnvironmentService } from '../../services-ngx/environment.service';
import { Constants } from '../../entities/Constants';

/**
 * Guard that ensures environments are loaded into Constants before
 * organization routes activate. This prevents services that rely on
 * `constants.org.currentEnv` from falling back to "DEFAULT" when the
 * user navigates directly to an organization page (e.g., on refresh).
 *
 * The management EnvironmentGuard will overwrite these values with
 * URL-aware data when the user enters management routes.
 */
export const OrganizationEnvironmentGuard: CanActivateFn = () => {
  const constants = inject(Constants);
  const environmentService = inject(EnvironmentService);

  if (constants.org.environments?.length > 0 && constants.org.currentEnv) {
    return of(true);
  }

  return environmentService.list().pipe(
    map((environments) => {
      if (environments?.length > 0) {
        constants.org.environments = environments;
        constants.org.currentEnv = environments[0];
      }
      return true;
    }),
    catchError((_) => {
      return of(true);
    }),
  );
};
