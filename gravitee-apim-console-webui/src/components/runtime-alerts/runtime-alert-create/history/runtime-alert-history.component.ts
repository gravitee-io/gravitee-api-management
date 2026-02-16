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
import { Component, DestroyRef, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { distinctUntilChanged, switchMap, take } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';
import { isEqual } from 'lodash';

import { AlertService } from '../../../../services-ngx/alert.service';
import { AlertHistoryEvent } from '../../../../entities/alerts/history';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'runtime-alert-history',
  imports: [GioTableWrapperModule, MatCardModule, MatButton, MatTableModule],
  templateUrl: './runtime-alert-history.component.html',
  styleUrl: './runtime-alert-history.component.scss',
})
export class RuntimeAlertHistoryComponent implements OnInit {
  public displayedColumns: string[] = ['date', 'message'];
  public historyEvents: AlertHistoryEvent[];
  public totalElements: number;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  public isLoading = false;

  constructor(
    private readonly alertService: AlertService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap(filters =>
          this.alertService.alertHistory(
            this.activatedRoute.snapshot.params.apiId,
            this.activatedRoute.snapshot.params.alertId,
            filters.pagination.index,
            filters.pagination.size,
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => {
          this.historyEvents = res.content;
          this.totalElements = res.totalElements;
        },
      });
  }

  public onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(filters);
  }

  public refreshHistory() {
    this.isLoading = true;
    this.alertService
      .alertHistory(
        this.activatedRoute.snapshot.params.apiId,
        this.activatedRoute.snapshot.params.alertId,
        this.filters.pagination.index,
        this.filters.pagination.size,
      )
      .pipe(take(1))
      .subscribe({
        next: res => {
          this.isLoading = false;
          this.historyEvents = res.content;
          this.totalElements = res.totalElements;
        },
      });
  }
}
