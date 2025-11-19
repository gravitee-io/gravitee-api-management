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
import { MatCardModule } from '@angular/material/card';

import { TreeNodeComponent } from './tree-node.component';

import { PortalNavigationItem, PortalNavigationItemType } from '../../../entities/management-api-v2';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

export interface SectionNode {
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  children?: SectionNode[];
}

type ProcessingNode = SectionNode & {
  __order: number;
  __parentId: string | null;
};

@Component({
  selector: 'portal-tree-component',
  standalone: true,
  imports: [TreeNodeComponent, EmptyStateComponent, MatCardModule],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent {
  links = input<PortalNavigationItem[] | null>(null);
  tree = computed(() => {
    const links = this.links();
    return links && Array.isArray(links) ? this.mapLinksToNodes(links) : [];
  });

  selectedId = input<string | null>(null);
  select = output<SectionNode>();

  constructor() {
    effect(() => {
      const currentTree = this.tree();
      const currentSelectedId = this.selectedId();

      if (currentTree.length > 0) {
        const isPageIdProvided = currentSelectedId !== null;
        const pageExists = isPageIdProvided && this.findNode(currentTree, (node) => node.id === currentSelectedId);

        if (isPageIdProvided && !pageExists) {
          return;
        }

        if (!isPageIdProvided) {
          const firstPageNode = this.findNode(currentTree, (node) => node.type === 'PAGE');
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

  private mapLinksToNodes(links: PortalNavigationItem[]): SectionNode[] {
    const nodesById = this.createNodesMap(links);
    const roots = this.connectNodes(nodesById);
    return this.sortAndCleanTree(roots);
  }

  private createNodesMap(links: PortalNavigationItem[]): Map<string, ProcessingNode> {
    const nodes = new Map<string, ProcessingNode>();

    for (const link of links) {
      const type = link.type;

      nodes.set(link.id, {
        id: link.id,
        label: link.title,
        type,
        data: link,
        children: type === 'FOLDER' ? [] : undefined,
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
}
