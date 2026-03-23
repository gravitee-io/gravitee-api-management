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

import { Api } from '../entities/api/api';
import { ApiService } from '../services/api.service';

export const apiResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  apiService: ApiService = inject(ApiService),
  router: Router = inject(Router),
): Observable<Api> =>
  apiService.details(route.params['apiId']).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 404) {
        void router.navigate(['/404']);
        return EMPTY;
      }
      throw err;
    }),
  )) satisfies ResolveFn<Api>;
