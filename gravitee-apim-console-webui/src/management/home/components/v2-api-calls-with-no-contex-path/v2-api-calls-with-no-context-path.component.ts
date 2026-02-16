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
import { BehaviorSubject, combineLatest } from 'rxjs';
import { filter, switchMap } from 'rxjs/operators';
import { MatTableModule } from '@angular/material/table';
import { DatePipe } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';

import { HomeService } from '../../../../services-ngx/home.service';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { PlatformLog } from '../../../../entities/platform/platformLogs';
import { PlatformService } from '../../../../services-ngx/platform.service';

@Component({
  selector: 'v2-api-calls-with-no-context-path',
  imports: [GioTableWrapperModule, MatTableModule, GioLoaderModule, DatePipe],
  templateUrl: './v2-api-calls-with-no-context-path.component.html',
  styleUrl: './v2-api-calls-with-no-context-path.component.scss',
})
export class V2ApiCallsWithNoContextPathComponent implements OnInit {
  public displayedColumns = ['date', 'method', 'path'];
  public isLoading = true;
  public platformLogs: PlatformLog[] = [];

  public totalLength = 0;
  public tableFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: { index: 1, size: 5 },
  };
  private tableFilters$ = new BehaviorSubject<GioTableWrapperFilters>(null);

  constructor(
    private readonly platformService: PlatformService,
    private readonly destroyRef: DestroyRef,
    private readonly homeService: HomeService,
    private readonly cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    combineLatest([this.tableFilters$, this.homeService.timeRangeParams()])
      .pipe(
        filter(([tableFilters]) => tableFilters !== null),
        switchMap(([tableFilters, timeRangeParams]) => {
          this.isLoading = true;
          return this.platformService.getPlatformV2Logs({
            from: timeRangeParams.from,
            to: timeRangeParams.to,
            page: tableFilters.pagination.index,
            size: tableFilters.pagination.size,
            field: '@timestamp',
            order: false,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => {
          this.platformLogs = res.logs;
          this.totalLength = res.total;
          this.isLoading = false;
          this.cd.detectChanges();
        },
      });
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.tableFilters$.next({ ...this.tableFilters$.value, ...filters });
  }
}
