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
import { Component, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';

export interface MenuItem {
  targetRoute?: string;
  baseRoute?: string;
  displayName: string;
  tabs?: MenuItem[];
}

interface GroupItem {
  title: string;
  items: MenuItem[];
}

@Component({
  selector: 'api-navigation',
  template: require('./api-navigation.component.html'),
  styles: [require('./api-navigation.component.scss')],
})
export class ApiNavigationComponent implements OnInit, OnDestroy {
  @Input()
  public apiName: string;
  @Input()
  public apiVersion: string;
  @Input()
  public apiState: string;
  @Input()
  public apiIsSync: boolean;
  @Input()
  public apiLifecycleState: string;
  @Input()
  public apiOrigin: string;

  public subMenuItems: MenuItem[] = [];
  public groupItems: GroupItem[] = [];
  public selectedItemWithTabs: MenuItem = undefined;
  public hasTitle = true;
  private unsubscribe$ = new Subject();

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioMenuService: GioMenuService,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduce.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasTitle = !reduced;
    });
    this.subMenuItems.push({
      displayName: 'Design',
      targetRoute: 'management.apis.detail.design.policies',
      baseRoute: 'management.apis.detail.design',
    });
    this.subMenuItems.push({
      displayName: 'Messages',
      targetRoute: 'management.apis.detail.messages',
      baseRoute: 'management.apis.detail.messages',
    });

    this.appendPortalGroup();
    this.appendProxyGroup();
    this.appendAnalyticsGroup();
    this.appendAuditGroup();
    this.appendNotificationsGroup();

    this.selectedItemWithTabs = this.findMenuItemWithTabs();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  private appendPortalGroup() {
    const portalGroup: GroupItem = {
      title: 'Portal',
      items: [
        {
          displayName: 'General',
          targetRoute: 'management.apis.detail.portal.general',
          baseRoute: 'management.apis.detail.portal.general',
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
        targetRoute: 'management.apis.detail.portal.plans.list',
        baseRoute: 'management.apis.detail.portal.plans',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Subscriptions',
        targetRoute: 'management.apis.detail.portal.subscriptions.list',
        baseRoute: 'management.apis.detail.portal.subscriptions',
      });
    }
    if (plansMenuItem.tabs.length > 0) {
      portalGroup.items.push(plansMenuItem);
    }

    // Documentation
    const documentationMenuItem: MenuItem = {
      displayName: 'Documentation',
      tabs: [],
    };
    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      documentationMenuItem.tabs.push({
        displayName: 'Pages',
        targetRoute: 'management.apis.detail.portal.documentation',
        baseRoute: 'management.apis.detail.portal.documentation',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-metadata-r'])) {
      documentationMenuItem.tabs.push({
        displayName: 'Metadata',
        targetRoute: 'management.apis.detail.portal.metadata',
        baseRoute: 'management.apis.detail.portal.metadata',
      });
    }
    if (documentationMenuItem.tabs.length > 0) {
      portalGroup.items.push(documentationMenuItem);
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
          targetRoute: 'management.apis.detail.portal.members',
          baseRoute: 'management.apis.detail.portal.members',
        },
        {
          displayName: 'Groups',
          targetRoute: 'management.apis.detail.portal.groups',
          baseRoute: 'management.apis.detail.portal.groups',
        },
      );
    }
    if (this.currentUserService.currentUser.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u'])) {
      userAndGroupAccessMenuItems.tabs.push({
        displayName: 'Transfer ownership',
        targetRoute: 'management.apis.detail.portal.transferownership',
        baseRoute: 'management.apis.detail.portal.transferownership',
      });
    }
    if (userAndGroupAccessMenuItems.tabs.length > 0) {
      portalGroup.items.push(userAndGroupAccessMenuItems);
    }

    this.groupItems.push(portalGroup);
  }

  private appendProxyGroup() {
    const proxyGroup: GroupItem = {
      title: 'Proxy',
      items: [],
    };

    // General
    const generalMenuItem: MenuItem = {
      displayName: 'General',
      tabs: [],
    };
    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      generalMenuItem.tabs.push({
        displayName: 'Entrypoints',
        targetRoute: 'management.apis.detail.proxy.ng-entrypoints',
        baseRoute: 'management.apis.detail.proxy.ng-entrypoints',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      generalMenuItem.tabs.push(
        {
          displayName: 'CORS',
          targetRoute: 'management.apis.detail.proxy.ng-cors',
          baseRoute: 'management.apis.detail.proxy.ng-cors',
        },
        {
          displayName: 'Deployments',
          targetRoute: 'management.apis.detail.proxy.ng-deployments',
          baseRoute: 'management.apis.detail.proxy.ng-deployments',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-response_templates-r'])) {
      generalMenuItem.tabs.push({
        displayName: 'Response Templates',
        targetRoute: 'management.apis.detail.proxy.ng-responsetemplates.list',
        baseRoute: 'management.apis.detail.proxy.ng-responsetemplates',
      });
    }
    if (generalMenuItem.tabs.length > 0) {
      proxyGroup.items.push(generalMenuItem);
    }

    // Backend services
    const backendServicesMenuItem: MenuItem = {
      displayName: 'Backend services',
      tabs: [],
    };

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      backendServicesMenuItem.tabs.push(
        {
          displayName: 'Endpoints',
          targetRoute: 'management.apis.detail.proxy.endpoints',
          baseRoute: 'management.apis.detail.proxy.endpoint',
        },
        {
          displayName: 'Failover',
          targetRoute: 'management.apis.detail.proxy.failover',
          baseRoute: 'management.apis.detail.proxy.failover',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesMenuItem.tabs.push({
        displayName: 'Health-check',
        targetRoute: 'management.apis.detail.proxy.healthcheck.visualize',
        baseRoute: 'management.apis.detail.proxy.healthcheck',
      });
    }
    if (backendServicesMenuItem.tabs.length > 0) {
      proxyGroup.items.push(backendServicesMenuItem);
    }
    if (proxyGroup.items.length > 0) {
      this.groupItems.push(proxyGroup);
    }
  }

  private appendAnalyticsGroup() {
    const analyticsGroup: GroupItem = {
      title: 'Analytics',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-analytics-r'])) {
      analyticsGroup.items.push({
        displayName: 'Overview',
        targetRoute: 'management.apis.detail.analytics.overview',
        baseRoute: 'management.apis.detail.analytics.overview',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-log-r'])) {
      analyticsGroup.items.push({
        displayName: 'Logs',
        targetRoute: 'management.apis.detail.analytics.logs.list',
        baseRoute: 'management.apis.detail.analytics.logs',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u'])) {
      analyticsGroup.items.push({
        displayName: 'Path mappings',
        targetRoute: 'management.apis.detail.analytics.pathMappings',
        baseRoute: 'management.apis.detail.analytics.pathMappings',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-alert-r'])) {
      analyticsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'management.apis.detail.analytics.alerts',
        baseRoute: 'management.apis.detail.analytics.alerts',
      });
    }
    if (analyticsGroup.items.length > 0) {
      this.groupItems.push(analyticsGroup);
    }
  }

  private appendAuditGroup() {
    const auditGroup: GroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        targetRoute: 'management.apis.detail.audit.general',
        baseRoute: 'management.apis.detail.audit.general',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      auditGroup.items.push({
        displayName: 'History',
        targetRoute: 'management.apis.detail.audit.history',
        baseRoute: 'management.apis.detail.audit.history',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-u'])) {
      auditGroup.items.push({
        displayName: 'Events',
        targetRoute: 'management.apis.detail.audit.events',
        baseRoute: 'management.apis.detail.audit.events',
      });
    }

    if (auditGroup.items.length > 0) {
      this.groupItems.push(auditGroup);
    }
  }
  private appendNotificationsGroup() {
    const notificationsGroup: GroupItem = {
      title: 'Notifications',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-notification-r'])) {
      notificationsGroup.items.push({
        displayName: 'Notifications',
        targetRoute: 'management.apis.detail.notifications',
        baseRoute: 'management.apis.detail.notifications',
      });
    }

    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      notificationsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'management.apis.detail.alerts.list',
        baseRoute: 'management.apis.detail.alerts',
      });
    }

    if (notificationsGroup.items.length > 0) {
      this.groupItems.push(notificationsGroup);
    }
  }

  private findMenuItemWithTabs(route?: string): MenuItem {
    let item: MenuItem = this.findActiveMenuItem(this.subMenuItems, route);
    if (item) {
      return item;
    }

    for (const groupItem of this.groupItems) {
      item = this.findActiveMenuItem(groupItem.items, route);
      if (item) {
        return item;
      }
    }
  }

  private findActiveMenuItem(items: MenuItem[], route: string) {
    return items
      .filter((item) => item.tabs)
      .find((item) => {
        if (route) {
          return item.tabs.some((tab) => tab.targetRoute === route);
        }
        return this.isTabActive(item.tabs);
      });
  }

  navigateTo(route: string) {
    this.selectedItemWithTabs = this.findMenuItemWithTabs(route);
    this.ajsState.go(route);
  }

  isActive(route: string): boolean {
    return this.ajsState.includes(route);
  }

  isTabActive(tabs: MenuItem[]): boolean {
    return tabs.some((tab) => this.ajsState.includes(tab.baseRoute));
  }
}
