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
import { AfterViewInit, Component, computed, effect, input, model, output } from '@angular/core';

import { TreeNodeComponent } from './tree-node.component';
import { PortalNavigationItem, PortalNavigationItemType } from '../../../../../entities/portal-navigation/portal-navigation-item';

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
  selector: 'app-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements AfterViewInit {
  items = input<PortalNavigationItem[] | null>(null);
  tree = computed(() => {
    const items = this.items();
    return items && Array.isArray(items) ? this.mapItemsToNodes(items) : [];
  });

  selectedId = model<string | null>(null);
  selectNode = output<string | null>();

  constructor() {
    effect(() => this.selectFirstPage());
  }

  ngAfterViewInit() {
    this.scrollIntoView();
  }

  onNodeSelected(id: string) {
    this.selectedId.set(id);
    this.selectNode.emit(id);
  }

  private mapItemsToNodes(items: PortalNavigationItem[]): SectionNode[] {
    const nodesById = this.createNodesMap(items);
    const roots = this.connectNodes(nodesById);
    return this.sortAndCleanTree(roots);
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

  private sortAndCleanTree(nodes: ProcessingNode[]): SectionNode[] {
    return nodes
      .sort((a, b) => a.__order - b.__order)
      .map(({ __order, __parentId, ...node }) => ({
        ...node,
        children: node.children ? this.sortAndCleanTree(node.children as ProcessingNode[]) : undefined,
      }));
  }

  private selectFirstPage() {
    const firstPageId = this.selectedId() ?? this.findFirstPageId(this.tree());
    if (firstPageId) {
      this.onNodeSelected(firstPageId);
    }
  }

  private findFirstPageId(nodes: SectionNode[]): string | null {
    for (const node of nodes) {
      if (node.type === 'PAGE') {
        return node.id;
      } else {
        const id = this.findFirstPageId(node.children ?? []);
        if (id) return id;
      }
    }
    return null;
  }

  private scrollIntoView() {
    const selectedId = this.selectedId();
    if (selectedId) {
      document.querySelector('#node-' + selectedId)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}
