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

import { Component, input, OnChanges, OnInit, SimpleChanges } from "@angular/core";
import { DecimalPipe } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';

import { GioTableWrapperModule } from "../../../gio-table-wrapper/gio-table-wrapper.module";
import { GioTableWrapperFilters } from "../../../gio-table-wrapper/gio-table-wrapper.component";
import { gioTableFilterCollection } from "../../../gio-table-wrapper/gio-table-wrapper.util";
import { WidgetConfig } from "../../../../../entities/management-api-v2/analytics/analytics";


const tableConfig = {
  'application-id': {
    nameLabel: 'App',
    countLabel: 'Requests Count',
  },
};

export interface TableWidgetDataItem {
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
export class TableWidgetComponent implements OnInit, OnChanges {
  public config = input<WidgetConfig>();

  public displayedColumns = ['name', 'count'];
  public nameLabel: string = '';
  public countLabel: string = '';
  public data: TableWidgetDataItem[];

  public filteredTableData: TableWidgetDataItem[] = [];
  public tableFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 5 },
    searchTerm: '',
    sort: {
      direction: 'desc',
    },
  };
  totalLength: number = 1;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnChanges(changes:SimpleChanges) {
    this.data = changes.config.currentValue.data;
    this.runFilters(this.tableFilters);
  }

  ngOnInit() {
    const { nameLabel, countLabel } = tableConfig[this.config().groupByField];
    this.nameLabel = nameLabel || 'Name';
    this.countLabel = countLabel || 'Count';
  }

  navigate(id: string) {
    this.router.navigate(['../../../..', 'applications', id, 'analytics'], {
      relativeTo: this.activatedRoute,
    });
  }

  runFilters(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.data, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = this.filteredTableData.length;
  }
}
