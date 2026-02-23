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

import { inject, Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { intersection, toLower } from 'lodash';
import { map, shareReplay } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

import UserService from '../../../services/user.service';
import { ApiService } from '../../../services-ngx/api.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { User } from '../../../entities/user/user';
import { CurrentUserService as AjsCurrentUserService } from '../../../ajs-upgraded-providers';
import { ApplicationService } from '../../../services-ngx/application.service';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { GroupV2Service } from '../../../services-ngx/group-v2.service';
import { ClusterService } from '../../../services-ngx/cluster.service';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';

export type GioTestingPermission = string[];

export type GioTestingRoleScopePermission = {
  roleScope: 'API' | 'APPLICATION' | 'CLUSTER' | 'API_PRODUCT';
  id: string;
  permissions: string[];
};

export const GioTestingPermissionProvider = new InjectionToken<GioTestingPermission>('GioTestingPermission');
export const GioTestingRolesScopePermissionProvider = new InjectionToken<GioTestingRoleScopePermission[]>(
  'GioTestingRolesScopePermissionProvider',
);

@Injectable({ providedIn: 'root' })
export class GioPermissionService {
  private readonly apiProductV2Service = inject(ApiProductV2Service);

  private currentOrganizationPermissions: string[] = [];
  private currentApiPermissions: string[] = [];
  private currentEnvironmentPermissions: string[] = [];
  private currentApplicationPermissions: string[] = [];
  private currentIntegrationPermissions: string[] = [];
  private currentClusterPermissions: string[] = [];
  private currentApiProductPermissions: string[] = [];
  private permissions: string[] = [];
  private roleScopePermissionsCache = new Map<string, Observable<string[]>>();

  constructor(
    @Optional() @Inject(AjsCurrentUserService) private readonly ajsCurrentUserService: UserService | null,
    @Optional() @Inject(GioTestingPermissionProvider) private readonly gioTestingPermission: GioTestingPermission | null,
    @Optional()
    @Inject(GioTestingRolesScopePermissionProvider)
    private readonly gioTestingRolesScopePermissionProvider: GioTestingRoleScopePermission[] | null,
    private readonly apiService: ApiService,
    private readonly environmentService: EnvironmentService,
    private readonly applicationService: ApplicationService,
    private readonly integrationService: IntegrationsService,
    private readonly clusterService: ClusterService,
    private readonly groupService: GroupV2Service,
    private readonly apiProductV2Service: ApiProductV2Service,
  ) {
    if (this.gioTestingPermission) {
      this._setPermissions(this.gioTestingPermission);
    }
    if (this.gioTestingRolesScopePermissionProvider) {
      this.gioTestingRolesScopePermissionProvider.forEach(roleScopePermission => {
        this.roleScopePermissionsCache.set(
          `${roleScopePermission.roleScope}:${roleScopePermission.id}`,
          of(roleScopePermission.permissions).pipe(shareReplay({ bufferSize: 1, refCount: false })),
        );
      });
    }
  }

  loadOrganizationPermissions(user: User): void {
    const organizationPermissions = user.roles.filter(role => role.scope === 'ORGANIZATION');
    this.currentOrganizationPermissions = organizationPermissions
      .flatMap(role => Object.entries(role.permissions))
      .flatMap(([key, crudValues]) => crudValues.map(crudValue => toLower(`ORGANIZATION-${key}-${crudValue}`)));

    if (this.ajsCurrentUserService) {
      // For legacy AngularJS permissions. Make permission ajs directive work (see : PermPermissionStore)
      // TODO: Remove when AngularJS API permissions are removed
      this.ajsCurrentUserService.currentUser.userPermissions = this.currentOrganizationPermissions;
      this.ajsCurrentUserService.reloadPermissions();
    }
  }

  loadApiPermissions(apiId: string): Observable<void> {
    return this.apiService.getPermissions(apiId).pipe(
      map(apiPermissions => {
        this.currentApiPermissions = Object.entries(apiPermissions).flatMap(([key, crudValues]) =>
          crudValues.map(crudValue => toLower(`API-${key}-${crudValue}`)),
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
      map(envPermissions => {
        this.currentEnvironmentPermissions = Object.entries(envPermissions).flatMap(([key, crudValues]) =>
          crudValues.map(crudValue => toLower(`ENVIRONMENT-${key}-${crudValue}`)),
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
      map(applicationPermissions => {
        this.currentApplicationPermissions = Object.entries(applicationPermissions).flatMap(([key, crudValues]) =>
          crudValues.map(crudValue => toLower(`APPLICATION-${key}-${crudValue}`)),
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
      map(integrationPermissions => {
        this.currentIntegrationPermissions = Object.entries(integrationPermissions).flatMap(([key, crudValues]) =>
          crudValues.split('').map(crudValue => toLower(`integration-${key}-${crudValue}`)),
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

  loadClusterPermissions(clusterId: string): Observable<void> {
    return this.clusterService.getPermissions(clusterId).pipe(
      map(clusterPermissions => {
        this.currentClusterPermissions = Object.entries(clusterPermissions).flatMap(([key, crudValues]) =>
          crudValues.split('').map(crudValue => toLower(`cluster-${key}-${crudValue}`)),
        );
      }),
    );
  }

  loadApiProductPermissions(apiProductId: string): Observable<void> {
    return this.apiProductV2Service.getPermissions(apiProductId).pipe(
      map(apiProductPermissions => {
        this.currentApiProductPermissions = Object.entries(apiProductPermissions).flatMap(([key, crudValues]) =>
          crudValues.split('').map(crudValue => toLower(`API_PRODUCT-${key}-${crudValue}`)),
        );
      }),
    );
  }

  public fetchGroupPermissions(groupId: string): Observable<string[]> {
    return this.groupService
      .getPermissions(groupId)
      .pipe(
        map(integrationPermissions =>
          Object.entries(integrationPermissions).flatMap(([key, crudValues]) =>
            crudValues.split('').map(crudValue => toLower(`group-${key}-${crudValue}`)),
          ),
        ),
      );
  }

  /**
   * Retrieves the permissions associated with a specific role scope and ID.
   * A Cache is used to store previously fetched permissions to optimize performance and reduce redundant API calls.
   * The Cache is cleared with scope clear methods in the service.
   *
   * @param {('API'|'APPLICATION'|'CLUSTER'|'API_PRODUCT')} roleScope - The scope of the role.
   * @param {string} id - The unique identifier associated with the role.
   * @return {Observable<string[]>} An observable that emits an array of string permissions corresponding to the specified role and ID.
   */
  getPermissionsByRoleScope(roleScope: 'API' | 'APPLICATION' | 'CLUSTER' | 'API_PRODUCT', id: string): Observable<string[]> {
    const cacheKey = `${roleScope}:${id}`;

    if (this.roleScopePermissionsCache.has(cacheKey)) {
      return this.roleScopePermissionsCache.get(cacheKey)!;
    }

    const mapToStringPermissions = (permissions: Record<string, ('C' | 'R' | 'U' | 'D')[] | string>): string[] => {
      return Object.entries(permissions).flatMap(([key, crudValuesOrString]) => {
        const crudValues = Array.isArray(crudValuesOrString) ? crudValuesOrString : crudValuesOrString.split('');
        return crudValues.map(crudValue => toLower(`${roleScope}-${key}-${crudValue}`));
      });
    };

    let permissions$: Observable<string[]>;
    switch (roleScope) {
      case 'API':
        permissions$ = this.apiService.getPermissions(id).pipe(map(mapToStringPermissions));
        break;
      case 'APPLICATION':
        permissions$ = this.applicationService.getPermissions(id).pipe(map(mapToStringPermissions));
        break;
      case 'CLUSTER':
        permissions$ = this.clusterService.getPermissions(id).pipe(map(mapToStringPermissions));
        break;
      case 'API_PRODUCT':
        permissions$ = this.apiProductV2Service.getPermissions(id).pipe(map(mapToStringPermissions));
        break;
      default:
        permissions$ = of([]);
    }

    const shared$ = permissions$.pipe(shareReplay({ bufferSize: 1, refCount: false }));
    this.roleScopePermissionsCache.set(cacheKey, shared$);
    return shared$;
  }

  // Set static permissions for tests
  _setPermissions(permissions: GioTestingPermission): void {
    this.permissions = permissions ?? [];
  }

  hasAnyMatching(permissions: string[]): boolean {
    if (!permissions) {
      return false;
    }

    return (
      intersection(this.currentOrganizationPermissions, permissions).length > 0 ||
      intersection(this.currentEnvironmentPermissions, permissions).length > 0 ||
      intersection(this.currentApiPermissions, permissions).length > 0 ||
      intersection(this.currentApplicationPermissions, permissions).length > 0 ||
      intersection(this.currentIntegrationPermissions, permissions).length > 0 ||
      intersection(this.currentClusterPermissions, permissions).length > 0 ||
      intersection(this.currentApiProductPermissions, permissions).length > 0 ||
      intersection(this.permissions, permissions).length > 0
    );
  }

  clearEnvironmentPermissions() {
    this.currentEnvironmentPermissions = [];
  }

  clearApiPermissions() {
    this.currentApiPermissions = [];
    this.clearRoleScopePermissionsCache('API');
  }

  clearApiProductPermissions() {
    this.currentApiProductPermissions = [];
    this.clearRoleScopePermissionsCache('API_PRODUCT');
  }

  clearApplicationPermissions() {
    this.currentApplicationPermissions = [];
    this.clearRoleScopePermissionsCache('APPLICATION');
  }

  clearIntegrationPermissions() {
    this.currentIntegrationPermissions = [];
  }

  clearClusterPermissions() {
    this.currentClusterPermissions = [];
    this.clearRoleScopePermissionsCache('CLUSTER');
  }

  clearRoleScopePermissionsCache(scope: 'API' | 'APPLICATION' | 'CLUSTER' | 'API_PRODUCT') {
    Array.from(this.roleScopePermissionsCache.keys())
      .filter(key => key.startsWith(`${scope}:`))
      .forEach(key => this.roleScopePermissionsCache.delete(key));
  }

  hasAllMatching(permissions: string[]): boolean {
    if (!permissions || permissions.length === 0) {
      return false;
    }

    const allUserPermissions = [
      ...this.currentOrganizationPermissions,
      ...this.currentEnvironmentPermissions,
      ...this.currentApiPermissions,
      ...this.currentApplicationPermissions,
      ...this.currentApiProductPermissions,
      ...this.permissions,
    ];

    return permissions.every(permission => allUserPermissions.includes(permission));
  }
}
