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
import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { intersection, toLower } from 'lodash';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import UserService from '../../../services/user.service';
import { ApiService } from '../../../services-ngx/api.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { User } from '../../../entities/user/user';
import { CurrentUserService as AjsCurrentUserService } from '../../../ajs-upgraded-providers';
import { ApplicationService } from '../../../services-ngx/application.service';
import { IntegrationsService } from '../../../services-ngx/integrations.service';

export type GioTestingPermission = string[];

export const GioTestingPermissionProvider = new InjectionToken<GioTestingPermission>('GioTestingPermission');

@Injectable({ providedIn: 'root' })
export class GioPermissionService {
  private currentOrganizationPermissions: string[] = [];
  private currentApiPermissions: string[] = [];
  private currentEnvironmentPermissions: string[] = [];
  private currentApplicationPermissions: string[] = [];
  private currentIntegrationPermissions: string[] = [];
  private permissions: string[] = [];

  constructor(
    @Optional() @Inject(AjsCurrentUserService) private readonly ajsCurrentUserService: UserService | null,
    @Optional() @Inject(GioTestingPermissionProvider) private readonly gioTestingPermission: GioTestingPermission | null,
    private readonly apiService: ApiService,
    private readonly environmentService: EnvironmentService,
    private readonly applicationService: ApplicationService,
    private readonly integrationService: IntegrationsService,
  ) {
    if (this.gioTestingPermission) {
      this._setPermissions(this.gioTestingPermission);
    }
  }

  loadOrganizationPermissions(user: User): void {
    const organizationPermissions = user.roles.filter((role) => role.scope === 'ORGANIZATION');
    this.currentOrganizationPermissions = organizationPermissions
      .flatMap((role) => Object.entries(role.permissions))
      .flatMap(([key, crudValues]) => crudValues.map((crudValue) => toLower(`ORGANIZATION-${key}-${crudValue}`)));

    if (this.ajsCurrentUserService) {
      // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
      // TODO: Remove when AngularJS API permissions are removed
      this.ajsCurrentUserService.currentUser.userPermissions = this.currentOrganizationPermissions;
      this.ajsCurrentUserService.reloadPermissions();
    }
  }

  loadApiPermissions(apiId: string): Observable<void> {
    return this.apiService.getPermissions(apiId).pipe(
      map((apiPermissions) => {
        this.currentApiPermissions = Object.entries(apiPermissions).flatMap(([key, crudValues]) =>
          crudValues.map((crudValue) => toLower(`API-${key}-${crudValue}`)),
        );

        if (this.ajsCurrentUserService) {
          // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
          // TODO: Remove when AngularJS API permissions are removed
          this.ajsCurrentUserService.currentUser.userApiPermissions = this.currentApiPermissions;
          this.ajsCurrentUserService.reloadPermissions();
        }
      }),
    );
  }

  loadEnvironmentPermissions(envId: string): Observable<void> {
    return this.environmentService.getPermissions(envId).pipe(
      map((envPermissions) => {
        this.currentEnvironmentPermissions = Object.entries(envPermissions).flatMap(([key, crudValues]) =>
          crudValues.map((crudValue) => toLower(`ENVIRONMENT-${key}-${crudValue}`)),
        );

        if (this.ajsCurrentUserService) {
          // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
          // TODO: Remove when AngularJS API permissions are removed
          this.ajsCurrentUserService.currentUser.userEnvironmentPermissions = this.currentEnvironmentPermissions;
          this.ajsCurrentUserService.reloadPermissions();
        }
      }),
    );
  }

  loadApplicationPermissions(applicationId: string): Observable<void> {
    return this.applicationService.getPermissions(applicationId).pipe(
      map((applicationPermissions) => {
        this.currentApplicationPermissions = Object.entries(applicationPermissions).flatMap(([key, crudValues]) =>
          crudValues.map((crudValue) => toLower(`APPLICATION-${key}-${crudValue}`)),
        );

        if (this.ajsCurrentUserService) {
          // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
          // TODO: Remove when AngularJS API permissions are removed
          this.ajsCurrentUserService.currentUser.userApplicationPermissions = this.currentApplicationPermissions;
          this.ajsCurrentUserService.reloadPermissions();
        }
      }),
    );
  }

  loadIntegrationPermissions(integrationId: string): Observable<void> {
    return this.integrationService.getPermissions(integrationId).pipe(
      map((integrationPermissions) => {
        this.currentIntegrationPermissions = Object.entries(integrationPermissions).flatMap(([key, crudValues]) =>
          crudValues.split('').map((crudValue) => toLower(`integration-${key}-${crudValue}`)),
        );

        if (this.ajsCurrentUserService) {
          // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
          // TODO: Remove when AngularJS API permissions are removed
          this.ajsCurrentUserService.currentUser.userIntegrationPermissions = this.currentIntegrationPermissions;
          this.ajsCurrentUserService.reloadPermissions();
        }
      }),
    );
  }

  // Set static permissions for tests
  _setPermissions(permissions: GioTestingPermission): void {
    this.permissions = permissions ?? [];
  }

  hasAnyMatching(permissions: string[]): boolean {
    if (!permissions) {
      return false;
    }

    const result =
      intersection(this.currentOrganizationPermissions, permissions).length > 0 ||
      intersection(this.currentEnvironmentPermissions, permissions).length > 0 ||
      intersection(this.currentApiPermissions, permissions).length > 0 ||
      intersection(this.currentApplicationPermissions, permissions).length > 0 ||
      intersection(this.currentIntegrationPermissions, permissions).length > 0 ||
      intersection(this.permissions, permissions).length > 0;
    return result;
  }

  clearEnvironmentPermissions() {
    this.currentEnvironmentPermissions = [];
  }

  clearApiPermissions() {
    this.currentApiPermissions = [];
  }

  clearApplicationPermissions() {
    this.currentApplicationPermissions = [];
  }

  clearIntegrationPermissions() {
    this.currentIntegrationPermissions = [];
  }
}
