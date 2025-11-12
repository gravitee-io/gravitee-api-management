/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, computed, effect, ErrorHandler, input, OnDestroy, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { TreeNodeComponent } from './tree-node.component';

import { PortalMenuLink } from '../../../entities/management-api-v2';

export type SectionNodeType = 'page' | 'folder' | 'link';

export interface SectionNode {
  id: string;
  label: string;
  type: SectionNodeType;
  data?: PortalMenuLink;
  children?: SectionNode[];
}

interface ApiPortalMenuLink {
  id: string;
  name: string;
  type: string;
  target?: string | null;
  visibility: string;
  order: number;
  parentId?: string | null;
}

type ProcessingNode = SectionNode & {
  __order: number;
  __parentId: string | null;
};

@Component({
  selector: 'portal-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements OnDestroy {
  private hadPageIdInRouteInitially = false;
  links = input<PortalMenuLink[] | null>(null);
  tree = computed(() => {
    if (this.links() && Array.isArray(this.links())) {
      return this.mapLinksToNodes(this.links());
    }
    return [];
  });

  selectedId = signal<string | null>(null);

  private routeSub: Subscription | null = null;
  private updatingFromRoute = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private errorService: ErrorHandler,
  ) {
    const initialId = this.route.snapshot.queryParamMap.get('pageId');
    if (initialId) {
      this.hadPageIdInRouteInitially = true;
      this.selectedId.set(initialId);
    }

    this.routeSub = this.route.queryParams.subscribe((params) => {
      const pageId = params['pageId'] ?? null;
      if (pageId !== this.selectedId()) {
        this.hadPageIdInRouteInitially = !!pageId;
        this.updatingFromRoute = true;
        this.selectedId.set(pageId);
        Promise.resolve().then(() => (this.updatingFromRoute = false));
      }
    });

    effect(() => {
      const id = this.selectedId();
      if (this.updatingFromRoute) return;

      const currentPageId = this.route.snapshot.queryParamMap.get('pageId');
      const idToCompare = id ?? null;
      if (currentPageId !== idToCompare) {
        this.router
          .navigate([], {
            queryParams: { pageId: id },
            queryParamsHandling: 'merge',
            replaceUrl: true,
          })
          .catch((err) => this.errorService.handleError(err));
      }
    });

    effect(() => {
      const nodes = this.tree();
      const cur = this.selectedId();

      if (!nodes || nodes.length === 0) return;
      const found = cur ? this.findNode(nodes, (node) => node.id === cur) : null;
      if (found) return;
      if (this.hadPageIdInRouteInitially) return;

      const firstPage = this.findNode(nodes, (node) => node.type === 'page');
      if (firstPage) {
        this.updatingFromRoute = true;
        this.selectedId.set(firstPage.id);
        Promise.resolve().then(() => (this.updatingFromRoute = false));
      } else {
        this.updatingFromRoute = true;
        this.selectedId.set(null);
        Promise.resolve().then(() => (this.updatingFromRoute = false));
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  select(node: SectionNode): void {
    this.selectedId.set(node.id);
  }

  private findNode(nodes: SectionNode[], predicate: (node: SectionNode) => boolean): SectionNode | null {
    for (const node of nodes) {
      if (predicate(node)) {
        return node;
      }
      if (node.children) {
        const found = this.findNode(node.children, predicate);
        if (found) return found;
      }
    }
    return null;
  }

  private mapLinksToNodes(links: ApiPortalMenuLink[]): SectionNode[] {
    const nodesById = this.createNodesMap(links);
    const roots = this.connectNodes(nodesById);
    return this.sortAndCleanTree(roots);
  }

  private createNodesMap(links: ApiPortalMenuLink[]): Map<string, ProcessingNode> {
    const nodes = new Map<string, ProcessingNode>();

    for (const link of links) {
      const type = this.getNodeType(link);

      nodes.set(link.id, {
        id: link.id,
        label: link.name,
        type,
        data: link as PortalMenuLink,
        children: type === 'folder' ? [] : undefined,
        __order: link.order ?? 0,
        __parentId: link.parentId ?? null,
      } as ProcessingNode);
    }
    return nodes;
  }

  private connectNodes(nodes: Map<string, ProcessingNode>): ProcessingNode[] {
    const roots: ProcessingNode[] = [];

    for (const node of nodes.values()) {
      const parent = node.__parentId ? nodes.get(node.__parentId) : null;
      if (parent) {
        parent.children ??= [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }

    return roots;
  }

  private sortAndCleanTree(nodes: ProcessingNode[]): SectionNode[] {
    return nodes
      .sort((a, b) => a.__order - b.__order)
      .map(({ __order, __parentId, ...node }) => ({
        ...node,
        children: node.children ? this.sortAndCleanTree(node.children as ProcessingNode[]) : undefined,
      }));
  }

  private getNodeType(link: ApiPortalMenuLink) {
    if (link.target === null || link.type === 'FOLDER') {
      return 'folder';
    }
    if (['LINK', 'EXTERNAL'].includes(link.type)) {
      return 'link';
    }
    return 'page';
  }
}
