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
import { Component, computed, effect, inject, input, output, viewChild } from '@angular/core';
import { MatTree, MatTreeModule } from '@angular/material/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDivider } from '@angular/material/divider';
import { MatTooltip } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, CdkDragStart, DragDropModule } from '@angular/cdk/drag-drop';

import { PortalNavigationItem, PortalNavigationItemType } from '../../../entities/management-api-v2';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { PortalNavigationItemIconPipe } from '../../icon/portal-navigation-item-icon.pipe';

export interface SectionNode {
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  children?: SectionNode[];
}

export interface NodeMovedEvent {
  node: SectionNode;
  newParentId: string | null;
  newOrder: number;
}

type NodeMenuActionType = 'create' | 'edit' | 'delete' | 'publish' | 'unpublish';

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
    DragDropModule,
    PortalNavigationItemIconPipe,
  ],
  templateUrl: './flat-tree.component.html',
  styleUrls: ['./flat-tree.component.scss'],
})
export class FlatTreeComponent {
  private permissionService = inject(GioPermissionService);

  links = input<PortalNavigationItem[] | null>(null);
  selectedId = input<string | null>(null);

  nodeSelect = output<SectionNode>();
  nodeMenuAction = output<NodeMenuActionEvent>();
  nodeMoved = output<NodeMovedEvent>();

  childrenAccessor = (node: SectionNode) => node.children ?? [];
  hasChild = (_: number, node: SectionNode) => !!node.children && node.children.length > 0;

  tree = computed(() => {
    const links = this.links();
    return links && Array.isArray(links) ? this.mapLinksToNodes(links) : [];
  });

  isSelected = (node: FlatTreeNode) => this.selectedId() === node.id;

  isUnpublished = (node: FlatTreeNode) => node.data?.published === false;

  treeBase = viewChild(MatTree);

  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;

  constructor() {
    this.canCreate = this.permissionService.hasAnyMatching(['environment-documentation-c']);
    this.canUpdate = this.permissionService.hasAnyMatching(['environment-documentation-u']);
    this.canDelete = this.permissionService.hasAnyMatching(['environment-documentation-d']);

    effect(() => {
      const nodes = this.tree();
      const tree = this.treeBase();

      if (nodes.length > 0 && tree) {
        queueMicrotask(() => {
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

  onPublish(node: FlatTreeNode) {
    this.nodeMenuAction.emit({
      action: 'publish',
      itemType: node.type,
      node: this.mapFlatTreeNodeToSectionNode(node),
    });
  }

  onUnpublish(node: FlatTreeNode) {
    this.nodeMenuAction.emit({
      action: 'unpublish',
      itemType: node.type,
      node: this.mapFlatTreeNodeToSectionNode(node),
    });
  }

  canShowMoreActions(node: FlatTreeNode): boolean {
    if (node.type === 'FOLDER' && this.canCreate) {
      return true;
    }
    return this.canUpdate || this.canDelete;
  }

  hasChildren(node: FlatTreeNode): boolean {
    return node.expandable;
  }

  onDrop(event: CdkDragDrop<SectionNode[]>) {
    const { previousIndex, currentIndex, item } = event;

    if (previousIndex === currentIndex) return;

    const nodeToMove = item.data as SectionNode;
    const visibleNodes = this.getVisibleNodes();

    let newParentId: string | null;
    let newOrder: number;

    if (currentIndex === 0) {
      // Dropped at the very top
      newParentId = null;
      newOrder = 0;
    } else {
      // Dropped elsewhere
      if (currentIndex >= visibleNodes.length) {
        // Dropped at the very end of the list
        const lastNode = visibleNodes.at(visibleNodes.length - 1);
        newParentId = lastNode.data.parentId ?? null;
        newOrder = lastNode.data.order + 1;
      } else {
        // Dropped between other items
        const replacedItem = visibleNodes.at(currentIndex);
        const isMovingDown = previousIndex < currentIndex;
        const isParentChange = nodeToMove.data.parentId !== replacedItem.data.parentId;

        newParentId = replacedItem.data.parentId ?? null;
        newOrder = isMovingDown && isParentChange ? replacedItem.data.order + 1 : replacedItem.data.order;
      }

      // Prevent self-parenting
      if (newParentId === nodeToMove.id) {
        newParentId = null;
      }
    }

    if (nodeToMove.type === 'FOLDER') {
      this.treeBase()?.expandAll();
    }

    this.nodeMoved.emit({ node: nodeToMove, newParentId, newOrder });
  }

  onDragStarted(event: CdkDragStart<SectionNode>) {
    const draggedNode = event.source.data;

    // If it's a folder, find all its descendants to hide them
    if (draggedNode.type === 'FOLDER' && draggedNode.children) {
      const tree = this.treeBase();
      tree.collapse(draggedNode);
    }
  }

  private getVisibleNodes(): SectionNode[] {
    const result: SectionNode[] = [];
    const tree = this.treeBase();

    const traverse = (nodes: SectionNode[]) => {
      for (const node of nodes) {
        result.push(node);
        // Only add children if the node is expanded
        if (node.children && tree?.isExpanded(node)) {
          traverse(node.children);
        }
      }
    };

    traverse(this.tree());
    return result;
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
