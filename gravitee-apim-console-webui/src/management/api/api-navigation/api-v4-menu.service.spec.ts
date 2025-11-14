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
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { TestBed } from '@angular/core/testing';
import { LICENSE_CONFIGURATION_TESTING, GioLicenseService } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { ApiV4MenuService } from './api-v4-menu.service';

import { GioTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';
import { ApiV4, fakeApiV4 } from '../../../entities/management-api-v2';

describe('ApiV4MenuService', () => {
  let service: ApiV4MenuService;
  let permissionService: { hasAnyMatching: jest.Mock };
  let environmentSettingsService: { getSnapshot: jest.Mock };
  let licenseService: { isMissingFeature$: jest.Mock };
  let apiDocumentationV2Service: { getApiPortalUrl: jest.Mock; getApiNotInPortalTooltip: jest.Mock };

  beforeEach(() => {
    permissionService = {
      hasAnyMatching: jest.fn().mockReturnValue(true),
    };
    environmentSettingsService = {
      getSnapshot: jest.fn().mockReturnValue({ apiScore: { enabled: false } }),
    };
    licenseService = {
      isMissingFeature$: jest.fn().mockReturnValue(of(false)),
    };
    apiDocumentationV2Service = {
      getApiPortalUrl: jest.fn().mockReturnValue('portal-url'),
      getApiNotInPortalTooltip: jest.fn().mockReturnValue('tooltip'),
    };

    TestBed.configureTestingModule({
      providers: [
        ApiV4MenuService,
        { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
        { provide: GioPermissionService, useValue: permissionService },
        { provide: EnvironmentSettingsService, useValue: environmentSettingsService },
        { provide: GioLicenseService, useValue: licenseService },
        { provide: ApiDocumentationV2Service, useValue: apiDocumentationV2Service },
      ],
      imports: [GioTestingModule],
    });
    service = TestBed.inject(ApiV4MenuService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should include Webhooks menu when webhook entrypoint is present', () => {
    const api = fakeApiV4();

    const menu = service.getMenu(api as ApiV4);

    expect(menu.subMenuItems.some((item) => item.displayName === 'Webhooks')).toBe(true);
  });

  it('should not include Webhooks menu when webhook entrypoint is absent', () => {
    const api = fakeApiV4((base) => ({
      ...base,
      listeners: [
        {
          type: 'HTTP',
          entrypoints: [{ type: 'http-proxy' }],
        },
      ],
    }));

    const menu = service.getMenu(api as ApiV4);

    expect(menu.subMenuItems.some((item) => item.displayName === 'Webhooks')).toBe(false);
  });

  it('should not include Webhooks menu when user lacks api-definition-u permission', () => {
    permissionService.hasAnyMatching.mockImplementation((permissions: unknown) => {
      const perms = permissions as string[];
      return !perms.includes('api-definition-u');
    });
    const api = fakeApiV4();

    const menu = service.getMenu(api as ApiV4);

    expect(menu.subMenuItems.some((item) => item.displayName === 'Webhooks')).toBe(false);
  });
});
