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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { RoleService } from '../../../services-ngx/role.service';
import { MembershipListItem } from '../../../entities/role/membershipListItem';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../shared/components/gio-users-selector/gio-users-selector.component';
import { SearchableUser } from '../../../entities/user/searchableUser';

@Component({
  selector: 'org-settings-role-members',
  template: require('./org-settings-role-members.component.html'),
  styles: [require('./org-settings-role-members.component.scss')],
})
export class OrgSettingsRoleMembersComponent implements OnInit, OnDestroy {
  membershipsTableDisplayedColumns = ['displayName', 'actions'];

  roleScope: string;
  role: string;
  memberships: MembershipListItem[];
  filteredMemberships: MembershipListItem[];

  private readonly unsubscribe$ = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: { roleScope: string; role: string },
    private readonly roleService: RoleService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.roleScope = this.ajsStateParams.roleScope;
    this.role = this.ajsStateParams.role;

    this.roleService
      .listMemberships(this.roleScope, this.role)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((memberships) => {
          this.memberships = memberships;
          this.filteredMemberships = memberships;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onMembershipFiltersChanged(filters: GioTableWrapperFilters) {
    this.filteredMemberships = gioTableFilterCollection(this.memberships, filters);
  }

  onAddMemberClicked() {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        width: '500px',
        data: {
          userFilterPredicate: (user) => !this.memberships.some((membership) => membership.id === user.id),
        },
        role: 'alertdialog',
        id: 'createMembershipConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((users) => !!users),
        switchMap((selectedUsers) =>
          combineLatest(
            selectedUsers.map((user) => {
              const membership = {
                id: user.id,
                reference: user.reference,
              };
              return this.roleService.createMembership(this.roleScope, this.role, membership);
            }),
          ),
        ),
        tap(() => {
          this.snackBarService.success('Membership successfully created');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => this.ngOnInit());
  }

  onDeleteMemberClicked(membership: MembershipListItem) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete a membership',
          content: `Are you sure you want to delete the role <strong>${this.roleScope} - ${this.role}</strong> to user <strong>${membership.displayName}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteMembershipConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.roleService.deleteMembership(this.roleScope, this.role, membership.id)),
        tap(() => this.snackBarService.success(`Membership has been successfully deleted`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => this.ngOnInit());
  }
}
