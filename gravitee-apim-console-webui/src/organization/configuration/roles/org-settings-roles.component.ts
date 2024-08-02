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
import { Subject, combineLatest, EMPTY, Observable } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService, LicenseOptions } from '@gravitee/ui-particles-angular';

import { RoleService } from '../../../services-ngx/role.service';
import { Role, RoleScope } from '../../../entities/role/role';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Constants } from '../../../entities/Constants';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

interface RoleVM {
  name: string;
  description: string;
  isDefault: boolean;
  isSystem: boolean;
  isReadOnly: boolean;
  canBeDeleted: boolean;
  icon: string;
  hasUserRoleManagement: boolean;
}

@Component({
  selector: 'org-settings-roles',
  templateUrl: './org-settings-roles.component.html',
  styleUrls: ['./org-settings-roles.component.scss'],
})
export class OrgSettingsRolesComponent implements OnInit, OnDestroy {
  rolesByScope: Array<{ scope: string; scopeId: string; roles: RoleVM[] }>;
  loading = true;
  customRolesLicenseOptions: LicenseOptions = { feature: ApimFeature.APIM_CUSTOM_ROLES };
  hasCustomRolesLock$: Observable<boolean>;

  constructor(
    private readonly roleService: RoleService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    @Inject(Constants) private readonly constants: Constants,
    private readonly licenseService: GioLicenseService,
  ) {}

  private readonly unsubscribe$ = new Subject<boolean>();

  ngOnInit(): void {
    this.hasCustomRolesLock$ = this.licenseService.isMissingFeature$(this.customRolesLicenseOptions.feature);
    combineLatest([
      this.roleService.list('ORGANIZATION'),
      this.roleService.list('ENVIRONMENT'),
      this.roleService.list('API'),
      this.roleService.list('APPLICATION'),
      this.roleService.list('INTEGRATION'),
    ])
      .pipe(
        tap(([orgRoles, envRoles, apiRoles, appRoles, integrationRoles]) => {
          this.rolesByScope = [
            { scope: 'Organization', scopeId: 'ORGANIZATION', roles: this.convertToRoleVMs(orgRoles) },
            { scope: 'Environment', scopeId: 'ENVIRONMENT', roles: this.convertToRoleVMs(envRoles) },
            { scope: 'API', scopeId: 'API', roles: this.convertToRoleVMs(apiRoles) },
            { scope: 'Application', scopeId: 'APPLICATION', roles: this.convertToRoleVMs(appRoles) },
            { scope: 'Integration', scopeId: 'INTEGRATION', roles: this.convertToRoleVMs(integrationRoles) },
          ];
          this.loading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
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
        filter((confirm) => confirm === true),
        switchMap(() => this.roleService.delete(scope, role.name)),
        tap(() => this.snackBarService.success(`Role ${role.name} successfully deleted!`)),
        catchError(() => {
          this.snackBarService.error(`Failed to delete Role ${role.name}`);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  private convertToRoleVMs(roles: Role[]): RoleVM[] {
    return roles.map((role) => ({
      name: role.name,
      description: role.description,
      isDefault: role.default,
      isSystem: role.system,
      hasUserRoleManagement: role.scope === 'ORGANIZATION',
      canBeDeleted: !role.default && !role.system,
      isReadOnly: this.isReadOnly(role),
      icon: OrgSettingsRolesComponent.getScopeIcon(role.scope),
    }));
  }

  private isReadOnly(role: Role) {
    if (this.hasSystemRoleEditionEnabled()) {
      return role.scope === 'ORGANIZATION' && role.name === 'ADMIN';
    }
    return role.system;
  }

  private hasSystemRoleEditionEnabled(): boolean {
    return this.constants?.org?.settings?.management?.systemRoleEdition?.enabled;
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
      case 'INTEGRATION':
        return 'list';
      default:
        return '';
    }
  }
}
