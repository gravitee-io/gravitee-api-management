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
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GioLicenseService, LicenseOptions, MenuSearchItem } from '@gravitee/ui-particles-angular';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { cleanRouterLink } from '../../../util/router-link.util';

export interface MenuItem {
  routerLink: string;
  displayName: string;
  permissions?: string[];
  licenseOptions?: LicenseOptions;
  iconRight$?: Observable<any>;
}

export interface GroupItem {
  title: string;
  items: MenuItem[];
}

export const ORGANIZATION_MENU_GROUP_ID = 'organization-menu';

@Injectable({
  providedIn: 'root',
})
export class OrganizationNavigationService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  public getOrganizationNavigationRoutes(): GroupItem[] {
    let groupItems: GroupItem[] = [];
    if (this.permissionService.hasAnyMatching(['organization-settings-r', 'organization-settings-u'])) {
      groupItems = [
        this.appendConsoleItems(),
        this.appendUserManagementItems(),
        this.appendGatewayItems(),
        this.appendNotificationsItems(),
        this.appendAuditItems(),
        this.appendCockpitItems(),
      ].filter(item => item != null);
    }
    return groupItems;
  }

  public getOrganizationNavigationSearchItems(): MenuSearchItem[] {
    return this.getOrganizationNavigationRoutes().flatMap(groupItem =>
      groupItem.items.map(item => {
        return {
          name: item.displayName,
          routerLink: `/_organization/${cleanRouterLink(item.routerLink)}`,
          category: `Organization / ${groupItem.title}`,
          groupIds: [ORGANIZATION_MENU_GROUP_ID],
        };
      }),
    );
  }

  private appendConsoleItems(): GroupItem {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Authentication',
        routerLink: 'identities',
        permissions: ['organization-identity_provider-r'],
      },
      {
        displayName: 'Settings',
        routerLink: 'settings',
        permissions: ['organization-settings-r'],
      },
    ]);
    return items.length > 0 ? { title: 'Console', items } : null;
  }

  private appendUserManagementItems(): GroupItem {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Users',
        routerLink: 'users',
        permissions: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
      },
      {
        displayName: 'Roles',
        routerLink: 'roles',
        permissions: ['organization-role-r'],
      },
    ]);
    return items.length > 0 ? { title: 'User Management', items } : null;
  }

  private appendGatewayItems(): GroupItem {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Entrypoints & Sharding Tags',
        routerLink: 'entrypoints-and-sharding-tags',
        permissions: ['organization-tag-r'],
      },
      {
        displayName: 'Tenants',
        routerLink: 'tenants',
        permissions: ['organization-tenant-r'],
      },
      {
        displayName: 'Policies',
        routerLink: 'policies',
        permissions: ['organization-policies-r'],
      },
    ]);
    return items.length > 0 ? { title: 'Gateway', items } : null;
  }

  private appendNotificationsItems(): GroupItem {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Templates',
        routerLink: 'notification-templates',
        permissions: ['organization-notification_templates-r'],
      },
    ]);
    return items.length > 0 ? { title: 'Notifications', items } : null;
  }

  private appendAuditItems() {
    const licenseOptions = { feature: ApimFeature.APIM_AUDIT_TRAIL, context: UTMTags.CONTEXT_ORGANIZATION };
    const iconRight$ = this.gioLicenseService
      .isMissingFeature$(licenseOptions.feature)
      .pipe(map(notAllowed => (notAllowed ? 'gio:lock' : null)));
    const items = this.filterMenuByPermission([
      {
        displayName: 'Audit',
        routerLink: 'audit',
        licenseOptions,
        iconRight$,
      },
    ]);

    return items.length > 0 ? { title: 'Audit', items } : null;
  }

  private appendCockpitItems(): GroupItem {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Discover Gravitee Cloud',
        routerLink: 'gravitee-cloud',
        permissions: ['organization-installation-r'],
      },
    ]);
    return items.length > 0 ? { title: 'Gravitee Cloud', items } : null;
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    return menuItems.filter(item => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
  }
}
