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
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { BehaviorSubject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, filter, map, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';

import {
  SharedPolicyGroupsAddEditDialogComponent,
  SharedPolicyGroupAddEditDialogData,
  SharedPolicyGroupAddEditDialogResult,
} from './shared-policy-groups-add-edit-dialog/shared-policy-groups-add-edit-dialog.component';
import { SharedPolicyGroupsStateBadgeComponent } from './shared-policy-groups-state-badge/shared-policy-groups-state-badge.component';

import { SharedPolicyGroupsService } from '../../../services-ngx/shared-policy-groups.service';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { ApiV4, SharedPolicyGroup, SharedPolicyGroupsSortByParam, toReadableExecutionPhase } from '../../../entities/management-api-v2';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

type PageTableVM = {
  items: {
    id: string;
    name: string;
    description: string;
    lifecycleState: SharedPolicyGroup['lifecycleState'];
    apiType: string;
    phase: string;
    updatedAt: Date;
    deployedAt: Date;
  }[];
  totalItems: number;
  isLoading: boolean;
};

@Component({
  selector: 'shared-policy-groups',
  templateUrl: './shared-policy-groups.component.html',
  styleUrls: ['./shared-policy-groups.component.scss'],
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
    SharedPolicyGroupsStateBadgeComponent,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPolicyGroupsComponent implements OnInit {
  private readonly sharedPolicyGroupsService = inject(SharedPolicyGroupsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private refreshPageTableVM$ = new BehaviorSubject<void>(undefined);

  protected displayedColumns: string[] = ['name', 'apiType', 'phase', 'lastUpdate', 'lastDeploy', 'actions'];
  protected filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };

  protected pageTableVM$: BehaviorSubject<PageTableVM> = new BehaviorSubject({
    items: [],
    totalItems: 0,
    isLoading: true,
  });

  protected isReadOnly = !this.permissionService.hasAnyMatching(['environment-shared_policy_group-u']);

  ngOnInit(): void {
    this.refreshPageTableVM$
      .pipe(
        debounceTime(200),
        tap(() => this.pageTableVM$.next({ items: [], totalItems: 0, isLoading: true })),
        switchMap(() =>
          this.sharedPolicyGroupsService.list(
            this.filters.searchTerm,
            toSharedPolicyGroupsSortByParam(this.filters.sort),
            this.filters.pagination.index,
            this.filters.pagination.size,
          ),
        ),
        map((pagedResult) => {
          const items = pagedResult.data.map((sharedPolicyGroup) => ({
            id: sharedPolicyGroup.id,
            name: sharedPolicyGroup.name,
            description: sharedPolicyGroup.description,
            lifecycleState: sharedPolicyGroup.lifecycleState,
            apiType: sharedPolicyGroup.apiType,
            phase: toReadableExecutionPhase(sharedPolicyGroup.phase),
            updatedAt: sharedPolicyGroup.updatedAt,
            deployedAt: sharedPolicyGroup.deployedAt,
          }));

          this.pageTableVM$.next({
            items,
            totalItems: pagedResult.pagination.totalCount ?? 0,
            isLoading: false,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected onFiltersChanged($event: GioTableWrapperFilters) {
    if (isEqual(this.filters, $event)) {
      return;
    }
    this.filters = $event;
    this.refreshPageTableVM$.next();
  }

  protected onAddEnvironmentFlow(apiType: ApiV4['type']) {
    return this.matDialog
      .open<SharedPolicyGroupsAddEditDialogComponent, SharedPolicyGroupAddEditDialogData, SharedPolicyGroupAddEditDialogResult>(
        SharedPolicyGroupsAddEditDialogComponent,
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
          this.sharedPolicyGroupsService.create({
            name: payload.name,
            description: payload.description,
            apiType,
            phase: payload.phase,
          }),
        ),
      )
      .subscribe({
        next: (sharedPolicyGroup) => {
          this.snackBarService.success('Shared Policy Group created');
          this.router.navigate([sharedPolicyGroup.id, 'studio'], { relativeTo: this.activatedRoute });
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group creation!');
        },
      });
  }

  protected onEdit(sharedPolicyGroupId: string) {
    this.router.navigate([sharedPolicyGroupId, 'studio'], { relativeTo: this.activatedRoute });
  }

  protected onRemove(sharedPolicyGroupId: string) {
    removeSharedPolicyGroup(this.matDialog, this.snackBarService, this.sharedPolicyGroupsService, sharedPolicyGroupId, () => {
      this.refreshPageTableVM$.next();
    });
  }
}

export const toSharedPolicyGroupsSortByParam = (sort: Sort): SharedPolicyGroupsSortByParam => {
  if (sort == null) {
    return undefined;
  }
  return ('desc' === sort.direction ? `-${sort.active}` : sort.active) as SharedPolicyGroupsSortByParam;
};

export const removeSharedPolicyGroup = (
  matDialog: MatDialog,
  snackBarService: SnackBarService,
  sharedPolicyGroupsService: SharedPolicyGroupsService,
  sharedPolicyGroupId: string,
  onSuccess: () => void = () => {},
) => {
  matDialog
    .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
      data: {
        title: 'Remove Shared Policy Group',
        content: `Are you sure you want to remove this Shared Policy Group?<br>
If this Shared Policy Group is used in API flows, be sure to inform API publishers before making this change.<br>
If an API flow still uses this Shared Policy Group, the API flow will ignore it and continue to run.`,

        confirmButton: 'Remove',
      },
      role: 'alertdialog',
      id: 'remove-spg-dialog',
      width: GIO_DIALOG_WIDTH.MEDIUM,
    })
    .afterClosed()
    .pipe(
      filter((result) => !!result),
      switchMap(() => sharedPolicyGroupsService.delete(sharedPolicyGroupId)),
    )
    .subscribe({
      next: () => {
        snackBarService.success('Shared Policy Group removed');
        onSuccess();
      },
      error: (e) => {
        snackBarService.error(e.error?.message ?? 'An error occurred while removing the Shared Policy Group');
        throw e;
      },
    });
};
