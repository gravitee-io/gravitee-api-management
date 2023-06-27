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
import { Component, Inject, Input, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import { castArray, flatMap } from 'lodash';
import { map, takeUntil } from 'rxjs/operators';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { Observable, Subject } from 'rxjs';

import { AjsRootScope, CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';
import { GioLicenseOptions } from '../../../shared/components/gio-license/gio-license.directive';
import { GioLicenseService } from '../../../shared/components/gio-license/gio-license.service';

export interface MenuItem {
  targetRoute?: string;
  baseRoute?: string | string[];
  displayName: string;
  tabs?: MenuItem[];
  license?: GioLicenseOptions;
  iconRight$?: Observable<any>;
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
export class ApiNavigationComponent implements OnInit {
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
  @Input()
  public graviteeVersion: string;

  public subMenuItems: MenuItem[] = [];
  public groupItems: GroupItem[] = [];
  public selectedItemWithTabs: MenuItem = undefined;
  public bannerState: string;
  public hasBreadcrumb = false;
  private unsubscribe$ = new Subject();
  public breadcrumbItems: string[] = [];

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    @Inject(AjsRootScope) private readonly ajsRootScope: IScope,
    private readonly gioMenuService: GioMenuService,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduce.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    this.bannerState = localStorage.getItem('gv-api-navigation-banner');

    this.appendDesign();

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
    this.breadcrumbItems = this.computeBreadcrumbItems();

    this.ajsRootScope.$on('$locationChangeStart', () => {
      this.selectedItemWithTabs = this.findMenuItemWithTabs();
    });
  }

  private appendDesign() {
    let tabs = undefined;
    if (this.graviteeVersion === '1.0.0') {
      tabs = [
        {
          displayName: 'Policies',
          targetRoute: 'management.apis.detail.design.policies',
          baseRoute: 'management.apis.detail.design.policies',
        },
        {
          displayName: 'Resources',
          targetRoute: 'management.apis.detail.design.resources',
          baseRoute: 'management.apis.detail.design.resources',
        },
        {
          displayName: 'Properties',
          targetRoute: 'management.apis.detail.design.properties',
          baseRoute: 'management.apis.detail.design.properties',
        },
      ];
    }

    this.subMenuItems.push({
      displayName: 'Design',
      targetRoute: 'management.apis.detail.design.policies',
      baseRoute: 'management.apis.detail.design',
      tabs,
    });
  }

  private appendPortalGroup() {
    const portalGroup: GroupItem = {
      title: 'General',
      items: [
        {
          displayName: 'Info',
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
        targetRoute: 'management.apis.detail.portal.plans',
        baseRoute: ['management.apis.detail.portal.plans', 'management.apis.detail.portal.plan'],
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

    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      proxyGroup.items.push({
        displayName: 'Entrypoints',
        targetRoute: 'management.apis.detail.proxy.entrypoints',
        baseRoute: 'management.apis.detail.proxy.entrypoints',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      proxyGroup.items.push(
        {
          displayName: 'CORS',
          targetRoute: 'management.apis.detail.proxy.cors',
          baseRoute: 'management.apis.detail.proxy.cors',
        },
        {
          displayName: 'Deployments',
          targetRoute: 'management.apis.detail.proxy.deployments',
          baseRoute: 'management.apis.detail.proxy.deployments',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-response_templates-r'])) {
      proxyGroup.items.push({
        displayName: 'Response Templates',
        targetRoute: 'management.apis.detail.proxy.responsetemplates.list',
        baseRoute: 'management.apis.detail.proxy.responsetemplates',
      });
    }

    if (proxyGroup.items.length > 0) {
      this.groupItems.push(proxyGroup);
    }

    // Backend services
    const backendServicesGroup: GroupItem = {
      title: 'Backend services',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      backendServicesGroup.items.push(
        {
          displayName: 'Endpoints',
          targetRoute: 'management.apis.detail.proxy.endpoints',
          baseRoute: ['management.apis.detail.proxy.endpoints', 'management.apis.detail.proxy.endpoint'],
        },
        {
          displayName: 'Failover',
          targetRoute: 'management.apis.detail.proxy.failover',
          baseRoute: 'management.apis.detail.proxy.failover',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check',
        targetRoute: 'management.apis.detail.proxy.healthcheck',
        baseRoute: 'management.apis.detail.proxy.healthcheck',
      });
    }

    // Health-check dashboard
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check dashboard',
        baseRoute: 'management.apis.detail.proxy.healthCheckDashboard.visualize',
        targetRoute: 'management.apis.detail.proxy.healthCheckDashboard.visualize',
      });
    }

    if (backendServicesGroup.items.length > 0) {
      this.groupItems.push(backendServicesGroup);
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
    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
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
    const license = { feature: 'apim-audit-trail' };
    const iconRight$ = this.gioLicenseService.notAllowed(license.feature).pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));

    const auditGroup: GroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        targetRoute: 'management.apis.detail.audit.general',
        baseRoute: 'management.apis.detail.audit.general',
        license,
        iconRight$,
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

  private findMenuItemWithTabs(): MenuItem {
    let item: MenuItem = this.findActiveMenuItem(this.subMenuItems);
    if (item) {
      return item;
    }

    for (const groupItem of this.groupItems) {
      item = this.findActiveMenuItem(groupItem.items);
      if (item) {
        return item;
      }
    }
  }

  private findActiveMenuItem(items: MenuItem[]) {
    return items.filter((item) => item.tabs).find((item) => this.isTabActive(item.tabs));
  }

  navigateTo(route: string) {
    this.ajsState.go(route);
  }

  isActive(baseRoute: MenuItem['baseRoute']): boolean {
    return castArray(baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }

  isTabActive(tabs: MenuItem[]): boolean {
    return flatMap(tabs, (tab) => tab.baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }

  closeBanner() {
    this.bannerState = 'close';
    localStorage.setItem('gv-api-navigation-banner', this.bannerState);
  }

  public computeBreadcrumbItems(): string[] {
    const breadcrumbItems: string[] = [];

    this.groupItems.forEach((groupItem) => {
      groupItem.items.forEach((item) => {
        if (this.isActive(item.baseRoute)) {
          breadcrumbItems.push(groupItem.title);
          breadcrumbItems.push(item.displayName);
        } else if (item.tabs && this.isTabActive(item.tabs)) {
          breadcrumbItems.push(groupItem.title);
          breadcrumbItems.push(item.displayName);
        }
      });
    });

    return breadcrumbItems;
  }
}
