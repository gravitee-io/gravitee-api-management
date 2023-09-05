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
import { AfterViewInit, Component, Inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { sortBy, toNumber } from 'lodash';
import { StateService } from '@uirouter/angularjs';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

export type TopApisData = {
  values: { [key: string]: number };
  metadata: { [key: string]: { name: string; version?: string; order: string; unknown?: boolean } };
};

type TableDataSource = {
  id: string;
  name: string;
  value: number;
  order: number;
  unknown?: boolean;
};
@Component({
  selector: 'gio-top-apis-table',
  template: require('./gio-top-apis-table.component.html'),
  styles: [require('./gio-top-apis-table.component.scss')],
})
export class GioTopApisTableComponent implements AfterViewInit, OnChanges {
  @Input()
  data: TopApisData;

  displayedColumns = ['name', 'value'];
  dataSource: TableDataSource[];
  filteredTableData: TableDataSource[];
  tableFilters: GioTableWrapperFilters = { pagination: { index: 0, size: 5 }, searchTerm: '' };

  totalLength: number;

  constructor(@Inject(UIRouterState) private readonly ajsState: StateService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.data) this.buildDataSource();
  }
  ngAfterViewInit(): void {
    if (this.data) this.buildDataSource();
  }

  navigateToApi(apiKey: string): void {
    this.ajsState.go('management.apis.ng.analytics-overview-v2', {
      apiId: apiKey,
    });
  }

  private buildDataSource() {
    this.dataSource = sortBy(
      Object.entries(this.data.values).map(([key, value]) => {
        return {
          id: key,
          name: this.data.metadata[key].name,
          value,
          order: toNumber(this.data.metadata[key].order),
          unknown: this.data.metadata[key].unknown,
        };
      }),
      'order',
    );
    this.totalLength = this.dataSource.length;
    this.filteredTableData = this.dataSource.slice(0, 5);
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.dataSource, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
  }
}
