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
import RoleService from './role.service';
import ApplicationService from './application.service';
import ApiService from './api.service';
import StringService from './string.service';
import { UrlService } from '@uirouter/angularjs';
import { PagedResult } from '../entities/pagedResult';
import Base64Service from './base64.service';
import _ = require('lodash');
import EnvironmentService from './environment.service';
import { IHttpResponse } from 'angular';

class UserService {
  /**
   * Current authenticated user or empty user if not authenticated.
   */
  public currentUser: User;
  private routerInitialized: boolean = false;
  private isLogout: boolean = false;

  constructor(
    private $http: ng.IHttpService,
    private $q: ng.IQService,
    private Constants,
    private RoleService: RoleService,
    private PermPermissionStore,
    private $urlService: UrlService,
    private ApplicationService: ApplicationService,
    private ApiService: ApiService,
    private EnvironmentService: EnvironmentService,
    private $location,
    private $cookies,
    private $window,
    private StringService: StringService,
    private Base64Service: Base64Service,
  ) {
    'ngInject';
  }

  list(query?: string, page = 1, size = 10): ng.IPromise<any> {
    let url = `${this.Constants.org.baseURL}/users/?page=${page}&size=${size}`;

    if (query) {
      url += '&q=' + query;
    }

    return this.$http.get(url);
  }

  get(code: string): ng.IPromise<User> {
    return this.$http.get(`${this.Constants.org.baseURL}/users/` + code).then((response) => Object.assign(new User(), response.data));
  }

