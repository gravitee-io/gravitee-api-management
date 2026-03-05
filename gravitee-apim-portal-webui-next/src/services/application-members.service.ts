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
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import {
  AddMembersRequest,
  ApplicationRolesV2Response,
  MemberV2,
  MembersV2Response,
  SearchUsersV2Response,
  TransferOwnershipRequest,
} from '../entities/application-members/application-members';

@Injectable({
  providedIn: 'root',
})
export class ApplicationMembersService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  list(applicationId: string, page?: number, size?: number, query?: string): Observable<MembersV2Response> {
    const params: Record<string, string | number> = {};
    if (page != null) params['page'] = page;
    if (size != null) params['size'] = size;
    if (query) params['query'] = query;

    return this.http.get<MembersV2Response>(`${this.configService.baseURL}/applications/${applicationId}/membersV2`, { params });
  }

  listRoles(): Observable<ApplicationRolesV2Response> {
    return this.http.get<ApplicationRolesV2Response>(`${this.configService.baseURL}/configuration/applications/rolesV2`);
  }

  updateMemberRole(applicationId: string, memberId: string, role: string): Observable<MemberV2> {
    return this.http.put<MemberV2>(`${this.configService.baseURL}/applications/${applicationId}/membersV2/${memberId}`, { role });
  }

  deleteMember(applicationId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.configService.baseURL}/applications/${applicationId}/membersV2/${memberId}`);
  }

  searchUsers(applicationId: string, query: string): Observable<SearchUsersV2Response> {
    return this.http.post<SearchUsersV2Response>(
      `${this.configService.baseURL}/applications/${applicationId}/membersV2/_search-users`,
      null,
      { params: { q: query } },
    );
  }

  addMembers(applicationId: string, request: AddMembersRequest): Observable<MemberV2[]> {
    return this.http.post<MemberV2[]>(`${this.configService.baseURL}/applications/${applicationId}/membersV2`, request);
  }

  transferOwnership(applicationId: string, request: TransferOwnershipRequest): Observable<void> {
    return this.http.post<void>(
      `${this.configService.baseURL}/applications/${applicationId}/membersV2/_transfer-ownership`,
      request,
    );
  }
}
