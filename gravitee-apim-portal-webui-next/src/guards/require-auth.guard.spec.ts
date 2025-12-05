/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ActivatedRoute, CanActivateFn, Router, UrlTree } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

import { requireAuthGuard } from './require-auth.guard';
import { fakeUser } from '../entities/user/user.fixtures';
import { CurrentUserService } from '../services/current-user.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('requireAuthGuard', () => {
  let currentUserService: CurrentUserService;
  let oauthService: OAuthService;
  let activatedRoute: ActivatedRoute;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) => TestBed.runInInjectionContext(() => requireAuthGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    currentUserService = TestBed.inject(CurrentUserService);
    oauthService = TestBed.inject(OAuthService);
    activatedRoute = TestBed.inject(ActivatedRoute);
    router = TestBed.inject(Router);
  });

  it('should allow authenticated user', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const navigate = jest.spyOn(router, 'navigate');
    currentUserService.user.set(fakeUser());

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(parseUrl).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should redirect authenticated user coming from SSO', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl').mockReturnValue(router.parseUrl('/redirectPath'));
    const navigate = jest.spyOn(router, 'navigate');
    currentUserService.user.set(fakeUser());
    oauthService.state = encodeURIComponent('/redirectPath');

    const result = executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(oauthService.state).toEqual('');
    expect(parseUrl).toHaveBeenCalledWith('/redirectPath');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should redirect unauthenticated user to login', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    currentUserService.user.set({});
    const url = '/applications/create';

    const result = executeGuard(activatedRoute.snapshot, { url, root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(createUrlTree).toHaveBeenCalledWith(['/log-in'], { queryParams: { redirectUrl: url } });
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should redirect unauthenticated user to login with current URL', () => {
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    currentUserService.user.set({});
    const url = '/test-route';

    const result = executeGuard(activatedRoute.snapshot, { url, root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(createUrlTree).toHaveBeenCalledWith(['/log-in'], { queryParams: { redirectUrl: url } });
  });

  it('should redirect unauthenticated user even when forceLogin is disabled', () => {
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    currentUserService.user.set({});
    const url = '/applications';

    const result = executeGuard(activatedRoute.snapshot, { url, root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(createUrlTree).toHaveBeenCalledWith(['/log-in'], { queryParams: { redirectUrl: url } });
  });

  it('should clear OAuth state when redirecting authenticated user from SSO', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl').mockReturnValue(router.parseUrl('/some-path'));
    currentUserService.user.set(fakeUser());
    oauthService.state = encodeURIComponent('/some-path');

    const result = executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(oauthService.state).toEqual('');
    expect(parseUrl).toHaveBeenCalledWith('/some-path');
  });
});
