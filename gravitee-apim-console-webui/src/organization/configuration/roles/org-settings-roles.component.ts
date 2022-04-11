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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject, combineLatest, EMPTY } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { StateService } from '@uirouter/angularjs';
import { MatDialog } from '@angular/material/dialog';

import { RoleService } from '../../../services-ngx/role.service';
import { Role, RoleScope } from '../../../entities/role/role';
import { UIRouterState } from '../../../ajs-upgraded-providers';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

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
  rolesByScope: Array<{ scope: string; scopeId: string; roles: RoleVM[] }>;
  loading = true;

  constructor(
    private readonly roleService: RoleService,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

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
            { scope: 'Organization', scopeId: 'ORGANIZATION', roles: this.convertToRoleVMs(orgRoles) },
            { scope: 'Environment', scopeId: 'ENVIRONMENT', roles: this.convertToRoleVMs(envRoles) },
            { scope: 'API', scopeId: 'API', roles: this.convertToRoleVMs(apiRoles) },
            { scope: 'Application', scopeId: 'APPLICATION', roles: this.convertToRoleVMs(appRoles) },
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

  onAddARoleClicked(scope: string) {
    this.ajsState.go('organization.settings.ng-rolenew', { roleScope: scope });
  }

  onEditRoleClicked(scope: string, role: RoleVM) {
    this.ajsState.go('organization.settings.ng-roleedit', { roleScope: scope, role: role.name });
  }

  onDeleteRoleClicked(scope: string, role: RoleVM) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete a Role',
          content: `Are you sure you want to delete the role <strong>${role.name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteRoleConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.roleService.delete(scope, role.name)),
        tap(() => this.snackBarService.success(`Role ${role.name} successfully deleted!`)),
        catchError(() => {
          this.snackBarService.error(`Failed to delete Role ${role.name}`);
          return EMPTY;
        }),
      )
      .subscribe(() => this.ngOnInit());
  }

  onMembersClicked(scope: string, role: RoleVM) {
    this.ajsState.go('organization.settings.ng-rolemembers', { roleScope: scope.toUpperCase(), role: role.name });
  }

  private convertToRoleVMs(roles: Role[]): RoleVM[] {
    return roles.map((role) => ({
      name: role.name,
      description: role.description,
      isDefault: role.default,
      isSystem: role.system,
      hasUserRoleManagement: role.scope === 'ORGANIZATION',
      canBeDeleted: !role.default && !role.system,
      isReadOnly: role.scope === 'ORGANIZATION' && role.name === 'ADMIN',
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
