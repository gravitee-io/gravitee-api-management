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
import RoleService from "./role.service";
import ApplicationService from './applications.service';
import ApiService from './api.service';
import _ = require('lodash');

class UserService {
  private baseURL: string;
  private usersURL: string;
  private userURL: string;
  private routerInitialized: boolean = false;
  private isLogout: boolean = false;

  /**
   * Current authenticated user or empty user if not authenticated.
   */
  public currentUser: User;

  constructor(private $http: ng.IHttpService, private $q: ng.IQService, Constants, private RoleService: RoleService,
              private PermPermissionStore, private $urlRouter, private ApplicationService: ApplicationService,
              private ApiService: ApiService, private $location) {
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

  isUserHasPermissions(permissions) {
    return this.currentUser && this.currentUser.allowedTo(permissions);
  }

  current() {
    let that = this;
    if (! this.currentUser || !this.currentUser.username) {
      const promises = [this.$http.get(this.userURL)];

      const applicationRegex = /applications\/([\w|\-]+)/;
      let applicationId = applicationRegex.exec(this.$location.$$path);
      if (!that.isLogout && applicationId) {
        promises.push(this.ApplicationService.getPermissions(applicationId[1]));
      }

      const apiRegex = /apis\/([\w|\-]+)/;
      const apiId = apiRegex.exec(this.$location.$$path);
      if (!that.isLogout && apiId) {
        promises.push(this.ApiService.getPermissions(apiId[1]));
      }

      return this.$q.all(promises)
        .then(response => {
          that.currentUser = Object.assign(new User(), response[0].data);

          that.currentUser.userPermissions = [];
          _.forEach(that.currentUser.roles, function (role) {
            _.forEach(_.keys(role.permissions), function (permission) {
              _.forEach(role.permissions[permission], function (right) {
                let permissionName = role.scope + '-' + permission + '-' + right;
                that.currentUser.userPermissions.push(_.toLower(permissionName));
              });
            });
          });

          if (response[1]) {
            if (_.includes(response[1].config.url, 'applications')) {
              this.currentUser.userApplicationPermissions = [];
              _.forEach(_.keys(response[1].data), function (permission) {
                _.forEach(response[1].data[permission], function (right) {
                  let permissionName = 'APPLICATION-' + permission + '-' + right;
                  that.currentUser.userApplicationPermissions.push(_.toLower(permissionName));
                });
              });
            } else if (_.includes(response[1].config.url, 'apis')) {
              this.currentUser.userApiPermissions = [];
              _.forEach(_.keys(response[1].data), function (permission) {
                _.forEach(response[1].data[permission], function (right) {
                  let permissionName = 'API-' + permission + '-' + right;
                  that.currentUser.userApiPermissions.push(_.toLower(permissionName));
                });
              });
            }
          }

          that.reloadPermissions();

          return that.currentUser;
        }).then(response => {
          if (!that.routerInitialized) {
            that.$urlRouter.sync();
            that.$urlRouter.listen();
            that.routerInitialized = true;
          }
          return response;
        });
    } else {
      return this.$q.resolve<User>(this.currentUser);
    }
  }

  reloadPermissions() {
    const rights: string[] = this.RoleService.listRights();
    const scopes = this.RoleService.listScopes();
    let allPermissions: string[] = [];

    const that = this;
    _.forEach(scopes, function (scope) {
      let permissionsByScope = that.RoleService.listPermissionsByScope(scope);
      _.forEach(permissionsByScope, function (permission) {
        _.forEach(rights, function (right) {
          let permissionName = scope + '-' + permission + '-' + right;
          allPermissions.push(_.toLower(permissionName));
        })
      })
    });

    this.PermPermissionStore.defineManyPermissions(allPermissions, function (permissionName) {
      return _.includes(that.currentUser.userPermissions, permissionName) ||
        _.includes(that.currentUser.userApiPermissions, permissionName) ||
        _.includes(that.currentUser.userApplicationPermissions, permissionName);
    });
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
      .then(function() {that.currentUser = new User(); that.isLogout = true;});
  }

  currentUserPicture(): ng.IPromise<string> {
    if (! this.currentUser || !this.currentUser.picture) {
      let that = this;
      return this.current().then((user) => {
        return that.$http.get(`${that.userURL + user.username}/picture`).then( (response) => {
          this.currentUser.picture = response.data.toString();
          return this.$q.resolve<string>(this.currentUser.picture);
        });
      });
    } else {
      return this.$q.resolve<string>(this.currentUser.picture);
    }
  }

  save(user) {
    return this.$http.put(`${this.userURL + user.username}/`, {username: user.username, picture: user.picture});
  }

  listPlanSubscription() {
    return this.$http.get(`${this.userURL}subscriptions`);
  }
}

export default UserService;
