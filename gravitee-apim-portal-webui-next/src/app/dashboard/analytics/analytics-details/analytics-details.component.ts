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
import { Component, computed, DestroyRef, effect, inject, Injector, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';

import {
  AddFilterDialogComponent,
  AddFilterDialogData,
  BasicTimeframe,
  Dashboard,
  DynamicFilterBarComponent,
  FilterCondition,
  GraviteeDashboardComponent,
  provideFilterDefinitions,
  provideFilterValues,
  TimeframeSelectorComponent,
  TimeframeValue,
  timeFrameRangesParams,
  timeFrames,
  TimeRange,
} from '@gravitee/gravitee-dashboard';

import { DashboardFiltersStore } from './dashboard-filters.store';
import { PortalAnalyticsFiltersService } from './portal-analytics-filters.service';
import { BannerComponent } from '../../../../components/banner/banner.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { AnalyticsDashboardService } from '../../../../services/analytics-dashboard.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { ConfigService } from '../../../../services/config.service';
import { analyticsListBreadcrumb } from '../analytics-breadcrumbs';

const DEFAULT_PERIOD: BasicTimeframe = '5m';

@Component({
  selector: 'app-analytics-details',
  imports: [
    GraviteeDashboardComponent,
    LoaderComponent,
    BannerComponent,
    TimeframeSelectorComponent,
    ReactiveFormsModule,
    DynamicFilterBarComponent,
  ],
  templateUrl: './analytics-details.component.html',
  styleUrl: './analytics-details.component.scss',
  providers: [
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    DashboardFiltersStore,
    PortalAnalyticsFiltersService,
    provideFilterDefinitions(PortalAnalyticsFiltersService),
    provideFilterValues(PortalAnalyticsFiltersService),
  ],
})
export default class AnalyticsDetailsComponent {
  private readonly configService = inject(ConfigService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly analyticsDashboardService = inject(AnalyticsDashboardService);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  readonly filtersStore = inject(DashboardFiltersStore);
  readonly dashboardId = input.required<string>();

  readonly baseURL = this.configService.baseURL;
  protected readonly defaultPeriod = DEFAULT_PERIOD;
  protected readonly timeFrames = timeFrames;
  protected readonly periodControl = new FormControl<TimeframeValue>(
    { period: DEFAULT_PERIOD, from: null, to: null },
    { nonNullable: true },
  );
  private readonly refreshTokenSignal = signal(0);
  readonly refreshToken = this.refreshTokenSignal.asReadonly();

  private readonly period = toSignal(this.periodControl.valueChanges, { initialValue: this.periodControl.value });

  readonly dashboardResource = rxResource<Dashboard | undefined, string>({
    params: () => this.dashboardId(),
    stream: ({ params }) => this.analyticsDashboardService.getById(params),
  });
  readonly dashboard = computed(() => (this.dashboardResource.error() ? undefined : this.dashboardResource.value()));
  readonly dashboardName = computed(() => this.dashboard()?.name ?? '');

  private readonly timeframeParams = computed(() => {
    this.refreshToken();
    const id = this.period().period;
    const known = timeFrames.some(timeFrame => timeFrame.id === id);
    return timeFrameRangesParams(known ? (id as BasicTimeframe) : DEFAULT_PERIOD);
  });
  readonly timeRange = computed<TimeRange>(() => {
    const params = this.timeframeParams();
    return { from: new Date(params.from).toISOString(), to: new Date(params.to).toISOString() };
  });
  readonly interval = computed(() => this.timeframeParams().interval);

  constructor() {
    effect(() => {
      const id = this.dashboardId();
      const name = this.dashboardName();
      this.breadcrumbService.set([analyticsListBreadcrumb(true), { id: `analytics-${id}`, label: name || id }]);
    });
  }

  refresh(): void {
    this.refreshTokenSignal.update(token => token + 1);
  }

  openAddFilter(): void {
    const params = this.timeframeParams();
    this.dialog
      .open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
        data: { timeFrom: params.from, timeTo: params.to },
        injector: this.injector,
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(condition => {
        if (condition) {
          this.filtersStore.add(condition);
        }
      });
  }

  openEditFilter(index: number, condition: FilterCondition): void {
    const params = this.timeframeParams();
    this.dialog
      .open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
        data: { existingCondition: condition, timeFrom: params.from, timeTo: params.to },
        injector: this.injector,
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(updated => {
        if (updated) {
          this.filtersStore.edit(index, updated);
        }
      });
  }
}
