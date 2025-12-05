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
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { UserEnvironmentPermissions } from '../entities/permission/permission';
import { CurrentUserService } from '../services/current-user.service';

type PermissionAction = 'C' | 'R' | 'U' | 'D';

type PermissionResource = keyof UserEnvironmentPermissions;

interface PermissionConfig {
  anyOf?: string[];
  unauthorizedFallbackTo?: string;
}

interface ParsedPermission {
  resource: PermissionResource;
  action: PermissionAction;
}

/**
 * Guard that checks if user has required environment-level permissions.
 *
 * Note: This guard only checks UserEnvironmentPermissions (available in current user).
 *
 * Expects route data with 'permissions.anyOf' array in format: ['RESOURCE-ACTION'] (e.g., ['APPLICATION-C'])
 * - RESOURCE: key from UserEnvironmentPermissions (e.g., 'APPLICATION', 'USER')
 * - ACTION: one of 'C' (Create), 'R' (Read), 'U' (Update), 'D' (Delete)
 *
 * Optionally supports 'permissions.unauthorizedFallbackTo' to specify redirect path.
 * If user doesn't have any of the required permissions:
 * - Redirects to 'permissions.unauthorizedFallbackTo' if specified
 * - Otherwise redirects to parent route (if exists) or homepage ('/')
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): boolean | UrlTree => {
  const permissionsConfig = getPermissionsConfig(route);
  if (!permissionsConfig) {
    return true;
  }

  const user = inject(CurrentUserService).user();
  const hasPermission = checkUserHasAnyPermission(user.permissions, permissionsConfig.anyOf || []);

  if (hasPermission) {
    return true;
  }

  return redirectUnauthorized(route, permissionsConfig);
};

function getPermissionsConfig(route: ActivatedRouteSnapshot): PermissionConfig | null {
  const permissions = route.data?.['permissions'] as PermissionConfig | undefined;

  if (!permissions?.anyOf || permissions.anyOf.length === 0) {
    return null;
  }

  return permissions;
}

function checkUserHasAnyPermission(userPermissions: UserEnvironmentPermissions | undefined, requiredPermissions: string[]): boolean {
  if (!userPermissions) {
    return false;
  }

  return requiredPermissions.some(requiredPermission => {
    const parsed = parsePermission(requiredPermission);
    if (!parsed) {
      return false;
    }

    return hasUserPermission(userPermissions, parsed);
  });
}

function parsePermission(permission: string): ParsedPermission | null {
  const [resource, action] = permission.split('-');

  if (!resource || !action) {
    console.warn(`Invalid permission format: ${permission}. Expected format: 'RESOURCE-ACTION'`);
    return null;
  }

  if (!isValidAction(action)) {
    console.warn(`Invalid permission action: ${action}. Expected one of: C, R, U, D`);
    return null;
  }

  return {
    resource: resource as PermissionResource,
    action: action as PermissionAction,
  };
}

function isValidAction(action: string): action is PermissionAction {
  return ['C', 'R', 'U', 'D'].includes(action);
}

function hasUserPermission(userPermissions: UserEnvironmentPermissions, parsed: ParsedPermission): boolean {
  const resourcePermissions = userPermissions[parsed.resource];
  return resourcePermissions?.includes(parsed.action) || false;
}

function redirectUnauthorized(route: ActivatedRouteSnapshot, config: PermissionConfig): UrlTree {
  const router = inject(Router);

  if (config.unauthorizedFallbackTo) {
    return router.parseUrl(config.unauthorizedFallbackTo);
  }

  const parentRoute = route.parent?.routeConfig?.path || '/';
  return router.parseUrl(parentRoute);
}
