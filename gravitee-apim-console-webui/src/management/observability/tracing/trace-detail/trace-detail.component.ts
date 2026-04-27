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
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import { TracingV2Service } from '../../../../services-ngx/tracing-v2.service';
import { Trace, TracingGraph } from '../tracing.model';
import { TimelineViewComponent } from './timeline-view/timeline-view.component';
import { FlowViewComponent } from './flow-view/flow-view.component';
import { DebugViewComponent } from './debug-view/debug-view.component';

@Component({
  selector: 'trace-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TimelineViewComponent,
    FlowViewComponent,
    DebugViewComponent,
  ],
  template: `
    <div class="trace-view-container">
      <div class="back-bar">
        <button mat-button (click)="goBack()">
          <mat-icon>arrow_back</mat-icon>
          Back to Tracing
        </button>
        <span class="trace-id-label" *ngIf="traceId">Trace: {{ traceId | slice: 0 : 16 }}…</span>
        <span class="spacer"></span>
        <div class="view-toggle">
          <button class="toggle-btn" [class.active]="activeView === 'timeline'" (click)="activeView = 'timeline'" title="Timeline">
            <span class="material-icons">view_timeline</span>
          </button>
          <button class="toggle-btn" [class.active]="activeView === 'flow'" (click)="activeView = 'flow'" title="Flow">
            <span class="material-icons">account_tree</span>
          </button>
          <button class="toggle-btn" [class.active]="activeView === 'debug'" (click)="activeView = 'debug'" title="Debug">
            <span class="material-icons">bug_report</span>
          </button>
        </div>
      </div>

      <div class="loading" *ngIf="loading">
        <mat-spinner diameter="48"></mat-spinner>
      </div>

      <div class="content" *ngIf="!loading && tracing">
        <div class="view-panel">
          @switch (activeView) {
            @case ('timeline') {
              <app-timeline-view [trace]="trace!"></app-timeline-view>
            }
            @case ('flow') {
              <app-flow-view [trace]="trace!" [tracing]="tracing!"></app-flow-view>
            }
            @case ('debug') {
              <app-debug-view [trace]="trace"></app-debug-view>
            }
          }
        </div>
      </div>
    </div>
  `,
  styleUrl: './trace-detail.component.scss',
  styles: [
    `
      .trace-view-container {
        height: 100%;
        display: flex;
        flex-direction: column;
      }
      .back-bar {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 0;
        margin-bottom: 8px;
      }
      .trace-id-label {
        font-family: 'SF Mono', 'Fira Code', monospace;
        font-size: 12px;
        color: #5c5959;
        background: #f7f7f8;
        padding: 4px 10px;
        border-radius: 4px;
      }
      .spacer {
        flex: 1;
      }
      .view-toggle {
        display: inline-flex;
        border-radius: 20px;
        overflow: hidden;
        border: 1px solid #e3e3e3;
        background: #f7f7f8;
      }
      .toggle-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 32px;
        border: none;
        background: transparent;
        cursor: pointer;
        color: #5c5959;
        transition: all 0.15s;
      }
      .toggle-btn:hover {
        background: rgba(0, 0, 0, 0.06);
      }
      .toggle-btn.active {
        background: #da3b00;
        color: #ffffff;
      }
      .toggle-btn .material-icons {
        font-size: 18px;
      }
      .loading {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 100px;
      }
      .content {
        flex: 1;
        overflow: hidden;
      }
      .view-panel {
        height: calc(100vh - 200px);
        background: #ffffff;
        border: 1px solid #e3e3e3;
        border-radius: 8px;
        overflow: hidden;
        display: flex;
        flex-direction: column;
      }
    `,
  ],
})
export class TraceDetailComponent implements OnInit {
  private readonly tracingService = inject(TracingV2Service);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  traceId = '';
  tracing: TracingGraph | null = null;
  trace: Trace | null = null;
  loading = false;
  activeView: 'timeline' | 'flow' | 'debug' = 'flow';

  ngOnInit(): void {
    this.traceId = this.route.snapshot.paramMap.get('traceId') || '';
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;
    this.cdr.detectChanges();
    forkJoin({
      tracing: this.tracingService.getTracingGraph(this.traceId),
      trace: this.tracingService.getTrace(this.traceId),
    }).subscribe({
      next: ({ tracing, trace }) => {
        this.tracing = tracing;
        this.trace = trace;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
