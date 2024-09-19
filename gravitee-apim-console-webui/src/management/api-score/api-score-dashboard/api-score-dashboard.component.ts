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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, switchMap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiScoringService } from '../../../services-ngx/api-scoring.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApisScoring, ApisScoringResponse } from '../api-score.model';

@Component({
  selector: 'app-api-score-dashboard',
  templateUrl: './api-score-dashboard.component.html',
  styleUrl: './api-score-dashboard.component.scss',
})
export class ApiScoreDashboardComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading = false;
  public apisScoringList: ApisScoring[] = [];
  public displayedColumns: string[] = ['picture', 'name', 'score', 'errors', 'warnings', 'infos', 'hints', 'actions'];
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  public nbTotalInstances: number = 0;
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  public overviewData = {
    overviewScore: {
      averageScore: 99,
    },
    overviewStatistics: [
      {
        name: 'Errors',
        count: 28,
      },
      {
        name: 'Warnings',
        count: 23,
      },
      {
        name: 'Infos',
        count: 10,
      },
      {
        name: 'Hints',
        count: 12,
      },
    ],
  };
  public evaluatingDate = new Date();

  constructor(
    private readonly apiScoringService: ApiScoringService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.initFilters();
  }

  public initFilters(): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) => {
          this.isLoading = true;
          return this.apiScoringService.getApisScoringList(filters.pagination.index, filters.pagination.size);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: ApisScoringResponse) => {
          this.isLoading = false;
          this.apisScoringList = res.data;
          this.nbTotalInstances = res.pagination.totalCount;
        },
        error: (e) => {
          this.isLoading = false;
          this.snackBarService.error(e.error?.message ?? 'An error occurred while loading list.');
        },
      });
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }
}
