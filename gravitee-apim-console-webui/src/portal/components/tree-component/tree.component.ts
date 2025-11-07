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
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';

import { TreeNodeComponent } from './tree-node.component';

import { ItemModalComponent } from '../item-modal/item-modal.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { UiPortalMenuLinksService } from '../../../services-ngx/ui-portal-menu-links.service';
import { PortalMenuLink } from '../../../entities/management-api-v2';

export type SectionNodeType = 'page' | 'folder' | 'link';

export interface SectionNode {
  id: string;
  label: string;
  type: SectionNodeType;
  data?: PortalMenuLink;
  unpublished?: boolean;
  link?: boolean;
  children?: SectionNode[];
}

export interface NodeMenuAction {
  icon: string;
  label: string;
  action: (node: SectionNode) => void;
  isDivider?: boolean;
  isDestructive?: boolean;
}

@Component({
  selector: 'portal-tree-component',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatMenuModule, MatDialogModule, TreeNodeComponent, HttpClientModule, DragDropModule],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements OnInit {
  tree = signal<SectionNode[]>([]);
  expandedIds = signal<Set<string>>(new Set());
  selectedId = signal<string | null>(null);

  menuActions: NodeMenuAction[] = [
    { icon: 'gio:page', label: 'Add Page', action: (node) => this.onAddPage(node) },
    { icon: 'gio:link', label: 'Add Link', action: (node) => this.onAddLink(node) },
    { icon: 'gio:folder', label: 'Add Folder', action: (node) => this.onAddFolder(node) },
    { icon: 'gio:folder-api', label: 'Add API Folder', action: (node) => this.onAddApiFolder(node) },
    { icon: 'gio:cloud-settings', label: 'Add API', action: (node) => this.onAddApi(node) },
    {
      isDivider: true,
      label: '',
      icon: '',
      action: () => {},
    },
    { icon: '', label: 'Edit Settings', action: (node) => this.onEditSettings(node) },
    { icon: '', label: 'Duplicate', action: (node) => this.onDuplicate(node) },
    { icon: '', label: 'Delete', action: (node) => this.onDelete(node), isDestructive: true },
  ];

  constructor(
    private dialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly uiPortalMenuLinksService: UiPortalMenuLinksService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.loadTree();
  }

  toggle(node: SectionNode): void {
    if (node.type !== 'folder') return;
    this.expandedIds.update((currentSet) => {
      const newSet = new Set(currentSet);
      if (newSet.has(node.id)) {
        newSet.delete(node.id);
      } else {
        newSet.add(node.id);
      }
      return newSet;
    });
  }

  select(node: SectionNode): void {
    this.selectedId.set(node.id);
    this.navigateToNode(node);
  }

  delete(node: SectionNode): void {
    this.confirmAndDelete(node);
  }

  onRootDrop(event: CdkDragDrop<SectionNode[]>) {
    if (event.previousIndex === event.currentIndex) return;
    this.tree.update((current) => {
      const updated = [...current];
      moveItemInArray(updated, event.previousIndex, event.currentIndex);
      return updated;
    });
  }

  onChildrenReordered(evt: { parentId: string; previousIndex: number; currentIndex: number }) {
    if (evt.previousIndex === evt.currentIndex) return;
    const { parentId, previousIndex, currentIndex } = evt;
    const reorderIn = (nodes: SectionNode[]): SectionNode[] =>
      nodes.map((n) => {
        if (n.id === parentId) {
          const children = n.children ? [...n.children] : [];
          if (!children.length) return n;
          moveItemInArray(children, previousIndex, currentIndex);
          return { ...n, children };
        }
        if (n.children && n.children.length) {
          return { ...n, children: reorderIn(n.children) };
        }
        return n;
      });

    this.tree.update((current) => reorderIn(current));
  }

  private loadTree(): void {
    // Use mock JSON to populate the tree
    this.http.get<{ data: PortalMenuLink[] }>('assets/mocks/portal-menu-links.json').subscribe({
      next: ({ data }) => {
        const nodes = this.mapLinksToNodes(data);
        this.tree.set(nodes);
        const navApis = nodes.find((n) => n.id === 'nav-apis');
        if (navApis) {
          this.expandedIds.update((current) => new Set([...current, navApis.id]));
        }
      },
      error: (err) => {
        this.snackBarService.error('Failed to load mock portal menu links :  ' + err);
      },
    });
  }

  private mapLinksToNodes(links: PortalMenuLink[]): SectionNode[] {
    type NodeWithMeta = SectionNode & { __order: number; __parentId: string | null };

    const nodesById = new Map<string, NodeWithMeta>();
    for (const link of links) {
      const isFolder = (link as any).target == null;
      const node: NodeWithMeta = {
        id: link.id,
        label: link.name,
        type: isFolder ? 'folder' : 'page',
        data: link,
        children: isFolder ? [] : undefined,
        link: (link as any).type === 'EXTERNAL',
        __order: (link as any).order ?? 0,
        __parentId: (link as any).parentId ?? null,
      };
      nodesById.set(link.id, node);
    }

    const roots: NodeWithMeta[] = [];
    for (const node of nodesById.values()) {
      const parentId = node.__parentId;
      if (!parentId) {
        roots.push(node);
      } else {
        const parent = nodesById.get(parentId);
        if (parent) {
          if (!parent.children) parent.children = [];
          (parent.children as NodeWithMeta[]).push(node);
        } else {
          roots.push(node);
        }
      }
    }

    for (const n of nodesById.values()) {
      if (Array.isArray(n.children) && n.children.length > 0) {
        n.type = 'folder';
      }
    }

    const sortRecursively = (arr: NodeWithMeta[]) => {
      arr.sort((a, b) => a.__order - b.__order);
      for (const n of arr) {
        if (Array.isArray(n.children) && n.children.length) {
          sortRecursively(n.children as NodeWithMeta[]);
        }
      }
    };

    sortRecursively(roots);

    const stripMeta = (arr: NodeWithMeta[]): SectionNode[] =>
      arr.map((n) => ({
        id: n.id,
        label: n.label,
        type: n.type,
        data: n.data,
        unpublished: n.unpublished,
        link: n.link,
        children: n.children ? stripMeta(n.children as NodeWithMeta[]) : undefined,
      }));

    return stripMeta(roots);
  }

  private navigateToNode(_node: SectionNode): void {
    // TODO: Navigate to node when backend is available
  }

  private confirmAndDelete(node: SectionNode): void {
    // Deletion is not implemented for mock data
    this.openModal('folder', 'delete', node);
  }

  onAddFolder(node: any) {
    this.openModal('folder', 'add', node);
  }

  onAddPage(node: any) {
    this.openModal('page', 'add', node);
  }

  onAddLink(node: any) {
    this.openModal('link', 'add', node);
  }

  onAddApiFolder(node: any) {
    this.onAddFolder(node);
  }

  onAddApi(node: any) {
    this.onAddPage(node);
  }

  onEditSettings(node: any) {
    const type = (node?.type ?? 'page') as 'folder' | 'page' | 'link';
    this.openModal(type, 'edit', node, node?.data);
  }

  onDuplicate(_node: any) {
    // TODO: Duplicate is not implemented for mock data
  }

  // Edit
  onEditFolder(node: any) {
    this.openModal('folder', 'edit', node, node.data);
  }

  onEditPage(node: any) {
    this.openModal('page', 'edit', node, node.data);
  }

  onEditLink(node: any) {
    this.openModal('link', 'edit', node, node.data);
  }

  openModal(type: 'folder' | 'page' | 'link', mode: 'add' | 'edit' | 'delete', node: any, initialData?: any) {
    const dialogRef = this.dialog.open(ItemModalComponent, { data: { node } });
    const instance = dialogRef.componentInstance;
    instance.type = type;
    instance.mode = mode;
    instance.initialData = initialData;

    if (mode === 'delete' && type === 'folder' && node.children?.length) {
      instance.extraInfo = `and its ${node.children.length} pages will be permanently deleted`;
    }

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // console.log(`${mode} confirmed for ${type}:`, result);
        // Perform add/edit/delete logic here
      }
    });
  }

  onDelete(node: SectionNode): void {
    this.confirmAndDelete(node);
  }
}
