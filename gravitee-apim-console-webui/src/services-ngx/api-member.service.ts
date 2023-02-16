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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { ApiMember, ApiMembership } from '../entities/api';

@Injectable({ providedIn: 'root' })
export class ApiMemberService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getMembers(api: string): Observable<ApiMember[]> {
    return this.http.get<ApiMember[]>(`${this.constants.env.baseURL}/apis/${api}/members`);
  }

  addOrUpdateMember(api: string, membership: ApiMembership): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/${api}/members`, membership);
  }

  deleteMember(api: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/apis/${api}/members?user=${userId}`);
  }

  // TODO add this when needed
  // transferOwnership(api: string, ownership: ApiMembership): Observable<any> {
  //   return this.http.post(`${this.constants.env.baseURL}/apis/${api}/members/transfer_ownership`, ownership);
  // }
}
