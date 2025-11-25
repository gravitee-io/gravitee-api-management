/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, input, effect, output, computed, inject, Signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute } from '@angular/router';
import moment, { Moment } from 'moment';
import { Observable, of, switchMap } from 'rxjs';
import { map, tap } from 'rxjs/operators';

import { DropdownSearchComponent } from '@gravitee/gravitee-dashboard';

import { HttpStatuses } from './population/http-statuses';
import { Plan } from '../../../../../../../src/entities/plan/plan';
import { ResultsLoaderInput, ResultsLoaderOutput, SelectOption } from '../dropdown-search/dropdown-search.component';
import { ApiV2Service } from '../services/api-v2.service';
import { ApplicationService } from '../services/application.service';
import { TimeFrameComponent } from '../time-frame/time-frame.component';
import { customTimeFrames, timeFrames } from '../time-frame/utils/time-frame-ranges';

interface FilterBarForm {
  httpStatuses: FormControl<string[] | null>;
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
  applications: FormControl<string[] | null>;
}

export interface Filters {
  period: string;
  from?: number | null;
  to?: number | null;
  httpStatuses: string[] | null;
  plans: string[] | null;
  applications: string[] | null;
}

interface FilterChip {
  key: string;
  value: string;
  display: string;
}

@Component({
  selector: 'gd-filter-bar',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    DropdownSearchComponent,
    MatChipsModule,
    MatTooltipModule,
    TimeFrameComponent,
    MatIcon,
  ],
  templateUrl: './filter-bar.component.html',
  styleUrl: './filter-bar.component.scss',
})
export class FilterBarComponent implements OnInit {
  activeFilters = input.required<Filters>();
  filtersChange = output<Filters>();
  refresh = output<void>();
  public readonly formBuilder = inject(FormBuilder);

  form: FormGroup<FilterBarForm> = this.formBuilder.group({
    httpStatuses: this.formBuilder.control<string[] | null>(null),
    timeframe: this.formBuilder.control<{ period: string; from: Moment | null; to: Moment | null } | null>(null),
    plans: this.formBuilder.control<string[] | null>(null),
    applications: this.formBuilder.control<string[] | null>(null),
  });
  customPeriod: string = 'custom';
  plans = input<Plan[]>([]);
  public planOptions = computed<SelectOption[]>(() => {
    const plans = this.plans() || [];
    return plans.map(plan => ({ value: plan.id, label: plan.name }));
  });

  public currentFilterChips = computed<FilterChip[]>(() => {
    const filters = this.activeFilters();
    const chips: FilterChip[] = [];

    if (filters?.httpStatuses?.length) {
      filters.httpStatuses.forEach(status => {
        const statusOption = this.httpStatuses?.find(opt => opt.value === status);
        chips.push({
          key: 'httpStatuses',
          value: status,
          display: statusOption?.label || status,
        });
      });
    }

    if (filters?.plans?.length) {
      const plans = this.plans();
      filters.plans.forEach(planId => {
        const plan = plans?.find(p => p.id === planId);
        const display = plan ? plan.name : planId;
        chips.push({
          key: 'plans',
          value: planId,
          display: display,
        });
      });
    }

    chips.push(...(this.applicationFilterChips() ?? []));

    return chips;
  });

  public isFiltering = computed(() => this.currentFilterChips().length > 0);