  remove(userId: string): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.org.baseURL}/users/` + userId);
  }

  removeCurrentUser(): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.org.baseURL}/user/`);
  }

  create(user): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/`, user);
  }

  register(user): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/registration`, user);
  }

  customUserFieldsToRegister(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/custom-user-fields`);
  }

  finalizeRegistration(user): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/registration/finalize`, user);
  }

  finalizeResetPassword(userId, user): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/${userId}/changePassword`, user);
  }

  search(query): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/search/users/?q=${query}`);
  }

  isUserHasPermissions(permissions) {
    return this.currentUser && this.currentUser.allowedTo(permissions);
  }

  isUserHasAllPermissions(permissions) {
    return this.currentUser && this.currentUser.allowedToAnd(permissions);
  }

  refreshEnvironmentPermissions(): ng.IPromise<User> {
    let that = this;

    return this.EnvironmentService.getPermissions(this.Constants.org.currentEnv.id).then((response) => {
      that.currentUser.userEnvironmentPermissions = this.getEnvironmentPermissions(response);
      return this.$q.resolve<User>(that.currentUser);
    });
  }

  current(): ng.IPromise<User> {
    let that = this;
    if (!this.currentUser || !this.currentUser.authenticated) {
      const promises: ng.IPromise<IHttpResponse<any>>[] = [
        this.$http.get(`${this.Constants.org.baseURL}/user/`, {
          silentCall: true,
          forceSessionExpired: true,
        } as ng.IRequestShortcutConfig),
      ];

      const applicationRegex = /applications\/([\w|\-]+)/;
      let applicationId = applicationRegex.exec(this.$location.$$path);
      if (this.Constants.org.currentEnv && !that.isLogout && applicationId && applicationId[1] !== 'create') {
        promises.push(this.ApplicationService.getPermissions(applicationId[1]));
      }

      const apiRegex = /apis\/([\w|\-]+)/;
      const apiId = apiRegex.exec(this.$location.$$path);
      if (this.Constants.org.currentEnv && !that.isLogout && apiId && apiId[1] !== 'new') {
        promises.push(this.ApiService.getPermissions(apiId[1]));
      }

      const environmentRegex = /environments\/([\w|\-]+)/;
      const environmentId = environmentRegex.exec(this.$location.$$path);
      if (environmentId && environmentId[1]) {
        promises.push(this.EnvironmentService.getPermissions(environmentId[1]));
      }

      return this.$q
        .all(promises)
        .then((response) => {
          that.currentUser = Object.assign(new User(), response[0].data);

          that.currentUser.userPermissions = [];
          _.forEach(that.currentUser.roles, function (role) {
            _.forEach(_.keys(role.permissions), function (permission) {
              _.forEach(role.permissions[permission], function (right) {
                if (role.scope === 'ORGANIZATION') {
                  let permissionName = role.scope + '-' + permission + '-' + right;
                  that.currentUser.userPermissions.push(_.toLower(permissionName));
                }
              });
            });
          });

          const apiOrApplicationResponse = response[1];
          if (apiOrApplicationResponse) {
            this.currentUser.userEnvironmentPermissions = this.getEnvironmentPermissions(apiOrApplicationResponse);

            if (_.includes(apiOrApplicationResponse.config.url, 'applications')) {
              this.currentUser.userApplicationPermissions = [];
              _.forEach(_.keys(apiOrApplicationResponse.data), function (permission) {
                _.forEach(apiOrApplicationResponse.data[permission], function (right) {
                  let permissionName = 'APPLICATION-' + permission + '-' + right;
                  that.currentUser.userApplicationPermissions.push(_.toLower(permissionName));
                });
              });
            } else if (_.includes(apiOrApplicationResponse.config.url, 'apis')) {
              this.currentUser.userApiPermissions = [];
              _.forEach(_.keys(apiOrApplicationResponse.data), function (permission) {
                _.forEach(apiOrApplicationResponse.data[permission], function (right) {
                  let permissionName = 'API-' + permission + '-' + right;
                  that.currentUser.userApiPermissions.push(_.toLower(permissionName));
                });
              });
            }
          }

          that.reloadPermissions();

          that.currentUser.authenticated = true;
          return this.$q.resolve<User>(that.currentUser);
        })
        .catch((error) => {
          // Returns an unauthenticated user
          this.currentUser = new User();
          this.currentUser.authenticated = false;
          return this.$q.resolve<User>(this.currentUser);
        })
        .finally(() => {
          if (!that.routerInitialized) {
            that.$urlService.sync();
            that.$urlService.listen();
            that.routerInitialized = true;
          }
        });
    } else {
      return this.$q.resolve<User>(this.currentUser);
    }
  }

  reloadPermissions() {
    const rights: string[] = this.RoleService.listRights();
    this.RoleService.listScopes().then((permissionsByScope) => {
      let allPermissions: string[] = [];
      _.forEach(permissionsByScope, function (permissions, scope) {
        _.forEach(permissions, function (permission) {
          _.forEach(rights, function (right) {
            let permissionName = scope + '-' + permission + '-' + right;
            allPermissions.push(_.toLower(permissionName));
          });
        });
      });

      this.PermPermissionStore.defineManyPermissions(allPermissions, (permissionName) => {
        return (
          _.includes(this.currentUser.userPermissions, permissionName) ||
          _.includes(this.currentUser.userEnvironmentPermissions, permissionName) ||
          _.includes(this.currentUser.userApiPermissions, permissionName) ||
          _.includes(this.currentUser.userApplicationPermissions, permissionName)
        );
      });
    });
  }

  isAuthenticated(): boolean {
    return this.currentUser !== undefined && this.currentUser.id !== undefined;
  }

  login(user): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.org.baseURL}/user/login`,
      {},
      {
        headers: {
          Authorization: `Basic ${this.Base64Service.encode(`${user.username}:${user.password}`)}`,
        },
      },
    );
  }

  logout(): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/user/logout`, {}).then(() => {
      this.removeCurrentUserData();
    });
  }

  removeCurrentUserData() {
    this.currentUser = new User();
    this.currentUser.authenticated = false;
    this.isLogout = true;
    this.$window.localStorage.removeItem('satellizer_token');
    this.$cookies.remove('Auth-Graviteeio-APIM');
    this.$window.localStorage.removeItem('newsletterProposed');
  }

  currentUserPicture(): string {
    if (this.currentUser && this.currentUser.id) {
      return `${this.Constants.org.baseURL}/user/avatar?${this.StringService.hashCode(this.currentUser.id)}`;
    }
  }

  getUserAvatar(id: string): string {
    return `${this.Constants.org.baseURL}/users/` + id + '/avatar';
  }

  getUserGroups(id: string): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/users/` + id + '/groups');
  }

  getCurrentUserGroups(): any {
    if (this.currentUser != null && this.currentUser.groupsByEnvironment != null) {
      return this.currentUser.groupsByEnvironment[this.Constants.org.currentEnv.id];
    }
    return [];
  }

  save(user): ng.IPromise<any> {
    return this.$http.put(`${this.Constants.org.baseURL}/user/`, {
      username: user.username,
      picture: user.picture,
      newsletter: user.newsletter,
      email: user.email,
      firstname: user.firstname,
      lastname: user.lastname,
      customFields: user.customFields,
    });
  }

  subscribeNewsletter(email): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.userURL}newsletter/subscribe`, email);
  }

  getNewsletterTaglines(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.userURL}newsletter/taglines`);
  }

  resetPassword(id: string): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/${id}/resetPassword`, {});
  }

  getMemberships(id: string, type: string): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/users/${id}/memberships?type=${type}`);
  }

  setTasks(tasks: PagedResult) {
    this.currentUser.tasks.populate(tasks);
    return this.currentUser;
  }

  getCurrentUserTags(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.org.baseURL}/user/tags`);
  }

  updateUserRoles(user: string, referenceType: string, referenceId: string, roles: string[]): ng.IPromise<any> {
    return this.$http.put(`${this.Constants.org.baseURL}/users/${user}/roles`, {
      user,
      referenceId,
      referenceType,
      roles,
    });
  }

  processRegistration(id: string, accepted: boolean): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.org.baseURL}/users/${id}/_process`, accepted);
  }

  private getEnvironmentPermissions(response: IHttpResponse<any>): string[] {
    if (!response.config.url.includes('environments')) {
      return [];
    }

    let permissions = [] as string[];

    response.data.forEach((envWithPermissions) => {
      Object.keys(envWithPermissions.permissions).forEach((permission) => {
        envWithPermissions.permissions[permission].forEach((right) => {
          let permissionName = `ENVIRONMENT-${permission}-${right}`.toLowerCase();
          permissions.push(permissionName);
        });
      });
    });

    return permissions;
  }
}

export default UserService;
