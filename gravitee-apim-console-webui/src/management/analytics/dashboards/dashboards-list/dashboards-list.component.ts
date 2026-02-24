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
import { Dashboard } from '@gravitee/gravitee-dashboard';
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
import { MatIcon } from '@angular/material/icon';
import { startWith, switchMap, map, debounceTime, filter, scan, catchError } from 'rxjs/operators';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { MatDivider } from '@angular/material/list';
import { KeyValuePipe, DatePipe } from '@angular/common';
import { MatTooltip } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  TemplateSelectorDialogComponent,
  TemplateSelectorDialogResult,
} from './ui/template-selector-dialog/template-selector-dialog.component';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { DashboardService } from '../../data-access/dashboard.service';
import { PagedResult } from '../../../../entities/management-api-v2';
import { UsersService } from '../../../../services-ngx/users.service';

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
    MatTable,
    MatNoDataRow,
    MatMenuTrigger,
    MatDivider,
    KeyValuePipe,
    DatePipe,
    MatTooltip,
    RouterLink,
  ],
  templateUrl: './dashboards-list.component.html',
  styleUrls: ['./dashboards-list.component.scss'],
})
export class DashboardsListComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly usersService = inject(UsersService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  public displayedColumns = ['name', 'createdBy', 'lastModified', 'labels', 'actions'];

  public filters = signal<GioTableWrapperFilters>({
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  });

  private readonly dashboardsResource = toSignal(
    toObservable(this.filters).pipe(
      debounceTime(200),
      switchMap(filters =>
        this.dashboardService.list(filters.pagination.index, filters.pagination.size).pipe(
          switchMap(result => {
            const userIds = [...new Set((result.data ?? []).map(d => d.createdBy).filter((id): id is string => !!id))];
            if (userIds.length === 0) {
              return of({ isLoading: false as const, result, userNames: new Map<string, string>() });
            }
            return forkJoin(
              userIds.map(id =>
                this.usersService.get(id).pipe(
                  map(user => [id, (user.displayName ?? [user.firstname, user.lastname].filter(Boolean).join(' ')) || 'Unknown'] as const),
                  catchError(() => of([id, 'Unknown'] as const)),
                ),
              ),
            ).pipe(map(entries => ({ isLoading: false as const, result, userNames: new Map(entries) })));
          }),
          startWith({ isLoading: true as const }),
        ),
      ),
      scan((prev, curr) => ('result' in curr ? curr : { ...prev, isLoading: true }), {
        isLoading: true,
        result: undefined as PagedResult<Dashboard> | undefined,
        userNames: new Map<string, string>(),
      }),
    ),
    {
      initialValue: {
        isLoading: true,
        result: undefined as PagedResult<Dashboard> | undefined,
        userNames: new Map<string, string>(),
      },
    },
  );

  public dashboardItems = computed(() => this.dashboardsResource().result?.data ?? []);
  public totalCount = computed(() => this.dashboardsResource().result?.pagination?.totalCount ?? 0);
  public isLoading = computed(() => this.dashboardsResource().isLoading);
  public userNames = computed(() => this.dashboardsResource().userNames);

  public onFiltersChanged(event: GioTableWrapperFilters) {
    this.filters.update(f => ({ ...f, ...event }));
  }

  public openTemplateDialog(): void {
    this.dialog
      .open(TemplateSelectorDialogComponent, {
        panelClass: 'template-selector-dialog-panel',
        width: '1300px',
        maxWidth: '90vw',
        height: '720px',
        maxHeight: '80vh',
      })
      .afterClosed()
      .pipe(
        filter((result): result is TemplateSelectorDialogResult => !!result),
        switchMap(result => this.dashboardService.create(this.dashboardService.toCreateDashboard(result.template))),
      )
      .subscribe(dashboard => {
        this.router.navigate(['./', dashboard.id], { relativeTo: this.route });
      });
  }
}