  protected readonly httpStatuses = [...HttpStatuses];
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  // INJECTIONS
  private readonly apiId = inject(ActivatedRoute).snapshot.params.apiId;
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);

  private applicationChipCache: Record<string, FilterChip> = {};
  private applicationFilterChips: Signal<FilterChip[]> = toSignal(
    this.form.controls.applications.valueChanges.pipe(switchMap(applications => this.getApplicationChips(applications))),
    { initialValue: [] },
  );

  constructor() {
    // Set up effect to update form when activeFilters changes
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  applicationResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.apiV2Service.getSubscribers(this.apiId, input.searchTerm, input.page, 20).pipe(
      map(response => ({
        data: response.data.map(app => ({ value: app.id, label: app.name })),
        hasNextPage: response.pagination.page < response.pagination.pageCount,
      })),
    );
  };

  ngOnInit() {
    this.form.controls.timeframe.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(tf => {
      if (tf?.period && tf.period !== this.customPeriod) {
        this.emitFilters({ period: tf.period, from: null, to: null });
      }
    });

    this.form.controls.httpStatuses.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(httpStatuses => {
      this.emitFilters({ httpStatuses: httpStatuses });
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(plans => {
      this.emitFilters({ plans });
    });

    this.form.controls.applications.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(applications => {
      this.emitFilters({ applications });
    });
  }

  applyCustomTimeframe() {
    const tf = this.form.controls.timeframe.value;
    const from = tf?.from?.valueOf() ?? null;
    const to = tf?.to?.valueOf() ?? null;

    const currentFilters = this.activeFilters();
    const updatedFilters = {
      ...currentFilters,
      from,
      to,
      period: this.customPeriod,
    };

    this.filtersChange.emit(updatedFilters);
  }

  refreshFilters() {
    this.refresh.emit();
  }

  removeFilter(key: string, value: string) {
    if (key === 'httpStatuses') {
      this.removeValueFromFilter(this.form.controls.httpStatuses.value, value, this.form.controls.httpStatuses);
    } else if (key === 'plans') {
      this.removeValueFromFilter(this.form.controls.plans.value, value, this.form.controls.plans);
    } else if (key === 'applications') {
      this.removeValueFromFilter(this.form.controls.applications.value, value, this.form.controls.applications);
    }
  }

  resetAllFilters() {
    this.emitFilters({ httpStatuses: null, plans: null, applications: null });
  }

  private removeValueFromFilter(currentList: string[] | null, value: string, formControl: FormControl<string[] | null>): void {
    const filteredList = (currentList || []).filter(item => item !== value);
    formControl.setValue(filteredList.length > 0 ? filteredList : null);
  }

  private updateFormFromFilters(filters: Filters) {
    if (this.form) {
      this.form.patchValue({
        timeframe: {
          period: filters.period,
          from: filters.from ? moment(filters.from) : null,
          to: filters.to ? moment(filters.to) : null,
        },
        plans: filters.plans,
        httpStatuses: filters.httpStatuses,
        applications: filters.applications,
      });
    }
  }

  private emitFilters(partial: Partial<Filters>) {
    this.filtersChange.emit({
      ...this.activeFilters(),
      ...partial,
    });
  }

  private getApplicationChips(applications: string[] | null): Observable<FilterChip[]> {
    if (!applications?.length) {
      return of([]);
    }

    const cachedChips: FilterChip[] = [];
    const uncachedIds: string[] = [];

    applications.forEach(appId => {
      if (this.applicationChipCache[appId]) {
        cachedChips.push(this.applicationChipCache[appId]);
      } else {
        uncachedIds.push(appId);
      }
    });

    if (uncachedIds.length === 0) {
      return of(cachedChips);
    }

    return this.fetchApplicationsAndUpdateCache$(uncachedIds).pipe(map(newChips => [...cachedChips, ...newChips]));
  }

  private fetchApplicationsAndUpdateCache$(uncachedIds: string[]): Observable<FilterChip[]> {
    return this.applicationService.findByIds(uncachedIds, 1, 200).pipe(
      map(response => {
        return uncachedIds.map(id => {
          const foundApp = response.data.find(app => app.id === id);
          return {
            key: 'applications',
            value: id,
            display: foundApp?.name ?? 'Unknown Application',
          };
        });
      }),
      tap(newChips => {
        newChips.forEach(chip => {
          this.applicationChipCache[chip.value] = chip;
        });
      }),
    );
  }
}
