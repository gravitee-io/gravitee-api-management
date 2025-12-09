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
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDivider } from '@angular/material/divider';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData } from '@gravitee/ui-particles-angular';

import { NodeMenuActionEvent, SectionNode } from './tree.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { PortalNavigationItemType } from '../../../entities/management-api-v2';

@Component({
  selector: 'app-tree-node',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatMenuModule, GioPermissionModule, MatDivider],
  templateUrl: './tree-node.component.html',
  styleUrls: ['./tree-node.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeNodeComponent {
  node = input.required<SectionNode>();
  level = input(0);
  selectedId = input<string | null>(null);

  nodeSelected = output<SectionNode>();
  nodeMenuAction = output<NodeMenuActionEvent>();

  triggerEdit() {
    const current = this.node();
    if (!current) return;

    this.nodeMenuAction.emit({
      action: 'edit',
      itemType: current.type,
      node: current,
    });
  }

  triggerCreate(itemType: PortalNavigationItemType) {
    const current = this.node();
    if (!current) return;

    this.nodeMenuAction.emit({
      action: 'create',
      itemType,
      node: current,
    });
  }
  delete = output<SectionNode>();

  isSelected = computed(() => this.selectedId() === this.node().id);
  isExpanded = signal<boolean>(true);

  constructor(private readonly matDialog: MatDialog) {}

  selectNode(): void {
    this.nodeSelected.emit(this.node());
  }

  toggleNode(): void {
    this.isExpanded.update((v) => !v);
  }

  isUnpublished(): boolean {
    return this.node().data?.published === false;
  }

  confirmDelete(node: SectionNode): void {
    const title = `Delete "${node.label}" ${node.type.toLowerCase()}`;

    let content = '';
    if (node.type === 'FOLDER') {
      content = 'This folder and its content will be permanently deleted. This change will be visible in the Developer Portal.';
    } else if (node.type === 'PAGE') {
      content = 'This page will no longer appear on your site.';
    } else if (node.type === 'LINK') {
      content = 'This link will no longer appear on your site.';
    }

    const data: GioConfirmAndValidateDialogData = {
      title,
      content,
      validationMessage: `Type <code>${node.label}</code> to confirm.`,
      validationValue: node.label,
      confirmButton: 'Delete',
    };

    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data,
        role: 'alertdialog',
        id: `deleteTreeNodeDialog-${node.id}`,
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed === true) {
          this.delete.emit(node);
        }
      });
  }
}
