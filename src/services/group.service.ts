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
import * as _ from 'lodash';

class GroupService {
  private groupsURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.groupsURL = `${Constants.baseURL}configuration/groups`;
  }

  get(groupId: string): ng.IPromise<any>{
    return this.$http.get(`${this.groupsURL}/${groupId}`);
  };

  list(): ng.IPromise<any> {
    return this.$http.get(this.groupsURL);
  }

  getMembers(groupId): ng.IPromise<any> {
    return this.$http.get([this.groupsURL, groupId, "members"].join("/"));
  }

  create(newGroup): ng.IPromise<any> {
    if(newGroup) {
      let grpEntity = GroupService._mapToEntity(newGroup);
      return this.$http.post(this.groupsURL, grpEntity);
    }
  }

  update(updatedGroup): ng.IPromise<any> {
    if(updatedGroup.id && updatedGroup) {
      let grpEntity = GroupService._mapToEntity(updatedGroup);
      return this.$http.put([this.groupsURL, updatedGroup.id].join("/"), grpEntity);
    }
  }

  static _mapToEntity(grp) {
    return {
      name: grp.name,
      roles: grp.roles,
      event_rules: grp.event_rules,
      max_invitation: grp.max_invitation,
      lock_api_role: grp.lock_api_role,
      lock_application_role: grp.lock_application_role,
      system_invitation: grp.system_invitation,
      email_invitation: grp.email_invitation
    };
  }

  remove(groupId): ng.IPromise<any> {
    if(groupId) {
      return this.$http.delete([this.groupsURL, groupId].join("/"));
    }
  }

  updateEventRules(group, defaultApi, defaultApplication) {
    let eventRules = [];
    if (defaultApi) {
      eventRules.push({event: "API_CREATE"});
    }
    if (defaultApplication) {
      eventRules.push({event: "APPLICATION_CREATE"});
    }

    group["event_rules"] = eventRules;
  }

  addOrUpdateMember(group, members: any[]): ng.IPromise<any> {
    let groupRole = [];
    //at least one role is mandatory
    let body = [];
    _.forEach(members, (member) => {
      let rolenames = _.filter(_.values(member.roles), (rolename) => !_.isEmpty(rolename));
      if (rolenames.length > 0) {
        let roleScopes = _.keys(member.roles);
        _.forEach(roleScopes, (roleScope) => {
          groupRole.push({
            scope: roleScope,
            name: member.roles[roleScope]
          });
        });
        body.push({
          'id': member.id,
          'reference': member.reference,
          'roles': groupRole
        });
      }
    });

    if (body.length > 0) {
      return this.$http.post([this.groupsURL, group, 'members'].join("/"), body);
    }
  }

  deleteMember(group, memberUsername): ng.IPromise<any> {
    return this.$http.delete([this.groupsURL, group, 'members', memberUsername].join("/"));
  }

  getMemberships(group: string, type: string): ng.IPromise<any> {
    return this.$http.get(`${this.groupsURL}/${group}/memberships?type=${type}`)
  }

  getInvitationsURL(groupId: string): string {
    return this.groupsURL + '/' + groupId + '/invitations/';
  };

  getInvitations(groupId: string): ng.IPromise<any> {
    return this.$http.get(this.getInvitationsURL(groupId));
  }

  inviteMember(group: any, email: string): ng.IPromise<any> {
    return this.$http.post(this.getInvitationsURL(group.id), {
      reference_type: 'GROUP',
      reference_id: group.id,
      email: email,
      api_role: group.api_role,
      application_role: group.application_role
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
}

export default GroupService;
