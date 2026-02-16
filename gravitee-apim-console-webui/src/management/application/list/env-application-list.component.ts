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
import { catchError, debounceTime, distinctUntilChanged, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import { PagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { Application } from '../../../entities/application/Application';
import { GioRoleService } from '../../../shared/components/gio-role/gio-role.service';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

interface ApplicationTableFilters extends GioTableWrapperFilters {
  status?: 'ACTIVE' | 'ARCHIVED';
}

type TableData = {
  applicationId: string;
  applicationPicture: string;
  name: string;
  type: string;
  owner: unknown;
  updated_at: number;
  status: string;
  origin: string;
};

@Component({
  selector: 'env-application-list',
  templateUrl: './env-application-list.component.html',
  styleUrls: ['./env-application-list.component.scss'],
  standalone: false,
})
export class EnvApplicationListComponent implements OnInit, OnDestroy {
  nbTotalApplications = 0;
  filters: ApplicationTableFilters;
  filteredTableData: TableData[];
  private displayedColumnsByStatus: Record<string, string[]> = {
    ARCHIVED: ['applicationPicture', 'name', 'updated_at', 'actions'],
    ACTIVE: ['applicationPicture', 'name', 'type', 'owner', 'actions'],
  };
  displayedColumns: string[] = [];

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private defaultFilters: ApplicationTableFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
    status: 'ACTIVE',
    sort: {
      direction: '',
    },
  };
  statusFilters: string[] = ['ACTIVE', 'ARCHIVED'];
  currentStatus: 'ACTIVE' | 'ARCHIVED';

  // Create filters stream
  private filtersStream = new BehaviorSubject<ApplicationTableFilters>(this.defaultFilters);

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly roleService: GioRoleService,
    private readonly applicationService: ApplicationService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    const { q, page, size, order } = this.activatedRoute.snapshot.queryParams;
    // Init filters stream with state params
    const initialSearchValue = q ?? this.defaultFilters.searchTerm;
    const initialPageNumber = page ? Number(page) : this.defaultFilters.pagination.index;
    const initialPageSize = size ? Number(size) : this.defaultFilters.pagination.size;
    this.currentStatus = this.getCurrentStatus();
    const initialSort = toSort(order, this.defaultFilters.sort);
    this.filters = {
      searchTerm: initialSearchValue,
      status: this.currentStatus,
      sort: initialSort,
      pagination: {
        ...this.filtersStream.value.pagination,
        index: initialPageNumber,
        size: initialPageSize,
      },
    };
    this.filtersStream.next(this.filters);

    // Create filters stream
    this.filtersStream
      .pipe(
        debounceTime(100),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        tap(({ pagination, searchTerm, status, sort }) => {
          // Change url params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: this.toQueryParams({ pagination, searchTerm, status, sort }),
            queryParamsHandling: 'merge',
          });
        }),
        switchMap(({ pagination, searchTerm, status, sort }) => {
          return this.applicationService.list(status, searchTerm, toOrder(sort), pagination.index, pagination.size).pipe(
            // Return empty page result in case of error and does not interrupt the research observable
            catchError(() => of(new PagedResult<Application>())),
          );
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(applications => this.setDataSourceFromApplicationsList(applications));
  }

  private toQueryParams(filters: ApplicationTableFilters) {
    const { searchTerm, pagination, status, sort } = filters;
    return { q: searchTerm, page: pagination.index, size: pagination.size, status, order: toOrder(sort) };
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filtersStream.next(this.filters);
  }

  private setDataSourceFromApplicationsList(applications: PagedResult<Application>) {
    this.filteredTableData = applications.data.map(a => ({
      applicationId: a.id,
      name: a.name,
      applicationPicture: a.picture_url,
      type: a.type,
      owner: a.owner,
      updated_at: a.updated_at,
      status: a.status,
      origin: a.origin,
    }));
    this.nbTotalApplications = applications.page.total_elements;
    this.displayedColumns = this.displayedColumnsByStatus[this.currentStatus];
  }

  onStatusChange() {
    const sort: Sort = this.currentStatus === 'ACTIVE' ? { active: 'name', direction: 'asc' } : { active: 'updated_at', direction: 'desc' };
    const filters = { ...this.defaultFilters, status: this.currentStatus, sort };
    this.onFiltersChanged(filters);
  }

  onRestoreActionClicked(application: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '620px',
        data: {
          title: `Would you like to restore the application "${application.name}"?`,
          content: `Every subscription belonging to this application will be restored in PENDING status.
                        Subscriptions can be reactivated as per requirements.
                        `,
          confirmButton: 'Restore',
        },
        role: 'alertdialog',
        id: 'restoreApplicationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.applicationService.restore(application.applicationId)),
        tap(() => this.snackBarService.success(`Application ${application.name} has been restored`)),
        switchMap(() =>
          this.applicationService
            .list(
              this.filters.status,
              this.filters.searchTerm,
              toOrder(this.filters.sort),
              this.filters.pagination.index,
              this.filters.pagination.size,
            )
            .pipe(catchError(() => of(new PagedResult<Application>()))),
        ),
        tap(applications => this.setDataSourceFromApplicationsList(applications)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() =>
        // TODO: redirects to former 'management.applications.application.subscriptions.list'
        this.router.navigate(['applications', application.applicationId, 'subscriptions/list'], {
          relativeTo: this.activatedRoute,
        }),
      );
  }

  private canListArchiveApplication() {
    return this.roleService.hasRole({ scope: 'ORGANIZATION', name: 'ADMIN' });
  }

  private getCurrentStatus() {
    if (this.canListArchiveApplication()) {
      const queryParamStatus = this.activatedRoute.snapshot.queryParams.status;

      if (queryParamStatus && this.statusFilters.includes(queryParamStatus.toUpperCase())) {
        return queryParamStatus.toUpperCase();
      }
    }
    return this.defaultFilters.status;
  }
}
