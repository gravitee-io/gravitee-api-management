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

import { Component, DestroyRef, input, OnInit } from '@angular/core';
import { switchMap } from 'rxjs';
import { DecimalPipe } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioTableWrapperModule } from '../../../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApiAnalyticsV2Service } from '../../../../../../../../services-ngx/api-analytics-v2.service';
import { SnackBarService } from '../../../../../../../../services-ngx/snack-bar.service';
import { WidgetConfig } from '../../../../../../../../entities/management-api-v2/analytics/analytics';

const tableConfig = {
  'application-id': {
    nameLabel: 'App',
    countLabel: 'Requests Count',
  },
};

interface TableWidgetDataItem {
  name: string;
  count: number;
  id: string;
  isUnknown?: boolean;
}

@Component({
  selector: 'table-widget',
  standalone: true,
  imports: [DecimalPipe, GioLoaderModule, GioTableWrapperModule, MatSort, MatSortHeader, MatTableModule],
  templateUrl: './table-widget.component.html',
  styleUrl: './table-widget.component.scss',
})
export class TableWidgetComponent implements OnInit {
  public config = input<WidgetConfig>();
  public displayedColumns = ['name', 'count'];
  public nameLabel: string = '';
  public countLabel: string = '';

  private data: TableWidgetDataItem[] = [];

  public filteredTableData: TableWidgetDataItem[] = [];
  public tableFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 5 },
    searchTerm: '',
    sort: {
      direction: 'desc',
    },
  };
  totalLength: number;

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    const { nameLabel, countLabel } = tableConfig[this.config().groupByField];
    this.nameLabel = nameLabel || 'Name';
    this.countLabel = countLabel || 'Count';

    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap((timeRangeParams) => {
          return this.apiAnalyticsV2Service.getGroupBy(this.config().apiId, timeRangeParams, { field: this.config().groupByField });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.data = Object.entries(res.metadata).reduce((acc, [id, metadataRecord]) => {
            const dataItem: TableWidgetDataItem = {
              id,
              name: metadataRecord.name,
              count: res.values[id],
              isUnknown: !!metadataRecord.unknown,
            };
            return [...acc, dataItem];
          }, []);

          this.totalLength = this.data.length;
          this.runFilters(this.tableFilters);
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
        },
      });
  }

  navigate(id: string) {
    this.router.navigate(['../../../..', 'applications', id, 'analytics'], {
      relativeTo: this.activatedRoute,
    });
  }

  runFilters(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.data, filters);
    this.filteredTableData = filtered.filteredCollection;
  }
}
