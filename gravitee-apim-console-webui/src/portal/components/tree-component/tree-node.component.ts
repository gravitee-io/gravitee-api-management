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
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';

import { SectionNode } from './tree.component';

@Component({
  selector: 'app-tree-node',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatMenuModule, DragDropModule, TreeNodeComponent],
  templateUrl: './tree-node.component.html',
  styleUrls: ['./tree-node.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeNodeComponent {
  node = input.required<SectionNode>();
  level = input(0);
  selectedId = input<string | null>(null);
  expandedIds = input<Set<string>>(new Set());
  nodeActionsMenu = input.required<import('@angular/material/menu').MatMenu>();

  nodeSelected = output<SectionNode>();
  nodeToggled = output<SectionNode>();
  childrenReordered = output<{ parentId: string; previousIndex: number; currentIndex: number }>();

  isSelected = computed(() => this.selectedId() === this.node().id);
  isExpanded = computed(() => this.expandedIds().has(this.node().id));

  selectNode(): void {
    this.nodeSelected.emit(this.node());
  }

  toggleNode(): void {
    this.nodeToggled.emit(this.node());
  }

  onDropChildren(event: CdkDragDrop<SectionNode[]>): void {
    this.childrenReordered.emit({
      parentId: this.node().id,
      previousIndex: event.previousIndex,
      currentIndex: event.currentIndex,
    });
  }
}
