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
import { ActivatedRoute, CanActivateFn, Router } from '@angular/router';

import { registrationEnabledGuard } from './registration-enabled.guard';
import { Configuration } from '../entities/configuration/configuration';
import { ConfigService } from '../services/config.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('registrationEnabledGuard', () => {
  let activatedRoute: ActivatedRoute;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => registrationEnabledGuard(...guardParameters));

  const init = (config: Configuration) => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: config,
          },
        },
      ],
    });
    activatedRoute = TestBed.inject(ActivatedRoute);
    router = TestBed.inject(Router);
  };

  it('should allow registration when user registration is enabled', () => {
    init({ portal: { userCreation: { enabled: true } } });
    const spy = jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should redirect to log-in when user registration is disabled', () => {
    init({ portal: { userCreation: { enabled: false } } });
    const spy = jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeFalsy();
    expect(spy).toHaveBeenCalledWith(['log-in']);
  });

  it('should redirect to log-in when user registration is not configured', () => {
    init({});
    const spy = jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeFalsy();
    expect(spy).toHaveBeenCalledWith(['log-in']);
  });
});
