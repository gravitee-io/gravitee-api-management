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
import { MatCheckboxChange } from '@angular/material/checkbox';
import { StateService } from '@uirouter/angularjs';
import { combineLatest, EMPTY, of, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Role } from '../../../../entities/role/role';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

type RolePermissionsTableDM = {
  permissionName: string;
  isMovedToOrganizationScope: boolean;
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
  selectAllCheckbox: Record<
    'C' | 'R' | 'U' | 'D',
    {
      state: 'checked' | 'unchecked' | 'indeterminate';
      label: string;
    }
  > = {
    C: { state: 'unchecked', label: 'Select all' },
    R: { state: 'unchecked', label: 'Select all' },
    U: { state: 'unchecked', label: 'Select all' },
    D: { state: 'unchecked', label: 'Select all' },
  };

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
            permissions: {},
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
            /**
             * Convert permissions rights into object to edit each CRUD item
             * Permissions from server: { permissions: { EX_P1: ['C'], EX_P2: ['C', 'R'] } }
             * Permissions in form: { permissions: { EX_P1: { C: true } }, EX_P2: { C: true, R: true } } }
             */
            permissions: new FormGroup(
              permissions.reduce((prev, permission) => {
                const disabled = isPermissionMovedToOrganizationScope(role, permission) || this.isReadOnly;

                // Create a new FormControl for each permission right and add observable to update the select all checkbox state
                const createFormControl = new FormControl({ value: role.permissions[permission]?.includes('C'), disabled });
                createFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('C'));

                const readFormControl = new FormControl({ value: role.permissions[permission]?.includes('R'), disabled });
                readFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('R'));

                const updateFormControl = new FormControl({ value: role.permissions[permission]?.includes('U'), disabled });
                updateFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('U'));

                const deleteFormControl = new FormControl({ value: role.permissions[permission]?.includes('D'), disabled });
                deleteFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('D'));

                return {
                  ...prev,
                  [permission]: new FormGroup({
                    C: createFormControl,
                    R: readFormControl,
                    U: updateFormControl,
                    D: deleteFormControl,
                  }),
                };
              }, {}),
            ),
          });
          this.initialRoleFormValue = this.roleForm.getRawValue();

          this.rolePermissionsTableDS = permissions.map((permission) => ({
            permissionName: permission,
            isMovedToOrganizationScope: isPermissionMovedToOrganizationScope(role, permission),
          }));

          if (this.isEditMode) {
            // 📝 Initialize select all checkbox state for edit mode
            this.updateSelectAllCheckboxState('C');
            this.updateSelectAllCheckboxState('R');
            this.updateSelectAllCheckboxState('U');
            this.updateSelectAllCheckboxState('D');
          }
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
      permissions: fromFormPermissionsToPermissions(roleFormValue.permissions),
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
      .subscribe((resultingRole) => {
        if (!this.isEditMode) {
          this.ajsState.go('organization.settings.ng-roleedit', { roleScope: this.roleScope, role: resultingRole.name });
        } else {
          this.ngOnInit();
        }
      });
  }

  toggleAll(rightKey: 'C' | 'R' | 'U' | 'D', change: MatCheckboxChange) {
    const permissionsFormGroup = this.roleForm.get('permissions') as FormGroup;
    Object.values(permissionsFormGroup.controls).forEach((permission) => {
      const rightFormControl = permission.get(rightKey);

      if (!rightFormControl.disabled) {
        rightFormControl.setValue(change.checked);
      }
    });
    permissionsFormGroup.markAsDirty();
  }

  private updateSelectAllCheckboxState(rightKey: 'C' | 'R' | 'U' | 'D') {
    const permissionsFormGroup = this.roleForm.get('permissions') as FormGroup;
    const nbChecked = Object.values(permissionsFormGroup.controls).filter((permission) => permission.get(rightKey).value).length;

    const nbPermissions = Object.keys((this.roleForm.get('permissions') as FormGroup).controls).length;

    this.selectAllCheckbox[rightKey] = {
      state: nbChecked === nbPermissions ? 'checked' : nbChecked === 0 ? 'unchecked' : 'indeterminate',
      label: `${nbChecked === nbPermissions ? 'Deselect' : 'Select'} all`,
    };
  }

  public get isReadOnly(): boolean {
    return this.role?.scope === 'ORGANIZATION' && this.role?.name === 'ADMIN';
  }
}

const isPermissionMovedToOrganizationScope = (role: Role, permission: string): boolean => {
  return role?.scope === 'ENVIRONMENT' && (permission === 'TAG' || permission === 'TENANT' || permission === 'ENTRYPOINT');
};

const fromFormPermissionsToPermissions = (formPermissions: Record<string, Record<'C' | 'R' | 'U' | 'D', boolean>>): Role['permissions'] => {
  return Object.entries(formPermissions).reduce((prev, [permission, right]) => {
    return {
      ...prev,
      [permission]: Object.entries(right)
        .filter(([, value]) => value === true)
        .map(([key]) => key),
    };
  }, {});
};
