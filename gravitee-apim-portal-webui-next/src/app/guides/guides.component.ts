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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, combineLatestWith, EMPTY, map, Observable, of, switchMap, tap } from 'rxjs';

import { LoaderComponent } from '../../components/loader/loader.component';
import { PageComponent } from '../../components/page/page.component';
import { PageTreeComponent, PageTreeNode } from '../../components/page-tree/page-tree.component';
import { Page } from '../../entities/page/page';
import { PageService } from '../../services/page.service';

@Component({
  selector: 'app-guides',
  standalone: true,
  imports: [AsyncPipe, LoaderComponent, PageComponent, PageTreeComponent, MatCard, MatCardContent],
  templateUrl: './guides.component.html',
  styleUrl: './guides.component.scss',
})
export class GuidesComponent implements OnInit {
  pagesData$: Observable<{ nodes: PageTreeNode[]; pages: Page[] }> = of();
  selectedPageData$: Observable<{ result?: Page; error?: string }> = of();
  selectedPageId = signal<string | undefined>(undefined);
  loadingPage = signal<boolean>(true);

  constructor(
    private pageService: PageService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.pagesData$ = this.pageService.listByEnvironment().pipe(
      combineLatestWith(this.activatedRoute.queryParams),
      map(([pagesResponse, queryParams]) => {
        this.selectedPageId.set(queryParams['page']);
        const pages = (pagesResponse.data ?? []).filter(p => p.type !== 'LINK');
        const nodes = this.pageService.mapToPageTreeNode(undefined, pages);
        return { pages, nodes };
      }),
    );

    this.selectedPageData$ = this.activatedRoute.queryParams.pipe(
      tap(({ page }) => {
        this.loadingPage.set(true);
        this.selectedPageId.set(page);
      }),
      switchMap(({ page }) => (page ? this.pageService.getById(page) : EMPTY)),
      map(result => ({ result })),
      catchError((error: HttpErrorResponse) => of({ error: error.message })),
      tap(_ => this.loadingPage.set(false)),
    );
  }

  showPage(page: string) {
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page,
      },
    });
  }
}
