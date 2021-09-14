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
import { Component, Inject, OnDestroy } from '@angular/core';
import { StateService } from '@uirouter/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormControl } from '@angular/forms';
import { isEmpty, size } from 'lodash';

import { UsersService } from '../../../services-ngx/users.service';
import { UIRouterStateParams, UIRouterState } from '../../../ajs-upgraded-providers';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PagedResult } from '../../../entities/pagedResult';
import { User } from '../../../entities/user/user';

type TableData = {
  userId: string;
  userPicture: string;
  displayName: string;
  status: string;
  email: string;
  source: string;
  primary_owner: boolean;
  number_of_active_tokens: number;
};
@Component({
  selector: 'org-settings-users',
  styles: [require('./org-settings-users.component.scss')],
  template: require('./org-settings-users.component.html'),
})
export class OrgSettingsUsersComponent implements OnDestroy {
  page = 0;

  displayedColumns: string[] = ['userPicture', 'displayName', 'status', 'email', 'source', 'actions'];

  dataSource = new MatTableDataSource([]);

  searchFormControl = new FormControl();

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private $stateParams,
    @Inject(UIRouterState) private $state: StateService,
    private readonly usersService: UsersService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.page = this.$stateParams?.page ?? 0;

    // Init page with data and dynamic Search when entering a search term
    this.searchFormControl.valueChanges
      .pipe(
        takeUntil(this.unsubscribe$),
        debounceTime(400),
        startWith(''),
        filter<string | null>((searchTerm) => size(searchTerm) >= 2 || isEmpty(searchTerm)),
        distinctUntilChanged(),
        switchMap((searchTerm) =>
          this.usersService.list(searchTerm).pipe(
            // Return empty page result in case of error and does not interrupt the research observable
            catchError(() => of(new PagedResult<User>())),
          ),
        ),
      )
      .subscribe((users) => this.setDataSourceFromUsersList(users));
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  nextPage() {
    this.$state.go('organization.settings.ng-users', { page: this.page++ });
  }

  onDisplayNameClick(userId: string) {
    this.$state.go('organization.settings.user', { userId });
  }

  onDeleteUserClick({ userId, displayName }: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '450px',
        data: {
          title: `Are you sure you want to remove the user "${displayName}"?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'removeUserConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.usersService.remove(userId)),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
      )
      .subscribe(() => this.ngOnInit());
  }

  private setDataSourceFromUsersList(users: PagedResult<User>) {
    this.dataSource = new MatTableDataSource<TableData>(
      users.data.map((u) => ({
        userId: u.id,
        displayName: u.displayName,
        email: u.email,
        source: u.source,
        status: u.status,
        userPicture: u.picture,
        primary_owner: u.primary_owner,
        number_of_active_tokens: u.number_of_active_tokens,
      })),
    );
  }
}
