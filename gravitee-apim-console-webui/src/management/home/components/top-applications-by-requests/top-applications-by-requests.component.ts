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
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { DecimalPipe, NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { EMPTY } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';

import { AnalyticsService } from '../../../../services-ngx/analytics.service';
import { HomeService } from '../../../../services-ngx/home.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { TimeRangeParams } from '../../../../shared/utils/timeFrameRanges';
import { TopApplication } from '../../../../entities/analytics/analytics';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@Component({
  selector: 'top-applications-by-requests',
  imports: [MatCardModule, GioTableWrapperModule, MatTableModule, MatSortModule, NgIf, DecimalPipe, GioLoaderModule, RouterLink],
  templateUrl: './top-applications-by-requests.component.html',
  styleUrl: './top-applications-by-requests.component.scss',
})
export class TopApplicationsByRequestsComponent implements OnInit {
  public isLoading = false;
  public topApplications: TopApplication[] = [];
  public timeFrame: TimeRangeParams;

  public displayedColumns = ['name', 'count'];
  public filteredTableData: TopApplication[] = [];
  public tableFilters: GioTableWrapperFilters = { pagination: { index: 1, size: 5 }, searchTerm: '' };
  public totalLength: number = 0;

  constructor(
    private readonly analyticsService: AnalyticsService,
    private readonly homeService: HomeService,
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
        tap((timeFrame: TimeRangeParams) => (this.timeFrame = timeFrame)),
        switchMap(({ from, to }: TimeRangeParams) => {
          this.isLoading = true;
          this.resetData();
          return this.analyticsService.getTopApplicationsByRequestsCount({
            from,
            to,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.topApplications = res.data;
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
    this.topApplications = [];
    this.tableFilters = { pagination: { index: 1, size: 5 }, searchTerm: '' };
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.tableFilters = { ...this.tableFilters, ...filters };
    const { filteredCollection, unpaginatedLength } = gioTableFilterCollection(this.topApplications, filters);
    this.filteredTableData = filteredCollection;
    this.totalLength = unpaginatedLength;
    this.changeDetector.detectChanges();
  }

  navigateToApplication(id: string): void {
    const queryParams = {
      from: this.timeFrame.from,
      to: this.timeFrame.to,
    };

    this.router.navigate(['../..', 'applications', id, 'analytics'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }
}
