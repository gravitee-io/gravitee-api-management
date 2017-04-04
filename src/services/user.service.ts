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
import { User } from '../entities/user';

class UserService {
  private baseURL: string;
  private usersURL: string;
  private userURL: string;

  /**
   * Current authenticated user or empty user if not authenticated.
   */
  private currentUser: User;

  constructor(private $http: ng.IHttpService, private $q: ng.IQService, private $rootScope, Constants) {
    'ngInject';
    this.baseURL = Constants.baseURL;
    this.usersURL = `${Constants.baseURL}users/`;
    this.userURL = `${Constants.baseURL}user/`;
  }

  list() {
    return this.$http.get(this.usersURL);
  }

  get(code: string): ng.IPromise<User> {
    return this.$http.get(this.usersURL + code).then(response => Object.assign(new User(), response.data));
  }

  create(user) {
    return this.$http.post(`${this.baseURL}users`, user);
  }

  register(user) {
    return this.$http.post(`${this.usersURL}register`, user);
  }

	search(query) {
		return this.$http.get(`${this.usersURL}?query=${query}`);
	}

	isUserInRoles(roles) {
    return this.currentUser && this.currentUser.allowedTo(roles);
    /*
	  if (!this.currentUser) {
	    return false;
	  }
    if (this.currentUser && (!roles || roles.length === 0)) {
	    return false;
	  }
    var rolesAllowed = false, that = this;
	  _.forEach(roles, function(role) {
	    _.forEach(that.currentUser.authorities, function(authority) {
        if (authority.authority === role) {
	        rolesAllowed = true;
	        return;
	      }
	    });
	  });
    return rolesAllowed;
    */
	}

  current(): ng.IPromise<User> {
    if (! this.currentUser || !this.currentUser.username) {
      let that = this;
      return this.$http.get(this.userURL)
        .then(response => Object.assign(new User(), response.data))
        .then(response => {
          that.currentUser = response;
          return that.currentUser;
        });
    } else {
      return this.$q.resolve<User>(this.currentUser);
    }
  }

  isAuthenticated(): boolean {
    return (this.currentUser !== undefined && this.currentUser.username !== undefined);
  }

  login(user) {
    return this.$http.post(`${this.userURL}login`, {}, {
      headers: {
        Authorization: `Basic ${btoa(`${user.username}:${user.password}`)}`
      }
    });
  }

  logout() {
    let that = this;
    return this.$http.post(`${this.userURL}logout`, {})
      .then(function() {that.currentUser = new User();});
  }

  currentUserPicture() {
    return this.$http.get(`${this.userURL + this.$rootScope.graviteeUser.username}/picture`);
  }

  save(user) {
    return this.$http.put(`${this.userURL + user.username}/`, {username: user.username, picture: user.picture});
  }

  listPlanSubscription() {
    return this.$http.get(`${this.userURL}subscriptions`);
  }
}

export default UserService;
