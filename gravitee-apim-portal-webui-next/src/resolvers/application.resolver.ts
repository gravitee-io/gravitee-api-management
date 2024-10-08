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
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { Application, ApplicationType } from '../entities/application/application';
import { UserApplicationPermissions } from '../entities/permission/permission';
import { ApplicationService } from '../services/application.service';
import { PermissionsService } from '../services/permissions.service';

export const applicationResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  applicationService: ApplicationService = inject(ApplicationService),
): Observable<Application> => applicationService.get(route.params['applicationId'])) satisfies ResolveFn<Application>;

export const applicationTypeResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  applicationService: ApplicationService = inject(ApplicationService),
): Observable<ApplicationType> => applicationService.getType(route.params['applicationId'])) satisfies ResolveFn<ApplicationType>;

export const applicationPermissionResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  permissionsService: PermissionsService = inject(PermissionsService),
): Observable<UserApplicationPermissions> =>
  permissionsService.getApplicationPermissions(route.params['applicationId'])) satisfies ResolveFn<UserApplicationPermissions>;
