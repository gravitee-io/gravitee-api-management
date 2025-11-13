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
import { Component, computed, effect, input, output } from '@angular/core';

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
export class TreeComponent {
  links = input<PortalMenuLink[] | null>(null);
  tree = computed(() => {
    const links = this.links();
    return links && Array.isArray(links) ? this.mapLinksToNodes(links) : [];
  });

  selectedId = input<string | null>(null);
  select = output<SectionNode>();
  pageNotFound = output<void>();

  constructor() {
    effect(() => {
      const currentTree = this.tree();
      const currentSelectedId = this.selectedId();

      if (currentTree.length > 0) {
        const isPageIdProvided = currentSelectedId !== null;
        const pageExists = isPageIdProvided && this.findNode(currentTree, (node) => node.id === currentSelectedId);

        if (isPageIdProvided && !pageExists) {
          this.pageNotFound.emit();
          return;
        }

        if (!isPageIdProvided) {
          const firstPageNode = this.findNode(currentTree, (node) => node.type === 'page');
          if (firstPageNode) {
            this.select.emit(firstPageNode);
          }
        }
      }
    });
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
