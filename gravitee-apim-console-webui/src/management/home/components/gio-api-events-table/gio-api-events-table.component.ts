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
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { isEqual, toNumber } from 'lodash';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { EventService } from '../../../../services-ngx/event.service';
import { TimeRangeParams } from '../gio-quick-time-range/gio-quick-time-range.component';

type TableDataSource = {
  apiId: string;
  apiName: string;
  apiVersion: string;
  deploymentNumber: number;
  updatedAt: Date;
  deploymentLabel: string;
  type: string;
};
@Component({
  selector: 'gio-api-events-table',
  templateUrl: './gio-api-events-table.component.html',
  styleUrls: ['./gio-api-events-table.component.scss'],
})
export class GioApiEventsTableComponent implements OnChanges {
  private filters$ = new BehaviorSubject<(GioTableWrapperFilters & TimeRangeParams) | null>(null);

  @Input()
  timeRangeParams: TimeRangeParams;

  isLoading = true;
  totalLength = 0;
  currentFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: { index: 1, size: 5 },
  };
  displayedColumns = ['name', 'deployment', 'date', 'type'];
  tableDataSource$: Observable<TableDataSource[]> = this.filters$.pipe(
    distinctUntilChanged(isEqual),
    tap(() => (this.isLoading = true)),
    switchMap((filters) => {
      if (filters === null) {
        return of([]);
      }
      const currentFilters = filters;
      return this.eventService
        .search(
          'START_API,STOP_API,PUBLISH_API,UNPUBLISH_API',
          '',
          filters.searchTerm,
          filters.from,
          filters.to,
          // Pagination index is 0-based
          filters.pagination.index - 1,
          filters.pagination.size,
        )
        .pipe(
          map((eventPage) => {
            this.totalLength = eventPage.totalElements;
            const displayableEventType = {
              PUBLISH_API: 'Deploy',
              UNPUBLISH_API: 'Undeploy',
              START_API: 'Start',
              STOP_API: 'Stop',
            };

            return eventPage.content?.map<TableDataSource>((event) => ({
              apiId: event.properties['api_id'],
              apiName: event.properties['api_name'],
              apiVersion: event.properties['api_version'],
              deploymentNumber: toNumber(event.properties['deployment_number']),
              deploymentLabel: event.properties['deployment_label'],
              updatedAt: event.updated_at,
              type: displayableEventType[event.type],
            }));
          }),
          tap(() => {
            this.isLoading = false;
            this.currentFilters = currentFilters;
          }),
          // When filters change, clear page data to display loading spinner
          startWith([]),
        );
    }),
  );

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.timeRangeParams) {
      this.filters$.next({ ...this.currentFilters, ...this.timeRangeParams });
    }
  }
  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters$.next({ ...this.filters$.value, ...filters });
  }

  navigateToApiAnalyticsOverview(params: { apiId: string; from: number; to: number }): void {
    this.router.navigate(['../../', 'apis', params.apiId, 'v2', 'analytics-overview'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        from: params.from,
        to: params.to,
      },
    });
  }
}
