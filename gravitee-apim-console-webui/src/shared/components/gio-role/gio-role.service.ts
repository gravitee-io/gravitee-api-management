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
import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';

import { User } from '../../../entities/user/user';

export type GioTestingRole = { scope?: string; name?: string }[];

export const GioTestingRoleProvider = new InjectionToken<GioTestingRole>('GioTestingRole');

@Injectable({ providedIn: 'root' })
export class GioRoleService {
  private currentUserRoles: { scope?: string; name?: string }[];

  constructor(@Optional() @Inject(GioTestingRoleProvider) roles: GioTestingRole) {
    this._setRoles(roles);
  }

  loadCurrentUserRoles(user: User): void {
    this.currentUserRoles = user.roles;
  }

  _setRoles(roles: GioTestingRole) {
    this.currentUserRoles = roles;
  }

  hasRole(role: { scope: string; name: string }): boolean {
    if (!role) {
      return false;
    }
    const { scope, name } = role;
    return this.currentUserRoles.some(role => role.scope === scope && role.name === name);
  }

  hasAnyMatching(roles: { scope: string; name: string }[]) {
    return roles.filter(role => this.hasRole(role)).length > 0;
  }

  isOrganizationAdmin(): boolean {
    return this.currentUserRoles?.some(role => role.scope === 'ORGANIZATION' && role.name === 'ADMIN');
  }
}
