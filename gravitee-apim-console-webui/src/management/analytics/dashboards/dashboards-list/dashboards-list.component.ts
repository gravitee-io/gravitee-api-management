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
import { Component, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GioActionMenuComponent, GioActionMenuItemComponent } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatIcon } from '@angular/material/icon';
import { startWith, switchMap, map, debounceTime } from 'rxjs/operators';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatDivider } from '@angular/material/list';
import { KeyValuePipe } from '@angular/common';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { TemplateSelectorDialogComponent } from './ui/template-selector-dialog/template-selector-dialog.component';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { DashboardService } from '../../data-access/dashboard.service';

@Component({
  selector: 'dashboards-list',
  imports: [
    GioActionMenuComponent,
    GioActionMenuItemComponent,
    GioTableWrapperModule,
    MatButton,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatRow,
    MatRowDef,
    MatSort,
    MatSortHeader,
    MatTable,
    MatNoDataRow,
    MatMenuTrigger,
    MatDivider,
    KeyValuePipe,
    MatTooltip,
    RouterLink,
  ],
  templateUrl: './dashboards-list.component.html',
  styleUrls: ['./dashboards-list.component.scss'],
})
export class DashboardsListComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly dialog = inject(MatDialog);

  public displayedColumns = ['name', 'createdBy', 'lastUpdated', 'labels', 'actions'];

  public filters = signal<GioTableWrapperFilters>({
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  });

  private readonly dashboardsResource = toSignal(
    toObservable(this.filters).pipe(
      debounceTime(200),
      switchMap(filters =>
        this.dashboardService.list(filters.searchTerm, filters.sort, filters.pagination.index, filters.pagination.size).pipe(
          map(result => ({ isLoading: false, result })),
          startWith({ isLoading: true, result: undefined }),
        ),
      ),
    ),
    { initialValue: { isLoading: true, result: undefined } },
  );

  public dashboardItems = computed(() => this.dashboardsResource().result?.data ?? []);
  public isLoading = computed(() => this.dashboardsResource().isLoading);

  public onFiltersChanged(event: GioTableWrapperFilters) {
    this.filters.update(f => ({ ...f, ...event }));
  }

  public onSortChanged(sort: { active: string; direction: string }) {
    this.filters.update(f => ({ ...f, sort: { active: sort.active, direction: sort.direction as 'asc' | 'desc' } }));
  }

  public openTemplateDialog(): void {
    this.dialog.open(TemplateSelectorDialogComponent, {
      panelClass: 'template-selector-dialog-panel',
      width: '1300px',
      maxWidth: '90vw',
      height: '720px',
      maxHeight: '80vh',
    });
  }
}
