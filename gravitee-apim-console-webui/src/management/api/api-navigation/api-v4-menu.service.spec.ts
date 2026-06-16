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
import { beforeEach, describe, expect, it } from '@jest/globals';
import { TestBed } from '@angular/core/testing';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';

import { ApiV4MenuService } from './api-v4-menu.service';

import { GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';
import { fakeApiV4 } from '../../../entities/management-api-v2';

describe('ApiV4MenuService', () => {
  let service: ApiV4MenuService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ApiV4MenuService,
        { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-log-r', 'api-log-u'],
        },
        {
          provide: EnvironmentSettingsService,
          useValue: {
            getSnapshot: () => ({ apiScore: { enabled: false } }),
          },
        },
        {
          provide: ApiDocumentationV2Service,
          useValue: {
            getApiPortalUrl: () => 'portal-url',
            getApiNotInPortalTooltip: () => 'tooltip',
          },
        },
      ],
      imports: [GioTestingModule],
    });
  });

  it('should be created', () => {
    service = TestBed.inject(ApiV4MenuService);
    expect(service).toBeTruthy();
  });

  it('should include Webhooks menu when webhook entrypoint is present', () => {
    service = TestBed.inject(ApiV4MenuService);
    const api = fakeApiV4();

    const menu = service.getMenu(api);

    expect(menu.subMenuItems.some(item => item.displayName === 'Webhooks')).toBe(true);
  });

  it('should not include Webhooks menu when webhook entrypoint is absent', () => {
    service = TestBed.inject(ApiV4MenuService);
    const api = fakeApiV4(base => ({
      ...base,
      listeners: [
        {
          type: 'HTTP',
          entrypoints: [{ type: 'http-proxy' }],
        },
      ],
    }));

    const menu = service.getMenu(api);

    expect(menu.subMenuItems.some(item => item.displayName === 'Webhooks')).toBe(false);
  });

  it('should include Webhooks menu when user has api-log-r permission', () => {
    service = TestBed.inject(ApiV4MenuService);
    const api = fakeApiV4();

    const menu = service.getMenu(api);

    expect(menu.subMenuItems.some(item => item.displayName === 'Webhooks')).toBe(true);
  });

  it('should include Webhooks menu when user has api-log-u permission', () => {
    TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-log-u'] });
    service = TestBed.inject(ApiV4MenuService);

    const api = fakeApiV4();
    const menu = service.getMenu(api);

    expect(menu.subMenuItems.some(item => item.displayName === 'Webhooks')).toBe(true);
  });

  it('should not include Webhooks menu when user lacks api-log-r and api-log-u permissions', () => {
    TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-definition-r'] });
    service = TestBed.inject(ApiV4MenuService);

    const api = fakeApiV4();
    const menu = service.getMenu(api);

    expect(menu.subMenuItems.some(item => item.displayName === 'Webhooks')).toBe(false);
  });

  describe('Entrypoints menu tabs', () => {
    const httpListener = {
      type: 'HTTP' as const,
      paths: [{ path: '/test' }],
      entrypoints: [{ type: 'http-proxy' }],
    };

    it('should expose Entrypoints and CORS tabs for MCP_PROXY (no MCP Entrypoint, no Response Templates)', () => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-response_templates-r'] });
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({
        type: 'MCP_PROXY',
        listeners: [{ ...httpListener, entrypoints: [{ type: 'mcp-proxy' }] }],
      });

      const menu = service.getMenu(api);
      const entrypoints = menu.subMenuItems.find(item => item.displayName === 'Entrypoints');

      expect(entrypoints).toBeDefined();
      expect(entrypoints.tabs?.map(t => t.displayName)).toEqual(['Entrypoints', 'CORS']);
    });

    it('should expose Entrypoints, MCP Entrypoint, Response Templates and CORS tabs for PROXY', () => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-response_templates-r'] });
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({ type: 'PROXY', listeners: [httpListener] });

      const menu = service.getMenu(api);
      const entrypoints = menu.subMenuItems.find(item => item.displayName === 'Entrypoints');

      expect(entrypoints.tabs?.map(t => t.displayName)).toEqual(['Entrypoints', 'MCP Entrypoint', 'Response Templates', 'CORS']);
    });

    it('should render a single Entrypoints route (no tabs) for NATIVE', () => {
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({ type: 'NATIVE' });
      const menu = service.getMenu(api);
      const entrypoints = menu.subMenuItems.find(item => item.displayName === 'Entrypoints');

      expect(entrypoints.tabs).toBeUndefined();
      expect(entrypoints.routerLink).toBe('v4/entrypoints');
    });

    it('should render a single Entrypoints route (no tabs) for LLM_PROXY', () => {
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({ type: 'LLM_PROXY', listeners: [httpListener] });
      const menu = service.getMenu(api);
      const entrypoints = menu.subMenuItems.find(item => item.displayName === 'Entrypoints');

      expect(entrypoints.tabs).toBeUndefined();
      expect(entrypoints.routerLink).toBe('v4/entrypoints');
    });
  });

  describe('API Traffic menu header', () => {
    beforeEach(() => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-analytics-r'] });
      service = TestBed.inject(ApiV4MenuService);
    });

    it.each([
      ['NATIVE', undefined],
      ['PROXY', undefined],
      ['MCP_PROXY', undefined],
      ['LLM_PROXY', undefined],
      ['MESSAGE', { title: 'API Traffic' }],
    ] as const)('apiType %s -> header %p', (type, expected) => {
      const apiTraffic = service.getMenu(fakeApiV4({ type })).subMenuItems.find(item => item.displayName === 'API Traffic');
      expect(apiTraffic?.header).toEqual(expected);
    });
  });

  describe('Logs menu for NATIVE API', () => {
    it('should include Logs menu pointing to v4/runtime-logs-native when user has api-native_log-r permission', () => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api-native_log-r'] });
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({ type: 'NATIVE' });
      const menu = service.getMenu(api);

      const logsEntry = menu.subMenuItems.find(item => item.displayName === 'Logs');
      expect(logsEntry).toBeDefined();
      expect(logsEntry.routerLink).toBe('v4/runtime-logs-native');
    });

    it('should not include Logs menu for NATIVE API when user has only api-log-r permission', () => {
      service = TestBed.inject(ApiV4MenuService);

      const api = fakeApiV4({ type: 'NATIVE' });
      const menu = service.getMenu(api);

      expect(menu.subMenuItems.some(item => item.displayName === 'Logs')).toBe(false);
    });
  });
});
