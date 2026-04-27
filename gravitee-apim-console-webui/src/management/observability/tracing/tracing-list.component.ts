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
import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { Observable, Subject, takeUntil } from 'rxjs';
import { map } from 'rxjs/operators';

import { TracingV2Service } from '../../../services-ngx/tracing-v2.service';
import { GioHeaderComponent } from '../../../shared/components/gio-header/gio-header.component';
import {
  GioSelectSearchComponent,
  ResultsLoaderInput,
  ResultsLoaderOutput,
} from '../../../shared/components/gio-select-search/gio-select-search.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Trace } from './tracing.model';

const TIME_RANGE_MS: Record<string, number> = {
  '5m': 5 * 60 * 1000,
  '15m': 15 * 60 * 1000,
  '30m': 30 * 60 * 1000,
  '1h': 60 * 60 * 1000,
  '6h': 6 * 60 * 60 * 1000,
  '24h': 24 * 60 * 60 * 1000,
  '7d': 7 * 24 * 60 * 60 * 1000,
};

@Component({
  selector: 'tracing-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
    GioHeaderComponent,
    GioSelectSearchComponent,
  ],
  template: `
    <gio-header title="Tracing" subtitle="Browse distributed traces from OpenTelemetry." />

    <mat-card class="tracing-section">
      <div class="filters-bar">
        <mat-form-field appearance="outline" class="period-field">
          <mat-label>Period</mat-label>
          <mat-select [(ngModel)]="selectedTimeRange" (selectionChange)="loadTraces()">
            <mat-option value="5m">Last 5 minutes</mat-option>
            <mat-option value="15m">Last 15 minutes</mat-option>
            <mat-option value="30m">Last 30 minutes</mat-option>
            <mat-option value="1h">Last 1 hour</mat-option>
            <mat-option value="6h">Last 6 hours</mat-option>
            <mat-option value="24h">Last 24 hours</mat-option>
            <mat-option value="7d">Last 7 days</mat-option>
          </mat-select>
        </mat-form-field>

        <gio-select-search
          class="api-field"
          label="API"
          [resultsLoader]="apiResultsLoader"
          [(ngModel)]="apiNames"
          (ngModelChange)="loadTraces()"
        ></gio-select-search>

        <button mat-stroked-button (click)="loadTraces()" [disabled]="loading">
          <mat-icon>refresh</mat-icon> Refresh
        </button>
      </div>

      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      }

      @if (!loading) {
        <div class="table-container">
          <table mat-table [dataSource]="dataSource" matSort class="tracing-table">
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let row">
                @if (row.hasError === true) {
                  <span class="status-badge status-error" title="At least one span in this trace has an ERROR status">
                    <mat-icon>error</mat-icon> Error
                  </span>
                } @else if (row.hasError === false) {
                  <span class="status-badge status-ok">
                    <mat-icon>check_circle</mat-icon> OK
                  </span>
                } @else {
                  <span class="status-badge status-unknown">—</span>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="traceId">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Trace ID</th>
              <td mat-cell *matCellDef="let row">
                <span class="trace-id-cell">{{ row.traceId | slice: 0 : 16 }}…</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="rootService">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>API</th>
              <td mat-cell *matCellDef="let row">{{ row.rootService }}</td>
            </ng-container>

            <ng-container matColumnDef="rootOperation">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Operation</th>
              <td mat-cell *matCellDef="let row">{{ row.rootOperation }}</td>
            </ng-container>

            <ng-container matColumnDef="startTime">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Start Time</th>
              <td mat-cell *matCellDef="let row">{{ formatTime(row.startTime) }}</td>
            </ng-container>

            <ng-container matColumnDef="durationNanos">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Duration</th>
              <td mat-cell *matCellDef="let row">{{ formatDuration(row.durationNanos) }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button (click)="viewTrace(row); $event.stopPropagation()">
                  <mat-icon>visibility</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns" (click)="viewTrace(row)" class="clickable-row"></tr>
          </table>

          <mat-paginator [pageSizeOptions]="[10, 25, 50]" [pageSize]="10" showFirstLastButtons></mat-paginator>
        </div>
      }

      @if (!loading && dataSource.data.length === 0) {
        <div class="empty-state">
          <mat-icon>hourglass_empty</mat-icon>
          <p>No traces found</p>
        </div>
      }
    </mat-card>
  `,
  styleUrl: './tracing-list.component.scss',
})
export class TracingListComponent implements OnInit, OnDestroy {
  private readonly tracingService = inject(TracingV2Service);
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  displayedColumns = ['status', 'traceId', 'rootService', 'rootOperation', 'startTime', 'durationNanos', 'actions'];
  dataSource = new MatTableDataSource<Trace>([]);
  loading = false;
  selectedTimeRange = '1h';
  apiNames: string[] = [];

  // API picker loader: returns API names (used both as the option value and label so the selection maps 1:1 to Tempo's service.name attribute).
  readonly apiResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> =>
    this.apiV2Service.search({ query: input.searchTerm }, undefined, input.page, 10).pipe(
      map(response => ({
        data: response.data.map(api => ({ value: api.name, label: api.name })),
        hasNextPage: response.pagination.pageCount > input.page,
      })),
    );

  @ViewChild(MatPaginator) set matPaginator(p: MatPaginator) {
    if (p) this.dataSource.paginator = p;
  }
  @ViewChild(MatSort) set matSort(s: MatSort) {
    if (s) this.dataSource.sort = s;
  }

  ngOnInit(): void {
    this.loadTraces();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTraces(): void {
    this.loading = true;
    this.cdr.detectChanges();

    const durationMs = TIME_RANGE_MS[this.selectedTimeRange] ?? TIME_RANGE_MS['1h'];
    const end = Math.floor(Date.now() / 1000);
    const start = Math.floor((Date.now() - durationMs) / 1000);
    // gio-select-search is multi-select; the first picked API drives the TraceQL service.name tag.
    const selectedApi = this.apiNames?.[0];
    const tags = selectedApi ? `service.name=${selectedApi}` : undefined;

    this.tracingService
      .searchTraces({ limit: 50, tags, start, end })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: traces => {
          this.dataSource.data = traces;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.dataSource.data = [];
          this.loading = false;
          this.cdr.detectChanges();
        },
      });
  }

  viewTrace(trace: Trace): void {
    this.router.navigate([trace.traceId], { relativeTo: this.route });
  }

  formatTime(value: string | number): string {
    if (!value) return '';
    const ms = this.toEpochMs(value);
    return ms > 0 ? new Date(ms).toLocaleString() : '';
  }

  private toEpochMs(value: string | number): number {
    if (typeof value === 'number') {
      return value < 1e12 ? value * 1000 : value;
    }
    const d = new Date(value);
    if (!isNaN(d.getTime())) return d.getTime();
    const num = parseFloat(value);
    if (!isNaN(num)) return num < 1e12 ? num * 1000 : num;
    return 0;
  }

  formatDuration(nanos: number): string {
    if (!nanos) return '—';
    const ms = nanos / 1_000_000;
    if (ms < 1000) return `${ms.toFixed(0)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }
}
