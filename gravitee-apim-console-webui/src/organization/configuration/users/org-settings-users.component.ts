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
import { StateService } from '@uirouter/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, combineLatest, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormControl } from '@angular/forms';
import { isEmpty, size } from 'lodash';
import { PageEvent } from '@angular/material/paginator';

import { UsersService } from '../../../services-ngx/users.service';
import { UIRouterStateParams, UIRouterState } from '../../../ajs-upgraded-providers';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PagedResult } from '../../../entities/pagedResult';
import { User } from '../../../entities/user/user';
import { UserHelper } from '../../../entities/user/userHelper';

type TableData = {
  userId: string;
  userPicture: string;
  displayName: string;
  status: string;
  email: string;
  source: string;
  primary_owner: boolean;
  number_of_active_tokens: number;
  badgeCSSClass: string;
};

@Component({
  selector: 'org-settings-users',
  styles: [require('./org-settings-users.component.scss')],
  template: require('./org-settings-users.component.html'),
})
export class OrgSettingsUsersComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['userPicture', 'displayName', 'status', 'email', 'source', 'actions'];

  dataSource = new MatTableDataSource([]);

  searchFormControl = new FormControl();

  resultsLength = 0;
  matPaginatorPageIndex = 0;
  pageSizeOptions = [25, 50, 100];

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create page stream
  private pageStream = new BehaviorSubject<{ pageNumber: number; pageSize: number }>({ pageNumber: 1, pageSize: this.pageSizeOptions[0] });

  constructor(
    @Inject(UIRouterStateParams) private $stateParams,
    @Inject(UIRouterState) private $state: StateService,
    private readonly usersService: UsersService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    // Init search value
    const initialSearchValue = this.$stateParams.q ?? '';
    this.searchFormControl.setValue(initialSearchValue, { emitEvent: false });

    if (this.$stateParams.page) {
      const pageNumber = Number(this.$stateParams.page);

      this.pageStream.next({ ...this.pageStream.value, pageNumber });
    }

    // Create search stream when entering a search term
    this.searchFormControl.valueChanges
      .pipe(
        takeUntil(this.unsubscribe$),
        // if the search is longer than 2 characters or is empty. And wait 200 ms before new search event
        filter<string | null>((searchTerm) => size(searchTerm) >= 2 || isEmpty(searchTerm)),
        debounceTime(300),
        distinctUntilChanged(),
        // If the user changes search, reset back to the first page.
        tap(
          () =>
            (this.pageStream = new BehaviorSubject({
              ...this.pageStream.value,
              pageNumber: 1,
            })),
        ),
        // Init first search with initial search value
        startWith(initialSearchValue),
        switchMap((searchTerm) => combineLatest([of(searchTerm), this.pageStream])),
        tap(([searchTerm, { pageNumber }]) => {
          // Change mat paginator index with pageStream value
          this.matPaginatorPageIndex = pageNumber - 1;
          // Change url params
          this.$state.go('.', { q: searchTerm, page: pageNumber }, { notify: false });
        }),
        switchMap(([searchTerm, { pageNumber, pageSize }]) =>
          this.usersService.list(searchTerm, pageNumber, pageSize).pipe(
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

  onDisplayNameClick(userId: string) {
    this.$state.go('organization.settings.ng-user', { userId });
  }

  onDeleteUserClick({ userId, displayName }: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '450px',
        data: {
          title: 'Delete a user',
          content: `Are you sure you want to remove the user <strong>${displayName}</strong>?`,
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
        tap(() => this.snackBarService.success(`User ${displayName} successfully deleted!`)),
      )
      .subscribe(() => this.ngOnInit());
  }

  onPageChange(pageEvent: PageEvent) {
    const pageNumber = pageEvent.pageIndex + 1;
    if (this.pageStream.value.pageNumber !== pageNumber || this.pageStream.value.pageSize !== pageEvent.pageSize) {
      this.pageStream.next({ pageNumber, pageSize: pageEvent.pageSize });
    }
  }

  onAddUserClick() {
    this.$state.go('organization.settings.ng-newuser');
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
        badgeCSSClass: UserHelper.getStatusBadgeCSSClass(u),
      })),
    );
    this.resultsLength = users.page.total_elements;
  }
}
