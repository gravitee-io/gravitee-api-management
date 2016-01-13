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
class UserService {

  constructor($http, $cookieStore, baseURL) {
    'ngInject';
    this.$http = $http;
    this.$cookieStore = $cookieStore;
    this.usersURL = baseURL + 'users/';
  }

  list() {
    return this.$http.get(this.usersURL);
  }

  get(code) {
    return this.$http.get(this.usersURL + code);
  }

  listTeams(code) {
    return this.$http.get(this.usersURL + code + '/teams');
  }

  create(user) {
    return this.$http.post(this.usersURL, user);
  }

	search(query) {
		return this.$http.get(this.usersURL + "?query=" + query);   
	}
	
	isUserInRoles(roles) {
	  let rolePrefix = "ROLE_";
	  let authenticatedUser = this.$cookieStore.get('authenticatedUser');
	  
	  if (!authenticatedUser) {
	    return false;
	  }
	  
	  if (authenticatedUser && (!roles || roles.length == 0)) {
	    return false;
	  }
	  
	  var rolesAllowed = false;
	  _.forEach(roles, function(role) {
	    _.forEach(authenticatedUser.principal.authorities, function(authority) {
	      if (authority.authority === (rolePrefix + role)) {
	        rolesAllowed = true;
	        return;
	      }
	    });
	  });
	  
	  return rolesAllowed;
	}
}

export default UserService;
