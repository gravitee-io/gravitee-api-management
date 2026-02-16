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
import { PagedResult } from 'src/entities/pagedResult';

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { isEmpty } from 'lodash';
import { Observable, of } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Group } from '../entities/group/group';
import { GroupMembership } from '../entities/group/groupMember';
import { Invitation } from '../entities/invitation/invitation';
import { Member } from '../management/settings/groups/group/membershipState';

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

  listPaginated(page: number = 1, size: number = 20, query: string = ''): Observable<PagedResult<Group>> {
    return this.http.get<PagedResult<Group>>(
      `${this.constants.env.baseURL}/configuration/groups/_paged?page=${page}&size=${size}&query=${query}`,
    );
  }

  listByOrganization(): Observable<Group[]> {
    return this.http.get<Group[]>(`${this.constants.org.baseURL}/groups`);
  }

  addOrUpdateMemberships(groupId: string, groupMemberships: GroupMembership[], environmentId?: string): Observable<void> {
    // Remove Membership with empty roles
    const filterEmptyMembershipRoles = (groupMembership: GroupMembership[]) => groupMembership.filter(m => !isEmpty(m.roles));

    const groupMembershipToSend = filterEmptyMembershipRoles(groupMemberships);
    if (isEmpty(groupMembershipToSend)) {
      return of(void 0);
    }
    const environmentUrl = environmentId ? `${this.constants.org.baseURL}/environments/${environmentId}` : this.constants.env.baseURL;
    return this.http.post<void>(`${environmentUrl}/configuration/groups/${groupId}/members`, groupMembershipToSend);
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/groups/${id}`);
  }

  get(id: string) {
    return this.http.get<Group>(`${this.constants.env.baseURL}/configuration/groups/${id}`);
  }

  addToExistingComponents(groupId: string, type: string) {
    return this.http.post<Group>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/memberships?type=${type}`, {});
  }

  saveOrUpdate(mode: string, group: Group) {
    if (mode === 'edit') {
      return this.http.put<Group>(`${this.constants.env.baseURL}/configuration/groups/${group.id}`, group);
    } else {
      return this.http.post<Group>(`${this.constants.env.baseURL}/configuration/groups`, group);
    }
  }

  getMembers(groupId: string): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/members`);
  }

  inviteMember(groupId: string, invitation: Invitation): Observable<any> {
    return this.http.post<Invitation>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/invitations`, invitation, {
      observe: 'response',
    });
  }

  deleteMember(groupId: string, memberId: string, environmentId?: string): Observable<void> {
    const environmentUrl = environmentId ? `${this.constants.org.baseURL}/environments/${environmentId}` : this.constants.env.baseURL;
    return this.http.delete<void>(`${environmentUrl}/configuration/groups/${groupId}/members/${memberId}`);
  }

  getInvitations(groupId: string): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/invitations`);
  }

  deleteInvitation(groupId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/invitations/${invitationId}`);
  }

  getMemberships(groupId: string, membershipType: string): Observable<any> {
    return this.http.get<any>(`${this.constants.env.baseURL}/configuration/groups/${groupId}/memberships?type=${membershipType}`);
  }
}
