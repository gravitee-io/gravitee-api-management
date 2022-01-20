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

class RoleService {
  private permissionsByScope: any;

  constructor(private $http, private Constants, private $q) {
    'ngInject';
  }

  listRights() {
    return ['c', 'r', 'u', 'd'];
  }

  listScopes() {
    return this.fetchScopes().then((permissionsByScope) => {
      return permissionsByScope;
    });
  }

  listPermissionsByScope(scope: string) {
    return this.fetchScopes().then((permissionsByScope) => {
      return permissionsByScope[scope];
    });
  }

  isUserRoleManagement(scope: string) {
    return 'ORGANIZATION' === scope;
  }

  get(roleScope, roleName) {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/rolescopes/` + roleScope + '/roles/' + roleName).then((response) => {
      const role = response.data;
      role.scope = _.toUpper(role.scope);
      return role;
    });
  }

  list(scope: string) {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/rolescopes/` + scope + '/roles').then((response) => {
      return _.map(response.data, (role: any) => {
        role.scope = _.toUpper(role.scope);
        return role;
      });
    });
  }

  create(role) {
    return this.$http.post(`${this.Constants.org.baseURL}/configuration/rolescopes/` + role.scope + '/roles', role).then((response) => {
      const role = response.data;
      role.scope = _.toUpper(role.scope);
      return role;
    });
  }

  update(role) {
    return this.$http
      .put(`${this.Constants.org.baseURL}/configuration/rolescopes/` + role.scope + '/roles/' + role.name, role)
      .then((response) => {
        const role = response.data;
        role.scope = _.toUpper(role.scope);
        return role;
      });
  }

  delete(role) {
    return this.$http.delete(`${this.Constants.org.baseURL}/configuration/rolescopes/` + role.scope + '/roles/' + role.name);
  }

  listUsers(roleScope, roleName) {
    return this.$http
      .get(`${this.Constants.org.baseURL}/configuration/rolescopes/` + roleScope + '/roles/' + roleName + '/users')
      .then((response) => response.data);
  }

  deleteUser(role, username) {
    return this.$http.delete(
      `${this.Constants.org.baseURL}/configuration/rolescopes/` + role.scope + '/roles/' + role.name + '/users/' + username,
    );
  }

  addRole(roleScope, roleName, user) {
    return this.$http.post(`${this.Constants.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${roleName}/users`, user);
  }

  private fetchScopes() {
    if (this.permissionsByScope) {
      return this.$q.resolve(this.permissionsByScope);
    } else {
      return this.$http.get(`${this.Constants.org.baseURL}/configuration/rolescopes/`).then((response) => {
        this.permissionsByScope = response.data;
        return this.permissionsByScope;
      });
    }
  }
}

export default RoleService;
