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
import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { catchError, combineLatest, filter, map, Observable, of, switchMap } from 'rxjs';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PageComponent } from '../../../components/page/page.component';
import { PageTreeComponent, PageTreeNode } from '../../../components/page-tree/page-tree.component';
import { Page } from '../../../entities/page/page';
import { PageService } from '../../../services/page.service';

interface SelectedPageData {
  result?: Page;
  error?: unknown;
}

@Component({
  selector: 'app-api-tab-documentation',
  standalone: true,
  imports: [PageTreeComponent, AsyncPipe, PageComponent, RouterModule, LoaderComponent],
  templateUrl: './api-tab-documentation.component.html',
  styleUrl: './api-tab-documentation.component.scss',
})
export class ApiTabDocumentationComponent implements OnInit {
  @Input()
  page!: string;
  pageNodes$: Observable<PageTreeNode[]> = of([]);
  selectedPageData$: Observable<SelectedPageData> = of();

  private apiId$: Observable<string>;

  constructor(
    private pageService: PageService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {
    this.apiId$ = this.activatedRoute.parent ? this.activatedRoute.parent.params.pipe(map(params => params['apiId'])) : of();
  }

  ngOnInit(): void {
    this.pageNodes$ = this.apiId$.pipe(
      switchMap(apiId => this.pageService.listByApiId(apiId)),
      map(resp => this.pageService.mapToPageTreeNode(undefined, resp.data ?? [])),
    );

    this.selectedPageData$ = combineLatest([this.apiId$, this.activatedRoute.queryParams]).pipe(
      map(([apiId, params]) => ({ apiId, pageId: params['page'] })),
      filter(res => !!res.pageId),
      switchMap(({ apiId, pageId }) => this.getSelectedPage$(apiId, pageId)),
    );
  }

  showPage(page: string) {
    this.router.navigate(['.'], { queryParams: { page }, relativeTo: this.activatedRoute });
  }

  private getSelectedPage$(apiId: string, pageId: string): Observable<SelectedPageData> {
    return this.pageService.getByApiIdAndId(apiId, pageId, true).pipe(
      map(result => ({ result })),
      catchError(error => of({ error })),
    );
  }
}
