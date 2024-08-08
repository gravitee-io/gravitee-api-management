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
import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { BehaviorSubject, map, Observable, scan, switchMap, tap } from 'rxjs';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { ApplicationCardComponent } from '../../components/application-card/application-card.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Application } from '../../entities/application/application';
import { ApplicationService } from '../../services/application.service';
import { ConfigService } from '../../services/config.service';

export interface ApplicationPaginatorVM {
  data: Application[];
  page: number;
  hasNextPage: boolean;
}

@Component({
  selector: 'app-applications',
  standalone: true,
  imports: [ApiCardComponent, AsyncPipe, InfiniteScrollModule, LoaderComponent, MatCard, MatCardContent, ApplicationCardComponent],
  templateUrl: './applications.component.html',
  styleUrl: './applications.component.scss',
})
export class ApplicationsComponent {
  applicationPaginator$: Observable<ApplicationPaginatorVM>;
  loadingPage$ = new BehaviorSubject(true);

  private applicationService = inject(ApplicationService);
  private page$ = new BehaviorSubject(1);

  constructor(private configService: ConfigService) {
    this.applicationPaginator$ = this.loadApplications$();
  }

  loadMoreApplications(paginator: ApplicationPaginatorVM) {
    if (!paginator.hasNextPage) {
      return;
    }

    this.page$.next(paginator.page + 1);
  }

  private loadApplications$(): Observable<ApplicationPaginatorVM> {
    return this.page$.pipe(
      tap(_ => this.loadingPage$.next(true)),
      switchMap(currentPage => this.applicationService.list({ page: currentPage, size: 9 })),
      map(resp => {
        const data = resp.data
          ? resp.data.map(application => ({
              id: application.id,
              description: application.description,
              name: application.name,
              picture: application._links?.picture,
            }))
          : [];
        const page = resp.metadata?.pagination?.current_page ?? 1;
        const hasNextPage = resp.metadata?.pagination?.total_pages ? page < resp.metadata.pagination.total_pages : false;
        return {
          data,
          page,
          hasNextPage,
        };
      }),
      scan(this.updatePaginator, { data: [], page: 1, hasNextPage: true }),
      tap(_ => this.loadingPage$.next(false)),
    );
  }

  private updatePaginator(accumulator: ApplicationPaginatorVM, value: ApplicationPaginatorVM): ApplicationPaginatorVM {
    if (value.page === 1) {
      return value;
    }

    accumulator.data.push(...value.data);
    accumulator.page = value.page;
    accumulator.hasNextPage = value.hasNextPage;

    return accumulator;
  }
}
