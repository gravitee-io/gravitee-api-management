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
import { Component, DestroyRef, effect, inject, input, InputSignal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { BreadcrumbService } from 'xng-breadcrumb';

import { LoaderComponent } from '../../../../../../components/loader/loader.component';
import { PageComponent } from '../../../../../../components/page/page.component';
import { Page } from '../../../../../../entities/page/page';
import { PageService } from '../../../../../../services/page.service';

interface SelectedPageData {
  result?: Page;
  error?: unknown;
}

@Component({
  selector: 'app-api-documentation',
  standalone: true,
  imports: [AsyncPipe, PageComponent, RouterModule, LoaderComponent],
  templateUrl: './api-documentation.component.html',
  styleUrl: './api-documentation.component.scss',
})
export class ApiDocumentationComponent {
  pageId: InputSignal<string> = input.required<string>();
  apiId: InputSignal<string> = input.required<string>();
  pages: InputSignal<Page[]> = input.required<Page[]>();
  selectedPageData$: Observable<SelectedPageData> = of();
  destroyRef = inject(DestroyRef);

  constructor(
    private readonly pageService: PageService,
    private readonly router: Router,
    private readonly breadcrumbService: BreadcrumbService,
  ) {
    effect(() => {
      this.selectedPageData$ = this.getSelectedPage$(this.pageId());
    });
  }

  private getSelectedPage$(pageId: string): Observable<SelectedPageData> {
    return this.pageService.getByApiIdAndId(this.apiId(), pageId, true).pipe(
      tap(page => {
        this.breadcrumbService.set('@pageName', page.name);
      }),
      map(result => ({ result })),
      catchError((error: HttpErrorResponse) => {
        return of({ error });
      }),
    );
  }
}
