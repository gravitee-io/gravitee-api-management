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
import { isEmpty } from 'lodash';
import { Observable, of } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Group } from '../entities/group/group';
import { GroupMembership } from '../entities/group/groupMember';
import { Member } from '../entities/members/members';
import { Invitation } from '../entities/invitation/invitation';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<Group[]> {
    return this.http.get<Group[]>(`${this.constants.env.baseURL}/configuration/groups`);
  }

  listByOrganization(): Observable<Group[]> {
    return this.http.get<Group[]>(`${this.constants.org.baseURL}/groups`);
  }

  addOrUpdateMemberships(groupId: string, groupMemberships: GroupMembership[]): Observable<void> {
    // Remove Membership with empty roles
    const filterEmptyMembershipRoles = (groupMembership: GroupMembership[]) => groupMembership.filter((m) => !isEmpty(m.roles));

    const groupMembershipToSend = filterEmptyMembershipRoles(groupMemberships);
    if (isEmpty(groupMembershipToSend)) {
      return of(void 0);
    }

    return this.http.post<void>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/members`, groupMembershipToSend);
  }

  deleteMember(groupId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/members/${memberId}`);
  }

  delete(id: string) {
    if (id) {
      return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/groups/${id}`);
    }
  }

  get(id: string) {
    return this.http.get<Group>(`${this.constants.env.baseURL}/configuration/groups/${id}`);
  }

  addToExistingComponents(id: string, type: string) {
    return this.http.post<Group>(`${this.constants.env.baseURL}/configuration/groups/${id}/memberships?type=${type}`, {});
  }

  saveOrUpdate(mode: string, group: Group) {
    if (mode === 'edit') {
      return this.http.put<Group>(`${this.constants.env.baseURL}/configuration/groups/${group.id}`, group);
    } else {
      return this.http.post<Group>(`${this.constants.env.baseURL}/configuration/groups`, group);
    }
  }

  getMembers(id: string): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.constants.env.baseURL}/configuration/groups/${id}/members`, {});
  }

  inviteMember(id: string, invitation: Invitation) {
    return this.http.post<any>(`${this.constants.env.baseURL}/configuration/groups/${id}/invitations/`, invitation);
  }
}
