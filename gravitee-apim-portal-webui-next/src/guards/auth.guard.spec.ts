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
import { OAuthService } from 'angular-oauth2-oidc';

import { authGuard } from './auth.guard';
import { fakeUser } from '../entities/user/user.fixtures';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('authGuard', () => {
  let currentUserService: CurrentUserService;
  let oauthService: OAuthService;
  let activatedRoute: ActivatedRoute;
  let configService: ConfigService;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) => TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    currentUserService = TestBed.inject(CurrentUserService);
    oauthService = TestBed.inject(OAuthService);
    activatedRoute = TestBed.inject(ActivatedRoute);
    configService = TestBed.inject(ConfigService);
    router = TestBed.inject(Router);
  });

  it('should allow authenticated user', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    currentUserService.user.set(fakeUser());

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(parseUrl).not.toHaveBeenCalled();
    expect(createUrlTree).not.toHaveBeenCalled();
  });

  it('should redirect authenticated user coming from SSO', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    currentUserService.user.set(fakeUser());
    oauthService.state = encodeURIComponent('/redirectPath');

    expect(executeGuard(activatedRoute.snapshot, { url: '', root: activatedRoute.snapshot })).toBeTruthy();
    expect(oauthService.state).toEqual('');
    expect(parseUrl).toHaveBeenCalledWith('/redirectPath');
    expect(createUrlTree).not.toHaveBeenCalled();
  });

  it('should redirect unauthenticated user to login', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    configService.configuration.authentication!.forceLogin!.enabled = true;
    const url = '/test';

    expect(executeGuard(activatedRoute.snapshot, { url, root: activatedRoute.snapshot })).toBeTruthy();
    expect(parseUrl).not.toHaveBeenCalled();
    expect(createUrlTree).toHaveBeenCalledWith(['/log-in'], { queryParams: { redirectUrl: url } });
  });

  it('should allow unauthenticated user, anonymous routes', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    configService.configuration.authentication!.forceLogin!.enabled = true;

    expect(executeGuard(activatedRoute.snapshot, { url: '/log-in', root: activatedRoute.snapshot })).toBeTruthy();
    expect(executeGuard(activatedRoute.snapshot, { url: '/sign-up', root: activatedRoute.snapshot })).toBeTruthy();
    expect(executeGuard(activatedRoute.snapshot, { url: '/log-in/reset-password', root: activatedRoute.snapshot })).toBeTruthy();

    expect(parseUrl).not.toHaveBeenCalled();
    expect(createUrlTree).not.toHaveBeenCalled();
  });

  it('should allow unauthenticated user, login not forced', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    const createUrlTree = jest.spyOn(router, 'createUrlTree');
    configService.configuration.authentication!.forceLogin!.enabled = false;

    expect(executeGuard(activatedRoute.snapshot, { url: '/test', root: activatedRoute.snapshot })).toBeTruthy();

    expect(parseUrl).not.toHaveBeenCalled();
    expect(createUrlTree).not.toHaveBeenCalled();
  });
});
