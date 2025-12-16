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
import { Injectable } from '@angular/core';

import { PortalNavigationItem, PortalNavigationItemType } from '../../../entities/portal-navigation/portal-navigation-item';
import { Breadcrumb } from '../components/documentation-folder/breadcrumb/breadcrumbs.component';

export interface TreeNode {
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  children?: TreeNode[];
  breadcrumbs?: Breadcrumb[];
}

type ProcessingNode = TreeNode & {
  __order: number;
  __parentId: string | null;
};

@Injectable({ providedIn: 'root' })
export class DocumentationTreeService {
  private parentItemBreadcrumb?: Breadcrumb;
  private nodesById = new Map<string, ProcessingNode>();

  setParentItem(item: PortalNavigationItem) {
    this.parentItemBreadcrumb = { id: item.id, label: item.title };
  }

  getParentItemBreadcrumb(): Breadcrumb | null {
    return this.parentItemBreadcrumb ?? null;
  }

  mapItemsToNodes(items: PortalNavigationItem[]) {
    this.nodesById = this.createNodesMap(items);
    const roots = this.connectNodes(this.nodesById);
    return this.sortAndCleanTree(this.attachBreadcrumbs(roots));
  }

  getBreadcrumbsByNodeId(id: string): Breadcrumb[] {
    if (!this.parentItemBreadcrumb) {
      return [];
    }
    return [this.parentItemBreadcrumb, ...(this.nodesById.get(id)?.breadcrumbs ?? [])];
  }

  private createNodesMap(items: PortalNavigationItem[]): Map<string, ProcessingNode> {
    const nodes = new Map<string, ProcessingNode>();

    for (const item of items) {
      const type = item.type;

      nodes.set(item.id, {
        id: item.id,
        label: item.title,
        type,
        data: item,
        children: type === 'FOLDER' ? [] : undefined,
        __order: item.order ?? 0,
        __parentId: item.parentId ?? null,
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

  private sortAndCleanTree(nodes: ProcessingNode[]): TreeNode[] {
    return nodes
      .toSorted((a, b) => a.__order - b.__order)
      .map(({ __order, __parentId, ...node }) => ({
        ...node,
        children: node.children ? this.sortAndCleanTree(node.children as ProcessingNode[]) : undefined,
      }));
  }

  private attachBreadcrumbs(nodes: ProcessingNode[], breadcrumbs: Breadcrumb[] = []): ProcessingNode[] {
    return nodes.map(node => {
      const children = (node.children ?? []) as ProcessingNode[];
      const newBreadcrumbs = [...breadcrumbs, { id: node.id, label: node.label }];
      if (children.length > 0) {
        node.children = this.attachBreadcrumbs(children, newBreadcrumbs);
      } else if (node.type === 'PAGE') {
        node.breadcrumbs = newBreadcrumbs;
      }
      return node;
    });
  }
}
