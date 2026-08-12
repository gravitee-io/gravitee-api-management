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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { PortalCategoriesResponse, PortalCategory } from '../entities/categories/portal-category';

@Injectable({
  providedIn: 'root',
})
export class PortalCategoriesService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  getCategories(): Observable<PortalCategory[]> {
    return this.http.get<PortalCategoriesResponse>(`${this.configService.baseURL}/portal-categories`).pipe(map(res => res.data ?? []));
  }
}
