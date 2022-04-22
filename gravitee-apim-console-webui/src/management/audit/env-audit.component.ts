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
import { FormGroup, FormControl } from '@angular/forms';
import { isEqual, mapValues } from 'lodash';
import { BehaviorSubject, EMPTY, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, switchMap, takeUntil, throttleTime } from 'rxjs/operators';

import { ApiService } from '../../services-ngx/api.service';
import { ApplicationService } from '../../services-ngx/application.service';
import { AuditService } from '../../services-ngx/audit.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

interface AuditDataTable {
  id: string;
  date: number;
  user: string;
  referenceType: string;
  reference: string;
  event: string;
  targets: Record<string, string>;
  patch: unknown;
  displayPatch: boolean;
}

@Component({
  selector: 'env-audit',
  template: require('./env-audit.component.html'),
  styles: [require('./env-audit.component.scss')],
})
export class EnvAuditComponent implements OnInit, OnDestroy {
  public displayedColumns = ['date', 'user', 'referenceType', 'reference', 'event', 'targets', 'patch'];
  public filteredTableData: AuditDataTable[] = [];
  public nbTotalAudit = 0;

  public filtersForm = new FormGroup({
    event: new FormControl(),
    referenceType: new FormControl(),
    applicationId: new FormControl(),
    apiId: new FormControl(),
    range: new FormGroup({
      start: new FormControl(),
      end: new FormControl(),
    }),
  });

  public eventsName$ = this.auditService.getAllEventsName();

  public apis$ = this.apiService.getAll();

  public applications$ = this.applicationService.getAll();

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create filters stream
  private filtersStream = new BehaviorSubject<{
    tableWrapper: GioTableWrapperFilters;
    auditFilters: {
      event?: string;
      referenceType?: string;
      applicationId?: string;
      apiId?: string;
      from?: number;
      to?: number;
    };
  }>({
    tableWrapper: {
      pagination: { index: 1, size: 10 },
      searchTerm: '',
    },
    auditFilters: {},
  });

  constructor(
    private auditService: AuditService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.filtersForm.valueChanges.subscribe(({ event, referenceType, applicationId, apiId, range }) => {
      this.filtersStream.next({
        tableWrapper: {
          ...this.filtersStream.value.tableWrapper,
          // go to first page when filters change
          pagination: { index: 1, size: this.filtersStream.value.tableWrapper.pagination.size },
        },
        auditFilters: {
          ...this.filtersStream.value.auditFilters,
          event,
          referenceType,
          applicationId,
          apiId,
          from: range?.start?.getTime() ?? undefined,
          to: range?.end?.getTime() ?? undefined,
        },
      });
    });

    this.filtersStream
      .pipe(
        takeUntil(this.unsubscribe$),
        throttleTime(100),
        distinctUntilChanged(isEqual),
        switchMap(({ auditFilters, tableWrapper }) =>
          this.auditService.list(auditFilters, tableWrapper.pagination.index, tableWrapper.pagination.size),
        ),
        catchError(() => {
          this.snackBarService.error('Unable to try the request, please try again');
          return EMPTY;
        }),
      )
      .subscribe((auditsPage) => {
        this.nbTotalAudit = auditsPage.totalElements;
        this.filteredTableData = (auditsPage.content ?? []).map((audit) => ({
          id: audit.id,
          date: audit.createdAt,
          user: (auditsPage.metadata[`USER:${audit.user}:name`] as string) ?? audit.user,
          referenceType: audit.referenceType,
          reference: (auditsPage.metadata[`${audit.referenceType}:${audit.referenceId}:name`] as string) ?? audit.referenceId,
          event: audit.event,
          targets: mapValues(audit.properties, (v, k) => auditsPage.metadata[k + ':' + v + ':name'] as string),
          patch: JSON.parse(audit.patch),
          displayPatch: false,
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next({ ...this.filtersStream.value, tableWrapper: filters });
  }
}
