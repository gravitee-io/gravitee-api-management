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
import { Component, Signal, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { workspaceListBreadcrumb } from './workspace-breadcrumbs';
import { WorkspaceCardComponent } from './workspace-card/workspace-card.component';
import { CardsGridComponent } from '../../../components/cards-grid/cards-grid.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginationComponent } from '../../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../../components/search-bar/search-bar.component';
import { AiWorkspaceBudget, AiWorkspaceSummary } from '../../../entities/ai-workspace/ai-workspace';
import { AiWorkspaceService } from '../../../services/ai-workspace.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';

export interface WorkspacePaginatorVM {
  data: {
    id: string;
    name: string;
    description?: string;
    budget?: AiWorkspaceBudget;
  }[];
  page: number;
  totalResults: number;
}

@Component({
  selector: 'app-workspaces',
  imports: [WorkspaceCardComponent, CardsGridComponent, PaginationComponent, SearchBarComponent, LoaderComponent],
  templateUrl: './workspaces.component.html',
  styleUrl: './workspaces.component.scss',
})
export default class WorkspacesComponent {
  loadingPage = signal(true);
  loadError = signal(false);
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];

  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly aiWorkspaceService = inject(AiWorkspaceService);
  private readonly router = inject(Router);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly page$ = new BehaviorSubject<number>(1);
  private readonly search$ = new BehaviorSubject<string>('');

  readonly workspacePaginator: Signal<WorkspacePaginatorVM> = toSignal(this.loadWorkspaces$(), {
    initialValue: { data: [], page: 1, totalResults: 0 },
  });

  constructor() {
    this.breadcrumbService.set([workspaceListBreadcrumb()]);
  }

  onSearchTermChange(term: string) {
    this.search$.next(term);
    this.page$.next(1);
  }

  onPageChange(page: number) {
    this.page$.next(page);
  }

  onPageSizeChange(newPageSize: number) {
    this.pageSize = newPageSize;
    this.page$.next(1);
  }

  navigateToWorkspace(id: string) {
    this.router.navigate([id], { relativeTo: this.activatedRoute });
  }

  private loadWorkspaces$() {
    return combineLatest([this.page$, this.search$]).pipe(
      map(([currentPage, query]) => ({ currentPage, pageSize: this.pageSize, query })),
      distinctUntilChanged(
        (prev, curr) => prev.currentPage === curr.currentPage && prev.pageSize === curr.pageSize && prev.query === curr.query,
      ),
      tap(() => {
        this.loadingPage.set(true);
        this.loadError.set(false);
      }),
      switchMap(({ currentPage, pageSize, query }) =>
        this.aiWorkspaceService.list(currentPage, pageSize, query).pipe(
          map(resp => {
            const data: WorkspacePaginatorVM['data'] = (resp.data ?? []).map((workspace: AiWorkspaceSummary) => ({
              id: workspace.id,
              name: workspace.name,
              description: workspace.description,
              budget: workspace.budget,
            }));
            const page = resp.metadata?.pagination?.current_page ?? 1;
            const totalResults = resp.metadata?.pagination?.total ?? data.length;
            return { data, page, totalResults };
          }),
          catchError(() => {
            this.loadError.set(true);
            return of({ data: [], page: 1, totalResults: 0 });
          }),
        ),
      ),
      tap(() => this.loadingPage.set(false)),
    );
  }
}
