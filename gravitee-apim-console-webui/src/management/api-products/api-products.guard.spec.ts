/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';

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

  describe('loadPermissions', () => {
    it('calls_load_api_product_permissions_with_route_api_product_id_and_returns_true', done => {
      const loadSpy = jest.spyOn(permissionService, 'loadApiProductPermissions').mockReturnValue(of(undefined));

      const fakeRoute = { params: { apiProductId: API_PRODUCT_ID } } as unknown as ActivatedRouteSnapshot;
      const fakeState = {} as RouterStateSnapshot;

      TestBed.runInInjectionContext(() => {
        const result$ = ApiProductsGuard.loadPermissions(fakeRoute, fakeState);
        (result$ as ReturnType<typeof of>).subscribe((canActivate: boolean) => {
          expect(loadSpy).toHaveBeenCalledWith(API_PRODUCT_ID);
          expect(canActivate).toBe(true);
          done();
        });
      });
    });
  });

  describe('clearPermissions', () => {
    it('calls_clear_api_product_permissions_and_returns_true', () => {
      const clearSpy = jest.spyOn(permissionService, 'clearApiProductPermissions');

      const fakeRoute = {} as ActivatedRouteSnapshot;
      const fakeState = {} as RouterStateSnapshot;
      const fakeNextState = {} as RouterStateSnapshot;

      let result: boolean;
      TestBed.runInInjectionContext(() => {
        result = ApiProductsGuard.clearPermissions(null, fakeRoute, fakeState, fakeNextState) as boolean;
      });

      expect(clearSpy).toHaveBeenCalled();
      expect(result).toBe(true);
    });
  });
});
