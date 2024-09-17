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
import { GroupsResponse, MembersResponse } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class GroupV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getMembers(groupId: string, page = 1, perPage = 10): Observable<MembersResponse> {
    return this.http.get<MembersResponse>(`${this.constants.env.v2BaseURL}/groups/${groupId}/members`, {
      params: {
        page,
        perPage,
      },
    });
  }

  list(page = 1, perPage = 10): Observable<GroupsResponse> {
    return this.http.get<GroupsResponse>(`${this.constants.env.v2BaseURL}/groups`, {
      params: {
        page,
        perPage,
      },
    });
  }

  public getPermissions(groupId: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.constants.env.v2BaseURL}/groups/${groupId}/permissions`);
  }
}
