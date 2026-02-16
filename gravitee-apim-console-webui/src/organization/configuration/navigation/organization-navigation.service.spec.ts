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
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';

import { OrganizationNavigationService } from './organization-navigation.service';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioTestingModule } from '../../../shared/testing';

describe('OrganizationNavigationService', () => {
  let service: OrganizationNavigationService;

  const init = (hasAnyMatching = true) => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule, GioLicenseTestingModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => hasAnyMatching,
          },
        },
      ],
    });
    service = TestBed.inject(OrganizationNavigationService);
  };

  it('should map routes to search items', () => {
    init();

    const menuSearchItems = service.getOrganizationNavigationSearchItems();

    expect(menuSearchItems).toHaveLength(10);
    expect(menuSearchItems).toEqual(
      expect.arrayContaining(
        [
          'Authentication',
          'Settings',
          'Users',
          'Roles',
          'Entrypoints & Sharding Tags',
          'Tenants',
          'Policies',
          'Templates',
          'Audit',
          'Discover Gravitee Cloud',
        ].map(name =>
          expect.objectContaining({
            name,
            routerLink: expect.not.stringContaining('./') && expect.stringContaining(`_organization/`),
          }),
        ),
      ),
    );
  });

  it('should not map any route to search items', () => {
    init(false);

    const menuSearchItems = service.getOrganizationNavigationSearchItems();

    expect(menuSearchItems).toStrictEqual([]);
  });
});
