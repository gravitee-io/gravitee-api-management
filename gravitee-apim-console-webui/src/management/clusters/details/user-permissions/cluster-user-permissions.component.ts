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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { isEmpty, isEqual, uniqueId } from 'lodash';
import { catchError, combineLatest, EMPTY, filter, forkJoin, Observable, tap } from 'rxjs';
import { Role } from 'src/entities/role/role';
import { SearchableUser } from 'src/entities/user/searchableUser';
import { MemberDataSource } from 'src/permissions/model/member-data-source';
import { ClusterMemberService } from 'src/services-ngx/cluster-member.service';
import { RoleService } from 'src/services-ngx/role.service';
import { SnackBarService } from 'src/services-ngx/snack-bar.service';
import { UsersService } from 'src/services-ngx/users.service';
import { GioPermissionService } from 'src/shared/components/gio-permission/gio-permission.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from 'src/shared/components/gio-users-selector/gio-users-selector.component';
import { Member } from '../../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Group } from '../../../../entities/group/group';
import { GroupData } from '../../../../permissions/model/group-data';
import { ClustersService } from '../../../../services-ngx/clusters.service';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';
import { ClusterTransferOwnershipComponent } from './transfer-ownership/cluster-transfer-ownership.component';
import { TransferOwnershipDialogData } from '../../../../permissions/model/transfer-ownership-dialog-data';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';
import { switchMap } from 'rxjs/operators';
import { ClusterManageGroupsComponent } from './manage-groups/cluster-manage-groups.component';

@Component({
  selector: 'cluster-user-permissions',
  templateUrl: './cluster-user-permissions.component.html',
  styleUrls: ['./cluster-user-permissions.component.scss'],
  standalone: false,
})
export class ClusterUserPermissionsComponent implements OnInit {
  clusterId: string;
  dataSource: MemberDataSource[];
  membersTableLength = 0;
  displayedColumns = ['picture', 'displayName', 'role'];
  roleNames: string[];
  form: FormGroup;
  membersToAdd: (Member & { _viewId: string; reference: string })[] = [];
  defaultRole?: Role;
  roles: Role[];
  groups: Group[];
  groupData: GroupData[];

