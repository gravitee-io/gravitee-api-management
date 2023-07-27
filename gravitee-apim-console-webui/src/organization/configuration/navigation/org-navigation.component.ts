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

import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { LicenseOptions, GioLicenseService } from '@gravitee/ui-particles-angular';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { UIRouterState } from '../../../ajs-upgraded-providers';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-features';
import { UTM_DATA, UTMMedium } from '../../../shared/components/gio-license/gio-license-utm';

interface MenuItem {
  targetRoute: string;
  displayName: string;
  permissions?: string[];
  licenseOptions?: LicenseOptions;
  iconRight$?: Observable<any>;
}

interface GroupItem {
  title: string;
  items: MenuItem[];
}

@Component({
  selector: 'org-navigation',
  styles: [require('./org-navigation.component.scss')],
  template: require('./org-navigation.component.html'),
})
export class OrgNavigationComponent implements OnInit {
  public groupItems: GroupItem[] = [];
  public hasGoBackButton: boolean;
  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly permissionService: GioPermissionService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
  ) {}
  ngOnInit(): void {
    this.hasGoBackButton = this.constants.org.environments && this.constants.org.environments.length > 0;
    this.appendConsoleItems();
    this.appendUserManagementItems();
    this.appendGatewayItems();
    this.appendNotificationsItems();
    this.appendAuditItems();
    this.appendCockpitItems();
  }

  navigateTo(route: string) {
    this.ajsState.go(route);
  }

  isActive(route: string): boolean {
    return this.ajsState.includes(route);
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
  }

  private appendConsoleItems() {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Authentication',
        targetRoute: 'organization.identities',
        permissions: ['organization-identity_provider-r'],
      },
      {
        displayName: 'Settings',
        targetRoute: 'organization.settings',
        permissions: ['organization-settings-r'],
      },
    ]);
    if (items.length > 0) {
      this.groupItems.push({
        title: 'Console',
        items,
      });
    }
  }

  private appendUserManagementItems() {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Users',
        targetRoute: 'organization.users',
        permissions: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
      },
      {
        displayName: 'Roles',
        targetRoute: 'organization.roles',
        permissions: ['organization-role-r'],
      },
    ]);
    if (items.length > 0) {
      this.groupItems.push({
        title: 'User Management',
        items,
      });
    }
  }

  private appendGatewayItems() {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Sharding tags',
        targetRoute: 'organization.tags',
        permissions: ['organization-tag-r'],
      },
      {
        displayName: 'Tenants',
        targetRoute: 'organization.tenants',
        permissions: ['organization-tenant-r'],
      },
      {
        displayName: 'Policies',
        targetRoute: 'organization.policies',
        permissions: ['organization-policies-r'],
      },
    ]);

    if (items.length > 0) {
      this.groupItems.push({
        title: 'Gateway',
        items,
      });
    }
  }

  private appendNotificationsItems() {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Templates',
        targetRoute: 'organization.notificationTemplates',
        permissions: ['organization-notification_templates-r'],
      },
    ]);

    if (items.length > 0) {
      this.groupItems.push({
        title: 'Notifications',
        items,
      });
    }
  }

  private appendAuditItems() {
    const licenseOptions = { feature: ApimFeature.APIM_AUDIT_TRAIL, utm: UTM_DATA[UTMMedium.AUDIT_TRAIL_ORG] };
    const iconRight$ = this.gioLicenseService.isMissingFeature$(licenseOptions).pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));
    const items = this.filterMenuByPermission([
      {
        displayName: 'Audit',
        targetRoute: 'organization.audit',
        licenseOptions,
        iconRight$,
      },
    ]);

    if (items.length > 0) {
      this.groupItems.push({
        title: 'Audit',
        items,
      });
    }
  }

  private appendCockpitItems() {
    const items = this.filterMenuByPermission([
      {
        displayName: 'Discover cockpit',
        targetRoute: 'organization.cockpit',
        permissions: ['organization-installation-r'],
      },
    ]);

    const groupItem = {
      title: 'Cockpit',
      items,
    };

    if (items.length > 0) {
      this.groupItems.push(groupItem);
    }
  }

  goBack() {
    this.navigateTo('management');
  }
}
