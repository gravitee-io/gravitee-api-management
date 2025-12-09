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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { SectionNode } from './tree.component';
import { PortalNavigationLink } from '../../../../../entities/portal-navigation/portal-navigation-item';

@Component({
  selector: 'app-tree-node',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './tree-node.component.html',
  styleUrls: ['./tree-node.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeNodeComponent {
  node = input.required<SectionNode>();
  level = input(0);
  selectedId = input<string | null>(null);

  nodeSelected = output<string>();

  isSelected = computed(() => this.selectedId() === this.node().id);
  isExpanded = signal<boolean>(true);

  link = viewChild<ElementRef>('link');

  get linkUrl() {
    return (this.node().data as PortalNavigationLink)?.url;
  }

  selectNode(): void {
    this.nodeSelected.emit(this.node().id);
  }

  toggleNode(): void {
    this.isExpanded.update(v => !v);
  }

  redirectToLink(): void {
    this.link()?.nativeElement.click();
  }

  onClick(): void {
    switch (this.node().type) {
      case 'FOLDER':
        this.toggleNode();
        break;
      case 'PAGE':
        this.selectNode();
        break;
      case 'LINK': {
        this.redirectToLink();
        break;
      }
    }
  }
}
