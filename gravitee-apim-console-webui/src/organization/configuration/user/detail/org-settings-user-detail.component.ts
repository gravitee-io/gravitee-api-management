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
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { combineLatest, EMPTY, from, Observable, Subject, zip } from 'rxjs';
import { catchError, filter, mergeMap, shareReplay, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEmpty, toString } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import {
  OrgSettingsUserDetailAddGroupDialogComponent,
  OrgSettingsUserDetailAddGroupDialogData,
  OrgSettingsUserDetailAddGroupDialogReturn,
} from './org-settings-user-detail-add-group-dialog.component';
import {
  OrgSettingsUserGenerateTokenComponent,
  OrgSettingsUserGenerateTokenDialogData,
} from './tokens/org-settings-user-generate-token.component';

import { Environment } from '../../../../entities/environment/environment';
import { Group } from '../../../../entities/group/group';
import { User } from '../../../../entities/user/user';
import { EnvironmentService } from '../../../../services-ngx/environment.service';
import { GroupService } from '../../../../services-ngx/group.service';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../services-ngx/users.service';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { UserHelper } from '../../../../entities/user/userHelper';
import { Token } from '../../../../entities/user/userTokens';
import { UsersTokenService } from '../../../../services-ngx/users-token.service';
import { Constants } from '../../../../entities/Constants';

interface UserVM extends User {
  organizationRoles: string;
  avatarUrl: string;
  badgeCSSClass: string;
  customFields?: Record<string, string>;
}

interface EnvironmentDS {
  id: string;
  name?: string;
  description?: string;
  roles: string;
}

interface GroupDS {
  id: string;
  name: string;
}

interface ApiDS {
  id: string;
  version: string;
  visibility: string;
  environmentId: string;
}

interface ApplicationDS {
  id: string;
  name: string;
  environmentId: string;
}

interface TokenDS {
  id: string;
  name: string;
  createdAt: number;
  lastUseAt?: number;
}

@Component({
  selector: 'org-settings-user-detail',
  templateUrl: './org-settings-user-detail.component.html',
  styleUrls: ['./org-settings-user-detail.component.scss'],
})
export class OrgSettingsUserDetailComponent implements OnInit, OnDestroy {
  user: UserVM;

  organizationRoles$ = this.roleService.list('ORGANIZATION').pipe(shareReplay(1));
  environmentRoles$ = this.roleService.list('ENVIRONMENT').pipe(shareReplay(1));
  apiRoles$ = this.roleService.list('API').pipe(shareReplay(1));
  applicationRoles$ = this.roleService.list('APPLICATION').pipe(shareReplay(1));
  integrationRoles$ = this.roleService.list('INTEGRATION').pipe(shareReplay(1));

  organizationRolesControl: UntypedFormControl;
  environmentsRolesFormGroup: UntypedFormGroup;
  groupsRolesFormGroup: UntypedFormGroup;

  environmentsTableDS: EnvironmentDS[];
  environmentsTableDisplayedColumns = ['name', 'description', 'roles'];

  groupsTableDS: GroupDS[];
  groupsTableDisplayedColumns = ['name', 'groupAdmin', 'apiRoles', 'applicationRole', 'integrationRole', 'delete'];

  apisTableDS: ApiDS[];
  apisTableDisplayedColumns = ['name', 'version', 'visibility'];

  applicationsTableDS: ApplicationDS[];
  applicationsTableDisplayedColumns = ['name'];

  tokensTableDS: TokenDS[];
  tokensTableDisplayedColumns = ['name', 'createdAt', 'lastUseAt', 'action'];

  openSaveBar = false;
  invalidStateSaveBar = false;

  private initialTableDS: Record<
    'environmentsTableDS' | 'groupsTableDS' | 'apisTableDS' | 'applicationsTableDS' | 'tokensTableDS',
    unknown[]
  > = { apisTableDS: [], applicationsTableDS: [], environmentsTableDS: [], groupsTableDS: [], tokensTableDS: [] };

