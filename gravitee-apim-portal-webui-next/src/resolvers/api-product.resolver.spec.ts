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
import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { filter, firstValueFrom, of, take, throwError } from 'rxjs';

import { apiProductResolver } from './api-product.resolver';
import { fakeApiProduct } from '../entities/api-product/api-product.fixture';
import { ApiProductsService } from '../services/api-products.service';

describe('apiProductResolver', () => {
  @Component({ template: '' })
  class StubComponent {}

  let router: Router;
  let apiProductsService: { getById: jest.Mock };

  beforeEach(async () => {
    apiProductsService = { getById: jest.fn() };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'api-product/:apiProductId', component: StubComponent, resolve: { apiProduct: apiProductResolver } },
          { path: '404', component: StubComponent },
        ]),
        { provide: ApiProductsService, useValue: apiProductsService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should resolve API Product details', async () => {
    const apiProduct = fakeApiProduct();
    apiProductsService.getById.mockReturnValue(of(apiProduct));

    await router.navigateByUrl(`/api-product/${apiProduct.id}`);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(apiProductsService.getById).toHaveBeenCalledWith(apiProduct.id);
    expect(router.routerState.snapshot.root.firstChild?.data['apiProduct']).toEqual(apiProduct);
  });

  it('should end navigation on /404 when API Product details return 404', async () => {
    apiProductsService.getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' })));
    const navigatedTo404 = firstValueFrom(
      router.events.pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        filter(event => event.urlAfterRedirects === '/404'),
        take(1),
      ),
    );

    void router.navigateByUrl('/api-product/unknown');

    await navigatedTo404;
    await TestBed.inject(ApplicationRef).whenStable();

    expect(router.url).toBe('/404');
  });
});
