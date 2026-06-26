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
import { Component, computed, effect, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { MatTree, MatTreeModule } from '@angular/material/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatDivider } from '@angular/material/divider';
import { MatTooltip } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, CdkDragMove, CdkDragStart, DragDropModule } from '@angular/cdk/drag-drop';

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
  level?: number;
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
  id: string;
  label: string;
  type: PortalNavigationItemType;
  data?: PortalNavigationItem;
  level: number;
  children?: SectionNode[];
}

interface PublishActionState {
  disabled: boolean;
  tooltip: string;
  ariaDisabled: boolean;
  tabIndex: -1 | null;
}

type ProcessingNode = SectionNode & {
  __order: number;
  __parentId: string | null;
};

type DropPosition = 'before' | 'inside' | 'after';

interface DropIntent {
  targetId: string;
  position: DropPosition;
}

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
  private readonly permissionService = inject(GioPermissionService);

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

  readonly publishActionEnabledState: PublishActionState = {
    disabled: false,
    tooltip: '',
    ariaDisabled: false,
    tabIndex: null,
  };

  private parentNavigationItemByChildId = computed(() => {
    const links = this.links();
    const parentByChildId = new Map<string, PortalNavigationItem | undefined>();

    if (!links || !Array.isArray(links)) {
      return parentByChildId;
    }

    const itemsById = new Map<string, PortalNavigationItem>(links.map(item => [item.id, item]));

    for (const link of links) {
      parentByChildId.set(link.id, link.parentId ? itemsById.get(link.parentId) : undefined);
    }

    return parentByChildId;
  });

  publishStateByNodeId = computed(() => {
    const links = this.links();
    const publishStateByNodeId = new Map<string, PublishActionState>();

    if (!links || !Array.isArray(links)) {
      return publishStateByNodeId;
    }

    const parentByChildId = this.parentNavigationItemByChildId();

    for (const link of links) {
      if (!link.parentId) {
        publishStateByNodeId.set(link.id, this.publishActionEnabledState);
        continue;
      }

      const parentNavigationItem = parentByChildId.get(link.id);
      if (!parentNavigationItem) {
        publishStateByNodeId.set(
          link.id,
          this.toDisabledPublishActionState('A navigation item cannot be published because its parent is unavailable'),
        );
        continue;
      }

      if (!parentNavigationItem.published) {
        publishStateByNodeId.set(
          link.id,
          this.toDisabledPublishActionState(
            `A navigation item cannot be published within an unpublished ${parentNavigationItem.type.toLocaleLowerCase()}`,
          ),
        );
        continue;
      }

      publishStateByNodeId.set(link.id, this.publishActionEnabledState);
    }

    return publishStateByNodeId;
  });

  readonly hasExpandedNode = signal(false);
  private readonly expandableNodes = computed(() => this.collectExpandableNodes(this.tree()));
  readonly hasExpandableNode = computed(() => this.expandableNodes().length > 0);

  isSelected = (node: FlatTreeNode) => this.selectedId() === node.id;

  isUnpublished = (node: FlatTreeNode) => node.data?.published === false;

  isPublishDisabled(node: SectionNode): boolean {
    return this.getPublishActionState(node).disabled;
  }

  getPublishDisabledTooltip(node: SectionNode): string {
    return this.getPublishActionState(node).tooltip;
  }

  treeBase = viewChild<MatTree<SectionNode, string>>(MatTree);
  contextMenuTrigger = viewChild('contextMenuTrigger', { read: MatMenuTrigger });
  contextMenuAnchor = viewChild('contextMenuTrigger', { read: ElementRef });
  contextMenuNode = signal<FlatTreeNode | null>(null);

  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;

  // Used by MatTree's [expansionKey] so expansion state is keyed by id, not object reference,
  // and therefore preserved across [dataSource] refreshes (after publish/delete/move).
  readonly getNodeId = (node: SectionNode): string => node.id;

  // Keyed by id, level AND whether it has children, so MatTree recreates the view (instead of reusing a
  // stale one) when a node changes depth after a move.
  readonly trackByNode = (_: number, node: SectionNode): string => `${node.id}:${node.level}:${node.children?.length ? 1 : 0}`;

  readonly dropIntent = signal<DropIntent | null>(null);

  private treeInitialized = false;
  private revealedSelectedId: string | null = null;

  constructor() {
    this.canCreate = this.permissionService.hasAnyMatching(['environment-documentation-c']);
    this.canUpdate = this.permissionService.hasAnyMatching(['environment-documentation-u']);
    this.canDelete = this.permissionService.hasAnyMatching(['environment-documentation-d']);

    effect(() => {
      const nodes = this.tree();
      const tree = this.treeBase();
      if (nodes.length === 0 || !tree || this.treeInitialized) return;

      queueMicrotask(() => {
        this.collapseAllNodes();
        this.treeInitialized = true;
      });
    });

    effect(() => {
      this.tree();
      queueMicrotask(() => this.syncExpansionState());
    });

    effect(() => {
      const selectedId = this.selectedId();
      const nodes = this.tree();
      const tree = this.treeBase();

      if (!selectedId) {
        this.revealedSelectedId = null;
        return;
      }

      if (nodes.length === 0 || !tree || selectedId === this.revealedSelectedId) {
        return;
      }

      queueMicrotask(() => {
        if (this.selectedId() !== selectedId || this.tree() !== nodes || this.treeBase() !== tree) {
          return;
        }

        if (this.expandAncestorsOfSelectedNode(selectedId, nodes, tree)) {
          this.revealedSelectedId = selectedId;
        }
      });
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

  onDrop(event: CdkDragDrop<SectionNode[]>) {
    const intent = this.dropIntent();
    this.dropIntent.set(null);

    if (!event.isPointerOverContainer || !intent) return;

    const nodeToMove = event.item.data as SectionNode;
    const target = this.findNodeById(intent.targetId);
    if (!target) return;

    let newParentId: string | null;
    let newOrder: number;
    if (intent.position === 'inside') {
      newParentId = target.id;
      newOrder = target.children?.length ?? 0;
      this.treeBase()?.expand(target);
    } else {
      newParentId = target.data?.parentId ?? null;
      newOrder = (target.data?.order ?? 0) + (intent.position === 'after' ? 1 : 0);
    }

    if (this.isContainer(nodeToMove)) {
      this.expandAllNodes();
    }

    this.nodeMoved.emit({ node: nodeToMove, newParentId, newOrder });
  }

  onContextMenu(event: MouseEvent, node: FlatTreeNode): void {
    event.preventDefault();
    if (!this.canShowMoreActions(node)) {
      return;
    }
    const trigger = this.contextMenuTrigger();
    const anchor = this.contextMenuAnchor()?.nativeElement as HTMLElement | undefined;
    if (!trigger || !anchor) {
      return;
    }
    this.contextMenuNode.set(node);
    anchor.style.left = `${event.clientX}px`;
    anchor.style.top = `${event.clientY}px`;
    trigger.openMenu();
  }

  onDragStarted(event: CdkDragStart<SectionNode>) {
    const draggedNode = event.source.data;

    // If it's a folder, find all its descendants to hide them
    if (draggedNode.type === 'FOLDER' && draggedNode.children) {
      this.collapseNode(draggedNode);
    }
  }

  expandAllNodes(): void {
    this.treeBase()?.expandAll();
    this.syncExpansionState();
  }

  collapseAllNodes(): void {
    this.treeBase()?.collapseAll();
    this.syncExpansionState();
  }

  onNodeToggle(): void {
    this.syncExpansionState();
  }

  private collapseNode(node: SectionNode): void {
    this.treeBase()?.collapse(node);
    this.syncExpansionState();
  }

  private syncExpansionState(): void {
    const tree = this.treeBase();
    if (!tree) {
      this.hasExpandedNode.set(false);
      return;
    }
    this.hasExpandedNode.set(this.expandableNodes().some(node => tree.isExpanded(node)));
  }

  private collectExpandableNodes(nodes: SectionNode[]): SectionNode[] {
    const expandableNodes: SectionNode[] = [];
    const walk = (currentNodes: SectionNode[]) => {
      for (const node of currentNodes) {
        if (node.children && node.children.length > 0) {
          expandableNodes.push(node);
          walk(node.children);
        }
      }
    };
    walk(nodes);
    return expandableNodes;
  }

  onDragMoved(event: CdkDragMove<SectionNode>) {
    const point = event.event instanceof MouseEvent ? event.event : (event.event.touches[0] ?? event.event.changedTouches[0]);
    if (!point) {
      this.dropIntent.set(null);
      return;
    }

    const row = (document.elementFromPoint(point.clientX, point.clientY) as HTMLElement | null)?.closest(
      'mat-tree-node',
    ) as HTMLElement | null;
    const target = row ? this.findNodeById(row.getAttribute('data-node-id') ?? '') : undefined;

    if (!row || !target || !this.canReorderRelativeTo(target, event.source.data)) {
      this.dropIntent.set(null);
      return;
    }

    const rect = row.getBoundingClientRect();
    if (!rect.height) {
      this.dropIntent.set(null);
      return;
    }

    const ratio = (point.clientY - rect.top) / rect.height;
    this.dropIntent.set({ targetId: target.id, position: this.resolveDropPosition(target, ratio) });
  }

  private resolveDropPosition(target: SectionNode, ratio: number): DropPosition {
    if (this.isContainer(target)) {
      if (ratio < 0.25) return 'before';
      if (ratio > 0.75) return 'after';
      return 'inside';
    }
    return ratio < 0.5 ? 'before' : 'after';
  }

  private isContainer(node: SectionNode): boolean {
    return node.type === 'FOLDER' || node.type === 'API';
  }

  // Reject the dragged node itself and its own descendants as targets: re-parenting a node under one
  // of its descendants would create a cycle in the tree.
  private canReorderRelativeTo(target: SectionNode, nodeToMove: SectionNode): boolean {
    return target.id !== nodeToMove.id && !this.isWithinSubtree(target.id, nodeToMove);
  }

  private isWithinSubtree(nodeId: string, root: SectionNode): boolean {
    return (root.children ?? []).some(child => child.id === nodeId || this.isWithinSubtree(nodeId, child));
  }

  // Returns true once the selected node has been located (and its ancestors expanded), false while it
  // is not yet in the tree — so the caller knows whether to mark the selection as revealed or retry.
  private expandAncestorsOfSelectedNode(selectedId: string, nodes: SectionNode[], tree: MatTree<SectionNode, string>): boolean {
    const selectedPath = this.findPathToNode(selectedId, nodes);

    if (!selectedPath) {
      return false;
    }

    if (selectedPath.length > 1) {
      selectedPath.slice(0, -1).forEach(node => tree.expand(node));
      this.syncExpansionState();
    }
    return true;
  }

  private findPathToNode(selectedId: string, nodes: SectionNode[]): SectionNode[] | null {
    const visitedNodes = new Set<SectionNode>();
    const stack: Array<{ node: SectionNode; path: SectionNode[] }> = [];

    for (let index = nodes.length - 1; index >= 0; index -= 1) {
      const node = nodes[index];
      if (node) {
        stack.push({ node, path: [node] });
      }
    }

    while (stack.length > 0) {
      const current = stack.pop();
      if (!current || visitedNodes.has(current.node)) {
        continue;
      }

      const { node, path } = current;
      visitedNodes.add(node);

      if (node.id === selectedId) {
        return path;
      }

      const children = node.children ?? [];
      for (let index = children.length - 1; index >= 0; index -= 1) {
        const child = children[index];
        if (child) {
          stack.push({ node: child, path: [...path, child] });
        }
      }
    }

    return null;
  }

  private findNodeById(id: string, nodes: SectionNode[] = this.tree()): SectionNode | undefined {
    for (const node of nodes) {
      if (node.id === id) return node;
      const found = node.children ? this.findNodeById(id, node.children) : undefined;
      if (found) return found;
    }
    return undefined;
  }

  private mapFlatTreeNodeToSectionNode(flatTreeNode: FlatTreeNode): SectionNode {
    return flatTreeNode as SectionNode;
  }

  private getPublishActionState(node: SectionNode): PublishActionState {
    return this.publishStateByNodeId().get(node.id) ?? this.publishActionEnabledState;
  }

  private toDisabledPublishActionState(tooltip: string): PublishActionState {
    return {
      disabled: true,
      tooltip,
      ariaDisabled: true,
      tabIndex: -1,
    };
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
        children: type === 'FOLDER' || type === 'API' ? [] : undefined,
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

  private sortAndCleanTree(nodes: ProcessingNode[], level = 0): SectionNode[] {
    return nodes
      .sort((a, b) => a.__order - b.__order)
      .map(({ __order, __parentId, ...node }) => {
        const children = node.children ? this.sortAndCleanTree(node.children as ProcessingNode[], level + 1) : undefined;
        return { ...node, level, children };
      });
  }
}
