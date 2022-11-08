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
  permissions?: string[];
  tabs?: MenuItem[];
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

    this.appendPortalSubMenu();
    this.appendProxySubMenu();
    this.appendAnalyticsSubMenu();
    this.appendAuditSubMenu();
    this.appendNotificationsSubMenu();

    this.selectedItemWithTabs = this.findMenuItemWithTabs();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  private appendPortalSubMenu() {
    this.subMenuItems.push({ displayName: 'Portal' });
    this.subMenuItems.push({
      displayName: 'General',
      targetRoute: 'management.apis.detail.portal.general',
      baseRoute: 'management.apis.detail.portal.general',
    });
    this.subMenuItems.push({
      displayName: 'Plans',
      tabs: [
        {
          displayName: 'Plans',
          targetRoute: 'management.apis.detail.portal.plans.list',
          baseRoute: 'management.apis.detail.portal.plans',
          permissions: ['api-plan-r'],
        },
        {
          displayName: 'Subscriptions',
          targetRoute: 'management.apis.detail.portal.subscriptions.list',
          baseRoute: 'management.apis.detail.portal.subscriptions',
          permissions: ['api-subscription-r'],
        },
      ],
    });
    this.subMenuItems.push({
      displayName: 'Documentation',
      tabs: [
        {
          displayName: 'Pages',
          targetRoute: 'management.apis.detail.portal.documentation',
          baseRoute: 'management.apis.detail.portal.documentation',
          permissions: ['api-documentation-r'],
        },
        {
          displayName: 'Metadata',
          targetRoute: 'management.apis.detail.portal.metadata',
          baseRoute: 'management.apis.detail.portal.metadata',
          permissions: ['api-metadata-r'],
        },
      ],
    });

    const userAndGroupAccessMenuItems: MenuItem = {
      displayName: 'User and group access',
      tabs: [
        {
          displayName: 'Members',
          targetRoute: 'management.apis.detail.portal.members',
          baseRoute: 'management.apis.detail.portal.members',
          permissions: ['api-member-r'],
        },
        {
          displayName: 'Groups',
          targetRoute: 'management.apis.detail.portal.groups',
          baseRoute: 'management.apis.detail.portal.groups',
          permissions: ['api-member-r'],
        },
      ],
    };
    if (this.currentUserService.currentUser.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u'])) {
      userAndGroupAccessMenuItems.tabs.push({
        displayName: 'Transfer ownership',
        targetRoute: 'management.apis.detail.portal.transferownership',
        baseRoute: 'management.apis.detail.portal.transferownership',
      });
    }
    this.subMenuItems.push(userAndGroupAccessMenuItems);
  }
  private appendProxySubMenu() {
    this.subMenuItems.push({ displayName: 'Proxy' });
    this.subMenuItems.push({
      displayName: 'General',
      tabs: [
        {
          displayName: 'Entrypoints',
          targetRoute: 'management.apis.detail.proxy.ng-entrypoints',
          baseRoute: 'management.apis.detail.proxy.ng-entrypoints',
          permissions: ['api-definition-r', 'api-health-r'],
        },
        {
          displayName: 'CORS',
          targetRoute: 'management.apis.detail.proxy.ng-cors',
          baseRoute: 'management.apis.detail.proxy.ng-cors',
          permissions: ['api-definition-r'],
        },
        {
          displayName: 'Deployments',
          targetRoute: 'management.apis.detail.proxy.ng-deployments',
          baseRoute: 'management.apis.detail.proxy.ng-deployments',
          permissions: ['api-definition-r'],
        },
        {
          displayName: 'Response Templates',
          targetRoute: 'management.apis.detail.proxy.ng-responsetemplates.list',
          baseRoute: 'management.apis.detail.proxy.ng-responsetemplates',
          permissions: ['api-response_templates-r'],
        },
      ],
    });
    this.subMenuItems.push({
      displayName: 'Backend services',
      tabs: [
        {
          displayName: 'Endpoints',
          targetRoute: 'management.apis.detail.proxy.endpoints',
          baseRoute: 'management.apis.detail.proxy.endpoint',
          permissions: ['api-definition-r'],
        },
        {
          displayName: 'Failover',
          targetRoute: 'management.apis.detail.proxy.failover',
          baseRoute: 'management.apis.detail.proxy.failover',
          permissions: ['api-definition-r'],
        },
        {
          displayName: 'Health-check',
          targetRoute: 'management.apis.detail.proxy.healthcheck.visualize',
          baseRoute: 'management.apis.detail.proxy.healthcheck',
          permissions: ['api-health-r'],
        },
      ],
    });
  }
  private appendAnalyticsSubMenu() {
    this.subMenuItems.push({ displayName: 'Analytics' });
    this.subMenuItems.push({
      displayName: 'Overview',
      targetRoute: 'management.apis.detail.analytics.overview',
      baseRoute: 'management.apis.detail.analytics.overview',
      permissions: ['api-analytics-r'],
    });
    this.subMenuItems.push({
      displayName: 'Logs',
      targetRoute: 'management.apis.detail.analytics.logs.list',
      baseRoute: 'management.apis.detail.analytics.logs',
      permissions: ['api-log-r'],
    });
    this.subMenuItems.push({
      displayName: 'Path mappings',
      targetRoute: 'management.apis.detail.analytics.pathMappings',
      baseRoute: 'management.apis.detail.analytics.pathMappings',
      permissions: ['api-definition-u'],
    });
    this.subMenuItems.push({
      displayName: 'Alerts',
      targetRoute: 'management.apis.detail.analytics.alerts',
      baseRoute: 'management.apis.detail.analytics.alerts',
      permissions: ['api-alert-r'],
    });
  }
  private appendAuditSubMenu() {
    this.subMenuItems.push({ displayName: 'Audit' });
    this.subMenuItems.push({
      displayName: 'Audit',
      targetRoute: 'management.apis.detail.audit.general',
      baseRoute: 'management.apis.detail.audit.general',
      permissions: ['api-audit-r'],
    });
    this.subMenuItems.push({
      displayName: 'History',
      targetRoute: 'management.apis.detail.audit.history',
      baseRoute: 'management.apis.detail.audit.history',
      permissions: ['api-event-r'],
    });
    this.subMenuItems.push({
      displayName: 'Events',
      targetRoute: 'management.apis.detail.audit.events',
      baseRoute: 'management.apis.detail.audit.events',
      permissions: ['api-event-u'],
    });
  }
  private appendNotificationsSubMenu() {
    this.subMenuItems.push({ displayName: 'Notifications' });
    this.subMenuItems.push({
      displayName: 'Notifications',
      targetRoute: 'management.apis.detail.notifications',
      baseRoute: 'management.apis.detail.notifications',
      permissions: ['api-notification-r'],
    });

    if (this.constants.org.settings.alert && this.constants.org.settings.alert.enabled) {
      this.subMenuItems.push({
        displayName: 'Alerts',
        targetRoute: 'management.apis.detail.alerts.list',
        baseRoute: 'management.apis.detail.alerts',
        permissions: ['api-alert-r'],
      });
    }
  }

  filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    if (menuItems) {
      return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
    }
    return [];
  }

  private findMenuItemWithTabs(route?: string): MenuItem {
    return this.subMenuItems
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
