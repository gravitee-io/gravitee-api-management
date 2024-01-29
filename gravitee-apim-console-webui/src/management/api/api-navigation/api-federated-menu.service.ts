/**
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Inject, Injectable } from '@angular/core';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { CurrentUserService } from '../../../ajs-upgraded-providers';
import UserService from '../../../services/user.service';

@Injectable()
export class ApiFederatedMenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
  ) {}
  public getMenu(): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [];

    const groupItems: MenuGroupItem[] = [this.getGeneralGroup(), this.getAuditGroup()].filter((group) => !!group);

    return { subMenuItems, groupItems };
  }
  private getGeneralGroup(): MenuGroupItem {
    const generalGroup: MenuGroupItem = {
      title: 'General',
      items: [
        {
          displayName: 'Info',
          routerLink: '.',
          routerLinkActiveOptions: { exact: true },
        },
      ],
    };
    // Plans
    const plansMenuItem: MenuItem = {
      displayName: 'Plans',
      tabs: [],
    };

    if (this.permissionService.hasAnyMatching(['api-plan-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Plans',
        routerLink: 'plans',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
      });
    }
    if (plansMenuItem.tabs.length > 0) {
      generalGroup.items.push(plansMenuItem);
    }

    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      generalGroup.items.push({
        displayName: 'Documentation',
        routerLink: 'v4/documentation',
      });
    }

    // Users
    const userAndGroupAccessMenuItems: MenuItem = {
      displayName: 'User and group access',
      tabs: [],
    };
    if (this.permissionService.hasAnyMatching(['api-member-r'])) {
      userAndGroupAccessMenuItems.tabs.push(
        {
          displayName: 'Members',
          routerLink: 'members',
        },
        {
          displayName: 'Groups',
          routerLink: 'groups',
        },
      );
    }
    if (this.currentUserService.currentUser.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u'])) {
      userAndGroupAccessMenuItems.tabs.push({
        displayName: 'Transfer ownership',
        routerLink: 'transfer-ownership',
      });
    }
    if (userAndGroupAccessMenuItems.tabs.length > 0) {
      generalGroup.items.push(userAndGroupAccessMenuItems);
    }

    return generalGroup;
  }

  private getAuditGroup(): MenuGroupItem {
    const auditGroup: MenuGroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        routerLink: 'DISABLED',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      auditGroup.items.push({
        displayName: 'History',
        routerLink: 'DISABLED',
      });
    }

    return auditGroup;
  }
}
