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
import { FormControl, FormGroup } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Role } from '../../../../entities/role/role';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'org-settings-role',
  template: require('./org-settings-role.component.html'),
  styles: [require('./org-settings-role.component.scss')],
})
export class OrgSettingsRoleComponent implements OnInit, OnDestroy {
  isLoading = true;

  roleScope: string;
  roleName: string;
  role: Role;
  roleForm: FormGroup;
  initialRoleFormValue: unknown;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: { roleScope: string; role: string },
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.roleScope = this.ajsStateParams.roleScope;
    this.roleName = this.ajsStateParams.role;

    this.roleService
      .get(this.roleScope, this.roleName)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((role) => {
          this.role = role;
          this.roleForm = new FormGroup({
            description: new FormControl({ value: role?.description, disabled: role.system }),
            default: new FormControl({ value: role?.default, disabled: role.system }),
          });
          this.initialRoleFormValue = this.roleForm.getRawValue();
        }),
      )
      .subscribe(() => (this.isLoading = false));
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    this.roleService
      .update({
        ...this.role,
        ...this.roleForm.getRawValue(),
      })
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
        this.ngOnInit();
      });
  }
}
