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
import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { catchError, map, Observable, of } from 'rxjs';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PageComponent } from '../../../../components/page/page.component';
import { PageTreeComponent, PageTreeNode } from '../../../../components/page-tree/page-tree.component';
import { Page } from '../../../../entities/page/page';
import { PageService } from '../../../../services/page.service';

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
export class ApiTabDocumentationComponent implements OnInit, OnChanges {
  @Input()
  page!: string;
  @Input()
  apiId!: string;
  @Input()
  pages!: Page[];
  pageNodes: PageTreeNode[] = [];
  selectedPageData$: Observable<SelectedPageData> = of();

  constructor(
    private pageService: PageService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.pageNodes = this.pageService.mapToPageTreeNode(undefined, this.pages);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['page'] && !!changes['page'].currentValue) {
      this.selectedPageData$ = this.getSelectedPage$(changes['page'].currentValue);
    }
  }

  showPage(page: string) {
    this.router.navigate(['.'], { queryParams: { page }, relativeTo: this.activatedRoute });
  }

  private getSelectedPage$(pageId: string): Observable<SelectedPageData> {
    return this.pageService.getByApiIdAndId(this.apiId, pageId, true).pipe(
      map(result => ({ result })),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          this.router.navigate(['404']);
        }
        return of({ error });
      }),
    );
  }
}
