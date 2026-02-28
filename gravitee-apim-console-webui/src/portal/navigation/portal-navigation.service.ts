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

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

export interface MenuItem {
  icon?: string;
  routerLink?: string;
  displayName: string;
  children?: MenuItem[];
}

interface MenuItemConfig {
  displayName: string;
  routerLink: string;
  icon: string;
  permissions: string[];
}

@Injectable({
  providedIn: 'root',
})
export class PortalNavigationService {
  constructor(private readonly permissionService: GioPermissionService) {}

  private allMenuItems: MenuItemConfig[] = [
    {
      displayName: 'Catalog',
      routerLink: 'catalog',
      icon: 'gio:report-columns',
      permissions: ['environment-category-r', 'environment-category-u'],
    },
    {
      displayName: 'Navigation',
      routerLink: 'navigation',
      icon: 'gio:page',
      permissions: ['environment-settings-r', 'environment-settings-u'],
    },
    {
      displayName: 'API',
      routerLink: 'api',
      icon: 'gio:cloud-settings',
      permissions: ['environment-settings-r', 'environment-settings-u'],
    },
    {
      displayName: 'Banner',
      routerLink: 'banner',
      icon: 'gio:chat-lines',
      permissions: ['environment-settings-r', 'environment-settings-u'],
    },
    {
      displayName: 'Theme',
      routerLink: 'theme',
      icon: 'gio:color-picker',
      permissions: ['environment-theme-r', 'environment-theme-u'],
    },
    {
      displayName: 'Homepage',
      routerLink: 'homepage',
      icon: 'gio:box',
      permissions: ['environment-documentation-r', 'environment-documentation-u'],
    },
    {
      displayName: 'Subscription Form',
      routerLink: 'subscription-form',
      icon: 'gio:list-check',
      permissions: ['environment-metadata-r', 'environment-metadata-u'],
    },
  ];

  public getMainMenuItems(): MenuItem[] {
    return this.allMenuItems
      .filter(item => this.permissionService.hasAnyMatching(item.permissions))
      .map(({ displayName, routerLink, icon }) => ({
        displayName,
        routerLink,
        icon,
      }));
  }
}
