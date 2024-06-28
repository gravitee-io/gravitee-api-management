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
import { BehaviorSubject, EMPTY, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil, tap, throttleTime } from 'rxjs/operators';
import { FormControl, FormGroup, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { isEqual, mapValues } from 'lodash';
import { Moment } from 'moment';

import { ApiAuditService } from '../../../services-ngx/api-audit.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface ApiAuditData {
  id: string;
  date: number;
  user: string;
  event: string;
  targets: Record<string, string>;
  patch: unknown;
  displayPatch: boolean;
}

@Component({
  selector: 'api-audit-list',
  templateUrl: './api-audit-list.component.html',
  styleUrls: ['./api-audit-list.component.scss'],
})
export class ApiAuditListComponent implements OnInit, OnDestroy {
  auditList: ApiAuditData[] = [];
  auditForm: UntypedFormGroup;
  nbTotalAudit = 0;

  public eventsName$ = this.apiAuditService.getEvents(this.activatedRoute.snapshot.params.apiId);
  public displayedColumns = ['date', 'user', 'event', 'targets', 'patch'];
  public filtersStream = new BehaviorSubject<{
    tableWrapper: GioTableWrapperFilters;
    auditFilters: {
      event?: string;
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
      from: undefined,
      to: undefined,
    },
  });
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private apiAuditService: ApiAuditService,
    public readonly activatedRoute: ActivatedRoute,
    private snackBarService: SnackBarService,
  ) {}

  public ngOnInit() {
    this.auditForm = new UntypedFormGroup({
      event: new UntypedFormControl(null),
      range: new FormGroup({
        start: new FormControl<Moment>(null),
        end: new FormControl<Moment>(null),
      }),
    });

    this.auditForm.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$))
      .subscribe(({ event, range }) => {
        this.filtersStream.next({
          tableWrapper: {
            ...this.filtersStream.value.tableWrapper,
            pagination: { index: 1, size: this.filtersStream.value.tableWrapper.pagination.size },
          },
          auditFilters: {
            ...this.filtersStream.value.auditFilters,
            event,
            from: range?.start?.valueOf() ?? undefined,
            to: range?.end?.valueOf() ?? undefined,
          },
        });
      });

    this.filtersStream
      .pipe(
        throttleTime(100),
        distinctUntilChanged(isEqual),
        switchMap(({ auditFilters, tableWrapper }) =>
          this.apiAuditService
            .getAudit(this.activatedRoute.snapshot.params.apiId, auditFilters, tableWrapper.pagination.index, tableWrapper.pagination.size)
            .pipe(
              catchError(() => {
                this.snackBarService.error('Unable to try the request, please try again');
                return EMPTY;
              }),
            ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((auditsList) => {
        this.nbTotalAudit = auditsList.totalElements;
        this.auditList = (auditsList.content ?? []).map((audit) => ({
          id: audit.id,
          date: audit.createdAt,
          user: (auditsList.metadata[`USER:${audit.user}:name`] as string) ?? audit.user,
          event: audit.event,
          targets: mapValues(audit.properties, (v, k) => auditsList.metadata[k + ':' + v + ':name'] as string),
          patch: JSON.parse(audit.patch),
          displayPatch: false,
        }));
      });
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next({ ...this.filtersStream.value, tableWrapper: filters });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
