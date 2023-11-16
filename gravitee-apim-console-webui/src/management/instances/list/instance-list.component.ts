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
import { InstanceService } from '../../../services-ngx/instance.service';
import { distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { BehaviorSubject, Subject } from 'rxjs';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { isEqual } from 'lodash';

type TableData = {
  id: string;
  hostname: string;
  state: string;
};

@Component({
  selector: 'instance-list',
  template: require('./instance-list.component.html'),
  styles: [require('./instance-list.component.scss')],
})
export class InstanceListComponent implements OnInit, OnDestroy {
  displayedColumns = ['hostname', 'state'];
  filteredTableData: TableData[] = [];
  nbTotalInstances = 0;

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly unsubscribe$ = new Subject<void>();

  constructor(private readonly instanceService: InstanceService) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) =>
          this.instanceService.search(true, 0, 0, filters.pagination.index - 1, filters.pagination.size),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((searchResult) => {
        this.nbTotalInstances = searchResult.totalElements;
        this.filteredTableData = searchResult.content.map((instance) => ({
          id: instance.event,
          hostname: instance.hostname,
          state: instance.state,
        }));
      });
  }
  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }
}
