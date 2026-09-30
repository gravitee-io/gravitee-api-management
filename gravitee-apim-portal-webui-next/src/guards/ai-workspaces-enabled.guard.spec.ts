/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { aiWorkspacesEnabledGuard } from './ai-workspaces-enabled.guard';
import { ConfigService } from '../services/config.service';

describe('aiWorkspacesEnabledGuard', () => {
  const dummyRoute = {} as ActivatedRouteSnapshot;
  const dummyState = {} as RouterStateSnapshot;

  it('should allow navigation when Portal AI Workspace is enabled', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { configuration: { portalNext: { aiWorkspaces: { enabled: true } } } } },
        { provide: Router, useValue: { parseUrl: jest.fn() } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => aiWorkspacesEnabledGuard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  it('should redirect to home when Portal AI Workspace is disabled', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { configuration: { portalNext: { aiWorkspaces: { enabled: false } } } } },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => aiWorkspacesEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
    expect(result).toBe('PARSED');
  });
});
