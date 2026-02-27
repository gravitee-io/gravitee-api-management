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
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, distinctUntilChanged, map, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApplicationCardComponent } from '../../components/application-card/application-card.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { ApplicationService } from '../../services/application.service';
import { CurrentUserService } from '../../services/current-user.service';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { CardsGridComponent } from 'src/components/cards-grid/cards-grid.component';

export interface ApplicationPaginatorVM {
  data: {
    id: string;
    name: string;
    description?: string;
  }[];
  page: number;
  totalResults: number;
}

@Component({
  selector: 'app-applications',
  imports: [
    ApplicationCardComponent,
    CardsGridComponent,
    MatButton,
    MatFormFieldModule,
    MatIcon,
    MatSelectModule,
    PaginationComponent,
    RouterLink,
  ],
  templateUrl: './applications.component.html',
  styleUrl: './applications.component.scss',
})
export class ApplicationsComponent {
  currentUser = inject(CurrentUserService).user;

  loadingPage: boolean = true;
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];

  canCreate = computed(() => this.currentUser().permissions?.APPLICATION?.includes('C') || false);
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;

  protected readonly applicationPaginator: ReturnType<typeof toSignal<ApplicationPaginatorVM, ApplicationPaginatorVM>>;
  private readonly applicationService = inject(ApplicationService);
  private readonly router = inject(Router);
  private readonly page$ = new BehaviorSubject<number>(1);

  constructor() {
    this.applicationPaginator = toSignal(this.loadApplications$(), { initialValue: { data: [], page: 1, totalResults: 0 } });
  }

  onPageChange(page: number) {
    this.page$.next(page);
  }

  onPageSizeChange(newPageSize: number) {
    this.pageSize = newPageSize;
    this.page$.next(1);
  }

  navigateToApplication(id: string) {
    this.router.navigate(['/applications', id]);
  }

  private loadApplications$() {
    return this.page$.pipe(
      map(currentPage => ({ currentPage, pageSize: this.pageSize })),
      distinctUntilChanged((prev, curr) => prev.currentPage === curr.currentPage && prev.pageSize === curr.pageSize),
      tap(_ => (this.loadingPage = true)),
      switchMap(({ currentPage, pageSize }) => this.applicationService.list(currentPage, pageSize)),
      map(resp => {
        const data = resp.data
          ? resp.data.map(application => ({
              id: application.id,
              name: application.name,
              description: application.description,
            }))
          : [];
        const page = resp.metadata?.pagination?.current_page ?? 1;
        const totalResults = resp.metadata?.pagination?.total ?? 0;
        return { data, page, totalResults };
      }),
      catchError(_ => of({ data: [], page: 1, totalResults: 0 })),
      tap(_ => (this.loadingPage = false)),
    );
  }
}
