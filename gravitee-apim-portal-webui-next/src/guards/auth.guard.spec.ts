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

import { authGuard } from './auth.guard';
import { fakeUser } from '../entities/user/user.fixtures';
import { CurrentUserService } from '../services/current-user.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('authGuard', () => {
  let currentUserService: CurrentUserService;
  let activatedRoute: ActivatedRoute;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) => TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    currentUserService = TestBed.inject(CurrentUserService);
    activatedRoute = TestBed.inject(ActivatedRoute);
    router = TestBed.inject(Router);
  });

  it('should not allow anonymous user', () => {
    currentUserService.user.set({});
    jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(router.navigate).toBeCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['']);
  });

  it('should allow authenticated user', () => {
    currentUserService.user.set(fakeUser());
    jest.spyOn(router, 'navigate');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(router.navigate).toBeCalledTimes(0);
  });
});
