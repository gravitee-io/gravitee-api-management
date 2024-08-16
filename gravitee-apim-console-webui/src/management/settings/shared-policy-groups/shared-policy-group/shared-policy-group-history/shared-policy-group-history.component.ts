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
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, switchMap } from 'rxjs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { debounceTime, map, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

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

type PageTableVM = {
  items: {
    version: number;
    name: string;
    description: string;
    lifecycleState: SharedPolicyGroup['lifecycleState'];
    updatedAt: Date;
    deployedAt: Date;
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
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPolicyGroupHistoryComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly sharedPolicyGroupsService = inject(SharedPolicyGroupsService);

  protected sharedPolicyGroup = toSignal(this.sharedPolicyGroupsService.get(this.activatedRoute.snapshot.params.sharedPolicyGroupId));

  private refreshPageTableVM$ = new BehaviorSubject<void>(undefined);

  protected displayedColumns = ['version', 'name', 'lastDeploy', 'actions'];
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
          const items: PageTableVM['items'] = pagedResult.data.map((sharedPolicyGroup) => ({
            version: sharedPolicyGroup.version,
            name: sharedPolicyGroup.name,
            description: sharedPolicyGroup.description,
            lifecycleState: sharedPolicyGroup.lifecycleState,
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
}

const toSharedPolicyGroupHistoriesSortByParam = (sort: Sort): SharedPolicyGroupHistoriesSortByParam => {
  if (sort == null) {
    return undefined;
  }
  return ('desc' === sort.direction ? `-${sort.active}` : sort.active) as SharedPolicyGroupHistoriesSortByParam;
};
