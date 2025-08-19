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
import { Constants } from 'src/entities/Constants';
import { AddMember, Member, MembersResponse, UpdateMember } from 'src/entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ClusterMemberService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getPermissions(clusterId: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/members/permissions`);
  }

  getMembers(clusterId: string, page = 1, perPage = 10): Observable<MembersResponse> {
    return this.http.get<MembersResponse>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/members`, {
      params: {
        page,
        perPage,
      },
    });
  }

  addMember(clusterId: string, membership: AddMember): Observable<Member> {
    return this.http.post<Member>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/members`, membership);
  }

  updateMember(clusterId: string, membership: UpdateMember): Observable<Member> {
    return this.http.put<Member>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/members/${membership.memberId}`, membership);
  }

  deleteMember(clusterId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/members/${memberId}`);
  }
}
