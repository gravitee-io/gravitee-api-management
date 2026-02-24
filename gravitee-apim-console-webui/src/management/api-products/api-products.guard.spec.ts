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
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';

import { ApiProductsGuard } from './api-products.guard';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING } from '../../shared/testing';
import { Constants } from '../../entities/Constants';

describe('ApiProductsGuard', () => {
  const API_PRODUCT_ID = 'product-guard-test';

  let permissionService: GioPermissionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GioPermissionService,
        { provide: Constants, useValue: CONSTANTS_TESTING },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    permissionService = TestBed.inject(GioPermissionService);
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    jest.clearAllMocks();
  });

  const fakeRoute = { params: { apiProductId: API_PRODUCT_ID } } as unknown as ActivatedRouteSnapshot;
  const fakeState = {} as RouterStateSnapshot;

  describe('loadPermissions', () => {
    it('loads API product permissions and returns true', async () => {
      const loadSpy = jest.spyOn(permissionService, 'loadApiProductPermissions').mockReturnValue(of(undefined));

      const result = await TestBed.runInInjectionContext(() =>
        firstValueFrom(ApiProductsGuard.loadPermissions(fakeRoute, fakeState) as Observable<boolean>),
      );

      expect(loadSpy).toHaveBeenCalledWith(API_PRODUCT_ID);
      expect(result).toBe(true);
    });

    it('propagates error when permission load fails', async () => {
      jest.spyOn(permissionService, 'loadApiProductPermissions').mockReturnValue(throwError(() => ({ error: { message: 'Forbidden' } })));

      await expect(
        TestBed.runInInjectionContext(() => firstValueFrom(ApiProductsGuard.loadPermissions(fakeRoute, fakeState) as Observable<boolean>)),
      ).rejects.toEqual({ error: { message: 'Forbidden' } });
    });
  });

  describe('clearPermissions', () => {
    it('clears API product permissions on deactivation', () => {
      const clearSpy = jest.spyOn(permissionService, 'clearApiProductPermissions');

      TestBed.runInInjectionContext(() => {
        ApiProductsGuard.clearPermissions(null, fakeRoute, fakeState, fakeState);
      });

      expect(clearSpy).toHaveBeenCalled();
    });
  });
});
