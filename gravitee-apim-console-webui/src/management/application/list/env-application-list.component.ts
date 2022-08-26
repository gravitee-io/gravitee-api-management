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
import { catchError, debounceTime, distinctUntilChanged, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { StateService } from '@uirouter/core';
import { MatDialog } from '@angular/material/dialog';

import { PagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { Application } from '../../../entities/application/application';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
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
};

@Component({
  selector: 'env-application-list',
  template: require('./env-application-list.component.html'),
  styles: [require('./env-application-list.component.scss')],
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
    pagination: { index: 1, size: 10 },
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
    @Inject(UIRouterStateParams) private $stateParams,
    @Inject(UIRouterState) private $state: StateService,
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
    // Init filters stream with state params
    const initialSearchValue = this.$stateParams.q ?? this.defaultFilters.searchTerm;
    const initialPageNumber = this.$stateParams.page ? Number(this.$stateParams.page) : this.defaultFilters.pagination.index;
    const initialPageSize = this.$stateParams.size ? Number(this.$stateParams.size) : this.defaultFilters.pagination.size;
    this.currentStatus = this.getCurrentStatus();
    const initialSort = toSort(this.$stateParams.order, this.defaultFilters.sort);
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
        takeUntil(this.unsubscribe$),
        debounceTime(100),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        tap(({ pagination, searchTerm, status, sort }) => {
          // Change url params
          this.$state.go('.', this.toQueryParams({ pagination, searchTerm, status, sort }), { notify: false });
        }),
        switchMap(({ pagination, searchTerm, status, sort }) => {
          return this.applicationService.list(status, searchTerm, toOrder(sort), pagination.index, pagination.size).pipe(
            // Return empty page result in case of error and does not interrupt the research observable
            catchError(() => of(new PagedResult<Application>())),
          );
        }),
      )
      .subscribe((applications) => this.setDataSourceFromApplicationsList(applications));
  }

  private toQueryParams(filters: ApplicationTableFilters) {
    const { searchTerm, pagination, status, sort } = filters;
    return { q: searchTerm, page: pagination.index, size: pagination.size, status, order: toOrder(sort) };
  }

  onAddApplicationClick() {
    this.$state.go('management.applications.create');
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filtersStream.next(this.filters);
  }

  private setDataSourceFromApplicationsList(applications: PagedResult<Application>) {
    this.filteredTableData = applications.data.map((a) => ({
      applicationId: a.id,
      name: a.name,
      applicationPicture: a.picture_url,
      type: a.type,
      owner: a.owner,
      updated_at: a.updated_at,
      status: a.status,
    }));
    this.nbTotalApplications = applications.page.total_elements;
    this.displayedColumns = this.displayedColumnsByStatus[this.currentStatus];
  }

  onStatusChange() {
    const sort: Sort = this.currentStatus === 'ACTIVE' ? { active: 'name', direction: 'asc' } : { active: 'updated_at', direction: 'desc' };
    const filters = { ...this.defaultFilters, status: this.currentStatus, sort };
    this.onFiltersChanged(filters);
  }

  onEditActionClicked(application: TableData) {
    this.$state.go('management.applications.application.general', { applicationId: application.applicationId });
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
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.applicationService.restore(application.applicationId)),
        tap(() => this.snackBarService.success(`Application ${application.name} has been restored`)),
      )
      .subscribe(() =>
        this.$state.go(
          'management.applications.application.subscriptions.list',
          { applicationId: application.applicationId },
          { reload: true },
        ),
      );
  }

  private canListArchiveApplication() {
    return this.roleService.hasRole({ scope: 'ORGANIZATION', name: 'ADMIN' });
  }

  private getCurrentStatus() {
    if (this.canListArchiveApplication()) {
      if (this.$stateParams.status && this.statusFilters.includes(this.$stateParams.status.toUpperCase())) {
        return this.$stateParams.status.toUpperCase();
      }
    }
    return this.defaultFilters.status;
  }
}
