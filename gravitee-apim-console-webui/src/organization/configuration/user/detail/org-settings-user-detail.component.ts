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
import { FormControl } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, shareReplay, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user/user';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../services-ngx/users.service';

interface UserVM extends User {
  organizationRoles: string;
  avatarUrl: string;
}

@Component({
  selector: 'org-settings-user-detail',
  template: require('./org-settings-user-detail.component.html'),
  styles: [require('./org-settings-user-detail.component.scss')],
})
export class OrgSettingsUserDetailComponent implements OnInit, OnDestroy {
  user: UserVM;

  organizationRoles$ = this.roleService.list('ORGANIZATION').pipe(shareReplay());

  organizationRolesControl: FormControl;

  openSaveBar = false;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly usersService: UsersService,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  ngOnInit(): void {
    this.usersService
      .get(this.ajsStateParams.userId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((user) => {
        const organizationRoles = user.roles.filter((r) => r.scope === 'ORGANIZATION');
        this.user = {
          ...user,
          organizationRoles: organizationRoles.map((r) => r.name ?? r.id).join(', '),
          avatarUrl: this.usersService.getUserAvatar(this.ajsStateParams.userId),
        };

        this.initOrganizationRolesForm();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  toggleSaveBar(open?: boolean) {
    this.openSaveBar = open ?? !this.openSaveBar;
  }

  onSaveBarSubmit() {
    if (this.organizationRolesControl.dirty) {
      this.usersService
        .updateUserRoles(this.user.id, 'ORGANIZATION', 'DEFAULT', this.organizationRolesControl.value)
        .pipe(
          takeUntil(this.unsubscribe$),
          tap(() => {
            this.snackBarService.success('Roles for organization "DEFAULT" updated');
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
        )
        .subscribe();
    }
    this.toggleSaveBar(false);
  }

  onSaveBarReset() {
    if (this.organizationRolesControl.touched) {
      this.initOrganizationRolesForm();
    }
    this.toggleSaveBar(false);
  }

  private initOrganizationRolesForm() {
    const organizationRoles = this.user.roles.filter((r) => r.scope === 'ORGANIZATION');

    this.organizationRolesControl = new FormControl({ value: organizationRoles.map((r) => r.id), disabled: this.user.status !== 'ACTIVE' });

    this.organizationRolesControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
  }
}
