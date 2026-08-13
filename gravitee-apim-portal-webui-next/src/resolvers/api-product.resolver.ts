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
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterStateSnapshot } from '@angular/router';
import { catchError, EMPTY, Observable } from 'rxjs';

import { ApiProduct } from '../entities/api-product/api-product';
import { ApiProductsService } from '../services/api-products.service';

export const apiProductResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  apiProductsService: ApiProductsService = inject(ApiProductsService),
  router: Router = inject(Router),
): Observable<ApiProduct> =>
  apiProductsService.getById(route.params['apiProductId']).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        void router.navigate(['/404']);
        return EMPTY;
      }
      throw error;
    }),
  )) satisfies ResolveFn<ApiProduct>;
