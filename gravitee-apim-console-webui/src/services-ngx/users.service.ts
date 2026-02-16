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
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { User } from '../entities/user/user';
import { NewPreRegisterUser } from '../entities/user/newPreRegisterUser';
import { Group } from '../entities/group/group';
import { UserMembership } from '../entities/user/userMembership';
import { SearchableUser } from '../entities/user/searchableUser';

@Injectable({
  providedIn: 'root',
})
export class UsersService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(query?: string, page = 1, size = 10): Observable<PagedResult<User>> {
    return this.http.get<PagedResult<User>>(`${this.constants.org.baseURL}/users`, {
      params: {
        page,
        size,
        ...(query ? { q: query } : {}),
      },
    });
  }

  get(userId: string): Observable<User> {
    return this.http.get<User>(`${this.constants.org.baseURL}/users/${userId}`);
  }

  getUserAvatar(userId: string): string {
    return `${this.constants.org.baseURL}/users/${userId}/avatar`;
  }

  getUserGroups(userId: string): Observable<Group[]> {
    return this.http.get<Group[]>(`${this.constants.org.baseURL}/users/${userId}/groups`);
  }

  create(user: NewPreRegisterUser): Observable<User> {
    return this.http.post<User>(`${this.constants.org.baseURL}/users`, user);
  }

  remove(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/users/${userId}`);
  }

  updateUserRoles(user: string, referenceType: string, referenceId: string, roles: string[]): Observable<void> {
    return this.http.put<void>(`${this.constants.org.baseURL}/users/${user}/roles`, {
      user,
      referenceId,
      referenceType,
      roles,
    });
  }

  getMemberships(id: string, type: 'api'): Observable<UserMembership<'api'>>;
  getMemberships(id: string, type: 'application'): Observable<UserMembership<'application'>>;
  getMemberships(id: string, type: string): Observable<UserMembership> {
    return this.http.get<UserMembership>(`${this.constants.org.baseURL}/users/${id}/memberships?type=${type}`).pipe(
      map(response => ({
        memberships: response?.memberships ?? [],
        metadata: response?.metadata ?? {},
      })),
    );
  }

  resetPassword(id: string): Observable<void> {
    return this.http.post<void>(`${this.constants.org.baseURL}/users/${id}/resetPassword`, {});
  }

  processRegistration(userId: string, accepted: boolean): Observable<void> {
    return this.http.post<void>(`${this.constants.org.baseURL}/users/${userId}/_process`, accepted);
  }

  search(query: string): Observable<SearchableUser[]> {
    return this.http.get<SearchableUser[]>(`${this.constants.org.baseURL}/search/users`, {
      params: {
        q: query,
      },
    });
  }
}
