import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateChildFn,
  CanActivateFn,
  createUrlTreeFromSnapshot,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { of } from 'rxjs';

import { GioPermissionService } from './gio-permission.service';

const hasPermission = (gioPermissionService: GioPermissionService, permissions: string[]): boolean => {
  if (!permissions) {
    return true;
  }
  return gioPermissionService.hasAnyMatching(permissions);
};

export const PermissionGuard: {
  checkRouteDataPermissions: CanActivateFn | CanActivateChildFn;
} = {
  checkRouteDataPermissions: (route: ActivatedRouteSnapshot, _state: RouterStateSnapshot) => {
    const gioPermissionService = inject(GioPermissionService);
    const router = inject(Router);
    const permissions = route.data['permissions']?.anyOf;
    const unauthorizedFallbackTo = route.data['permissions']?.unauthorizedFallbackTo;
    if (hasPermission(gioPermissionService, permissions)) {
      return of(true);
    }
    if (unauthorizedFallbackTo) {
      const urlTree = createUrlTreeFromSnapshot(route, [unauthorizedFallbackTo]);
      router.navigateByUrl(urlTree);
      return of(false);
    }

    return of(false);
  },
};
