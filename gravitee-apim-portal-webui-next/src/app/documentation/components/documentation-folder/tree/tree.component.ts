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
import { AfterViewInit, Component, effect, input, model, output, untracked } from '@angular/core';

import { TreeNodeComponent } from './tree-node.component';
import { TreeNode } from '../../../services/documentation-tree.service';

@Component({
  selector: 'app-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements AfterViewInit {
  tree = input.required<TreeNode[]>();
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

  private selectFirstPage() {
    const tree = this.tree();
    const firstPageId = untracked(this.selectedId) ?? this.findFirstPageId(tree);
    if (firstPageId) {
      this.onNodeSelected(firstPageId);
    }
  }

  private findFirstPageId(nodes: TreeNode[]): string | null {
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
