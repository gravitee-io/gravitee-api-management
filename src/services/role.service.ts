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
import _ = require('lodash');

class RoleService {
  private roleURL: string;

  private permissionsByScope: any = {
    MANAGEMENT: {
      permissions: ['INSTANCE', 'GROUP', 'TAG', 'TENANT', 'API', 'ROLE', 'APPLICATION', 'PLATFORM', 'AUDIT'],
      userRoleManagement: true
    },
    PORTAL: {
      permissions: ['METADATA', 'DOCUMENTATION', 'APPLICATION', 'VIEW'],
      userRoleManagement: true
    },
    API: {
      permissions: ['DEFINITION', 'GATEWAY_DEFINITION', 'PLAN', 'SUBSCRIPTION', 'MEMBER', 'METADATA', 'ANALYTICS', 'EVENT', 'HEALTH', 'LOG', 'DOCUMENTATION', 'AUDIT', 'RATING', 'RATING_ANSWER']
    },
    APPLICATION: {
      permissions: ['DEFINITION', 'MEMBER', 'ANALYTICS', 'LOG', 'SUBSCRIPTION']
    }
  };

  constructor(private $http, Constants) {
    'ngInject';
    this.roleURL = `${Constants.baseURL}configuration/rolescopes/`;
  }

  listRights() {
    return ['c', 'r', 'u', 'd'];
  }

  listScopes() {
    return _.keys(this.permissionsByScope);
  }

  listPermissionsByScope(scope: string) {
    return this.permissionsByScope[scope].permissions;
  }

  isUserRoleManagement(scope: string) {
    return this.permissionsByScope[scope].userRoleManagement;
  }

  get(roleScope, roleName) {
    return this.$http.get(this.roleURL + roleScope + '/roles/' + roleName).then(response => {
      let role = response.data;
      role.scope = _.toUpper(role.scope);
      return role;
    });
  }

  list(scope: string) {
    return this.$http.get(this.roleURL + scope + '/roles').then(response => {
      return _.map(response.data, function (role: any) {
        role.scope = _.toUpper(role.scope);
        return role;
      });
    });
  }

  create(role) {
    return this.$http.post(this.roleURL + role.scope + '/roles', role).then(response => {
      let role = response.data;
      role.scope = _.toUpper(role.scope);
      return role;
    });
  }

  update(role) {
    return this.$http.put(this.roleURL + role.scope + '/roles/' + role.name, role).then(response => {
      let role = response.data;
      role.scope = _.toUpper(role.scope);
      return role;
    });
  }

  delete(role) {
    return this.$http.delete(this.roleURL + role.scope + '/roles/' + role.name);
  }

  listUsers(roleScope, roleName) {
    return this.$http.get(this.roleURL + roleScope + '/roles/' + roleName + '/users').then(response => response.data);
  }

  deleteUser(role, username) {
    return this.$http.delete(this.roleURL + role.scope + '/roles/' + role.name + '/users/' + username);
  }

  addRole(roleScope, roleName, user) {
    return this.$http.post(`${this.roleURL}${roleScope}/roles/${roleName}/users`, user);
  }
}

export default RoleService;
