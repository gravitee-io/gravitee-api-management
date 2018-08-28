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

  update(groupId, updatedGroup): ng.IPromise<any> {
    if(groupId && updatedGroup) {
      let grpEntity = GroupService._mapToEntity(updatedGroup);
      return this.$http.put( [this.groupsURL, groupId].join("/"), grpEntity);
    }
  }

  static _mapToEntity(grp) {
    let grpEntity = {
      name: grp.name,
      roles: grp.roles
    };
    let eventRules = [];
    if (grp.defaultApi) {
      eventRules.push({event: "API_CREATE"});
    }
    if (grp.defaultApplication) {
      eventRules.push({event: "APPLICATION_CREATE"});
    }
    if (eventRules.length > 0) {
      grpEntity["event_rules"] = eventRules;
    }
    
    return grpEntity;
  }

  remove(groupId): ng.IPromise<any> {
    if(groupId) {
      return this.$http.delete([this.groupsURL, groupId].join("/"));
    }
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

}

export default GroupService;
