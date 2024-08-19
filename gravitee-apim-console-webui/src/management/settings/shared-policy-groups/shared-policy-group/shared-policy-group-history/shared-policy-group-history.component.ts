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
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, switchMap } from 'rxjs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { debounceTime, filter, map, tap } from 'rxjs/operators';
import { isEqual, isNil } from 'lodash';
import { MatIcon } from '@angular/material/icon';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { MatCheckbox } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';

import {
  HistoryJsonDialogComponent,
  HistoryJsonDialogData,
  HistoryJsonDialogResult,
} from './history-json-dialog/history-json-dialog.component';
import {
  HistoryStudioDialogComponent,
  HistoryStudioDialogData,
  HistoryStudioDialogResult,
} from './history-studio-dialog/history-studio-dialog.component';
import {
  HistoryCompareDialogComponent,
  HistoryCompareDialogData,
  HistoryCompareDialogResult,
} from './history-compare-dialog/history-compare-dialog.component';

import { GioTableWrapperFilters, Sort } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SharedPolicyGroupsStateBadgeComponent } from '../../shared-policy-groups-state-badge/shared-policy-groups-state-badge.component';
import { SharedPolicyGroupsService } from '../../../../../services-ngx/shared-policy-groups.service';
import {
  SharedPolicyGroup,
  SharedPolicyGroupHistoriesSortByParam,
  toReadableExecutionPhase,
} from '../../../../../entities/management-api-v2';
import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

type PageTableVM = {
  items: {
    _id: string;
    selected: boolean;
    sharedPolicyGroup: SharedPolicyGroup;
  }[];
  totalItems: number;
  isLoading: boolean;
};

