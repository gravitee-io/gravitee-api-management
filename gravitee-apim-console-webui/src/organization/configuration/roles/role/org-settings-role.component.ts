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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { StateService } from '@uirouter/angularjs';
import { combineLatest, EMPTY, of, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Role } from '../../../../entities/role/role';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

type RolePermissionsTableDM = {
  permissionName: string;
};
@Component({
  selector: 'org-settings-role',
  template: require('./org-settings-role.component.html'),
  styles: [require('./org-settings-role.component.scss')],
})
export class OrgSettingsRoleComponent implements OnInit, OnDestroy {
  isLoading = true;
  isEditMode = false;

  roleScope: string;
  roleName: string;
  role: Role;
  roleForm: FormGroup;
  initialRoleFormValue: unknown;

  rolePermissionsTableDisplayedColumns = ['permissionName', 'create', 'read', 'update', 'delete'];
  rolePermissionsTableDS: RolePermissionsTableDM[];

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: { roleScope: string; role: string },
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.roleScope = this.ajsStateParams.roleScope;
    this.isEditMode = !!this.ajsStateParams.role;
    this.roleName = this.ajsStateParams.role;

    combineLatest([
      this.isEditMode
        ? this.roleService.get(this.roleScope, this.roleName)
        : of({
            scope: this.roleScope,
            system: false,
          } as Role),
      this.roleService.getPermissionsByScope(this.roleScope),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([role, permissions]) => {
          this.role = role;
          this.roleForm = new FormGroup({
            name: new FormControl({ value: role.name ?? '', disabled: this.isEditMode }, [Validators.required]),
            description: new FormControl({ value: role.description ?? '', disabled: role.system }),
            default: new FormControl({ value: role.default ?? false, disabled: role.system }),
          });
          this.initialRoleFormValue = this.roleForm.getRawValue();

          this.rolePermissionsTableDS = permissions.map((permission) => ({
            permissionName: permission,
          }));
        }),
      )
      .subscribe(() => (this.isLoading = false));
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const roleFormValue = this.roleForm.getRawValue();

    const roleToSend = {
      ...this.role,
      ...roleFormValue,
      name: roleFormValue.name.toUpperCase(),
    };

    const createOrUpdateRole$ = this.isEditMode ? this.roleService.update(roleToSend) : this.roleService.create(roleToSend);

    createOrUpdateRole$
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => {
          this.snackBarService.success('Role successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => {
        if (!this.isEditMode) {
          this.ajsState.go('organization.settings.ng-roleedit', { roleScope: this.roleScope, role: roleFormValue.name.toUpperCase() });
        } else {
          this.ngOnInit();
        }
      });
  }
}
