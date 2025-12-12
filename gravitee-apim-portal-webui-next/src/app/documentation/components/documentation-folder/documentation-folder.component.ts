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
import { Component, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, concat, distinctUntilChanged, map, Observable, of, shareReplay, switchMap } from 'rxjs';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { SectionNode, TreeComponent } from './tree-component/tree.component';
import { InnerLinkDirective } from '../../../../directives/inner-link.directive';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective, TreeComponent, GraviteeMarkdownViewerModule, InnerLinkDirective, AsyncPipe],
  standalone: true,
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
})
export class DocumentationFolderComponent {
  navId = input.required<string>();
  navId$ = toObservable(this.navId).pipe(distinctUntilChanged());

  children$: Observable<PortalNavigationItem[] | null> = this.navId$.pipe(
    switchMap(navId => {
      if (!navId) return of([]);

      // Emits null to force reset
      return concat(of(null), this.itemsService.getNavigationItems('TOP_NAVBAR', true, navId));
    }),
    shareReplay(1),
  );

  pageId$ = this.activatedRoute.queryParams.pipe(map(params => params['pageId'] ?? null));

  selectedPageContent = toSignal(
    combineLatest([this.children$, this.pageId$]).pipe(switchMap(([children, pageId]) => this.loadContentOrRedirect(children, pageId))),
    { initialValue: '' },
  );

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly itemsService: PortalNavigationItemsService,
  ) {}

  onSelect(selectedPageId: string | null) {
    if (selectedPageId) {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { pageId: selectedPageId },
        replaceUrl: true,
      });
    }
  }

  private loadContentOrRedirect(children: PortalNavigationItem[] | null, pageId: string | undefined): Observable<string> {
    if (!children || children.length === 0) {
      if (pageId) {
        this.resetPageId();
      }
      return of('');
    }

    if (!pageId) {
      const firstPageId = this.findFirstPageId(children);
      if (firstPageId) {
        this.onSelect(firstPageId);
      }
      return of('');
    }

    const pageExists = children.find(item => item.id === pageId);
    if (!pageExists) {
      this.resetPageId();
      return of('');
    }

    return this.itemsService.getNavigationItemContent(pageId);
  }

  private resetPageId() {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: {},
      replaceUrl: true,
    });
  }

  private findFirstPageId(items: PortalNavigationItem[]): string | null {
    const tree = this.buildTreeFromItems(items);
    return this.findFirstPageIdInTree(tree);
  }

  private findFirstPageIdInTree(nodes: SectionNode[]): string | null {
    for (const node of nodes) {
      if (node.type === 'PAGE') {
        return node.id;
      } else if (node.children) {
        const id = this.findFirstPageIdInTree(node.children);
        if (id) return id;
      }
    }
    return null;
  }

  private buildTreeFromItems(items: PortalNavigationItem[]): SectionNode[] {
    const roots: SectionNode[] = [];
    const nodeMap = new Map<string, SectionNode>();

    items.forEach(item => {
      const node: SectionNode = {
        id: item.id,
        label: item.title,
        type: item.type,
        data: item,
        children: item.type === 'FOLDER' ? [] : undefined,
      };
      nodeMap.set(item.id, node);
    });

    items.forEach(item => {
      const node = nodeMap.get(item.id)!;
      const parentId = item.parentId;

      if (parentId && nodeMap.has(parentId)) {
        const parent = nodeMap.get(parentId)!;
        parent.children?.push(node);
      } else {
        roots.push(node);
      }
    });

    const sortNodes = (nodes: SectionNode[]): SectionNode[] => {
      return nodes
        .sort((a, b) => (a.data?.order ?? 0) - (b.data?.order ?? 0))
        .map(node => ({
          ...node,
          children: node.children ? sortNodes(node.children) : undefined,
        }));
    };

    return sortNodes(roots);
  }
}