@Component({
  selector: 'shared-policy-group-history',
  templateUrl: './shared-policy-group-history.component.html',
  styleUrls: ['./shared-policy-group-history.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTooltipModule,
    MatTableModule,
    MatSortModule,
    GioPermissionModule,
    GioTableWrapperModule,
    SharedPolicyGroupsStateBadgeComponent,
    MatIcon,
    MatCheckbox,
    FormsModule,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPolicyGroupHistoryComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sharedPolicyGroupsService = inject(SharedPolicyGroupsService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);

  protected sharedPolicyGroup = toSignal(this.sharedPolicyGroupsService.get(this.activatedRoute.snapshot.params.sharedPolicyGroupId));

  private refreshPageTableVM$ = new BehaviorSubject<void>(undefined);

  protected displayedColumns = ['checkbox', 'version', 'name', 'lastDeploy', 'actions'];
  protected filters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 25,
    },
  };
  protected pageTableVM$: BehaviorSubject<PageTableVM> = new BehaviorSubject({
    items: [],
    totalItems: 0,
    isLoading: true,
  });
  protected readonly toReadableExecutionPhase = toReadableExecutionPhase;
  protected compareSPG?: [PageTableVM['items'][number], PageTableVM['items'][number]] = [null, null];
  protected get compareTwoSPGLabel(): string {
    if (this.compareSPG[0] === null) {
      return 'Select two versions to compare';
    }
    if (this.compareSPG[1] === null) {
      return `Select another version to compare`;
    }
    return `Compare version ${this.compareSPG[0].sharedPolicyGroup.version} with ${this.compareSPG[1].sharedPolicyGroup.version}`;
  }
  protected get disableCompareTwoSPG(): boolean {
    return this.compareSPG.filter((e) => !isNil(e)).length < 2;
  }
  protected get comparePendingSPGLabel(): string {
    const lastSelected = this.compareSPG.filter((e) => !isNil(e))?.pop();
    return lastSelected
      ? `Compare version ${lastSelected.sharedPolicyGroup.version} with Pending`
      : 'Select a version to compare with Pending version';
  }
  protected get disableComparePendingSPG(): boolean {
    return this.compareSPG.filter((e) => !isNil(e)).length < 1;
  }

  ngOnInit() {
    this.refreshPageTableVM$
      .pipe(
        debounceTime(200),
        tap(() => this.pageTableVM$.next({ items: [], totalItems: 0, isLoading: true })),
        switchMap(() =>
          this.sharedPolicyGroupsService.listHistories(
            this.activatedRoute.snapshot.params.sharedPolicyGroupId,
            toSharedPolicyGroupHistoriesSortByParam(this.filters.sort),
            this.filters.pagination.index,
            this.filters.pagination.size,
          ),
        ),
        map((pagedResult) => {
          const items: PageTableVM['items'] = pagedResult.data.map((sharedPolicyGroup) => {
            const id = `${sharedPolicyGroup.version}-${sharedPolicyGroup.updatedAt.toString()}`;

            return {
              _id: id,
              selected: this.getSelected(id),
              sharedPolicyGroup,
            };
          });

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

  protected onShowJsonSource(sharedPolicyGroup: SharedPolicyGroup): void {
    this.matDialog
      .open<HistoryJsonDialogComponent, HistoryJsonDialogData, HistoryJsonDialogResult>(HistoryJsonDialogComponent, {
        data: {
          sharedPolicyGroup,
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
      })
      .afterClosed()
      .subscribe();
  }

  protected onShowDetailsOrRestore(sharedPolicyGroup: SharedPolicyGroup): void {
    this.matDialog
      .open<HistoryStudioDialogComponent, HistoryStudioDialogData, HistoryStudioDialogResult>(HistoryStudioDialogComponent, {
        data: {
          sharedPolicyGroup,
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => result === 'RESTORE_VERSION'),
        switchMap(() => this.sharedPolicyGroupsService.restore(sharedPolicyGroup)),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Version has been restored. Review changes and click ‘Deploy’ to finalize the restoration.');
          this.router.navigate(['../'], { relativeTo: this.activatedRoute });
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group restore!');
        },
      });
  }

  protected openCompareTwoSPGDialog() {
    this.matDialog
      .open<HistoryCompareDialogComponent, HistoryCompareDialogData, HistoryCompareDialogResult>(HistoryCompareDialogComponent, {
        data: {
          left: this.compareSPG[0]?.sharedPolicyGroup,
          right: this.compareSPG[1]?.sharedPolicyGroup,
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
      })
      .afterClosed()
      .subscribe();
  }

  protected openComparePendingSPGDialog() {
    const spg = this.sharedPolicyGroup();
    this.matDialog
      .open<HistoryCompareDialogComponent, HistoryCompareDialogData, HistoryCompareDialogResult>(HistoryCompareDialogComponent, {
        data: {
          left: this.compareSPG[0]?.sharedPolicyGroup,
          right: spg,
          rightIsPending: true,
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
      })
      .afterClosed()
      .subscribe();
  }

  protected selectRow(spg: PageTableVM['items'][number]) {
    if (this.compareSPG[0] === null) {
      this.compareSPG[0] = spg;
    } else if (this.compareSPG[0]._id === spg._id) {
      this.compareSPG = [this.compareSPG[1], null];
    } else if (this.compareSPG[1] !== null && this.compareSPG[1]._id === spg._id) {
      this.compareSPG[1] = null;
    } else {
      this.compareSPG[1] = spg;
    }

    this.pageTableVM$.next({
      ...this.pageTableVM$.value,
      items: this.pageTableVM$.getValue().items.map((spg) => {
        spg.selected = this.getSelected(spg._id);
        return spg;
      }),
    });
  }

  private getSelected(spgId: string): boolean {
    return this.compareSPG
      .filter((e) => !isNil(e))
      .map((e) => e._id)
      .includes(spgId);
  }
}

const toSharedPolicyGroupHistoriesSortByParam = (sort: Sort): SharedPolicyGroupHistoriesSortByParam => {
  if (sort == null) {
    return undefined;
  }
  return ('desc' === sort.direction ? `-${sort.active}` : sort.active) as SharedPolicyGroupHistoriesSortByParam;
};
