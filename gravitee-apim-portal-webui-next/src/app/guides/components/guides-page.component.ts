/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, DestroyRef, inject, input, InputSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { Observable, of, tap } from 'rxjs';
import { BreadcrumbService } from 'xng-breadcrumb';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PageComponent } from '../../../components/page/page.component';
import { Page } from '../../../entities/page/page';
import { PageService } from '../../../services/page.service';

@Component({
  selector: 'app-guides-page',
  imports: [PageComponent, AsyncPipe, LoaderComponent],
  standalone: true,
  template: `
    @if (selectedPageData$ | async; as selectedPageData) {
      @if (selectedPageData) {
        <app-page class="page-content__container" [page]="selectedPageData" [pages]="pages()" />
      }
    } @else {
      <app-loader />
    }
  `,
})
export class GuidesPageComponent {
  pages: InputSignal<Page[]> = input.required<Page[]>();
  selectedPageData$: Observable<Page> = of();

  constructor(
    private readonly pageService: PageService,
    private readonly breadcrumbService: BreadcrumbService,
    private readonly destroyRef: DestroyRef,
  ) {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(paramMap => {
        const pageId = paramMap.get('pageId');
        if (pageId) {
          this.selectedPageData$ = this.getSelectedPage$(pageId);
        } else if (this.pages().length > 0) {
          this.selectedPageData$ = this.getSelectedPage$(this.pages()[0].id);
        }
      });
  }

  private getSelectedPage$(pageId: string): Observable<Page> {
    return this.pageService.getById(pageId, true).pipe(
      tap(page => {
        this.breadcrumbService.set('@pageName', page.name);
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }
}
