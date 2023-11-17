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
import { Inject, Injectable } from '@angular/core';
import { intersection, toLower } from 'lodash';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import UserService from '../../../services/user.service';
import { ApiService } from '../../../services-ngx/api.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { User } from '../../../entities/user/user';
import { CurrentUserService as AjsCurrentUserService } from '../../../ajs-upgraded-providers';

@Injectable({ providedIn: 'root' })
export class GioPermissionService {
  private currentOrganizationPermissions: string[] = [];
  private currentApiPermissions: string[] = [];
  private currentEnvironmentPermissions: string[] = [];

  constructor(
    @Inject(AjsCurrentUserService) private readonly ajsCurrentUserService: UserService,
    private readonly apiService: ApiService,
    private readonly environmentService: EnvironmentService,
  ) {}

  loadOrganizationPermissions(user: User): void {
    const organizationPermissions = user.roles.filter((role) => role.scope === 'ORGANIZATION');
    this.currentOrganizationPermissions = organizationPermissions
      .flatMap((role) => Object.entries(role.permissions))
      .flatMap(([key, crudValues]) => crudValues.map((crudValue) => toLower(`ORGANIZATION-${key}-${crudValue}`)));

    // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
    // TODO: Remove when AngularJS API permissions are removed
    this.ajsCurrentUserService.currentUser.userPermissions = this.currentOrganizationPermissions;
    this.ajsCurrentUserService.reloadPermissions();
  }

  loadApiPermissions(apiId: string): Observable<void> {
    return this.apiService.getPermissions(apiId).pipe(
      map((apiPermissions) => {
        this.currentApiPermissions = Object.entries(apiPermissions).flatMap(([key, crudValues]) =>
          crudValues.map((crudValue) => toLower(`API-${key}-${crudValue}`)),
        );

        // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
        // TODO: Remove when AngularJS API permissions are removed
        this.ajsCurrentUserService.currentUser.userApiPermissions = this.currentApiPermissions;
        this.ajsCurrentUserService.reloadPermissions();
      }),
    );
  }

  loadEnvironmentPermissions(envId: string): Observable<void> {
    return this.environmentService.getPermissions(envId).pipe(
      map((envPermissions) => {
        this.currentEnvironmentPermissions = Object.entries(envPermissions).flatMap(([key, crudValues]) =>
          crudValues.map((crudValue) => toLower(`ENVIRONMENT-${key}-${crudValue}`)),
        );

        // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
        this.ajsCurrentUserService.currentUser.userEnvironmentPermissions = this.currentEnvironmentPermissions;
        this.ajsCurrentUserService.reloadPermissions();
      }),
    );
  }

  hasAnyMatching(permissions: string[]): boolean {
    if (!permissions || !this.ajsCurrentUserService.currentUser.userPermissions) {
      return false;
    }
    return (
      intersection(this.ajsCurrentUserService.currentUser.userPermissions, permissions).length > 0 ||
      intersection(this.ajsCurrentUserService.currentUser.userEnvironmentPermissions, permissions).length > 0 ||
      // Legacy: When AngularJS API|Application permissions are loaded
      intersection(this.ajsCurrentUserService.currentUser.userApiPermissions, permissions).length > 0 ||
      intersection(this.ajsCurrentUserService.currentUser.userApplicationPermissions, permissions).length > 0 ||
      // When Angular API permissions are loaded
      intersection(this.currentApiPermissions, permissions).length > 0 ||
      intersection(this.currentEnvironmentPermissions, permissions).length > 0
    );
  }

  clearEnvironmentPermissions() {
    this.currentEnvironmentPermissions = [];
  }

  clearApiPermissions() {
    this.currentApiPermissions = [];
  }
}
