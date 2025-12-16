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
import { Component, computed, effect, input, output, viewChild } from '@angular/core';
import { MatTree, MatTreeModule } from '@angular/material/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDivider } from '@angular/material/divider';
import { MatTooltip } from '@angular/material/tooltip';
import { CommonModule, LowerCasePipe } from '@angular/common';

import { PortalNavigationItem, PortalNavigationItemType } from '../../../entities/management-api-v2';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

export interface SectionNode {
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  children?: SectionNode[];
}

type NodeMenuActionType = 'create' | 'edit' | 'delete';

export interface NodeMenuActionEvent {
  node: SectionNode;
  action: NodeMenuActionType;
  itemType: PortalNavigationItemType;
}

interface FlatTreeNode {
  expandable: boolean;
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  level: number;
}

type ProcessingNode = SectionNode & {
  __order: number;
  __parentId: string | null;
};

@Component({
  selector: 'portal-flat-tree-component',
  standalone: true,
  imports: [
    CommonModule,
    MatTreeModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDivider,
    MatTooltip,
    EmptyStateComponent,
    GioPermissionModule,
    LowerCasePipe,
  ],
  templateUrl: './flat-tree.component.html',
  styleUrls: ['./flat-tree.component.scss'],
})
export class FlatTreeComponent {
  links = input<PortalNavigationItem[] | null>(null);
  selectedId = input<string | null>(null);

  nodeSelect = output<SectionNode>();
  nodeMenuAction = output<NodeMenuActionEvent>();

  childrenAccessor = (node: SectionNode) => node.children ?? [];
  hasChild = (_: number, node: SectionNode) => !!node.children && node.children.length > 0;

  tree = computed(() => {
    const links = this.links();
    return links && Array.isArray(links) ? this.mapLinksToNodes(links) : [];
  });

  isSelected = (node: FlatTreeNode) => this.selectedId() === node.id;

  isUnpublished = (node: FlatTreeNode) => node.data?.published === false;

  treeBase = viewChild(MatTree);

  constructor() {
    effect(() => {
      const nodes = this.tree();
      const tree = this.treeBase();

      if (nodes.length > 0 && tree) {
        setTimeout(() => {
          tree.expandAll();
        });
      }
    });
  }

  onNodeClick(node: FlatTreeNode) {
    this.nodeSelect.emit(this.mapFlatTreeNodeToSectionNode(node));
  }

  onEdit(node: FlatTreeNode) {
    this.nodeMenuAction.emit({
      action: 'edit',
      itemType: node.type,
      node: this.mapFlatTreeNodeToSectionNode(node),
    });
  }

  onCreate(node: FlatTreeNode, itemType: PortalNavigationItemType) {
    this.nodeMenuAction.emit({
      action: 'create',
      itemType,
      node: this.mapFlatTreeNodeToSectionNode(node),
    });
  }

  onDelete(node: FlatTreeNode) {
    this.nodeMenuAction.emit({
      action: 'delete',
      itemType: node.type,
      node: this.mapFlatTreeNodeToSectionNode(node),
    });
  }

  hasChildren(node: FlatTreeNode): boolean {
    return node.expandable;
  }

  private mapFlatTreeNodeToSectionNode(flatTreeNode: FlatTreeNode): SectionNode {
    return flatTreeNode as SectionNode;
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
