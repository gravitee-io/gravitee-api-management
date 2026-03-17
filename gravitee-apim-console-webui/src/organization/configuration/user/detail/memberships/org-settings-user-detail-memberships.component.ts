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
import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, of } from 'rxjs';
import { catchError, filter, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import {
  OrgSettingsUserDetailAddGroupDialogComponent,
  OrgSettingsUserDetailAddGroupDialogData,
  OrgSettingsUserDetailAddGroupDialogReturn,
} from './org-settings-user-detail-add-group-dialog.component';
import { leastOneGroupRoleIsRequiredValidator } from './group-role-validators';

import { Group } from '../../../../../entities/group/group';
import { GroupService } from '../../../../../services-ngx/group.service';
import { RoleService } from '../../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersV2Service } from '../../../../../services-ngx/users-v2.service';
import { Constants } from '../../../../../entities/Constants';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

export interface EnvironmentTab {
  id: string;
  name?: string;
}

export interface MembershipGroupDS {
  id: string;
  name: string;
  environmentId?: string;
  isApiPrimaryOwner?: boolean;
}

export interface MembershipApiDS {
  id: string;
  name: string;
  version: string;
  visibility: string;
  environmentId: string;
}

export interface MembershipApplicationDS {
  id: string;
  name: string;
  environmentId: string;
}

@Component({
  selector: 'org-settings-user-detail-memberships',
  templateUrl: './org-settings-user-detail-memberships.component.html',
  styleUrls: ['./org-settings-user-detail-memberships.component.scss'],
  standalone: false,
})
export class OrgSettingsUserDetailMembershipsComponent {
  readonly environments = input<EnvironmentTab[]>([]);
  readonly userId = input<string>();
  readonly userStatus = input<string>();
  readonly userDisplayName = input<string>();
  readonly isReadOnly = input(false);

  readonly groupRolesChanged = output<UntypedFormGroup>();
  readonly requestReload = output<void>();

  private readonly usersV2Service = inject(UsersV2Service);
  private readonly groupService = inject(GroupService);
  private readonly roleService = inject(RoleService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly matDialog = inject(MatDialog);
  private readonly constants = inject(Constants);
  private readonly destroyRef = inject(DestroyRef);

  readonly apiRoles = toSignal(this.roleService.list('API').pipe(shareReplay(1)), { initialValue: [] });
  readonly applicationRoles = toSignal(this.roleService.list('APPLICATION').pipe(shareReplay(1)), { initialValue: [] });
  readonly integrationRoles = toSignal(this.roleService.list('INTEGRATION').pipe(shareReplay(1)), { initialValue: [] });

  groupsRolesFormGroup: UntypedFormGroup;
  groupsTableDisplayedColumns = ['name', 'groupAdmin', 'apiRoles', 'applicationRole', 'integrationRole', 'delete'];
  apisTableDisplayedColumns = ['name', 'version', 'visibility'];
  applicationsTableDisplayedColumns = ['name'];

  selectedEnvironmentId: string;

  // Filtered (paginated/searched) data for display
  membershipApis = signal<MembershipApiDS[]>([]);
  membershipApplications = signal<MembershipApplicationDS[]>([]);
  membershipGroups = signal<MembershipGroupDS[]>([]);

  membershipApisLoaded = signal(false);
  membershipApplicationsLoaded = signal(false);
  membershipGroupsLoaded = signal(false);

  // Unpaginated lengths for gio-table-wrapper
  apisUnpaginatedLength = signal(0);
  applicationsUnpaginatedLength = signal(0);
  groupsUnpaginatedLength = signal(0);

  // Store initial (unfiltered) data for client-side filtering
  private initialApis: MembershipApiDS[] = [];
  private initialApplications: MembershipApplicationDS[] = [];
  private initialGroups: MembershipGroupDS[] = [];

  // Store initial group roles to compare changes
  initialGroupRoles: Record<string, { GROUP?: string; API?: string; APPLICATION?: string; INTEGRATION?: string }> = {};

  constructor() {
    effect(() => {
      const envs = this.environments();
      if (envs.length > 0 && !this.selectedEnvironmentId) {
        this.onTabChange(0);
      }
    });
  }

  reload() {
    if (this.selectedEnvironmentId) {
      this.loadMembershipsForEnvironment(this.selectedEnvironmentId);
    }
  }

  onTabChange(index: number) {
    const envs = this.environments();
    if (!envs || index >= envs.length) {
      return;
    }
    this.selectedEnvironmentId = envs[index].id;
    this.loadMembershipsForEnvironment(this.selectedEnvironmentId);
  }

  onGroupsFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.initialGroups, filters, {
      searchTermIgnoreKeys: ['id'],
    });
    this.membershipGroups.set(filtered.filteredCollection as MembershipGroupDS[]);
    this.groupsUnpaginatedLength.set(filtered.unpaginatedLength);
  }

  onApisFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.initialApis, filters, {
      searchTermIgnoreKeys: ['id', 'visibility'],
    });
    this.membershipApis.set(filtered.filteredCollection as MembershipApiDS[]);
    this.apisUnpaginatedLength.set(filtered.unpaginatedLength);
  }

  onApplicationsFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.initialApplications, filters, {
      searchTermIgnoreKeys: ['id'],
    });
    this.membershipApplications.set(filtered.filteredCollection as MembershipApplicationDS[]);
    this.applicationsUnpaginatedLength.set(filtered.unpaginatedLength);
  }

  onDeleteGroupClick(group: MembershipGroupDS) {
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
        switchMap(() => this.groupService.deleteMember(group.id, this.userId(), group.environmentId)),
        tap(() => this.snackBarService.success(`"${this.userDisplayName()}" has been deleted from the group "${group.name}"`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.requestReload.emit());
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
          environmentId: this.selectedEnvironmentId,
          groupIdAlreadyAdded: this.initialGroups.map((g) => g.id),
        },
        role: 'alertdialog',
        id: 'addGroupConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((groupeAdded) => !isEmpty(groupeAdded)),
        switchMap((groupeAdded) =>
          this.groupService.addOrUpdateMemberships(
            groupeAdded.groupId,
            [
              {
                id: this.userId(),
                roles: [
                  { scope: 'GROUP' as const, name: groupeAdded.isAdmin ? 'ADMIN' : '' },
                  { scope: 'API' as const, name: groupeAdded.apiRole },
                  { scope: 'APPLICATION' as const, name: groupeAdded.applicationRole },
                  { scope: 'INTEGRATION' as const, name: groupeAdded.integrationRole },
                ],
              },
            ],
            groupeAdded.environmentId ?? this.constants.org.currentEnv.name,
          ),
        ),
        tap(() => {
          this.snackBarService.success('Roles successfully updated');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.requestReload.emit());
  }

  isApiRolePrimaryOwner(groupId: string): boolean {
    const groupControl = this.groupsRolesFormGroup?.get(groupId);
    if (!groupControl) return false;

    const apiControl = groupControl.get('API');
    if (!apiControl) return false;

    const group = this.initialGroups.find((g) => g.id === groupId);
    return apiControl.value === 'PRIMARY_OWNER' && !!group?.isApiPrimaryOwner;
  }

  private loadMembershipsForEnvironment(environmentId: string) {
    // Load Groups via v2 endpoint
    this.membershipGroupsLoaded.set(false);
    this.usersV2Service
      .getUserGroups(this.userId(), environmentId, 1, 9999)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of({ data: [], pagination: { page: 1, perPage: 9999, pageCount: 0, pageItemsCount: 0, totalCount: 0 } })),
      )
      .subscribe((response) => {
        const groupsWithPrimaryOwner = response.data;
        const groups: Group[] = groupsWithPrimaryOwner.map((g) => ({
          id: g.id,
          name: g.name,
          environmentId: g.environmentId,
          roles: g.roles,
        }));
        this.initialGroups = groupsWithPrimaryOwner.map((g) => ({
          id: g.id,
          name: g.name,
          environmentId: g.environmentId,
          isApiPrimaryOwner: g.isApiPrimaryOwner,
        }));
        this.membershipGroups.set(this.initialGroups);
        this.groupsUnpaginatedLength.set(this.initialGroups.length);
        this.initGroupsRolesForm(groups);
        this.membershipGroupsLoaded.set(true);
      });

    // Load APIs via v2 endpoint
    this.membershipApisLoaded.set(false);
    this.usersV2Service
      .getUserApis(this.userId(), environmentId, 1, 9999)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of({ data: [], pagination: { page: 1, perPage: 9999, pageCount: 0, pageItemsCount: 0, totalCount: 0 } })),
      )
      .subscribe((response) => {
        this.initialApis = response.data.map((api) => ({
          id: api.id,
          name: api.name,
          version: api.version,
          visibility: api.visibility,
          environmentId: api.environmentId,
        }));
        this.membershipApis.set(this.initialApis);
        this.apisUnpaginatedLength.set(this.initialApis.length);
        this.membershipApisLoaded.set(true);
      });

    // Load Applications via v2 endpoint
    this.membershipApplicationsLoaded.set(false);
    this.usersV2Service
      .getUserApplications(this.userId(), environmentId, 1, 9999)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of({ data: [], pagination: { page: 1, perPage: 9999, pageCount: 0, pageItemsCount: 0, totalCount: 0 } })),
      )
      .subscribe((response) => {
        this.initialApplications = response.data.map((app) => ({
          id: app.id,
          name: app.name,
          environmentId: app.environmentId,
        }));
        this.membershipApplications.set(this.initialApplications);
        this.applicationsUnpaginatedLength.set(this.initialApplications.length);
        this.membershipApplicationsLoaded.set(true);
      });
  }

  private initGroupsRolesForm(groups: Group[]) {
    this.initialGroupRoles = {};
    this.groupsRolesFormGroup = new UntypedFormGroup({
      ...groups.reduce((result, group) => {
        this.initialGroupRoles[group.id] = {
          GROUP: group.roles['GROUP'],
          API: group.roles['API'],
          APPLICATION: group.roles['APPLICATION'],
          INTEGRATION: group.roles['INTEGRATION'],
        };

        return {
          ...result,
          [group.id]: this.createGroupRolesFormGroup(group),
        };
      }, {}),
    });

    this.groupsRolesFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.groupsRolesFormGroup.dirty) {
        this.groupRolesChanged.emit(this.groupsRolesFormGroup);
      }
    });
    this.groupRolesChanged.emit(this.groupsRolesFormGroup);
  }

  private createGroupRolesFormGroup(group: Group): UntypedFormGroup {
    const groupDS = this.initialGroups.find((g) => g.id === group.id);
    const isPrimaryOwnerAndAssociatedWithApi = group.roles['API'] === 'PRIMARY_OWNER' && !!groupDS?.isApiPrimaryOwner;
    const apiControl = new UntypedFormControl({
      value: group.roles['API'],
      disabled: this.userStatus() !== 'ACTIVE' || this.isReadOnly() || isPrimaryOwnerAndAssociatedWithApi,
    });

    return new UntypedFormGroup(
      {
        GROUP: new UntypedFormControl({
          value: group.roles['GROUP'] === 'ADMIN',
          disabled: this.userStatus() !== 'ACTIVE' || this.isReadOnly(),
        }),
        API: apiControl,
        APPLICATION: new UntypedFormControl({
          value: group.roles['APPLICATION'],
          disabled: this.userStatus() !== 'ACTIVE' || this.isReadOnly(),
        }),
        INTEGRATION: new UntypedFormControl({
          value: group.roles['INTEGRATION'],
          disabled: this.userStatus() !== 'ACTIVE' || this.isReadOnly(),
        }),
      },
      [leastOneGroupRoleIsRequiredValidator],
    );
  }
}
