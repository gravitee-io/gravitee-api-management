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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { User } from '../entities/user/user';
import { NewExternalUser } from '../entities/user/newExternalUser';

@Injectable({
  providedIn: 'root',
})
export class UsersService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  list(query?: string, page = 1, size = 10): Observable<PagedResult<User>> {
    return this.http.get<PagedResult<User>>(`${this.constants.org.baseURL}/users`, {
      params: {
        page,
        size,
        ...(query ? { q: query } : {}),
      },
    });
  }

  create(user: NewExternalUser): Observable<User> {
    return this.http.post<User>(`${this.constants.org.baseURL}/users`, user);
  }

  remove(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/users/${userId}`);
  }
}
