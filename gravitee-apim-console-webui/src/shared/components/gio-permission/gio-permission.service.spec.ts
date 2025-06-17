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
import { of } from 'rxjs';

import { GioPermissionService, GioTestingPermissionProvider } from './gio-permission.service';

import { ApiService } from '../../../services-ngx/api.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { ApplicationService } from '../../../services-ngx/application.service';

const mockApiService = {
  getPermissions: jest.fn().mockReturnValue(of({})),
};
const mockEnvironmentService = {
  getPermissions: jest.fn().mockReturnValue(of({})),
};
const mockApplicationService = {
  getPermissions: jest.fn().mockReturnValue(of({})),
};
const mockAjsCurrentUserService = null;
describe('Permission matching', () => {
  let service: GioPermissionService;

  function setTestPermissions(permissions: string[]) {
    (service as any)._setPermissions(permissions);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GioPermissionService,
        { provide: GioTestingPermissionProvider, useValue: [] },
        { provide: 'CurrentUserService', useValue: mockAjsCurrentUserService },
        { provide: ApiService, useValue: mockApiService },
        { provide: EnvironmentService, useValue: mockEnvironmentService },
        { provide: ApplicationService, useValue: mockApplicationService },
      ],
    });

    service = TestBed.inject(GioPermissionService);
  });

  describe('hasAllMatching', () => {
    it('should return true when all permissions are present', () => {
      setTestPermissions(['api-definition-u', 'api-gateway_definition-u']);
      expect(service.hasAllMatching(['api-definition-u', 'api-gateway_definition-u'])).toBe(true);
    });

    it('should return false when only one permission is present', () => {
      setTestPermissions(['api-definition-u']);
      expect(service.hasAllMatching(['api-definition-u', 'api-gateway_definition-u'])).toBe(false);
    });

    it('should return false when no permissions match', () => {
      setTestPermissions(['other-permission']);
      expect(service.hasAllMatching(['api-definition-u', 'api-gateway_definition-u'])).toBe(false);
    });
  });

  describe('hasAnyMatching', () => {
    it('should return true when the permission is present', () => {
      setTestPermissions(['api-definition-u']);
      expect(service.hasAnyMatching(['api-definition-u'])).toBe(true);
    });

    it('should return false when the permission is not present', () => {
      setTestPermissions(['api-gateway_definition-u']);
      expect(service.hasAnyMatching(['api-definition-u'])).toBe(false);
    });

    it('should return false with empty permissions', () => {
      setTestPermissions([]);
      expect(service.hasAnyMatching(['api-definition-u'])).toBe(false);
    });
  });
});
