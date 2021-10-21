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
import { combineLatest, EMPTY, from, Observable, Subject, zip } from 'rxjs';
import { catchError, map, mapTo, mergeMap, shareReplay, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Environment } from '../../../../entities/environment/environment';
import { User } from '../../../../entities/user/user';
import { EnvironmentService } from '../../../../services-ngx/environment.service';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../services-ngx/users.service';

interface UserVM extends User {
  organizationRoles: string;
  avatarUrl: string;
}

interface EnvironmentDS {
  id: string;
  name?: string;
  description?: string;
  roles: string;
}

@Component({
  selector: 'org-settings-user-detail',
  template: require('./org-settings-user-detail.component.html'),
  styles: [require('./org-settings-user-detail.component.scss')],
})
export class OrgSettingsUserDetailComponent implements OnInit, OnDestroy {
  user: UserVM;

  organizationRoles$ = this.roleService.list('ORGANIZATION').pipe(shareReplay());
  environmentRoles$ = this.roleService.list('ENVIRONMENT').pipe(shareReplay());

  organizationRolesControl: FormControl;
  environmentsRolesFormGroup: FormGroup;

  environmentsTableDS: EnvironmentDS[];
  environmentsTableDisplayedColumns = ['name', 'description', 'roles'];

  openSaveBar = false;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly usersService: UsersService,
    private readonly roleService: RoleService,
    private readonly snackBarService: SnackBarService,
    private readonly environmentService: EnvironmentService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  ngOnInit(): void {
    combineLatest([this.usersService.get(this.ajsStateParams.userId), this.environmentService.list()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([user, environments]) => {
        const organizationRoles = user.roles.filter((r) => r.scope === 'ORGANIZATION');
        this.user = {
          ...user,
          organizationRoles: organizationRoles.map((r) => r.name ?? r.id).join(', '),
          avatarUrl: this.usersService.getUserAvatar(this.ajsStateParams.userId),
        };

        this.initOrganizationRolesForm();

        this.environmentsTableDS = environments.map((e) => ({ id: e.id, name: e.name, description: e.description, roles: '' }));

        this.initEnvironmentsRolesForm(environments);
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
    let observableToZip: Observable<string>[] = [];

    // Organization
    if (this.organizationRolesControl.dirty) {
      observableToZip.push(
        this.usersService
          .updateUserRoles(this.user.id, 'ORGANIZATION', 'DEFAULT', this.organizationRolesControl.value)
          .pipe(takeUntil(this.unsubscribe$), mapTo('Roles for organization "DEFAULT" updated')),
      );
    }

    // Environments
    if (this.environmentsRolesFormGroup.dirty) {
      observableToZip.push(
        from(Object.keys(this.environmentsRolesFormGroup.controls)).pipe(
          mergeMap((envId) => {
            const envRolesControl = this.environmentsRolesFormGroup.get(envId) as FormControl;
            if (envRolesControl.dirty) {
              return this.usersService.updateUserRoles(this.user.id, 'ENVIRONMENT', envId, envRolesControl.value).pipe(mapTo(envId));
            }
            // skip if no change on environment roles
            return EMPTY;
          }),
          map((envId) => `Roles for environment "${envId}" updated`),
        ),
      );
    }

    // After all observables emit, emit all success message as an array
    zip(...observableToZip)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((successMessages) => {
          this.snackBarService.success(successMessages.join('\n'));
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => {
        observableToZip = [];
        this.toggleSaveBar(false);
      });
  }

  onSaveBarReset() {
    this.ngOnInit();

    this.toggleSaveBar(false);
  }

  private initOrganizationRolesForm() {
    const organizationRoles = this.user.roles.filter((r) => r.scope === 'ORGANIZATION');

    this.organizationRolesControl = new FormControl({ value: organizationRoles.map((r) => r.id), disabled: this.user.status !== 'ACTIVE' });

    this.organizationRolesControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
  }

  private initEnvironmentsRolesForm(environments: Environment[]) {
    this.environmentsRolesFormGroup = new FormGroup(
      environments.reduce((result, environment) => {
        const userEnvRoles = this.user.envRoles[environment.id] ?? [];

        return {
          ...result,
          [environment.id]: new FormControl({ value: userEnvRoles.map((r) => r.id), disabled: this.user.status !== 'ACTIVE' }),
        };
      }, {}),
    );

    this.environmentsRolesFormGroup.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
  }
}
