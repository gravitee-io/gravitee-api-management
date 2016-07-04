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

  constructor($http, $rootScope, Constants) {
    'ngInject';
    this.$http = $http;
    this.$rootScope = $rootScope;
    this.usersURL = Constants.baseURL + 'users/';
    this.userURL = Constants.baseURL + 'user/';
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
    roles = (Array.isArray(roles)) ? roles : [roles];
    
	  if (!this.$rootScope.graviteeUser) {
	    return false;
	  }

	  if (this.$rootScope.graviteeUser && (!roles || roles.length == 0)) {
	    return false;
	  }

	  var rolesAllowed = false, that = this;
	  _.forEach(roles, function(role) {
	    _.forEach(that.$rootScope.graviteeUser.authorities, function(authority) {
	      if (authority.authority === role) {
	        rolesAllowed = true;
	        return;
	      }
	    });
	  });

	  return rolesAllowed;
	}

  current() {
    return this.$http.get(this.userURL);
  }

  login(user) {
    var req = {
      method: 'POST',
      url: this.userURL + 'login',
      headers: {
        'Authorization': "Basic " + btoa(user.username + ":" + user.password)
      }
    };
    return this.$http(req);
  }

  logout() {
    return this.$http.post(this.userURL + 'logout');
  }

  currentUserPicture() {
    return this.$http.get(this.userURL + this.$rootScope.graviteeUser.username + '/picture');
  }

  save(user) {
    return this.$http.put(this.userURL + this.$rootScope.graviteeUser.username + '/', {username: user.username, picture: user.picture});
  }
}

export default UserService;