  private members: Member[];

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly clusterMemberService: ClusterMemberService,
    private readonly userService: UsersService,
    private readonly roleService: RoleService,
    private readonly clusterService: ClustersService,
    private readonly groupService: GroupV2Service,
    private readonly snackBarService: SnackBarService,
    public readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.clusterId = this.activatedRoute.snapshot.params.clusterId;
    if (this.permissionService.hasPermissionsByScope('CLUSTER', ['cluster-member-d']) && !this.displayedColumns.includes('delete')) {
      this.displayedColumns.push('delete');
    }
    this.getMembersWithPagination(this.filters.pagination.index, this.filters.pagination.size);
  }

  private getMembersWithPagination(page = 1, perPage = 10): void {
    forkJoin([
      this.clusterMemberService.getMembers(this.clusterId, page, perPage),
      this.roleService.list('CLUSTER'),
      this.clusterService.get(this.clusterId),
      this.groupService.list(1, 9999),
    ])
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        tap(([members, roles, cluster, groups]) => {
          this.members = members.data;
          this.membersTableLength = members.pagination.totalCount;
          this.roleNames = roles.map((r) => r.name) ?? [];
          this.defaultRole = roles.find((role) => role.default);
          this.groups = groups.data;
          this.groupData = cluster.groups?.map((id) => ({
              id,
              name: groups.data.find((g) => g.id === id)?.name,
              isVisible: true,
            }));
          this.initDataSource();
          this.initForm();
        }),
      )
      .subscribe();
  }

  private initDataSource() {
    this.dataSource = this.members?.map((member) => {
      return {
        id: member.id,
        role: member.roles[0].name,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
      };
    });
  }

  get isReadOnly(): boolean {
    return !this.permissionService.hasPermissionsByScope('CLUSTER', ['cluster-member-u']);
  }

  private initForm() {
    this.form = new FormGroup({});
    this.members?.forEach((member) => {
      this.form.addControl(
        member.id,
        new FormControl({
          value: member.roles[0].name,
          disabled: this.isReadOnly || member.roles[0].name === 'PRIMARY_OWNER',
        }),
      );
    });
  }

  public get canAddMembers() {
    return this.permissionService.hasPermissionsByScope('CLUSTER', ['cluster-member-c']);
  }

  public get canTransferOwnership() {
    return this.permissionService.hasPermissionsByScope('CLUSTER', ['cluster-member-u']);
  }

  public transferOwnership() {
    this.matDialog
      .open<ClusterTransferOwnershipComponent, TransferOwnershipDialogData>(ClusterTransferOwnershipComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        role: 'alertdialog',
        id: 'transferOwnershipDialog',
        data: {
          groups: this.groups,
          roles: this.roles,
          members: this.members,
        },
      })
      .afterClosed()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap(() => {
          return this.clusterMemberService.transferOwnership(
            this.clusterId,
          );
        }),
      )
      .subscribe(() => this.ngOnInit());
  }

  public get canManageGroups() {
    return this.permissionService.hasPermissionsByScope('CLUSTER', ['cluster-member-u']);
  }

  public updateGroups() {
    this.matDialog
      .open<ClusterManageGroupsComponent>(ClusterManageGroupsComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        role: 'alertdialog',
        id: 'addGroupsDialog',
      })
      .afterClosed()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  public addMembers() {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        width: '500px',
        data: {
          userFilterPredicate: (user) => !this.members.some((member) => member.id === user.id),
        },
        role: 'alertdialog',
        id: 'addUserDialog',
      })
      .afterClosed()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((selectedUsers) => !isEmpty(selectedUsers)),
        tap((selectedUsers) => {
          selectedUsers.forEach((user) => {
            this.addMemberToForm(user);
          });
        }),
      )
      .subscribe();
  }

  addMemberToForm(user: SearchableUser) {
    const member = {
      ...user,
      _viewId: `to-add-${uniqueId()}`,
      id: user.id,
    };
    this.membersToAdd.push({
      _viewId: member._viewId,
      id: member.id,
      reference: member.reference,
      displayName: member.displayName,
      roles: [this.defaultRole],
    });

    this.dataSource = [
      ...this.dataSource,
      {
        id: member._viewId,
        displayName: member.displayName,
        picture: this.userService.getUserAvatar(member.id),
        role: this.defaultRole.name,
      },
    ];
    const roleFormControl = new FormControl(this.defaultRole?.name, [Validators.required]);
    roleFormControl.markAsDirty();
    roleFormControl.markAsTouched();
    this.form.addControl(member._viewId, roleFormControl);
    this.form.markAsDirty();
  }

  public removeMember(member: Member) {
    this.clusterMemberService.deleteMember(this.clusterId, member.id).subscribe({
      next: () => {
        // remove from members
        this.members = this.members.filter((m) => m.id !== member.id);
        this.initDataSource();
        // remove from form
        this.form.get(member.id).reset();
        this.form.removeControl(member.id);

        this.snackBarService.success(`Member ${member.displayName} has been removed.`);
      },
      error: (error) => {
        this.snackBarService.error(error.message);
      },
    });
  }

  public onFiltersChanged(filters: GioTableWrapperFilters): void {
    if (!isEqual(this.filters, filters)) {
      this.filters = filters;
      this.getMembersWithPagination(filters.pagination.index, filters.pagination.size);
    }
  }

  onReset() {
    this.ngOnInit();
  }

  onSubmit() {
    const queries = [];
    if (this.form.dirty) {
      queries.push(
        ...Object.entries((this.form as FormGroup).controls)
          .filter(([_, formControl]) => formControl.dirty)
          .map(([memberFormId, roleFormControl]) => {
            return this.getSaveMemberQuery$(memberFormId, roleFormControl.value);
          }),
      );
    }

    combineLatest(queries)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        tap(() => {
          this.snackBarService.success('Changes successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
      )
      .subscribe();
  }

  private getSaveMemberQuery$(memberFormId: string, newRole: string): Observable<Member> {
    const memberToUpdate = this.members.find((m) => m.id === memberFormId);
    if (memberToUpdate) {
      return this.clusterMemberService.updateMember(this.clusterId, {
        memberId: memberToUpdate.id,
        roleName: newRole,
      });
    } else {
      const memberToAdd = this.membersToAdd.find((m) => m._viewId === memberFormId);
      return this.clusterMemberService.addMember(this.clusterId, {
        userId: memberToAdd.id,
        roleName: newRole,
        externalReference: memberToAdd.reference,
      });
    }
  }
}
