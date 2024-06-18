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
import { Router } from '@angular/router';

import { redirectGuard } from './redirect.guard';
import { ConfigService } from '../services/config.service';

describe('redirectGuard', () => {
  let configService: ConfigService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { portalNext: { access: { enabled: false } } } },
        { provide: Router, useValue: { navigateByUrl: jest.fn() } },
      ],
    });
    configService = TestBed.inject(ConfigService);
    router = TestBed.inject(Router);
  });

  it('should redirect when access is not enabled', () => {
    const href = 'http://localhost/next';
    Object.defineProperty(window, 'location', {
      value: { href },
    });
    expect(TestBed.runInInjectionContext(() => redirectGuard())).toBeTruthy();
    expect(router.navigateByUrl).toHaveBeenCalledWith('http://localhost/404');
  });

  it('should not redirect when access is enabled', () => {
    if (configService.portalNext && configService.portalNext.access) {
      configService.portalNext.access.enabled = true;
    }
    expect(TestBed.runInInjectionContext(() => redirectGuard())).toBeTruthy();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
