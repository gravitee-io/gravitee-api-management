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

import { catalogTabsViewGuard } from './catalog-tabs-view.guard';
import { Configuration } from '../entities/configuration/configuration';
import { ConfigService } from '../services/config.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('catalogTabsViewGuard', () => {
  let activatedRoute: ActivatedRoute;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) => TestBed.runInInjectionContext(() => catalogTabsViewGuard(...guardParameters));

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

  it('should allow tabs view if no mode is specified', () => {
    init({ portalNext: { catalog: { viewMode: undefined } } });
    const spy = jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(spy).toHaveBeenCalledTimes(0);
  });

  it('should allow tabs view if TABS mode is specified', () => {
    init({ portalNext: { catalog: { viewMode: 'TABS' } } });
    jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(router.navigate).toHaveBeenCalledTimes(0);
  });

  it('should not allow tabs view if CATEGORIES mode is specified', () => {
    init({ portalNext: { catalog: { viewMode: 'CATEGORIES' } } });
    const spy = jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeFalsy();
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith(['catalog', 'categories']);
  });
});
