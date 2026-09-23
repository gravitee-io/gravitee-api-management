/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, Signal, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, forkJoin, map, of, switchMap, tap } from 'rxjs';

import { workspaceListBreadcrumb } from './workspace-breadcrumbs';
import { AiWorkspaceCardComponent } from '../../../components/ai-workspace-card/ai-workspace-card.component';
import { CardsGridComponent } from '../../../components/cards-grid/cards-grid.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginationComponent } from '../../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../../components/search-bar/search-bar.component';
import { formatAimBudget } from '../../../entities/ai-workspace/aim-ai-workspace';
import { Subscription, SubscriptionMetadata, SubscriptionStatusEnum } from '../../../entities/subscription';
import { AimAiWorkspaceService } from '../../../services/aim-ai-workspace.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { parsePageParam, parseSizeParam } from '../../../utils/common.utils';

interface WorkspaceTileVM {
  id: string;
  subscriptionId: string;
  title: string;
  version?: string;
  description?: string;
  budgetLabel?: string;
}

interface WorkspacePaginatorVM {
  data: WorkspaceTileVM[];
  page: number;
  totalResults: number;
}

@Component({
  selector: 'app-workspaces',
  imports: [AiWorkspaceCardComponent, CardsGridComponent, LoaderComponent, PaginationComponent, SearchBarComponent],
  templateUrl: './workspaces.component.html',
  styleUrl: './workspaces.component.scss',
})
export default class WorkspacesComponent {
  private static readonly DEFAULT_PAGE = 1;
  private static readonly DEFAULT_PAGE_SIZE = 20;
  private static readonly MAX_PAGE_SIZE = 100;
  private static readonly ACTIVE_STATUSES: SubscriptionStatusEnum[] = [
    SubscriptionStatusEnum.ACCEPTED,
    SubscriptionStatusEnum.PAUSED,
    SubscriptionStatusEnum.PENDING,
  ];

  loadingPage = signal(true);
  pageSizeOptions = [8, 20, 40, 80];

  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly aimAiWorkspaceService = inject(AimAiWorkspaceService);
  private readonly router = inject(Router);
  private readonly breadcrumbService = inject(BreadcrumbService);

  private readonly queryParams = toSignal(this.activatedRoute.queryParams, { initialValue: {} as Record<string, unknown> });

  readonly searchQuery = computed(() => {
    const query = this.queryParams()['query'];
    return typeof query === 'string' ? query.trim() : '';
  });
  readonly currentPage = computed(() => parsePageParam(this.queryParams()['page'], WorkspacesComponent.DEFAULT_PAGE));
  readonly pageSize = computed(() =>
    parseSizeParam(this.queryParams()['size'], WorkspacesComponent.DEFAULT_PAGE_SIZE, WorkspacesComponent.MAX_PAGE_SIZE),
  );

  protected readonly workspacePaginator: Signal<WorkspacePaginatorVM> = toSignal(this.loadWorkspaces$(), {
    initialValue: { data: [], page: 1, totalResults: 0 },
  });

  readonly workspaceCountLabel = computed(() => {
    const count = this.workspacePaginator().totalResults;
    return count === 1
      ? $localize`:@@workspacesSingleResult:1 workspace`
      : $localize`:@@workspacesResultCount:${count}:count: workspaces`;
  });

  constructor() {
    this.breadcrumbService.set([workspaceListBreadcrumb()]);
  }

  onSearchTermChange(query: string) {
    this.updateQueryParams({ query: query.trim() || null, page: 1 });
  }

  onPageChange(page: number) {
    this.updateQueryParams({ page });
  }

  onPageSizeChange(size: number) {
    this.updateQueryParams({ size, page: 1 });
  }

  navigateToWorkspace(subscriptionId: string) {
    this.router.navigate(['/dashboard', 'workspaces', subscriptionId]);
  }

  private loadWorkspaces$() {
    return toObservable(this.queryParams).pipe(
      map(params => ({
        query: typeof params['query'] === 'string' ? params['query'].trim() : '',
        page: parsePageParam(params['page'], WorkspacesComponent.DEFAULT_PAGE),
        size: parseSizeParam(params['size'], WorkspacesComponent.DEFAULT_PAGE_SIZE, WorkspacesComponent.MAX_PAGE_SIZE),
      })),
      distinctUntilChanged((prev, curr) => prev.query === curr.query && prev.page === curr.page && prev.size === curr.size),
      tap(() => this.loadingPage.set(true)),
      switchMap(({ query, page, size }) =>
        this.subscriptionService
          .list({
            referenceTypes: ['API_PRODUCT'],
            statuses: WorkspacesComponent.ACTIVE_STATUSES,
            size: -1,
          })
          .pipe(
            switchMap(response => this.toWorkspaceTiles(response.data ?? [], response.metadata)),
            map(workspaces => this.filterWorkspaces(workspaces, query)),
            map(workspaces => this.paginate(workspaces, page, size)),
            catchError(() => of({ data: [], page, totalResults: 0 })),
          ),
      ),
      tap(() => this.loadingPage.set(false)),
    );
  }

  private filterWorkspaces(workspaces: WorkspaceTileVM[], query: string): WorkspaceTileVM[] {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return workspaces;
    }
    return workspaces.filter(workspace => {
      const haystack = [workspace.title, workspace.version, workspace.description, workspace.budgetLabel]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(normalized);
    });
  }

  private toWorkspaceTiles(subscriptions: Subscription[], metadata?: SubscriptionMetadata) {
    const uniqueByProduct = new Map<string, Subscription>();
    for (const subscription of subscriptions) {
      if (!subscription.reference_id || uniqueByProduct.has(subscription.reference_id)) {
        continue;
      }
      uniqueByProduct.set(subscription.reference_id, subscription);
    }

    const entries = [...uniqueByProduct.entries()];
    if (entries.length === 0) {
      return of([] as WorkspaceTileVM[]);
    }

    return forkJoin(
      entries.map(([workspaceId, subscription]) =>
        forkJoin({
          workspace: this.aimAiWorkspaceService.getById(workspaceId).pipe(catchError(() => of(null))),
          budgets: this.aimAiWorkspaceService.listBudgets(workspaceId).pipe(catchError(() => of([]))),
        }).pipe(
          map(({ workspace, budgets }) => {
            if (!workspace) {
              return this.mapFallbackTile(workspaceId, subscription, metadata);
            }
            const allotted = budgets.find(budget => budget.id === subscription.plan);
            return {
              id: workspace.id,
              subscriptionId: subscription.id,
              title: workspace.name,
              version: workspace.version,
              description: workspace.description ?? undefined,
              budgetLabel: formatAimBudget(allotted) ?? undefined,
            } satisfies WorkspaceTileVM;
          }),
        ),
      ),
    );
  }

  private mapFallbackTile(
    workspaceId: string,
    subscription: Subscription,
    metadata?: SubscriptionMetadata,
  ): WorkspaceTileVM {
    const productMeta = metadata?.[workspaceId];
    return {
      id: workspaceId,
      subscriptionId: subscription.id,
      title: productMeta?.name ?? $localize`:@@unavailableWorkspace:Unavailable workspace`,
      version: productMeta?.apiVersion,
    };
  }

  private paginate(workspaces: WorkspaceTileVM[], page: number, size: number): WorkspacePaginatorVM {
    const totalResults = workspaces.length;
    const start = (page - 1) * size;
    return {
      data: workspaces.slice(start, start + size),
      page,
      totalResults,
    };
  }

  private updateQueryParams(queryParams: Record<string, unknown>) {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }
}
