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
import { Component, DestroyRef, OnInit, input, effect, output, computed, inject, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { Moment } from 'moment';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { map, tap } from 'rxjs/operators';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';
import {
  GioSelectSearchComponent,
  ResultsLoaderInput,
  ResultsLoaderOutput,
  SelectOption,
} from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { Plan } from '../../../../../../entities/management-api-v2';
import { GioTimeframeComponent } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';
import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../../../../services-ngx/application.service';
import { 
  ApiAnalyticsBaseFilterBarService, 
  BaseFilterBarFilters, 
  FilterChip 
} from '../api-analytics-base-filter-bar/api-analytics-base-filter-bar.service';

interface ApiAnalyticsProxyFilterBarForm {
  httpStatuses: FormControl<string[] | null>;
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
  applications: FormControl<string[] | null>;
}

export interface ApiAnalyticsProxyFilters extends BaseFilterBarFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  httpStatuses: string[] | null;
  plans: string[] | null;
  applications: string[] | null;
}

@Component({
  selector: 'api-analytics-proxy-filter-bar',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    GioSelectSearchComponent,
    MatChipsModule,
    MatTooltipModule,
    GioTimeframeComponent,
  ],
  templateUrl: './api-analytics-proxy-filter-bar.component.html',
  styleUrl: './api-analytics-proxy-filter-bar.component.scss',
})
export class ApiAnalyticsProxyFilterBarComponent implements OnInit {
  activeFilters = input.required<ApiAnalyticsProxyFilters>();
  filtersChange = output<ApiAnalyticsProxyFilters>();
  refresh = output<void>();

  protected readonly httpStatuses = [...httpStatuses];
  plans = input<Plan[]>([]);
  
  // Inject services
  private readonly baseService = inject(ApiAnalyticsBaseFilterBarService);
  
  // Use base service properties
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  protected readonly customPeriod = 'custom';
  private readonly apiId = inject(ActivatedRoute).snapshot.params.apiId;
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  
  // Define supported filters for proxy component: plans + timeframe + httpStatuses + applications
  private readonly supportedFilters: string[] = ['period', 'from', 'to', 'httpStatuses', 'plans', 'applications'];

  public planOptions = computed<SelectOption[]>(() => 
    this.baseService.generatePlanOptions(this.plans())
  );

  applicationResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.apiV2Service.getSubscribers(this.apiId, input.searchTerm, input.page, 20).pipe(
      map((response) => ({
        data: response.data.map((app) => ({ value: app.id, label: app.name })),
        hasNextPage: response.pagination.page < response.pagination.pageCount,
      })),
    );
  };

  public currentFilterChips = computed<FilterChip[]>(() => {
    const filters = this.activeFilters();
    const chips: FilterChip[] = [];

    // HTTP Status chips - proxy specific
    if (filters?.httpStatuses?.length) {
      filters.httpStatuses.forEach((status) => {
        const statusOption = this.httpStatuses?.find((opt) => opt.value === status);
        chips.push({
          key: 'httpStatuses',
          value: status,
          display: statusOption?.label || status,
        });
      });
    }

    // Plan chips - use base service
    chips.push(...this.baseService.generatePlanFilterChips(filters?.plans, this.plans()));

    // Application chips - proxy specific
    chips.push(...(this.applicationFilterChips() ?? []));

    return chips;
  });

  public isFiltering = computed(() => 
    this.baseService.isFiltering(this.currentFilterChips())
  );

  form: FormGroup<ApiAnalyticsProxyFilterBarForm> = this.formBuilder.group({
    httpStatuses: this.formBuilder.control<string[] | null>(null),
    ...this.baseService.createBaseForm(this.formBuilder),
    applications: this.formBuilder.control<string[] | null>(null),
  });

  private applicationChipCache: Record<string, FilterChip> = {};
  private applicationFilterChips: Signal<FilterChip[]> = toSignal(
    this.form.controls.applications.valueChanges.pipe(switchMap((applications) => this.getApplicationChips(applications))),
  );

  constructor() {
    // Set up effect to update form when activeFilters changes
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.form.controls.timeframe.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((tf) => {
      this.baseService.handleTimeframeChange(tf, (partial) => this.emitFilters(partial));
    });

    this.form.controls.httpStatuses.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((httpStatuses) => {
      this.emitFilters({ httpStatuses });
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((plans) => {
      this.baseService.handlePlansChange(plans, (partial) => this.emitFilters(partial));
    });

    this.form.controls.applications.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((applications) => {
      this.emitFilters({ applications });
    });
  }

  applyCustomTimeframe() {
    this.baseService.applyCustomTimeframe(
      this.form.controls.timeframe.value,
      this.activeFilters(),
      (filters) => this.filtersChange.emit(filters)
    );
  }

  refreshFilters() {
    this.refresh.emit();
  }

  removeFilter(key: string, value: string) {
    this.baseService.handleRemoveFilter(key, value, this.form, this.supportedFilters);
  }

  resetAllFilters() {
    const resetFilters = this.baseService.getResetFiltersObject<ApiAnalyticsProxyFilters>(this.supportedFilters);
    this.emitFilters(resetFilters);
  }

  private updateFormFromFilters(filters: ApiAnalyticsProxyFilters) {
    this.baseService.updateFormFromFilters(filters, this.form, this.supportedFilters);
  }

  private emitFilters(partial: Partial<ApiAnalyticsProxyFilters>) {
    this.baseService.emitFilters(
      this.activeFilters(),
      partial,
      (filters) => this.filtersChange.emit(filters)
    );
  }

  private getApplicationChips(applications: string[] | null): Observable<FilterChip[]> {
    if (!applications?.length) {
      return of([]);
    }

    const cachedChips: FilterChip[] = [];
    const uncachedIds: string[] = [];

    applications.forEach((appId) => {
      if (this.applicationChipCache[appId]) {
        cachedChips.push(this.applicationChipCache[appId]);
      } else {
        uncachedIds.push(appId);
      }
    });

    if (uncachedIds.length === 0) {
      return of(cachedChips);
    }

    return this.fetchApplicationsAndUpdateCache$(uncachedIds).pipe(map((newChips) => [...cachedChips, ...newChips]));
  }

  private fetchApplicationsAndUpdateCache$(uncachedIds: string[]): Observable<FilterChip[]> {
    return this.applicationService.findByIds(uncachedIds, 1, 200).pipe(
      map((response) => {
        return uncachedIds.map((id) => {
          const foundApp = response.data.find((app) => app.id === id);
          return {
            key: 'applications',
            value: id,
            display: foundApp?.name ?? 'Unknown Application',
          };
        });
      }),
      tap((newChips) => {
        newChips.forEach((chip) => {
          this.applicationChipCache[chip.value] = chip;
        });
      }),
    );
  }
}
