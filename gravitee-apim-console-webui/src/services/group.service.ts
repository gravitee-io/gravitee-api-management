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
import { PageQuery } from '../entities/pageQuery';

class GroupService {
  static _mapToEntity(grp) {
    return {
      name: grp.name,
      roles: grp.roles,
      event_rules: grp.event_rules,
      max_invitation: grp.max_invitation,
      lock_api_role: grp.lock_api_role,
      lock_application_role: grp.lock_application_role,
      system_invitation: grp.system_invitation,
      email_invitation: grp.email_invitation,
      disable_membership_notifications: grp.disable_membership_notifications,
    };
  }

  constructor(private $http, private Constants) {
    'ngInject';
  }

  get(groupId: string): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/groups/${groupId}`);
  }

  list(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/groups`);
  }

  listByOrganization(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/groups`);
  }

  getMembers(groupId: string, page?: PageQuery, opts?: any): ng.IPromise<any> {
    let url = `${this.Constants.env.baseURL}/configuration/groups/${groupId}/members`;
    if (page != null) {
      url += '/_paged';
    }

    opts = opts || {};
    opts.params = {
      ...page,
    };

    return this.$http.get(url, opts);
  }

  create(newGroup): ng.IPromise<any> {
    if (newGroup) {
      const grpEntity = GroupService._mapToEntity(newGroup);
      return this.$http.post(`${this.Constants.env.baseURL}/configuration/groups`, grpEntity);
    }
  }

  update(updatedGroup): ng.IPromise<any> {
    if (updatedGroup.id && updatedGroup) {
      const grpEntity = GroupService._mapToEntity(updatedGroup);
      return this.$http.put([`${this.Constants.env.baseURL}/configuration/groups`, updatedGroup.id].join('/'), grpEntity);
    }
  }

  remove(groupId): ng.IPromise<any> {
    if (groupId) {
      return this.$http.delete([`${this.Constants.env.baseURL}/configuration/groups`, groupId].join('/'));
    }
  }

  updateEventRules(group, defaultApi, defaultApplication) {
    const eventRules = [];
    if (defaultApi) {
      eventRules.push({ event: 'API_CREATE' });
    }
    if (defaultApplication) {
      eventRules.push({ event: 'APPLICATION_CREATE' });
    }

    group.event_rules = eventRules;
  }

  addOrUpdateMember(group, members: any[]): ng.IPromise<any> {
    const body = this.mapMembers(members);

    if (body.length > 0) {
      return this.$http.post([`${this.Constants.env.baseURL}/configuration/groups`, group, 'members'].join('/'), body);
    }
  }

  deleteMember(group, memberUsername): ng.IPromise<any> {
    return this.$http.delete([`${this.Constants.env.baseURL}/configuration/groups`, group, 'members', memberUsername].join('/'));
  }

  getMemberships(group: string, type: string): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/groups/${group}/memberships?type=${type}`);
  }

  associate(group: string, type: string): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/configuration/groups/${group}/memberships?type=${type}`);
  }

  getInvitationsURL(groupId: string): string {
    return `${this.Constants.env.baseURL}/configuration/groups` + '/' + groupId + '/invitations/';
  }

  getInvitations(groupId: string): ng.IPromise<any> {
    return this.$http.get(this.getInvitationsURL(groupId));
  }

  inviteMember(group: any, email: string): ng.IPromise<any> {
    return this.$http.post(this.getInvitationsURL(group.id), {
      reference_type: 'GROUP',
      reference_id: group.id,
      email: email,
      api_role: group.api_role,
      application_role: group.application_role,
    });
  }

  deleteInvitation(groupId: string, invitation: string): ng.IPromise<any> {
    return this.$http.delete(this.getInvitationsURL(groupId) + invitation);
  }

  updateInvitation(groupId: string, invitation: any): ng.IPromise<any> {
    delete invitation.created_at;
    delete invitation.updated_at;
    return this.$http.put(this.getInvitationsURL(groupId) + invitation.id, invitation);
  }

  private mapRoles(roles = {}) {
    return Object.entries(roles)
      .filter(([scope]) => !!scope)
      .map(([scope, name]) => {
        return { scope, name };
      });
  }

  private mapMembers(members = []) {
    return members
      .map(({ id, roles, reference }) => {
        return {
          id,
          reference,
          roles: this.mapRoles(roles),
        };
      })
      .filter(({ roles }) => !!roles.length); // at least one role is mandatory
  }
}

export default GroupService;
