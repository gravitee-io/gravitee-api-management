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

import { PagedResult } from './pagedResult';

/**
 * @deprecated No longer seems to reflect the real user type.
 * With the Angular migration the User Type will be split in 2, one for the
 * user management and one for the currently authenticated user.
 * New type for user : "src/entities/user"
 */
export class User {
  public id: string;
  public username: string;
  public displayName: string;
  public email: string;
  public source: string;
  public roles: any[];
  public groupsByEnvironment: any[];
  public primaryOwner: boolean;
  public userPermissions: string[];
  public userApiPermissions: string[];
  public userEnvironmentPermissions: string[];
  public userApplicationPermissions: string[];
  public tasks: PagedResult;
  public notifications: PagedResult;
  public authenticated = false;
  public firstLogin: boolean;
  public picture: string;
  public picture_url: string;
  public number_of_active_tokens: number;
  constructor() {
    'ngInject';
    this.tasks = new PagedResult();
    this.notifications = new PagedResult();
  }

  allowedTo(permissions: string[]): boolean {
    if (!permissions || !this.userPermissions) {
      return false;
    }
    return (
      _.intersection(this.userPermissions, permissions).length > 0 ||
      _.intersection(this.userEnvironmentPermissions, permissions).length > 0 ||
      _.intersection(this.userApiPermissions, permissions).length > 0 ||
      _.intersection(this.userApplicationPermissions, permissions).length > 0
    );
  }

  allowedToAnd(permissions: string[]): boolean {
    if (!permissions || !this.userPermissions) {
      return false;
    }

    const allPermissions = _.concat(
      this.userPermissions,
      this.userEnvironmentPermissions,
      this.userApiPermissions,
      this.userApplicationPermissions,
    );

    return _.difference(permissions, allPermissions).length === 0;
  }

  isOrganizationAdmin(): boolean {
    return this.roles?.some((role) => role.scope === 'ORGANIZATION' && role.name === 'ADMIN');
  }
}
