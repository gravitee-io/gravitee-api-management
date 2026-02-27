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
import { ActivatedRoute, ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';

import { permissionGuard } from './permission.guard';
import { fakeUser } from '../entities/user/user.fixtures';
import { CurrentUserService } from '../services/current-user.service';
import { AppTestingModule } from '../testing/app-testing.module';

describe('permissionGuard', () => {
  let currentUserService: CurrentUserService;
  let activatedRoute: ActivatedRoute;
  let router: Router;
  const executeGuard: CanActivateFn = (...guardParameters) => TestBed.runInInjectionContext(() => permissionGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    currentUserService = TestBed.inject(CurrentUserService);
    activatedRoute = TestBed.inject(ActivatedRoute);
    router = TestBed.inject(Router);
  });

  it('should allow access when no permissions required', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(fakeUser());

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, { data: {} });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should allow access when user has required permission', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['C'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
    });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should allow access when user has any of the required permissions', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['R'],
          USER: ['C'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C', 'USER-C'],
        },
      },
    });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should block access when user does not have required permission', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['R'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('/');
  });

  it('should redirect to unauthorizedFallbackTo when specified', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['R'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
          unauthorizedFallbackTo: '/applications',
        },
      },
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('/applications');
  });

  it('should redirect to parent route when no fallback specified', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['R'],
        },
      }),
    );

    const parentRoute: Partial<ActivatedRouteSnapshot> = Object.assign({}, activatedRoute.snapshot, {
      routeConfig: { path: 'applications' },
    });

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
      parent: parentRoute as ActivatedRouteSnapshot,
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('applications');
  });

  it('should redirect to homepage when no parent route and no fallback', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['R'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
      parent: null,
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('/');
  });

  it('should block access when user has no permissions', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(fakeUser({ permissions: undefined }));

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('/');
  });

  it('should block access when user permissions object is empty', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(fakeUser({ permissions: {} }));

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C'],
        },
      },
    });

    const result = executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot });
    expect(result).toBeInstanceOf(UrlTree);
    expect(parseUrl).toHaveBeenCalledWith('/');
  });

  it('should handle all CRUD actions', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['C', 'R', 'U', 'D'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C', 'APPLICATION-R', 'APPLICATION-U', 'APPLICATION-D'],
        },
      },
    });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should handle different resources', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(
      fakeUser({
        permissions: {
          APPLICATION: ['C'],
          USER: ['R'],
        },
      }),
    );

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: ['APPLICATION-C', 'USER-R'],
        },
      },
    });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });

  it('should allow access when anyOf is empty array', () => {
    const parseUrl = jest.spyOn(router, 'parseUrl');
    currentUserService.user.set(fakeUser());

    const routeSnapshot = Object.assign({}, activatedRoute.snapshot, {
      data: {
        permissions: {
          anyOf: [],
        },
      },
    });

    expect(executeGuard(routeSnapshot, { url: '', root: activatedRoute.snapshot })).toBe(true);
    expect(parseUrl).not.toHaveBeenCalled();
  });
});
