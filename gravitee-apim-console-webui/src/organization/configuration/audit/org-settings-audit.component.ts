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
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { isEqual, mapValues } from 'lodash';
import { BehaviorSubject, EMPTY, forkJoin, Observable, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, shareReplay, switchMap, takeUntil, tap, throttleTime } from 'rxjs/operators';

import { Api } from '../../../entities/api';
import { ApiService } from '../../../services-ngx/api.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { AuditService } from '../../../services-ngx/audit.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { endOfDay } from '../../../util/date.util';

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
  selector: 'org-settings-audit',
  templateUrl: './org-settings-audit.component.html',
  styleUrls: ['./org-settings-audit.component.scss'],
  standalone: false,
})
export class OrgSettingsAuditComponent implements OnInit, OnDestroy {
  public displayedColumns = ['date', 'user', 'referenceType', 'reference', 'event', 'targets', 'patch'];
  public filteredTableData: AuditDataTable[] = [];
  public tableIsLoading = true;
  public nbTotalAudit = 0;

  public filtersForm = new UntypedFormGroup({
    event: new UntypedFormControl(),
    referenceType: new UntypedFormControl(),
    environmentId: new UntypedFormControl(),
    applicationId: new UntypedFormControl(),
    apiId: new UntypedFormControl(),
    range: new UntypedFormGroup({
      start: new UntypedFormControl(),
      end: new UntypedFormControl(),
    }),
  });

  public eventsName$ = this.auditService.getAllEventsNameByOrganization();
  public environments$ = this.environmentService.list().pipe(shareReplay(1));

  // Fetch all environment and for each one fetch all apis
  public environmentsApis$: Observable<Record<string, Api[]>> = this.environments$.pipe(
    switchMap((envs) =>
      forkJoin(
        envs.reduce(
          (res, env) => ({
            ...res,
            [env.name]: this.apiService.getAll({
              environmentId: env.id,
            }),
          }),
          {} as Record<string, Observable<Api[]>>,
        ),
      ),
    ),
  );

  // Fetch all environment and for each one fetch all applications
  public environmentsApplications$: Observable<Record<string, Api[]>> = this.environments$.pipe(
    switchMap((envs) =>
      forkJoin(
        envs.reduce(
          (res, env) => ({
            ...res,
            [env.name]: this.applicationService.getAll({
              environmentId: env.id,
            }),
          }),
          {} as Record<string, Observable<Api[]>>,
        ),
      ),
    ),
  );

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create filters stream
  public filtersStream = new BehaviorSubject<{
    tableWrapper: GioTableWrapperFilters;
    auditFilters: {
      event?: string;
      referenceType?: string;
      environmentId?: string;
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
    auditFilters: {
      event: null,
      referenceType: null,
      environmentId: null,
      applicationId: null,
      apiId: null,
      from: undefined,
      to: undefined,
    },
  });

  constructor(
    private auditService: AuditService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private environmentService: EnvironmentService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.filtersForm.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$))
      .subscribe(({ event, referenceType, environmentId, applicationId, apiId, range }) => {
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
            environmentId,
            applicationId,
            apiId,
            from: range?.start?.valueOf() ?? undefined,
            to: endOfDay(range?.end) ?? undefined,
          },
        });
      });

    this.filtersStream
      .pipe(
        throttleTime(100),
        distinctUntilChanged(isEqual),
        tap(() => {
          this.filteredTableData = [];
          this.tableIsLoading = true;
        }),
        switchMap(({ auditFilters, tableWrapper }) =>
          this.auditService.listByOrganization(auditFilters, tableWrapper.pagination.index, tableWrapper.pagination.size).pipe(
            catchError(() => {
              this.snackBarService.error('Unable to run the request, please try again');
              return EMPTY;
            }),
          ),
        ),
        takeUntil(this.unsubscribe$),
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
        this.tableIsLoading = false;
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
