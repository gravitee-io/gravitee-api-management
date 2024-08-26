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
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';

import { ApiFederatedMenuService } from './api-federated-menu.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { Constants } from '../../../entities/Constants';

const DEFAULT_PERMISSIONS: GioTestingPermission = ['api-member-r', 'api-audit-r', 'api-documentation-r', 'api-metadata-r', 'api-plan-r'];
const DEFAULT_SETTINGS: ConsoleSettings = { scoring: { enabled: true } };

describe('ApiFederatedMenuService', () => {
  const init = async (
    { permissions, settings } = {
      permissions: DEFAULT_PERMISSIONS,
      settings: DEFAULT_SETTINGS,
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [
        { provide: ApiFederatedMenuService },
        { provide: Constants, useValue: { ...CONSTANTS_TESTING, org: { settings } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
      ],
    }).compileComponents();
  };

  describe('getMenu', () => {
    it('should return items for Federated API with expected permission and license', async () => {
      await init();
      const service = TestBed.inject(ApiFederatedMenuService);

      expect(service.getMenu()).toEqual({
        subMenuItems: [
          {
            displayName: 'Configuration',
            header: {
              subtitle: 'Manage general settings, user permissions, properties, and resources, and track changes to your API',
              title: 'Configuration',
            },
            icon: 'settings',
            routerLink: '',
            tabs: [
              {
                displayName: 'General',
                routerLink: '.',
                routerLinkActiveOptions: {
                  exact: true,
                },
              },
              {
                displayName: 'User Permissions',
                routerLink: 'members',
              },
              {
                displayName: 'Audit Logs',
                routerLink: 'v4/audit',
                license: {
                  context: 'api',
                  feature: 'apim-audit-trail',
                },
                iconRight$: expect.anything(),
              },
            ],
          },
          {
            displayName: 'API Score',
            icon: 'shield-check',
            routerLink: 'api-score',
            header: {
              title: 'API Score',
            },
          },
          {
            displayName: 'Consumers',
            header: {
              subtitle: 'Manage how your API is consumed',
              title: 'Consumers',
            },
            icon: 'cloud-consumers',
            routerLink: '',
            tabs: [
              {
                displayName: 'Plans',
                routerLink: 'plans',
              },
              {
                displayName: 'Broadcasts',
                routerLink: 'messages',
              },
            ],
          },
          {
            displayName: 'Documentation',
            header: {
              title: 'Documentation',
              subtitle: 'Documentation pages appear in the Developer Portal and inform API consumers how to use your API',
            },
            icon: 'book',
            routerLink: '',
            tabs: [
              {
                displayName: 'Main Pages',
                routerLink: 'v4/documentation/main-pages',
                routerLinkActiveOptions: { exact: false },
              },
              {
                displayName: 'Documentation Pages',
                routerLink: 'v4/documentation/pages',
                routerLinkActiveOptions: { exact: false },
              },
              {
                displayName: 'Metadata',
                routerLink: 'v4/documentation/metadata',
                routerLinkActiveOptions: { exact: true },
              },
            ],
          },
        ],
        groupItems: [],
      });
    });

    it('should hide elements when not enough permissions', async () => {
      await init({ permissions: [], settings: DEFAULT_SETTINGS });
      const service = TestBed.inject(ApiFederatedMenuService);

      expect(service.getMenu()).toEqual({
        subMenuItems: [
          {
            displayName: 'Configuration',
            header: {
              subtitle: 'Manage general settings, user permissions, properties, and resources, and track changes to your API',
              title: 'Configuration',
            },
            icon: 'settings',
            routerLink: '',
            tabs: [
              {
                displayName: 'General',
                routerLink: '.',
                routerLinkActiveOptions: {
                  exact: true,
                },
              },
            ],
          },
          {
            displayName: 'API Score',
            icon: 'shield-check',
            routerLink: 'api-score',
            header: {
              title: 'API Score',
            },
          },
          {
            displayName: 'Consumers',
            header: {
              subtitle: 'Manage how your API is consumed',
              title: 'Consumers',
            },
            icon: 'cloud-consumers',
            routerLink: '',
            tabs: [
              {
                displayName: 'Broadcasts',
                routerLink: 'messages',
              },
            ],
          },
          {
            displayName: 'Documentation',
            header: {
              subtitle: 'Documentation pages appear in the Developer Portal and inform API consumers how to use your API',
              title: 'Documentation',
            },
            icon: 'book',
            routerLink: '',
            tabs: [],
          },
        ],
        groupItems: [],
      });
    });

    it('should hide scoring elements when feature is disabled', async () => {
      await init({ permissions: DEFAULT_PERMISSIONS, settings: { scoring: { enabled: false } } });
      const service = TestBed.inject(ApiFederatedMenuService);

      const menu = service.getMenu();

      expect(menu.subMenuItems.find((item) => item.displayName === 'API Score')).toBeUndefined();
    });
  });
});