  public tablesUnpaginatedLength: Record<
    'environmentsTableDS' | 'groupsTableDS' | 'apisTableDS' | 'applicationsTableDS' | 'tokensTableDS',
    number
  > = { apisTableDS: 0, applicationsTableDS: 0, environmentsTableDS: 0, groupsTableDS: 0, tokensTableDS: 0 };

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly usersService: UsersService,
    private readonly usersTokenService: UsersTokenService,
    private readonly roleService: RoleService,
    private readonly groupService: GroupService,
    private readonly snackBarService: SnackBarService,
    private readonly environmentService: EnvironmentService,
    private readonly matDialog: MatDialog,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.usersService.get(this.activatedRoute.snapshot.params.userId),
      this.environmentService.list(),
      this.usersService.getUserGroups(this.activatedRoute.snapshot.params.userId),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([user, environments, groups]) => {
        const organizationRoles = user.roles.filter((r) => r.scope === 'ORGANIZATION');
        this.user = {
          ...user,
          organizationRoles: organizationRoles.map((r) => r.name ?? r.id).join(', '),
          avatarUrl: this.usersService.getUserAvatar(this.activatedRoute.snapshot.params.userId),
          badgeCSSClass: UserHelper.getStatusBadgeCSSClass(user),
          customFields: Object.entries(user.customFields ?? {}).reduce((result, [key, value]) => {
            return { ...result, [key]: toString(value) };
          }, {}),
        };

        this.initOrganizationRolesForm();

        this.environmentsTableDS = environments.map((e) => ({ id: e.id, name: e.name, description: e.description, roles: '' }));
        this.initialTableDS['environmentsTableDS'] = this.environmentsTableDS;
        this.tablesUnpaginatedLength['environmentsTableDS'] = this.environmentsTableDS.length;

        this.initEnvironmentsRolesForm(environments);

        this.groupsTableDS = groups.map((g) => ({
          id: g.id,
          name: g.name,
        }));
        this.initialTableDS['groupsTableDS'] = this.groupsTableDS;
        this.tablesUnpaginatedLength['groupsTableDS'] = this.groupsTableDS.length;
        this.initGroupsRolesForm(groups);
      });

    this.usersService
      .getMemberships(this.activatedRoute.snapshot.params.userId, 'api')
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(({ metadata }) => {
        this.apisTableDS = Object.entries(metadata).map(([apiId, apiMetadata]: [string, any]) => ({
          id: apiId,
          name: apiMetadata.name,
          version: apiMetadata.version,
          visibility: apiMetadata.visibility,
          environmentId: apiMetadata.environmentId,
        }));
        this.initialTableDS['apisTableDS'] = this.apisTableDS;
        this.tablesUnpaginatedLength['apisTableDS'] = this.apisTableDS.length;
      });

    this.usersService
      .getMemberships(this.activatedRoute.snapshot.params.userId, 'application')
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(({ metadata }) => {
        this.applicationsTableDS = Object.entries(metadata).map(([applicationId, applicationMetadata]: [string, any]) => ({
          id: applicationId,
          name: applicationMetadata.name,
          environmentId: applicationMetadata.environmentId,
        }));
        this.initialTableDS['applicationsTableDS'] = this.applicationsTableDS;
        this.tablesUnpaginatedLength['applicationsTableDS'] = this.applicationsTableDS.length;
      });

    this.usersTokenService
      .getTokens(this.activatedRoute.snapshot.params.userId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((response) => {
        this.tokensTableDS = response.map((token) => ({
          id: token.id,
          createdAt: token.created_at,
          lastUseAt: token.last_use_at,
          name: token.name,
        }));
        this.initialTableDS['tokensTableDS'] = this.tokensTableDS;
        this.tablesUnpaginatedLength['tokensTableDS'] = this.tokensTableDS.length;
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
    let observableToZip: Observable<void>[] = [];

    // Organization
    if (this.organizationRolesControl.dirty) {
      observableToZip.push(
        this.usersService
          .updateUserRoles(this.user.id, 'ORGANIZATION', this.constants.org.id, this.organizationRolesControl.value)
          .pipe(takeUntil(this.unsubscribe$)),
      );
    }

    // Environments
    if (this.environmentsRolesFormGroup.dirty) {
      observableToZip.push(
        from(Object.keys(this.environmentsRolesFormGroup.controls)).pipe(
          mergeMap((envId) => {
            const envRolesControl = this.environmentsRolesFormGroup.get(envId) as UntypedFormControl;
            if (envRolesControl.dirty) {
              return this.usersService.updateUserRoles(this.user.id, 'ENVIRONMENT', envId, envRolesControl.value);
            }
            // skip if no change on environment roles
            return EMPTY;
          }),
        ),
      );
    }

    // Groups
    if (this.groupsRolesFormGroup.dirty) {
      observableToZip.push(
        from(Object.keys(this.groupsRolesFormGroup.controls)).pipe(
          mergeMap((groupId) => {
            const groupRolesFormGroup = this.groupsRolesFormGroup.get(groupId) as UntypedFormGroup;
            if (groupRolesFormGroup.dirty) {
              const { GROUP, API, APPLICATION, INTEGRATION } = groupRolesFormGroup.value;

              return this.groupService.addOrUpdateMemberships(groupId, [
                {
                  id: this.user.id,
                  roles: [
                    { scope: 'GROUP' as const, name: GROUP ? 'ADMIN' : '' },
                    { scope: 'API' as const, name: API },
                    { scope: 'APPLICATION' as const, name: APPLICATION },
                    { scope: 'INTEGRATION' as const, name: INTEGRATION },
                  ],
                },
              ]);
            }
            // skip if no change on groups roles
            return EMPTY;
          }),
        ),
      );
    }

    // After all observables emit, emit all success message as an array
    zip(...observableToZip)
      .pipe(
        tap(() => {
          this.snackBarService.success('Roles successfully updated');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        observableToZip = [];
        this.toggleSaveBar(false);
      });
  }

  onResetPassword() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Reset user password',
          content: `
          Are you sure you want to reset password of user <strong>${this.user.displayName}</strong> ?
          <br>
          The user will receive an email with a link to set a new password.
          `,
          confirmButton: 'Reset',
        },
        role: 'alertdialog',
        id: 'resetUserPasswordConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.usersService.resetPassword(this.user.id)),
        tap(() => this.snackBarService.success(`The password of user "${this.user.displayName}" has been successfully reset`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onProcessRegistration(state: 'accept' | 'reject') {
    const wording = {
      accept: {
        content: `Are you sure you want to accept the registration request of <strong>${this.user.displayName}</strong> ?`,
        confirmButton: 'Accept',
        success: `User "${this.user.displayName}" has been accepted`,
      },
      reject: {
        content: `Are you sure you want to reject the registration request of <strong>${this.user.displayName}</strong> ?`,
        confirmButton: 'Reject',
        success: `User "${this.user.displayName}" has been rejected`,
      },
    };

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'User registration',
          content: wording[state].content,
          confirmButton: wording[state].confirmButton,
        },
        role: 'alertdialog',
        id: 'userRegistrationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.usersService.processRegistration(this.user.id, state === 'accept')),
        tap(() => this.snackBarService.success(wording[state].success)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onFiltersChanged(tableDSPropertyKey: string, filters: GioTableWrapperFilters) {
    let initialCollection = this.initialTableDS[tableDSPropertyKey];

    if (!initialCollection) {
      // If no initial collection save the first one
      this.initialTableDS[tableDSPropertyKey] = this[tableDSPropertyKey];
      initialCollection = this[tableDSPropertyKey];
    }
    const filtered = gioTableFilterCollection(initialCollection, filters, {
      searchTermIgnoreKeys: ['id', 'visibility'],
    });

    this[tableDSPropertyKey] = filtered.filteredCollection;
    this.tablesUnpaginatedLength[tableDSPropertyKey] = filtered.unpaginatedLength;
  }

  onSaveBarReset() {
    this.ngOnInit();

    this.toggleSaveBar(false);
  }

  onDeleteGroupClick(group: Group) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete user form the group',
          content: `Are you sure you want to delete the user from the group <strong>${group.name}</strong> ?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'removeGroupMemberConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.groupService.deleteMember(group.id, this.user.id)),
        tap(() => this.snackBarService.success(`"${this.user.displayName}" has been deleted from the group "${group.name}"`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onAddGroupClicked() {
    this.matDialog
      .open<
        OrgSettingsUserDetailAddGroupDialogComponent,
        OrgSettingsUserDetailAddGroupDialogData,
        OrgSettingsUserDetailAddGroupDialogReturn
      >(OrgSettingsUserDetailAddGroupDialogComponent, {
        width: '500px',
        data: {
          groupIdAlreadyAdded: this.groupsTableDS.map((g) => g.id),
        },
        role: 'alertdialog',
        id: 'addGroupConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((groupeAdded) => !isEmpty(groupeAdded)),
        switchMap((groupeAdded) =>
          this.groupService.addOrUpdateMemberships(groupeAdded.groupId, [
            {
              id: this.user.id,
              roles: [
                { scope: 'GROUP' as const, name: groupeAdded.isAdmin ? 'ADMIN' : '' },
                { scope: 'API' as const, name: groupeAdded.apiRole },
                { scope: 'APPLICATION' as const, name: groupeAdded.applicationRole },
                { scope: 'INTEGRATION' as const, name: groupeAdded.integrationRole },
              ],
            },
          ]),
        ),
        tap(() => {
          this.snackBarService.success('Roles successfully updated');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  private initOrganizationRolesForm() {
    const organizationRoles = this.user.roles.filter((r) => r.scope === 'ORGANIZATION');

    this.organizationRolesControl = new UntypedFormControl({
      value: organizationRoles.map((r) => r.id),
      disabled: this.user.status !== 'ACTIVE',
    });

    this.organizationRolesControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
  }

  private initEnvironmentsRolesForm(environments: Environment[]) {
    this.environmentsRolesFormGroup = new UntypedFormGroup(
      environments.reduce((result, environment) => {
        const userEnvRoles = this.user.envRoles[environment.id] ?? [];

        return {
          ...result,
          [environment.id]: new UntypedFormControl({ value: userEnvRoles.map((r) => r.id), disabled: this.user.status !== 'ACTIVE' }),
        };
      }, {}),
    );

    this.environmentsRolesFormGroup.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
  }

  private initGroupsRolesForm(groups: Group[]) {
    const leastOneGroupRoleIsRequiredValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
      const groupRolesFormGroup = control as UntypedFormGroup;

      const GROUP = groupRolesFormGroup.get('GROUP').value;
      const API = groupRolesFormGroup.get('API').value;
      const APPLICATION = groupRolesFormGroup.get('APPLICATION').value;
      const INTEGRATION = groupRolesFormGroup.get('INTEGRATION').value;

      if (GROUP || API || APPLICATION || INTEGRATION) {
        return null;
      }

      return {
        leastOneIsRequired: true,
      };
    };

    this.groupsRolesFormGroup = new UntypedFormGroup({
      ...groups.reduce((result, group) => {
        return {
          ...result,
          [group.id]: new UntypedFormGroup(
            {
              GROUP: new UntypedFormControl({ value: group.roles['GROUP'], disabled: this.user.status !== 'ACTIVE' }),
              API: new UntypedFormControl({ value: group.roles['API'], disabled: this.user.status !== 'ACTIVE' }),
              APPLICATION: new UntypedFormControl({ value: group.roles['APPLICATION'], disabled: this.user.status !== 'ACTIVE' }),
              INTEGRATION: new UntypedFormControl({ value: group.roles['INTEGRATION'], disabled: this.user.status !== 'ACTIVE' }),
            },
            [leastOneGroupRoleIsRequiredValidator],
          ),
        };
      }, {}),
    });

    this.groupsRolesFormGroup.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.toggleSaveBar(true);
    });
    this.groupsRolesFormGroup.statusChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((status) => {
      this.invalidStateSaveBar = status !== 'VALID';
    });
  }

  onDeleteTokenClicked(token: TokenDS) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '450px',
        data: {
          title: 'Revoke a token',
          content: `Are you sure you want to revoke the token <strong>${token.name}</strong>?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'revokeUsersTokenDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.usersTokenService.revokeToken(this.activatedRoute.snapshot.params.userId, token.id)),
        tap(() => this.snackBarService.success(`Token successfully deleted!`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onGenerateTokenClicked() {
    this.matDialog
      .open<OrgSettingsUserGenerateTokenComponent, OrgSettingsUserGenerateTokenDialogData, Token>(OrgSettingsUserGenerateTokenComponent, {
        width: '750px',
        data: {
          userId: this.activatedRoute.snapshot.params.userId,
        },
        role: 'dialog',
        id: 'generateTokenDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => this.ngOnInit());
  }

  isServiceUser(): boolean {
    return !this.user.firstname;
  }
}
