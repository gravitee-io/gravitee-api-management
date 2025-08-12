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
import { GIO_DIALOG_WIDTH, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { BehaviorSubject, EMPTY, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, map, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { get, isEqual } from 'lodash';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ClustersAddDialogComponent, ClustersAddDialogData, ClustersAddDialogResult } from '../add-dialog/clusters-add-dialog.component';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ClustersService } from '../../../services-ngx/clusters.service';
import { ClustersSortByParam } from '../../../entities/management-api-v2';

type PageTableVM = {
  items: {
    id: string;
    name: string;
    bootstrapServer: string;
    security: string;
    updatedAt: Date;
  }[];
  totalItems: number;
  isLoading: boolean;
};

@Component({
  selector: 'clusters-list-page',
  templateUrl: './list-page.component.html',
  styleUrls: ['./list-page.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTooltipModule,
    MatTableModule,
    MatSortModule,
    GioIconsModule,
    GioPermissionModule,
    GioTableWrapperModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class ClustersListPageComponent implements OnInit {
  private readonly clustersService = inject(ClustersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private refreshPageTableVM$ = new BehaviorSubject<void>(undefined);

  protected displayedColumns: string[] = ['name', 'bootstrapServer', 'security', 'actions'];
  protected filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };

  protected pageTableVM$: BehaviorSubject<PageTableVM> = new BehaviorSubject({
    items: [],
    totalItems: 0,
    isLoading: true,
  });

  // TODO: When permissions are implemented
  protected isReadOnly = false;

  ngOnInit(): void {
    this.refreshPageTableVM$
      .pipe(
        debounceTime(200),
        tap(() => this.pageTableVM$.next({ items: [], totalItems: 0, isLoading: true })),
        switchMap(() =>
          this.clustersService.list(
            this.filters.searchTerm,
            toClustersSortByParam(this.filters.sort),
            this.filters.pagination.index,
            this.filters.pagination.size,
          ),
        ),
        map((pagedResult) => {
          const items = pagedResult.data.map((cluster) => ({
            id: cluster.id,
            name: cluster.name,
            bootstrapServer: cluster.configuration.bootstrapServers,
            security: get(cluster.configuration, 'security.protocol', 'PLAINTEXT') as string,
            updatedAt: cluster.updatedAt,
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

  protected addCluster() {
    this.matDialog
      .open<ClustersAddDialogComponent, ClustersAddDialogData, ClustersAddDialogResult>(ClustersAddDialogComponent, {
        role: 'alertdialog',
        id: 'addCluster',
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        switchMap((result: ClustersAddDialogResult) => {
          if (!result) {
            return EMPTY;
          }

          return this.clustersService.create({
            name: result.name,
            description: result.description,
            configuration: {
              bootstrapServers: result.bootstrapServers,
            },
          });
        }),
        tap((cluster) => {
          this.snackBarService.success('Cluster created successfully');
          return this.router.navigate([cluster.id], {
            relativeTo: this.activatedRoute,
          });
        }),
        catchError((e) => {
          this.snackBarService.error(e.error?.message ?? 'An error occurred while creating the cluster!');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  protected remove(clusterId: string) {
    // TODO
    // eslint-disable-next-line
    console.log('Remove cluster button clicked for cluster ID:', clusterId);
  }
}

export const toClustersSortByParam = (sort: Sort): ClustersSortByParam => {
  if (sort == null) {
    return undefined;
  }
  switch (sort.active) {
    case 'name':
      return sort.direction === 'desc' ? '-name' : 'name';
  }
};
