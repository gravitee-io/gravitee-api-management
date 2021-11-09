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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, combineLatest } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { RoleService } from '../../../services-ngx/role.service';
import { Role, RoleScope } from '../../../entities/role/role';

interface RoleVM {
  name: string;
  description: string;
  isDefault: boolean;
  isSystem: boolean;
  canBeDeleted: boolean;
  icon: string;
  hasUserRoleManagement: boolean;
}

@Component({
  selector: 'org-settings-roles',
  template: require('./org-settings-roles.component.html'),
  styles: [require('./org-settings-roles.component.scss')],
})
export class OrgSettingsRolesComponent implements OnInit, OnDestroy {
  rolesByScope: Array<{ scope: string; roles: RoleVM[] }>;
  loading = true;

  constructor(private readonly roleService: RoleService) {}

  private readonly unsubscribe$ = new Subject<boolean>();

  ngOnInit(): void {
    combineLatest([
      this.roleService.list('ORGANIZATION'),
      this.roleService.list('ENVIRONMENT'),
      this.roleService.list('API'),
      this.roleService.list('APPLICATION'),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([orgRoles, envRoles, apiRoles, appRoles]) => {
          this.rolesByScope = [
            { scope: 'Organization', roles: this.convertToRoleVMs(orgRoles) },
            { scope: 'Environment', roles: this.convertToRoleVMs(envRoles) },
            { scope: 'API', roles: this.convertToRoleVMs(apiRoles) },
            { scope: 'Application', roles: this.convertToRoleVMs(appRoles) },
          ];
          this.loading = false;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onAddARoleClicked(scope: string) {}

  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onEditRoleClicked(role: RoleVM) {}

  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onDeleteRoleClicked(role: RoleVM) {}

  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onMembersClicked(role: RoleVM) {}

  private convertToRoleVMs(roles: Role[]): RoleVM[] {
    return roles.map((role) => ({
      name: role.name,
      description: role.description,
      isDefault: role.default,
      isSystem: role.system,
      hasUserRoleManagement: role.scope === 'ORGANIZATION',
      canBeDeleted: !role.default && !role.system,
      icon: OrgSettingsRolesComponent.getScopeIcon(role.scope),
    }));
  }

  private static getScopeIcon(scope: RoleScope): string {
    switch (scope) {
      case 'API':
        return 'dashboard';
      case 'APPLICATION':
        return 'list';
      case 'ENVIRONMENT':
        return 'dns';
      case 'ORGANIZATION':
        return 'corporate_fare';
      default:
        return '';
    }
  }
}
