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
});
