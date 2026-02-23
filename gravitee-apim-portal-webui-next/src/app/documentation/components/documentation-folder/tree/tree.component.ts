/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AfterViewInit, Component, input, output } from '@angular/core';

import { TreeNodeComponent } from './tree-node.component';
import { TreeNode } from '../../../services/tree.service';

@Component({
  selector: 'app-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements AfterViewInit {
  tree = input.required<TreeNode[]>();
  selectedId = input<string | null>(null);
  selectNode = output<string>();

  ngAfterViewInit() {
    this.scrollIntoView();
  }

  onNodeSelected(id: string) {
    this.selectNode.emit(id);
  }

  private scrollIntoView() {
    const selectedId = this.selectedId();
    if (selectedId) {
      document.querySelector('#node-' + selectedId)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}
