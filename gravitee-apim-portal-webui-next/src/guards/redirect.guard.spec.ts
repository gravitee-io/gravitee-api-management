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

import { redirectGuard } from './redirect.guard';
import { ConfigService } from '../services/config.service';

describe('redirectGuard', () => {
  let configService: ConfigService;

  Object.defineProperty(window, 'location', {
    configurable: true,
    enumerable: true,
    writable: true,
    value: {
      href: '',
    },
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ConfigService, useValue: { configuration: { portalNext: { access: { enabled: false } } } } }],
    });
    configService = TestBed.inject(ConfigService);
    window.location.href = 'http://localhost/next';
  });

  it('should redirect when access is not enabled', () => {
    expect(TestBed.runInInjectionContext(() => redirectGuard())).toBeTruthy();
    expect(window.location.href).toEqual('http://localhost/404');
  });

  it('should not redirect when access is enabled', () => {
    if (configService.configuration?.portalNext && configService.configuration?.portalNext.access) {
      configService.configuration.portalNext.access.enabled = true;
    }
    expect(TestBed.runInInjectionContext(() => redirectGuard())).toBeTruthy();
    expect(window.location.href).toEqual('http://localhost/next');
  });
});
