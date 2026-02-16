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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import { UsersService } from '../../../services-ngx/users.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PagedResult } from '../../../entities/pagedResult';
import { User } from '../../../entities/user/user';
import { UserHelper } from '../../../entities/user/userHelper';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

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
  styleUrls: ['./org-settings-users.component.scss'],
  templateUrl: './org-settings-users.component.html',
  standalone: false,
})
export class OrgSettingsUsersComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['userPicture', 'displayName', 'status', 'email', 'source', 'actions'];

  filters: GioTableWrapperFilters;
  nbTotalUsers = 0;
  filteredTableData: TableData[] = [];

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create filters stream
  private filtersStream = new BehaviorSubject<GioTableWrapperFilters>({ pagination: { index: 1, size: 10 }, searchTerm: '' });

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly usersService: UsersService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    // Init filters stream with state params
    const initialSearchValue = this.activatedRoute.snapshot.queryParams.q ?? '';
    const initialPageNumber = this.activatedRoute.snapshot.queryParams.page ? Number(this.activatedRoute.snapshot.queryParams.page) : 1;
    this.filters = {
      searchTerm: initialSearchValue,
      pagination: {
        ...this.filtersStream.value.pagination,
        index: initialPageNumber,
      },
    };
    this.filtersStream.next(this.filters);

    // Create filters stream
    this.filtersStream
      .pipe(
        debounceTime(100),
        distinctUntilChanged(),
        tap(({ pagination, searchTerm }) => {
          // Change url params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: { q: searchTerm, page: pagination.index },
            queryParamsHandling: 'merge',
          });
        }),
        switchMap(({ pagination, searchTerm }) =>
          this.usersService.list(searchTerm, pagination.index, pagination.size).pipe(
            // Return empty page result in case of error and does not interrupt the research observable
            catchError(() => of(new PagedResult<User>())),
          ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(users => this.setDataSourceFromUsersList(users));
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onDeleteUserClick({ userId, displayName }: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '450px',
        data: {
          title: 'Delete a user',
          content: `Are you sure you want to delete the user <strong>${displayName}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteUserConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.usersService.remove(userId)),
        tap(() => this.snackBarService.success(`User ${displayName} is being deleted!`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.filtersStream.next({ ...this.filtersStream.value }));
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next(filters);
  }

  private setDataSourceFromUsersList(users: PagedResult<User>) {
    this.filteredTableData = users.data.map(u => ({
      userId: u.id,
      displayName: u.displayName,
      email: u.email,
      source: u.source,
      status: u.status === 'ARCHIVED' ? 'Deletion In Progress' : u.status,
      userPicture: this.usersService.getUserAvatar(u.id),
      primary_owner: u.primary_owner,
      number_of_active_tokens: u.number_of_active_tokens,
      badgeCSSClass: UserHelper.getStatusBadgeCSSClass(u),
    }));
    this.nbTotalUsers = users.page.total_elements;
  }
}
