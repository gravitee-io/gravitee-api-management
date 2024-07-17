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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { BehaviorSubject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';

import {
  EnvironmentFlowsAddEditDialogComponent,
  EnvironmentFlowsAddEditDialogData,
  EnvironmentFlowsAddEditDialogResult,
} from './environment-flows-add-edit-dialog/environment-flows-add-edit-dialog.component';

import { EnvironmentFlowsService } from '../../../services-ngx/environment-flows.service';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { ApiV4, EnvironmentFlowsSortByParam } from '../../../entities/management-api-v2';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

type PageTableVM = {
  items: {
    id: string;
    name: string;
    description: string;
    updatedAt: Date;
    deployedAt: Date;
  }[];
  totalItems: number;
  isLoading: boolean;
};

@Component({
  selector: 'environment-flows',
  templateUrl: './environment-flows.component.html',
  styleUrls: ['./environment-flows.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTabsModule,
    MatTooltipModule,
    MatSortModule,
    MatTableModule,
    MatMenuModule,
    MatSnackBarModule,
    GioIconsModule,
    GioPermissionModule,
    GioTableWrapperModule,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnvironmentFlowsComponent implements OnInit {
  protected displayedColumns: string[] = ['name', 'phase', 'lastUpdate', 'lastDeploy', 'actions'];
  protected filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };

  protected pageTableVM$: BehaviorSubject<PageTableVM> = new BehaviorSubject({
    items: [],
    totalItems: 0,
    isLoading: true,
  });

  protected isReadOnly = false;

  private readonly environmentFlowsService = inject(EnvironmentFlowsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private refreshPageTableVM$ = new BehaviorSubject<void>(undefined);

  ngOnInit(): void {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-environment_flows-r']);
    this.refreshPageTableVM$
      .pipe(
        tap(() => this.pageTableVM$.next({ items: [], totalItems: 0, isLoading: true })),
        switchMap(() =>
          this.environmentFlowsService.list(
            this.filters.searchTerm,
            toEnvironmentFlowsSortByParam(this.filters.sort),
            this.filters.pagination.index,
            this.filters.pagination.size,
          ),
        ),
        map((pagedResult) => {
          const items = pagedResult.data.map((environmentFlow) => ({
            id: environmentFlow.id,
            name: environmentFlow.name,
            description: environmentFlow.description,
            phase: environmentFlow.phase,
            updatedAt: environmentFlow.updatedAt,
            deployedAt: environmentFlow.deployedAt,
          }));

          this.pageTableVM$.next({
            items,
            totalItems: pagedResult.pagination.totalCount,
            isLoading: false,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected onFiltersChanged($event: GioTableWrapperFilters) {
    this.filters = $event;
    this.refreshPageTableVM$.next();
  }

  protected onAddEnvironmentFlow(apiType: ApiV4['type']) {
    return this.matDialog
      .open<EnvironmentFlowsAddEditDialogComponent, EnvironmentFlowsAddEditDialogData, EnvironmentFlowsAddEditDialogResult>(
        EnvironmentFlowsAddEditDialogComponent,
        {
          data: { apiType },
          role: 'dialog',
          id: 'test-story-dialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((payload) =>
          this.environmentFlowsService.create({
            name: payload.name,
            description: payload.description,
            apiType,
            phase: payload.phase,
          }),
        ),
      )
      .subscribe({
        next: (environmentFlow) => {
          this.snackBarService.success('Environment flow created');
          this.router.navigate([environmentFlow.id, 'studio'], { relativeTo: this.activatedRoute });
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during environment flow creation!');
        },
      });
  }

  protected onEdit(environmentFlowId: string) {
    this.router.navigate([environmentFlowId, 'studio'], { relativeTo: this.activatedRoute });
  }
}

export const toEnvironmentFlowsSortByParam = (sort: Sort): EnvironmentFlowsSortByParam => {
  if (sort == null) {
    return undefined;
  }
  return ('desc' === sort.direction ? `-${sort.active}` : sort.active) as EnvironmentFlowsSortByParam;
};
