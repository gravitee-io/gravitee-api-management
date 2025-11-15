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

import { Component, computed, DestroyRef, effect } from '@angular/core';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCard, MatCardContent } from '@angular/material/card';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, of, switchMap, distinctUntilChanged, startWith } from 'rxjs';

import { LoaderComponent } from '../../components/loader/loader.component';
import { PageTreeComponent, PageTreeNode } from '../../components/page-tree/page-tree.component';
import { Page } from '../../entities/page/page';
import { PageService } from '../../services/page.service';

@Component({
  selector: 'app-guides',
  imports: [LoaderComponent, PageTreeComponent, MatCard, MatCardContent, RouterOutlet],
  templateUrl: './guides.component.html',
  styleUrls: ['./guides.component.scss'],
})
export class GuidesComponent {
  protected pagesData = toSignal(
    this.pageService.listByEnvironment().pipe(
      map(pagesResponse => {
        const pages = (pagesResponse.data ?? []).filter(p => p.type !== 'LINK');
        const nodes = this.pageService.mapToPageTreeNode(undefined, pages);
        return { pages, nodes };
      }),
    ),
  );

  protected firstPageId = computed(() => {
    const pagesData = this.pagesData();
    if (!pagesData || pagesData.nodes.length === 0) {
      return null;
    }
    const pagesMap = new Map(pagesData.pages.map(p => [p.id, p]));
    return this.findFirstPageId(pagesData.nodes, pagesMap);
  });

  protected pageId = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.activatedRoute.firstChild),
      switchMap(firstChild => (firstChild ? firstChild.paramMap : of(null))),
      map(params => (params ? params.get('pageId') : null)),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ),
    { initialValue: null },
  );

  private redirectOnLoad = effect(() => {
    const page = this.pageId();
    const firstPage = this.firstPageId();

    if (firstPage && page === null) {
      this.router.navigate(['.', firstPage], {
        relativeTo: this.activatedRoute,
        replaceUrl: true,
      });
    }
  });

  constructor(
    private readonly pageService: PageService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly destroyRef: DestroyRef,
  ) {}

  showPage(pageId: string): void {
    this.router.navigate(['.', pageId], { relativeTo: this.activatedRoute });
  }

  private findFirstPageId(nodes: PageTreeNode[], pagesMap: Map<string, Page>): string | null {
    for (const node of nodes) {
      const page = pagesMap.get(node.id);

      if (page && page.type !== 'FOLDER') {
        return page.id;
      }

      if (node.children && node.children.length > 0) {
        const childId = this.findFirstPageId(node.children, pagesMap);
        if (childId) {
          return childId;
        }
      }
    }
    return null;
  }
}
