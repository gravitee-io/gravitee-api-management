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

import { ChangeDetectorRef, Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { switchMap, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { DecimalPipe } from '@angular/common';

import { HomeService } from '../../../../services-ngx/home.service';
import { AnalyticsService } from '../../../../services-ngx/analytics.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { TimeRangeParams } from '../../../../shared/utils/timeFrameRanges';
import { AnalyticsTopFailedApi, AnalyticsDefinitionVersion } from '../../../../entities/analytics/analytics';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@Component({
  selector: 'top-failed-apis',
  imports: [GioLoaderModule, GioTableWrapperModule, MatCardModule, MatSortModule, MatTableModule, DecimalPipe],
  templateUrl: './top-failed-apis.component.html',
  styleUrl: './top-failed-apis.component.scss',
})
export class TopFailedApisComponent implements OnInit {
  public isLoading = true;
  public timeframe: TimeRangeParams;
  public topFailedApis: AnalyticsTopFailedApi[];

  public tableColumns = ['name', 'failedCalls', 'failedCallsRatio'];
  public tableFilters: GioTableWrapperFilters = { pagination: { index: 1, size: 5 }, searchTerm: '' };
  public filteredTableData: AnalyticsTopFailedApi[] = [];
  public totalLength: number = 0;

  constructor(
    private readonly homeService: HomeService,
    private readonly analyticsService: AnalyticsService,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.homeService
      .timeRangeParams()
      .pipe(
        tap((timeframe: TimeRangeParams) => (this.timeframe = timeframe)),
        switchMap(({ from, to }: TimeRangeParams) => {
          this.isLoading = true;
          this.resetData();
          return this.analyticsService.getTopFailedApis(from, to);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.topFailedApis = res.data;
          this.isLoading = false;
          this.onFiltersChanged(this.tableFilters);
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        },
      });
  }

  resetData() {
    this.filteredTableData = [];
    this.topFailedApis = [];
    this.tableFilters = { pagination: { index: 1, size: 5 }, searchTerm: '' };
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.tableFilters = { ...this.tableFilters, ...filters };
    const { filteredCollection, unpaginatedLength } = gioTableFilterCollection(this.topFailedApis, filters);
    this.filteredTableData = filteredCollection;
    this.totalLength = unpaginatedLength;
    this.changeDetector.detectChanges();
  }

  navigateToApi(id: string, definitionVersion: AnalyticsDefinitionVersion): void {
    const customTimeframeParams = this.timeframe.id === 'custom' ? { from: this.timeframe.from, to: this.timeframe.to } : {};
    this.router.navigate(
      ['../../', 'apis', id, definitionVersion.toLowerCase(), definitionVersion === 'V2' ? 'analytics-overview' : 'analytics'],
      {
        relativeTo: this.activatedRoute,
        queryParams: { period: this.timeframe.id, ...customTimeframeParams },
      },
    );
  }
}
