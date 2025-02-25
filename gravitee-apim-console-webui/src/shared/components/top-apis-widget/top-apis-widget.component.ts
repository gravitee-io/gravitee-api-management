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

import { Component, Input, OnChanges } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { GioTableWrapperModule } from '../gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../gio-table-wrapper/gio-table-wrapper.util';
import { TimeRangeParams } from '../../utils/timeFrameRanges';
import { AnalyticsDefinitionVersion, AnalyticsTopApis } from '../../../entities/analytics/analytics';

@Component({
  selector: 'app-top-apis-widget',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatCardModule,
    MatSortModule,
    GioTableWrapperModule,
    GioLoaderModule,
    MatIcon,
    MatTooltip,
  ],
  templateUrl: './top-apis-widget.component.html',
  styleUrl: './top-apis-widget.component.scss',
})
export class TopApisWidgetComponent implements OnChanges {
  @Input() data: AnalyticsTopApis[];
  @Input() period: TimeRangeParams;

  displayedColumns = ['name', 'count'];
  filteredTableData: AnalyticsTopApis[];
  tableFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 5 },
    searchTerm: '',
  };
  totalLength: number;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnChanges() {
    this.runFilters(this.tableFilters);
  }

  navigateToApi(apiKey: string, definitionVersion: AnalyticsDefinitionVersion): void {
    const customTimeframeParams = this.period.id === 'custom' ? { from: this.period.from, to: this.period.to } : {};

    this.router.navigate(
      ['../../', 'apis', apiKey, definitionVersion.toLowerCase(), definitionVersion === 'V2' ? 'analytics-overview' : 'analytics'],
      {
        relativeTo: this.activatedRoute,
        queryParams: { period: this.period.id, ...customTimeframeParams },
      },
    );
  }

  runFilters(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.data, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
  }
}
