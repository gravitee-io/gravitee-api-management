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
import {
  AddFilterDialogComponent,
  AddFilterDialogData,
  customTimeFrames,
  Dashboard,
  DashboardCapabilities,
  DEFAULT_CAPABILITIES,
  DynamicFilterBarComponent,
  FilterCondition,
  GraviteeDashboardComponent,
  provideFilterDefinitions,
  provideFilterValues,
  SaveState,
  TimeframeSelectorComponent,
  timeFrames,
} from '@gravitee/gravitee-dashboard';

import { Component, computed, DestroyRef, inject, Injector, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';

import { DashboardFiltersStore } from './dashboard-filters.store';
import { FilterLabelResolver } from './filter-label.resolver';

import { ObservabilityFiltersApiService } from '../../../data-access/observability-filters-api.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../../entities/Constants';

@Component({
  selector: 'dashboard-viewer',
  imports: [GraviteeDashboardComponent, DynamicFilterBarComponent, TimeframeSelectorComponent, ReactiveFormsModule],
  templateUrl: './dashboard-viewer.component.html',
  styleUrl: './dashboard-viewer.component.scss',
  providers: [
    DashboardFiltersStore,
    ObservabilityFiltersApiService,
    FilterLabelResolver,
    provideFilterDefinitions(ObservabilityFiltersApiService),
    provideFilterValues(ObservabilityFiltersApiService),
  ],
})
export class DashboardViewerComponent {
  dashboard = input.required<Dashboard>();
  capabilities = input<DashboardCapabilities>(DEFAULT_CAPABILITIES);
  showTitle = input<boolean>(true);

  readonly deleteRequested = output<void>();
  readonly nameChanged = output<string>();
  readonly saveStateChange = output<SaveState>();

  readonly baseURL = inject(Constants).env.v2BaseURL;

  readonly filtersStore = inject(DashboardFiltersStore);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);

  readonly canEditFilters = computed(() => this.permissionService.hasAnyMatching(['environment-dashboard-u']));

  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  openAddFilter(): void {
    if (!this.canEditFilters()) return;
    const { from, to } = this.filtersStore.timeRangeEpoch();
    this.dialog
      .open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
        data: { timeFrom: from, timeTo: to },
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
    if (!this.canEditFilters()) return;
    const { from, to } = this.filtersStore.timeRangeEpoch();
    this.dialog
      .open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
        data: { existingCondition: condition, timeFrom: from, timeTo: to },
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
