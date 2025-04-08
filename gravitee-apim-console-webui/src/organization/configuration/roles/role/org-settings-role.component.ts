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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { combineLatest, EMPTY, of, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { Role } from '../../../../entities/role/role';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { Constants } from '../../../../entities/Constants';

type RolePermissionsTableDM = {
  permissionName: string;
  isMovedToOrganizationScope: boolean;
};
@Component({
  selector: 'org-settings-role',
  templateUrl: './org-settings-role.component.html',
  styleUrls: ['./org-settings-role.component.scss'],
  standalone: false,
})
export class OrgSettingsRoleComponent implements OnInit, OnDestroy {
  isLoading = true;
  isEditMode = false;

  roleScope: string;
  roleName: string;
  role: Role;
  roleForm: UntypedFormGroup;
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
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    @Inject(Constants) private readonly constants: Constants,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.roleScope = this.activatedRoute.snapshot.params.roleScope;
    this.isEditMode = !!this.activatedRoute.snapshot.params.role;
    this.roleName = this.activatedRoute.snapshot.params.role;

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
        tap(([role, permissions]) => {
          this.role = role;
          this.roleForm = new UntypedFormGroup({
            name: new UntypedFormControl({ value: role.name ?? '', disabled: this.isEditMode }, [Validators.required]),
            description: new UntypedFormControl({ value: role.description ?? '', disabled: role.system }),
            default: new UntypedFormControl({ value: role.default ?? false, disabled: role.system }),
            /**
             * Convert permissions rights into object to edit each CRUD item
             * Permissions from server: { permissions: { EX_P1: ['C'], EX_P2: ['C', 'R'] } }
             * Permissions in form: { permissions: { EX_P1: { C: true } }, EX_P2: { C: true, R: true } } }
             */
            permissions: new UntypedFormGroup(
              permissions.reduce((prev, permission) => {
                const disabled = isPermissionMovedToOrganizationScope(role, permission) || this.isReadOnly;

                // Create a new FormControl for each permission right and add observable to update the select all checkbox state
                const createFormControl = new UntypedFormControl({ value: role.permissions[permission]?.includes('C'), disabled });
                createFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('C'));

                const readFormControl = new UntypedFormControl({ value: role.permissions[permission]?.includes('R'), disabled });
                readFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('R'));

                const updateFormControl = new UntypedFormControl({ value: role.permissions[permission]?.includes('U'), disabled });
                updateFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('U'));

                const deleteFormControl = new UntypedFormControl({ value: role.permissions[permission]?.includes('D'), disabled });
                deleteFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.updateSelectAllCheckboxState('D'));

                return {
                  ...prev,
                  [permission]: new UntypedFormGroup({
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
        takeUntil(this.unsubscribe$),
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
        tap(() => {
          this.snackBarService.success('Role successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((resultingRole) => {
        if (!this.isEditMode) {
          this.router.navigate(['.', resultingRole.name], { relativeTo: this.activatedRoute });
        } else {
          this.ngOnInit();
        }
      });
  }

  toggleAll(rightKey: 'C' | 'R' | 'U' | 'D', change: MatCheckboxChange) {
    const permissionsFormGroup = this.roleForm.get('permissions') as UntypedFormGroup;
    Object.values(permissionsFormGroup.controls).forEach((permission) => {
      const rightFormControl = permission.get(rightKey);

      if (!rightFormControl.disabled) {
        rightFormControl.setValue(change.checked);
      }
    });
    permissionsFormGroup.markAsDirty();
  }

  private updateSelectAllCheckboxState(rightKey: 'C' | 'R' | 'U' | 'D') {
    const permissionsFormGroup = this.roleForm.get('permissions') as UntypedFormGroup;
    const nbChecked = Object.values(permissionsFormGroup.controls).filter((permission) => permission.get(rightKey).value).length;

    const nbPermissions = Object.keys((this.roleForm.get('permissions') as UntypedFormGroup).controls).length;

    this.selectAllCheckbox[rightKey] = {
      state: nbChecked === nbPermissions ? 'checked' : nbChecked === 0 ? 'unchecked' : 'indeterminate',
      label: `${nbChecked === nbPermissions ? 'Deselect' : 'Select'} all`,
    };
  }

  public get isReadOnly(): boolean {
    if (this.hasSystemRoleEditionEnabled()) {
      return this.role.scope === 'ORGANIZATION' && this.role.name === 'ADMIN';
    }
    return this.role.system;
  }

  private hasSystemRoleEditionEnabled(): boolean {
    return this.constants?.org?.settings?.management?.systemRoleEdition?.enabled;
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
