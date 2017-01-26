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
class GroupService {
  private groupsURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.groupsURL = `${Constants.baseURL}configuration/groups`;
  }

  getEmptyGroup() {
    return  {
      "id": null,
      "name": "NONE"
    };
  }

  list(type) {
    if (type) {
      return this.$http.get(this.groupsURL + "?type=" + type);
    }
    return this.$http.get(this.groupsURL);
  }

  getMembers(groupId) {
    return this.$http.get([this.groupsURL, groupId, "members"].join("/"));
  }

  create(newGroup) {
    if(newGroup) {
      return this.$http.post(this.groupsURL, newGroup);
    }
  }

  update(groupId, name) {
    if(groupId && name) {
      return this.$http.put( [this.groupsURL, groupId].join("/"), { 'name': name });
    }
  }

  remove(groupId) {
    if(groupId) {
      return this.$http.delete([this.groupsURL, groupId].join("/"));
    }
  }

  addOrUpdateMember(group, member) {
    return this.$http.post([this.groupsURL, group, 'members?user=' + member.username + '&type=' + member.type].join("/"));
  }

  deleteMember(group, memberUsername) {
    return this.$http.delete([this.groupsURL, group, 'members?user=' + memberUsername].join("/"));
  }
}

export default GroupService;
