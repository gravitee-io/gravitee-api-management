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

import { GioTableWrapperModule } from '../gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../gio-table-wrapper/gio-table-wrapper.util';

export interface TopApisV4 {
  id: string;
  name: string;
  count: number;
}

@Component({
  selector: 'app-top-apis-widget',
  standalone: true,
  imports: [CommonModule, RouterModule, MatTableModule, MatCardModule, MatSortModule, GioTableWrapperModule, GioLoaderModule],
  templateUrl: './top-apis-widget.component.html',
  styleUrl: './top-apis-widget.component.scss',
})
export class TopApisWidgetComponent implements OnChanges {
  @Input() data: TopApisV4[];

  displayedColumns = ['name', 'count'];
  filteredTableData: TopApisV4[];
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

  navigateToApi(apiKey: string): void {
    this.router.navigate(['../../', 'apis', apiKey, 'v4', 'analytics'], {
      relativeTo: this.activatedRoute,
    });
  }

  runFilters(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.data, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
  }
}
