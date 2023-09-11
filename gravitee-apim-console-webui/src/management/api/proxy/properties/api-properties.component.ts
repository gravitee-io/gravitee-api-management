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
import { Subject } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';
import { takeUntil, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2, ApiV4 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type TableDataSource = {
  key: string;
  value: string;
  encrypted: boolean;
};

@Component({
  selector: 'api-properties',
  template: require('./api-properties.component.html'),
  styles: [require('./api-properties.component.scss')],
})
export class ApiPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public isReadOnly = false;
  public isLoading = true;
  public totalLength = 0;
  public tableFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  public displayedColumns = ['key', 'value', 'encrypted', 'actions'];
  public filteredTableData: TableDataSource[] = [];
  public api: ApiV2 | ApiV4;

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams, private readonly apiService: ApiV2Service) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api) => {
          if (api.definitionVersion === 'V1') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
          this.api = api;

          // Initialize the properties table data
          this.onFiltersChanged(this.tableFilters);

          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addProperties() {
    // TODO: implement
  }
  removeProperty(_: TableDataSource) {
    // TODO: implement
  }
  togglePropertyEncryption(_: TableDataSource) {
    // TODO: implement
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    if (isEmpty(this.api?.properties)) return;

    const filtered = gioTableFilterCollection(
      this.api.properties.map((p) => ({
        key: p.key,
        value: p.value,
        encrypted: p.encrypted,
      })),
      filters,
    );
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
  }
}
