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

import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import {
  GioBreadcrumbModule,
  GioIconsModule,
  GioMenuSearchService,
  GioMenuService,
  GioSubmenuModule,
  MenuSearchItem,
} from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { flatMap } from 'lodash';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

import { ClusterNavigationTabsComponent } from './cluster-navigation-tabs/cluster-navigation-tabs.component';

import { cleanRouterLink, getPathFromRoot } from '../../../util/router-link.util';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ClusterService } from '../../../services-ngx/cluster.service';
import { Cluster } from '../../../entities/management-api-v2';

export interface MenuItem {
  displayName: string;
  permissions?: string[];
  tabs?: MenuItem[];
  header?: MenuItemHeader;
  routerLink?: string;
  routerLinkActiveOptions?: { exact: boolean };
}

export interface MenuItemHeader {
  title?: string;
  subtitle?: string;
}

@Component({
  selector: 'cluster-navigation',
  templateUrl: './cluster-navigation.component.html',
  styleUrls: ['./cluster-navigation.component.scss'],
  imports: [CommonModule, GioSubmenuModule, GioBreadcrumbModule, RouterModule, GioIconsModule, ClusterNavigationTabsComponent],
})
export class ClusterNavigationComponent implements OnInit, OnDestroy {
  public cluster: Cluster;
  public subMenuItems: MenuItem[] = [];
  public hasBreadcrumb = false;
  public selectedItemWithTabs: MenuItem = undefined;
  private unsubscribe$ = new Subject<void>();

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly permissionService = inject(GioPermissionService);
  private readonly gioMenuService = inject(GioMenuService);
  private readonly clusterService = inject(ClusterService);
  private readonly gioMenuSearchService = inject(GioMenuSearchService);

  ngOnInit() {
    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe(reduced => {
      this.hasBreadcrumb = reduced;
    });

    this.clusterService
      .get(this.activatedRoute.snapshot.params.clusterId)
      .pipe(
        tap(cluster => {
          this.cluster = cluster;

          this.subMenuItems = this.filterMenuByPermission([
            {
              displayName: 'General',
              routerLink: '',
              permissions: ['cluster-definition-r'],
              tabs: this.filterMenuByPermission([
                {
                  displayName: 'General',
                  routerLink: 'general',
                  permissions: ['cluster-definition-r'],
                },
                {
                  displayName: 'Explorer',
                  routerLink: 'explorer',
                  permissions: ['cluster-definition-r'],
                },
                {
                  displayName: 'Configuration',
                  routerLink: 'configuration',
                  permissions: ['cluster-configuration-r'],
                },
                {
                  displayName: 'User Permissions',
                  routerLink: 'user-permissions',
                  permissions: ['cluster-member-r'],
                },
              ]),
            },
          ]);

          this.selectedItemWithTabs = this.subMenuItems.find(item => item.tabs && this.isTabActive(item.tabs));

          this.gioMenuSearchService.addMenuSearchItems(this.getClusterNavigationSearchItems());
        }),
        switchMap(() => this.router.events),
        filter(event => event instanceof NavigationEnd),
        tap(() => {
          this.selectedItemWithTabs = this.subMenuItems.find(item => item.tabs && this.isTabActive(item.tabs));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    this.gioMenuSearchService.removeMenuSearchItems([this.activatedRoute.snapshot.params.clusterId]);
  }

  isActive(item: MenuItem): boolean {
    if (!item.routerLink) {
      return false;
    }
    return this.router.isActive(this.router.createUrlTree([item.routerLink], { relativeTo: this.activatedRoute }), {
      paths: item.routerLinkActiveOptions?.exact ? 'exact' : 'subset',
      queryParams: 'subset',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });
  }

  computeBreadcrumbItems(): string[] {
    const breadcrumbItems: string[] = [];

    this.subMenuItems.forEach(item => {
      if (this.isActive(item)) {
        breadcrumbItems.push(item.displayName);
      }
    });

    return breadcrumbItems;
  }

  isTabActive(tabs: MenuItem[]): boolean {
    return flatMap(tabs, tab => tab).some(tab => this.isActive(tab));
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    if (menuItems) {
      return menuItems.filter(item => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
    }
    return [];
  }

  private getClusterNavigationSearchItems(): MenuSearchItem[] {
    const environmentId = this.activatedRoute.snapshot.params.envHrid;
    const clusterId = this.activatedRoute.snapshot.params.clusterId;
    const parentRouterLink = getPathFromRoot(this.activatedRoute);

    return this.subMenuItems.reduce((acc: MenuSearchItem[], item: MenuItem) => {
      acc.push({
        name: item.displayName,
        routerLink: `${parentRouterLink}/${cleanRouterLink(item.routerLink)}`,
        category: `Clusters`,
        groupIds: [environmentId, clusterId],
      });

      item.tabs?.forEach(tab => {
        acc.push({
          name: tab.displayName,
          routerLink: `${parentRouterLink}/${cleanRouterLink(tab.routerLink)}`,
          category: `Clusters / ${item.displayName}`,
          groupIds: [environmentId, clusterId],
        });
      });

      return acc;
    }, []);
  }
}
