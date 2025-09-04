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

import { TestBed } from '@angular/core/testing';

import { PortalNavigationService } from './portal-navigation.service';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

describe('PortalNavigationService', () => {
  let service: PortalNavigationService;
  let permissionService: GioPermissionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PortalNavigationService,
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(true),
          },
        },
      ],
    });
    service = TestBed.inject(PortalNavigationService);
    permissionService = TestBed.inject(GioPermissionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getMainMenuItems', () => {
    it('should return all menu items when all permissions are granted', () => {
      const menuItems = service.getMainMenuItems();
      expect(menuItems).toEqual([
        {
          displayName: 'Catalog',
          routerLink: 'catalog',
          icon: 'gio:report-columns',
        },
        {
          displayName: 'Top Bar',
          routerLink: 'top-bar',
          icon: 'gio:top-bar',
        },
        {
          displayName: 'API',
          routerLink: 'api',
          icon: 'gio:cloud-settings',
        },
        {
          displayName: 'Banner',
          routerLink: 'banner',
          icon: 'gio:chat-lines',
        },
        {
          displayName: 'Theme',
          routerLink: 'theme',
          icon: 'gio:color-picker',
        },
        {
          displayName: 'Homepage',
          routerLink: 'homepage',
          icon: 'gio:box',
        },
      ]);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledTimes(6);
    });

    it('should return only allowed menu items when some permissions are not granted', () => {
      permissionService.hasAnyMatching = jest.fn((permissions: string[]) => {
        return permissions.includes('environment-category-r');
      });

      const menuItems = service.getMainMenuItems();
      expect(menuItems).toEqual([
        {
          displayName: 'Catalog',
          routerLink: 'catalog',
          icon: 'gio:report-columns',
        },
      ]);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledTimes(6);
    });

    it('should include menu items with empty permission list', () => {
      // Temporarily modify the service to include a menu item with empty permissions
      service['allMenuItems'] = [
        {
          displayName: 'Test Item',
          routerLink: 'test',
          icon: 'gio:test',
          permissions: [],
        },
      ];
      permissionService.hasAnyMatching = jest.fn((permissions: string[]) => {
        return permissions.length === 0;
      });

      const menuItems = service.getMainMenuItems();
      expect(menuItems).toEqual([
        {
          displayName: 'Test Item',
          routerLink: 'test',
          icon: 'gio:test',
        },
      ]);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledWith([]);
    });

    it('should return no menu items when no permissions are granted', () => {
      permissionService.hasAnyMatching = jest.fn().mockReturnValue(false);

      const menuItems = service.getMainMenuItems();
      expect(menuItems).toEqual([]);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledTimes(6);
    });

    it('should call hasAnyMatching with the correct permissions for each menu item', () => {
      service.getMainMenuItems();

      expect(permissionService.hasAnyMatching).toHaveBeenCalledWith(['environment-category-r', 'environment-category-u']);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledWith(['environment-settings-r', 'environment-settings-u']);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledWith(['environment-theme-r', 'environment-theme-u']);
      expect(permissionService.hasAnyMatching).toHaveBeenCalledWith(['environment-documentation-r', 'environment-documentation-u']);
    });
  });
});
