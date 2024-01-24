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
import { Member } from '../entities/members/members';
import { ApplicationTransferOwnership } from '../entities/application/application';

@Injectable({
  providedIn: 'root',
})
export class ApplicationMembersService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  get(applicationId: string): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.constants.env.baseURL}/applications/${applicationId}/members`);
  }

  delete(applicationId: string, userId: string): Observable<Member> {
    return this.http.delete<Member>(`${this.constants.env.baseURL}/applications/${applicationId}/members?user=${userId}`);
  }

  update(applicationId: string, newMember: Member): Observable<Member> {
    return this.http.post<Member>(`${this.constants.env.baseURL}/applications/${applicationId}/members`, newMember);
  }

  transferOwnership(applicationId: string, ownership: ApplicationTransferOwnership): Observable<ApplicationTransferOwnership> {
    return this.http.post<ApplicationTransferOwnership>(
      `${this.constants.env.baseURL}/applications/${applicationId}/members/transfer_ownership`,
      ownership,
    );
  }
}
