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
import { Component, inject, Signal, input, output, effect, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { map, tap } from 'rxjs/operators';

import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';
import {
  GioSelectSearchComponent,
  ResultsLoaderInput,
  ResultsLoaderOutput,
} from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { GioTimeframeComponent } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';
import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../../../../services-ngx/application.service';
import { Plan } from '../../../../../../entities/management-api-v2';
import { CommonFiltersComponent } from '../common/common-filters.component';
import { ProxyFilters, ProxyFilterForm, ProxySpecificForm, FilterChip, FILTER_KEYS } from '../common/common-filters.types';

export type ApiAnalyticsProxyFilters = ProxyFilters;

@Component({
  selector: 'api-analytics-proxy-filter-bar',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatChipsModule,
    MatTooltipModule,
    GioSelectSearchComponent,
    GioTimeframeComponent,
  ],
  templateUrl: './api-analytics-proxy-filter-bar.component.html',
  styleUrl: './api-analytics-proxy-filter-bar.component.scss',
})
export class ApiAnalyticsProxyFilterBarComponent extends CommonFiltersComponent<ProxyFilters> implements OnInit {
  activeFilters = input.required<ProxyFilters>();
  filtersChange = output<ProxyFilters>();
  refresh = output<void>();
  plans = input<Plan[]>([]);

  protected readonly httpStatuses = [...httpStatuses];

  applicationResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.apiV2Service.getSubscribers(this.apiId, input.searchTerm, input.page, 20).pipe(
      map((response) => ({
        data: response.data.map((app) => ({ value: app.id, label: app.name })),
        hasNextPage: response.pagination.page < response.pagination.pageCount,
      })),
    );
  };

  private readonly apiId = inject(ActivatedRoute).snapshot.params.apiId;
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly applicationService = inject(ApplicationService);

  form: FormGroup<ProxyFilterForm> = this.formBuilder.group({
    ...this.createCommonFormControls(),
    ...this.createProxySpecificFormControls(),
  } as ProxyFilterForm);

  private applicationChipCache: Record<string, FilterChip> = {};
  private applicationFilterChips: Signal<FilterChip[]> = toSignal(
    this.form.get(FILTER_KEYS.APPLICATIONS)?.valueChanges.pipe(switchMap((applications) => this.getApplicationChips(applications))) ||
      of([]),
    { initialValue: [] },
  );

  constructor() {
    super();
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.setupCommonFormSubscriptions();
    this.setupProxySpecificFormSubscriptions();
  }

  protected override getAllFilterChips(): FilterChip[] {
    const filters = this.activeFilters();
    const commonChips = this.getCommonFilterChips(filters);
    const proxyChips = this.getProxySpecificFilterChips(filters);
    return [...commonChips, ...proxyChips];
  }

  private getProxySpecificFilterChips(filters: ProxyFilters): FilterChip[] {
    const chips: FilterChip[] = [];

    if (filters?.httpStatuses?.length) {
      filters.httpStatuses.forEach((status) => {
        const statusOption = this.httpStatuses?.find((opt) => opt.value === status);
        chips.push({
          key: FILTER_KEYS.HTTP_STATUSES,
          value: status,
          display: statusOption?.label || status,
        });
      });
    }

    chips.push(...(this.applicationFilterChips() ?? []));

    return chips;
  }

  private createProxySpecificFormControls(): ProxySpecificForm {
    return {
      [FILTER_KEYS.HTTP_STATUSES]: this.formBuilder.control<string[] | null>(null),
      [FILTER_KEYS.APPLICATIONS]: this.formBuilder.control<string[] | null>(null),
    };
  }

  private setupProxySpecificFormSubscriptions(): void {
    this.form
      .get(FILTER_KEYS.HTTP_STATUSES)
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((httpStatuses: any) => {
        this.emitFilters({ [FILTER_KEYS.HTTP_STATUSES]: httpStatuses });
      });

    this.form
      .get(FILTER_KEYS.APPLICATIONS)
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((applications: any) => {
        this.emitFilters({ [FILTER_KEYS.APPLICATIONS]: applications });
      });
  }

  protected override getSpecificFormData(filters: ProxyFilters): any {
    return {
      [FILTER_KEYS.HTTP_STATUSES]: filters[FILTER_KEYS.HTTP_STATUSES],
      [FILTER_KEYS.APPLICATIONS]: filters[FILTER_KEYS.APPLICATIONS],
    };
  }

  protected override getSpecificFilterReset(): Partial<ProxyFilters> {
    return {
      [FILTER_KEYS.HTTP_STATUSES]: null,
      [FILTER_KEYS.APPLICATIONS]: null,
    };
  }

  protected override removeSpecificFilter(key: string, value: string): void {
    if (key === FILTER_KEYS.HTTP_STATUSES) {
      const control = this.form.get(FILTER_KEYS.HTTP_STATUSES) as FormControl<string[] | null>;
      this.removeValueFromFilter(control.value, value, control);
    } else if (key === FILTER_KEYS.APPLICATIONS) {
      const control = this.form.get(FILTER_KEYS.APPLICATIONS) as FormControl<string[] | null>;
      this.removeValueFromFilter(control.value, value, control);
    }
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
            key: FILTER_KEYS.APPLICATIONS,
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
