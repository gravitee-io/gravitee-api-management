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
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';
import { map, Observable } from 'rxjs';

import { Page } from '../entities/page/page';
import { PageService } from '../services/page.service';

export const pagesResolver = ((
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  pageService: PageService = inject(PageService),
): Observable<Page[]> => pageService.listByApiId(route.params['apiId']).pipe(map(({ data }) => data ?? []))) satisfies ResolveFn<Page[]>;
