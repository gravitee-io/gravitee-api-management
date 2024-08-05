/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, computed, Input, OnInit, Signal, signal, WritableSignal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { Application } from '../../../../entities/application/application';
import { LogsResponseMetadataApi, LogsResponseMetadataTotalData } from '../../../../entities/log/log';
import { CapitalizeFirstPipe } from '../../../../pipe/capitalize-first.pipe';
import { ApplicationLogService } from '../../../../services/application-log.service';

interface LogVM {
  apiName: string;
  apiVersion: string;
  timestamp: number;
  method: string;
  status: number;
}

@Component({
  selector: 'app-application-tab-logs',
  standalone: true,
  imports: [
    AsyncPipe,
    LoaderComponent,
    CapitalizeFirstPipe,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatRow,
    MatRowDef,
    MatTable,
    MatHeaderCellDef,
    DatePipe,
    MatIconButton,
    MatButton,
  ],
  templateUrl: './application-tab-logs.component.html',
  styleUrl: './application-tab-logs.component.scss',
})
export class ApplicationTabLogsComponent implements OnInit {
  @Input()
  application!: Application;

  logs$: Observable<LogVM[]> = of([]);

  pagination: Signal<{ hasPreviousPage: boolean; hasNextPage: boolean; currentPage: number; totalPages: number }> = computed(() => {
    const totalPages = Math.ceil(this.totalLogs() / 10);

    return {
      hasPreviousPage: this.currentLogsPage() > 1,
      hasNextPage: this.currentLogsPage() < totalPages,
      currentPage: this.currentLogsPage(),
      totalPages,
    };
  });

  displayedColumns: string[] = ['api', 'timestamp', 'httpMethod', 'responseStatus'];

  private currentLogsPage: WritableSignal<number> = signal(1);
  private totalLogs: WritableSignal<number> = signal(0);

  constructor(
    private applicationLogService: ApplicationLogService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.logs$ = this.activatedRoute.queryParams.pipe(
      distinctUntilChanged(),
      switchMap(queryParams => {
        const page: number = queryParams['page'] ? +queryParams['page'] : 1;
        this.currentLogsPage.set(page);
        return this.applicationLogService.list(this.application.id, { page });
      }),
      tap(({ metadata }) => {
        this.totalLogs.set((metadata['data'] as LogsResponseMetadataTotalData).total);
      }),
      map(response =>
        response.data.map(log => ({
          apiName: (response.metadata[log.api] as LogsResponseMetadataApi).name,
          apiVersion: (response.metadata[log.api] as LogsResponseMetadataApi).version,
          method: log.method,
          status: log.status,
          timestamp: log.timestamp,
        })),
      ),
      catchError(err => {
        console.error(err);
        return of([]);
      }),
    );
  }

  goToPreviousPage() {
    if (this.currentLogsPage() > 0) {
      this.navigate({ page: this.currentLogsPage() - 1 });
    }
  }

  goToNextPage() {
    if (this.currentLogsPage() < this.pagination().totalPages) {
      this.navigate({ page: this.currentLogsPage() + 1 });
    }
  }

  goToPage(page: number) {
    if (page > 0 && page <= this.pagination().totalPages) {
      this.navigate({ page });
    }
  }

  private navigate(params: { page: number }) {
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page: params.page,
      },
    });
  }
}
